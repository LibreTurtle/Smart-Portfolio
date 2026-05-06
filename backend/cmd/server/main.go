package main

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"

	"github.com/ZRishu/smart-portfolio/internal/config"
	"github.com/ZRishu/smart-portfolio/internal/database"
	adminhandler "github.com/ZRishu/smart-portfolio/internal/modules/admin/handler"
	aihandler "github.com/ZRishu/smart-portfolio/internal/modules/ai/handler"
	airepository "github.com/ZRishu/smart-portfolio/internal/modules/ai/repository"
	aiservice "github.com/ZRishu/smart-portfolio/internal/modules/ai/service"
	contenthandler "github.com/ZRishu/smart-portfolio/internal/modules/content/handler"
	contentrepository "github.com/ZRishu/smart-portfolio/internal/modules/content/repository"
	contentservice "github.com/ZRishu/smart-portfolio/internal/modules/content/service"
	contentworker "github.com/ZRishu/smart-portfolio/internal/modules/content/worker"
	notifservice "github.com/ZRishu/smart-portfolio/internal/modules/notification/service"
	paymenthandler "github.com/ZRishu/smart-portfolio/internal/modules/payment/handler"
	paymentrepository "github.com/ZRishu/smart-portfolio/internal/modules/payment/repository"
	paymentservice "github.com/ZRishu/smart-portfolio/internal/modules/payment/service"
	"github.com/ZRishu/smart-portfolio/internal/modules/payment/worker"
	"github.com/ZRishu/smart-portfolio/internal/platform/cache"
	"github.com/ZRishu/smart-portfolio/internal/platform/eventbus"
	"github.com/ZRishu/smart-portfolio/internal/server"
)

var version = "dev"

