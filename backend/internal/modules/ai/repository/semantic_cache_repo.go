package repository

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rs/zerolog/log"
)

type SemanticCacheRepository struct {
	pool *pgxpool.Pool
}

func NewSemanticCacheRepository(pool *pgxpool.Pool) *SemanticCacheRepository {
	return &SemanticCacheRepository{pool: pool}
}

func (r *SemanticCacheRepository) FindCachedResponse(ctx context.Context, promptText string) (string, bool, error) {
	const query = `
		SELECT cached_response
		FROM ai_semantic_cache
		WHERE prompt_hash = $1
		LIMIT 1
	`

	var cachedResponse string
	err := r.pool.QueryRow(ctx, query, promptHash(promptText)).Scan(&cachedResponse)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return "", false, nil
		}
		return "", false, fmt.Errorf("semantic_cache_repo.FindCachedResponse: query failed: %w", err)
	}

	log.Debug().Msg("semantic_cache_repo: exact cache HIT")
	return cachedResponse, true, nil
}

func (r *SemanticCacheRepository) SaveToCache(ctx context.Context, promptText string, response string) error {
	const query = `
		INSERT INTO ai_semantic_cache (prompt_text, prompt_hash, cached_response)
		VALUES ($1, $2, $3)
		ON CONFLICT (prompt_hash) DO UPDATE
		SET prompt_text = EXCLUDED.prompt_text,
		    cached_response = EXCLUDED.cached_response,
		    created_at = now()
	`

	_, err := r.pool.Exec(ctx, query, strings.TrimSpace(promptText), promptHash(promptText), response)
	if err != nil {
		return fmt.Errorf("semantic_cache_repo.SaveToCache: upsert failed: %w", err)
	}

	log.Info().
		Str("prompt", truncate(promptText, 80)).
		Msg("semantic_cache_repo: saved response to exact cache")

	return nil
}

func (r *SemanticCacheRepository) PurgeOlderThan(ctx context.Context, interval string) (int64, error) {
	const query = `
		DELETE FROM ai_semantic_cache
		WHERE created_at < now() - $1::interval
	`

	tag, err := r.pool.Exec(ctx, query, interval)
	if err != nil {
		return 0, fmt.Errorf("semantic_cache_repo.PurgeOlderThan: delete failed: %w", err)
	}

	return tag.RowsAffected(), nil
}

func (r *SemanticCacheRepository) Count(ctx context.Context) (int64, error) {
	const query = `SELECT COUNT(*) FROM ai_semantic_cache`

	var count int64
	err := r.pool.QueryRow(ctx, query).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("semantic_cache_repo.Count: query failed: %w", err)
	}

	return count, nil
}

func promptHash(prompt string) string {
	normalized := strings.ToLower(strings.Join(strings.Fields(strings.TrimSpace(prompt)), " "))
	sum := sha256.Sum256([]byte(normalized))
	return hex.EncodeToString(sum[:])
}

func truncate(s string, maxLen int) string {
	if len(s) <= maxLen {
		return s
	}
	return s[:maxLen] + "..."
}