func main() {
	// ─────────────────────────────────────────────────────────────────────
	// 1. Logger
	// ─────────────────────────────────────────────────────────────────────
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnix
	zerolog.SetGlobalLevel(zerolog.InfoLevel)

	if os.Getenv("LOG_LEVEL") == "debug" {
		zerolog.SetGlobalLevel(zerolog.DebugLevel)
	}

	// Use pretty console output in development, JSON in production.
	if os.Getenv("ENV") != "production" {
		log.Logger = log.Output(zerolog.ConsoleWriter{
			Out:        os.Stdout,
			TimeFormat: time.RFC3339,
		})
	}

	log.Info().Str("version", version).Msg("smart-portfolio: starting application")

	// ─────────────────────────────────────────────────────────────────────
	// 2. Configuration
	// ─────────────────────────────────────────────────────────────────────
	cfg, err := config.Load()
	if err != nil {
		log.Fatal().Err(err).Msg("failed to load configuration")
	}

	log.Info().Msg("configuration loaded successfully")

	// ─────────────────────────────────────────────────────────────────────
	// 3. Root context — cancelled on SIGINT / SIGTERM
	// ─────────────────────────────────────────────────────────────────────
	rootCtx, rootCancel := context.WithCancel(context.Background())
	defer rootCancel()

	// ─────────────────────────────────────────────────────────────────────
	// 4. Database
	// ─────────────────────────────────────────────────────────────────────
	pg, err := database.New(rootCtx, cfg.Database)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to PostgreSQL")
	}
	defer pg.Close()

	// Run idempotent migrations.
	if err := pg.RunMigrations(rootCtx, "migrations"); err != nil {
		log.Fatal().Err(err).Msg("database migrations failed")
	}

	// ─────────────────────────────────────────────────────────────────────
	// 5. Platform services (cache, event bus)
	// ─────────────────────────────────────────────────────────────────────
	appCache := cache.New(cfg.Cache)
	bus := eventbus.New(rootCtx)

	// ─────────────────────────────────────────────────────────────────────
	// 6. Notification service
	// ─────────────────────────────────────────────────────────────────────
	discordService := notifservice.NewDiscordNotificationService(cfg.Discord)

	// ─────────────────────────────────────────────────────────────────────
	// 7. Content module (projects + contact messages + profile)
	// ─────────────────────────────────────────────────────────────────────
	projectRepo := contentrepository.NewProjectRepository(pg.Pool)
	githubProfileRepo := contentrepository.NewGitHubProfileRepository(pg.Pool)
	githubRepoRepo := contentrepository.NewGitHubRepositoryRepository(pg.Pool)
	contactRepo := contentrepository.NewContactRepository(pg.Pool)
	profileRepo := contentrepository.NewProfileRepository(pg)

	projectSvc := contentservice.NewProjectService(projectRepo, githubRepoRepo, githubProfileRepo, appCache)
	contactSvc := contentservice.NewContactMessageService(contactRepo, discordService)
	profileSvc := contentservice.NewProfileService(profileRepo)

	contactHandler := contenthandler.NewContactHandler(contactSvc, cfg.Admin.APIKey)
	profileHandler := contenthandler.NewProfileHandler(profileSvc)

	log.Info().Msg("content module: initialized (projects + contact messages + profile)")

	// ─────────────────────────────────────────────────────────────────────
	// 8. AI module (embeddings, RAG, ingestion)
	// ─────────────────────────────────────────────────────────────────────
	embeddingSvc, err := aiservice.NewEmbeddingService(cfg.Embedding)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to initialize embedding service")
	}

	semanticCacheRepo := airepository.NewSemanticCacheRepository(pg.Pool)
	ingestionManifestRepo := airepository.NewIngestionManifestRepository(pg.Pool)
	vectorStoreRepo, err := airepository.NewVectorStoreRepository(cfg.Vector, cfg.Embedding.Dimensions)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to initialize vector store")
	}
	if err := vectorStoreRepo.Ensure(rootCtx); err != nil {
		log.Fatal().Err(err).Msg("failed to ensure vector store")
	}
	githubEmbeddingRepo, err := airepository.NewGitHubEmbeddingRepository(cfg.Vector, cfg.Embedding.Dimensions)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to initialize GitHub vector store")
	}

	ragSvc := aiservice.NewRAGService(embeddingSvc, semanticCacheRepo, vectorStoreRepo, cfg.AI)
	ingestionSvc := aiservice.NewIngestionService(
		embeddingSvc,
		vectorStoreRepo,
		ingestionManifestRepo,
		cfg.Embedding.Model,
		cfg.Vector.Provider,
	)
	seedProfileKnowledge(rootCtx, profileRepo, ingestionSvc)
	githubSyncSvc := contentservice.NewGitHubSyncService(
		cfg.GitHub,
		githubProfileRepo,
		githubRepoRepo,
		githubEmbeddingRepo,
		embeddingSvc,
		contentservice.InvalidateProjectCaches(appCache),
	)

	aiHandler := aihandler.NewAIHandler(ragSvc, ingestionSvc)
	projectHandler := contenthandler.NewProjectHandler(
		projectSvc,
		githubSyncSvc,
		githubSyncSvc.Username(),
		cfg.GitHub.ProjectsLimit,
	)

	log.Info().Msg("ai module: initialized (embeddings + RAG + ingestion)")

	// ─────────────────────────────────────────────────────────────────────
	// 9. Payment module (Razorpay webhooks + outbox)
	// ─────────────────────────────────────────────────────────────────────
	paymentRepo := paymentrepository.NewPaymentRepository(pg.Pool)
	paymentSvc := paymentservice.NewPaymentService(paymentRepo, cfg.Razorpay)
	webhookHandler := paymenthandler.NewWebhookHandler(paymentSvc)
	paymentPublicHandler := paymenthandler.NewPaymentHandler(paymentSvc)

	log.Info().Msg("payment module: initialized (Razorpay webhooks + public routes)")

	// ─────────────────────────────────────────────────────────────────────
	// 10. Admin module (dashboard stats, sponsors listing, deep health)
	// ─────────────────────────────────────────────────────────────────────
	adminH := adminhandler.NewAdminHandler(
		pg,
		projectRepo,
		githubRepoRepo,
		contactRepo,
		paymentRepo,
		vectorStoreRepo,
		semanticCacheRepo,
		githubSyncSvc,
	)

	log.Info().Msg("admin module: initialized (stats + sponsors + deep health)")

	// ─────────────────────────────────────────────────────────────────────
	// 11. Event bus subscribers
	// ─────────────────────────────────────────────────────────────────────
	bus.Subscribe("SPONSOR_CREATED", func(ctx context.Context, event eventbus.Event) error {
		log.Info().Msg("event_handler: received SPONSOR_CREATED event — sending Discord notification")

		var payload struct {
			SponsorName string  `json:"sponsorName"`
			Amount      float64 `json:"amount"`
			Currency    string  `json:"currency"`
			Email       string  `json:"email"`
			Status      string  `json:"status"`
			PaymentID   string  `json:"paymentId"`
		}

		if err := json.Unmarshal([]byte(event.Payload), &payload); err != nil {
			log.Error().Err(err).Str("payload", event.Payload).Msg("event_handler: failed to parse SPONSOR_CREATED payload")
			return err
		}

		discordService.SendSponsorNotification(
			ctx,
			payload.SponsorName,
			payload.Email,
			payload.Currency,
			payload.Amount,
			payload.PaymentID,
			payload.Status,
		)
		return nil
	})

	log.Info().
		Int("total_handlers", bus.HandlerCount()).
		Msg("event bus: all subscribers registered")

	// ─────────────────────────────────────────────────────────────────────
	// 12. Outbox poller (background goroutine)
	// ─────────────────────────────────────────────────────────────────────
	var outboxPoller *worker.OutboxPoller
	if paymentSvc.Ready() {
		outboxPoller = worker.NewOutboxPoller(paymentRepo, bus, cfg.Outbox.PollInterval, 50)
		outboxPoller.Start(rootCtx)
		log.Info().
			Dur("interval", cfg.Outbox.PollInterval).
			Msg("outbox poller: background worker started")
	} else {
		log.Warn().Msg("outbox poller: skipped — payment service is not fully configured")
	}

	githubSyncWorker := contentworker.NewGitHubSyncWorker(githubSyncSvc, cfg.GitHub.SyncInterval)
	githubSyncWorker.Start(rootCtx)

	// ─────────────────────────────────────────────────────────────────────
	// 13. HTTP server
	// ─────────────────────────────────────────────────────────────────────
	srv := server.New(cfg)
	srv.RegisterRoutes(server.ModuleRoutes{
		Projects:        projectHandler.Routes(),
		Profile:         profileHandler.Routes(),
		Contact:         contactHandler.Routes(),
		Chat:            aiHandler.ChatRoutes(),
		Ingest:          aiHandler.IngestRoutes(),
		RazorpayWebhook: webhookHandler.Routes(),
		Payments:        paymentPublicHandler.PaymentRoutes(),
		Sponsors:        paymentPublicHandler.SponsorRoutes(),
		Admin:           adminH.Routes(),
	})

	serverErrors := make(chan error, 1)
	go func() {
		serverErrors <- srv.Start()
	}()

	log.Info().
		Str("port", cfg.Server.Port).
		Msg("smart-portfolio: server is ready and accepting connections")

	// ─────────────────────────────────────────────────────────────────────
	// 14. Graceful shutdown
	// ─────────────────────────────────────────────────────────────────────
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	select {
	case err := <-serverErrors:
		if err != nil {
			log.Error().Err(err).Msg("server error — initiating shutdown")
		}
	case sig := <-quit:
		log.Info().Str("signal", sig.String()).Msg("shutdown signal received")
	}

	rootCancel()

	log.Info().Msg("shutting down gracefully — draining in-flight work...")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer shutdownCancel()

	shutdownDone := make(chan struct{})
	go func() {
		defer close(shutdownDone)

		if outboxPoller != nil {
			outboxPoller.Stop()
			log.Info().Msg("shutdown: outbox poller stopped")
		}

		bus.Shutdown()
		log.Info().Msg("shutdown: event bus drained")

		discordService.Shutdown()
		log.Info().Msg("shutdown: discord notifications drained")

		type cacheShutdowner interface {
			ShutdownCacheWorkers()
		}
		if cs, ok := ragSvc.(cacheShutdowner); ok {
			cs.ShutdownCacheWorkers()
			log.Info().Msg("shutdown: RAG cache workers drained")
		}
	}()

	select {
	case <-shutdownDone:
		log.Info().Msg("shutdown: all background services stopped cleanly")
	case <-shutdownCtx.Done():
		log.Warn().Msg("shutdown: timed out waiting for background services — forcing exit")
	}

	log.Info().Msg("smart-portfolio: shutdown complete — goodbye!")
}

func seedProfileKnowledge(ctx context.Context, profileRepo *contentrepository.ProfileRepository, ingestionSvc aiservice.IngestionService) {
	seedCtx, cancel := context.WithTimeout(ctx, 90*time.Second)
	defer cancel()

	var sections []string

	if profile, err := profileRepo.GetProfile(seedCtx); err == nil && profile != nil {
		sections = append(sections, fmt.Sprintf(
			"Profile: %s %s. Role: %s. Specialization: %s. Location: %s. Summary: %s.",
			profile.FirstName,
			profile.LastName,
			profile.PrimaryRole,
			profile.Specialization,
			profile.Location,
			profile.Summary,
		))
	} else if err != nil {
		log.Warn().Err(err).Msg("profile seed: profile data unavailable")
	}

	if education, err := profileRepo.GetEducation(seedCtx); err == nil {
		for _, item := range education {
			sections = append(sections, fmt.Sprintf(
				"Education: %s, %s, %s, %s to %s, GPA %s. Coursework: %s.",
				item.Institution,
				item.Degree,
				item.Location,
				item.StartDate,
				item.EndDate,
				item.GPA,
				item.Coursework,
			))
		}
	} else {
		log.Warn().Err(err).Msg("profile seed: education data unavailable")
	}

	if experience, err := profileRepo.GetExperience(seedCtx); err == nil {
		for _, item := range experience {
			sections = append(sections, fmt.Sprintf(
				"Experience: %s as %s, %s, %s to %s. %s Tech stack: %s.",
				item.Company,
				item.Role,
				item.Location,
				item.StartDate,
				item.EndDate,
				item.Summary,
				item.TechStack,
			))
		}
	} else {
		log.Warn().Err(err).Msg("profile seed: experience data unavailable")
	}

	if certifications, err := profileRepo.GetCertifications(seedCtx); err == nil {
		for _, item := range certifications {
			sections = append(sections, fmt.Sprintf(
				"Certification: %s from %s, issued %s. URL: %s.",
				item.Name,
				item.Issuer,
				item.IssueDate,
				item.URL,
			))
		}
	} else {
		log.Warn().Err(err).Msg("profile seed: certification data unavailable")
	}

	if achievements, err := profileRepo.GetAchievements(seedCtx); err == nil {
		for _, item := range achievements {
			sections = append(sections, fmt.Sprintf(
				"Achievement: %s, metric %s, date %s. %s.",
				item.Title,
				item.Metric,
				item.Date,
				item.Description,
			))
		}
	} else {
		log.Warn().Err(err).Msg("profile seed: achievement data unavailable")
	}

	if skills, err := profileRepo.GetSkills(seedCtx); err == nil {
		grouped := make(map[string][]string)
		for _, item := range skills {
			grouped[item.Category] = append(grouped[item.Category], item.Name)
		}
		for category, names := range grouped {
			sections = append(sections, fmt.Sprintf("Skills: %s: %s.", category, strings.Join(names, ", ")))
		}
	} else {
		log.Warn().Err(err).Msg("profile seed: skills data unavailable")
	}

	text := strings.TrimSpace(strings.Join(sections, "\n\n"))
	if text == "" {
		log.Warn().Msg("profile seed: no structured profile text generated")
		return
	}

	resp, err := ingestionSvc.IngestText(seedCtx, text, "structured-profile-seed")
	if err != nil {
		log.Warn().Err(err).Msg("profile seed: vector ingestion failed")
		return
	}
	log.Info().Int("new_chunks", resp.Chunks).Msg("profile seed: vector store is initialized")
}
