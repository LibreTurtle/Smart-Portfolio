package repository

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/ZRishu/smart-portfolio/internal/config"
	"github.com/rs/zerolog/log"
)

const pineconeAPIVersion = "2026-04"

type EmbeddingDocument struct {
	ID        string            `json:"id"`
	Content   string            `json:"content"`
	Embedding []float32         `json:"-"`
	Metadata  map[string]string `json:"metadata,omitempty"`
}

type VectorStoreRepository struct {
	cfg        config.VectorStoreConfig
	dimensions int
	namespace  string
	host       string
	client     *http.Client
}

func NewVectorStoreRepository(cfg config.VectorStoreConfig, dimensions int) (*VectorStoreRepository, error) {
	if cfg.Provider != "" && cfg.Provider != "pinecone" {
		return nil, fmt.Errorf("vector_store_repo: unsupported provider %q; this build expects pinecone", cfg.Provider)
	}
	if cfg.PineconeAPIKey == "" {
		return nil, fmt.Errorf("vector_store_repo: PINECONE_API_KEY is required")
	}
	if cfg.PineconeIndex == "" {
		cfg.PineconeIndex = "smart-portfolio"
	}
	if cfg.PineconeCloud == "" {
		cfg.PineconeCloud = "aws"
	}
	if cfg.PineconeRegion == "" {
		cfg.PineconeRegion = "us-east-1"
	}
	if cfg.PineconeResumeNS == "" {
		cfg.PineconeResumeNS = "resume"
	}

	return &VectorStoreRepository{
		cfg:        cfg,
		dimensions: dimensions,
		namespace:  cfg.PineconeResumeNS,
		client:     &http.Client{Timeout: 45 * time.Second},
	}, nil
}

func (r *VectorStoreRepository) WithNamespace(namespace string) *VectorStoreRepository {
	clone := *r
	if namespace != "" {
		clone.namespace = namespace
	}
	return &clone
}

func (r *VectorStoreRepository) Ensure(ctx context.Context) error {
	if r.host != "" {
		return nil
	}

	index, found, err := r.describeIndex(ctx)
	if err != nil {
		return err
	}
	if !found {
		if err := r.createIndex(ctx); err != nil {
			return err
		}
	}

	deadline := time.Now().Add(2 * time.Minute)
	for {
		index, found, err = r.describeIndex(ctx)
		if err != nil {
			return err
		}
		if found && index.Host != "" && index.Status.Ready {
			r.host = normalizePineconeHost(index.Host)
			log.Info().
				Str("index", r.cfg.PineconeIndex).
				Str("host", r.host).
				Msg("vector_store_repo: Pinecone index ready")
			return nil
		}
		if time.Now().After(deadline) {
			return fmt.Errorf("vector_store_repo.Ensure: timed out waiting for Pinecone index %q", r.cfg.PineconeIndex)
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(3 * time.Second):
		}
	}
}

func (r *VectorStoreRepository) SimilaritySearch(ctx context.Context, queryEmbedding []float32, topK int) ([]EmbeddingDocument, error) {
	if len(queryEmbedding) != r.dimensions {
		return nil, fmt.Errorf("vector_store_repo.SimilaritySearch: embedding dimensions mismatch: got %d, expected %d", len(queryEmbedding), r.dimensions)
	}
	if topK <= 0 {
		topK = 3
	}
	if err := r.Ensure(ctx); err != nil {
		return nil, err
	}

	var resp pineconeQueryResponse
	err := r.dataPost(ctx, "/query", pineconeQueryRequest{
		Namespace:       r.namespace,
		Vector:          queryEmbedding,
		TopK:            topK,
		IncludeMetadata: true,
		IncludeValues:   false,
	}, &resp)
	if err != nil {
		return nil, fmt.Errorf("vector_store_repo.SimilaritySearch: %w", err)
	}

	docs := make([]EmbeddingDocument, 0, len(resp.Matches))
	for _, match := range resp.Matches {
		content := metadataString(match.Metadata, "content")
		if content == "" {
			continue
		}
		docs = append(docs, EmbeddingDocument{
			ID:       match.ID,
			Content:  content,
			Metadata: metadataToStringMap(match.Metadata),
		})
	}

	return docs, nil
}

func (r *VectorStoreRepository) AddDocuments(ctx context.Context, docs []EmbeddingDocument) error {
	return r.UpsertDocuments(ctx, docs)
}

func (r *VectorStoreRepository) UpsertDocuments(ctx context.Context, docs []EmbeddingDocument) error {
	if len(docs) == 0 {
		return nil
	}
	if err := r.Ensure(ctx); err != nil {
		return err
	}

	const batchSize = 100
	for start := 0; start < len(docs); start += batchSize {
		end := start + batchSize
		if end > len(docs) {
			end = len(docs)
		}
		vectors := make([]pineconeVector, 0, end-start)
		for i, doc := range docs[start:end] {
			if doc.ID == "" {
				return fmt.Errorf("vector_store_repo.UpsertDocuments: document %d has empty id", start+i)
			}
			if len(doc.Embedding) != r.dimensions {
				return fmt.Errorf("vector_store_repo.UpsertDocuments: document %d has %d dimensions, expected %d", start+i, len(doc.Embedding), r.dimensions)
			}
			metadata := map[string]any{"content": doc.Content}
			for k, v := range doc.Metadata {
				metadata[k] = v
			}
			vectors = append(vectors, pineconeVector{
				ID:       doc.ID,
				Values:   doc.Embedding,
				Metadata: metadata,
			})
		}
		if err := r.dataPost(ctx, "/vectors/upsert", pineconeUpsertRequest{
			Namespace: r.namespace,
			Vectors:   vectors,
		}, nil); err != nil {
			return fmt.Errorf("vector_store_repo.UpsertDocuments: %w", err)
		}
	}

	log.Info().
		Str("namespace", r.namespace).
		Int("count", len(docs)).
		Msg("vector_store_repo: documents upserted")
	return nil
}

func (r *VectorStoreRepository) ExistingIDs(ctx context.Context, ids []string) (map[string]bool, error) {
	existing := make(map[string]bool, len(ids))
	if len(ids) == 0 {
		return existing, nil
	}
	if err := r.Ensure(ctx); err != nil {
		return nil, err
	}

	const batchSize = 100
	for start := 0; start < len(ids); start += batchSize {
		end := start + batchSize
		if end > len(ids) {
			end = len(ids)
		}
		values := url.Values{}
		values.Set("namespace", r.namespace)
		for _, id := range ids[start:end] {
			values.Add("ids", id)
		}
		var resp pineconeFetchResponse
		if err := r.dataGet(ctx, "/vectors/fetch?"+values.Encode(), &resp); err != nil {
			return nil, fmt.Errorf("vector_store_repo.ExistingIDs: %w", err)
		}
		for id := range resp.Vectors {
			existing[id] = true
		}
	}
	return existing, nil
}

func (r *VectorStoreRepository) DeleteAll(ctx context.Context) (int64, error) {
	count, _ := r.Count(ctx)
	if err := r.Ensure(ctx); err != nil {
		return 0, err
	}
	if err := r.dataPost(ctx, "/vectors/delete", pineconeDeleteRequest{
		Namespace: r.namespace,
		DeleteAll: true,
	}, nil); err != nil {
		return 0, fmt.Errorf("vector_store_repo.DeleteAll: %w", err)
	}
	log.Info().Str("namespace", r.namespace).Int64("deleted_estimate", count).Msg("vector_store_repo: namespace cleared")
	return count, nil
}

func (r *VectorStoreRepository) DeleteByMetadata(ctx context.Context, filter map[string]string) error {
	if len(filter) == 0 {
		return nil
	}
	if err := r.Ensure(ctx); err != nil {
		return err
	}
	pineconeFilter := make(map[string]any, len(filter))
	for k, v := range filter {
		pineconeFilter[k] = map[string]string{"$eq": v}
	}
	if err := r.dataPost(ctx, "/vectors/delete", pineconeDeleteRequest{
		Namespace: r.namespace,
		Filter:    pineconeFilter,
	}, nil); err != nil {
		return fmt.Errorf("vector_store_repo.DeleteByMetadata: %w", err)
	}
	return nil
}

func (r *VectorStoreRepository) Count(ctx context.Context) (int64, error) {
	if err := r.Ensure(ctx); err != nil {
		return 0, err
	}
	var resp pineconeStatsResponse
	if err := r.dataPost(ctx, "/describe_index_stats", map[string]any{}, &resp); err != nil {
		return 0, fmt.Errorf("vector_store_repo.Count: %w", err)
	}
	if ns, ok := resp.Namespaces[r.namespace]; ok {
		return ns.VectorCount, nil
	}
	return 0, nil
}

func (r *VectorStoreRepository) describeIndex(ctx context.Context) (pineconeIndex, bool, error) {
	var index pineconeIndex
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, "https://api.pinecone.io/indexes/"+url.PathEscape(r.cfg.PineconeIndex), nil)
	if err != nil {
		return index, false, err
	}
	r.setControlHeaders(req)
	resp, err := r.client.Do(req)
	if err != nil {
		return index, false, fmt.Errorf("vector_store_repo.describeIndex: %w", err)
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return index, false, err
	}
	if resp.StatusCode == http.StatusNotFound {
		return index, false, nil
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return index, false, fmt.Errorf("vector_store_repo.describeIndex: status %d: %s", resp.StatusCode, truncateBody(body))
	}
	if err := json.Unmarshal(body, &index); err != nil {
		return index, false, err
	}
	return index, true, nil
}

func (r *VectorStoreRepository) createIndex(ctx context.Context) error {
	payload := pineconeCreateIndexRequest{
		Name:               r.cfg.PineconeIndex,
		VectorType:         "dense",
		Dimension:          r.dimensions,
		Metric:             "cosine",
		DeletionProtection: "disabled",
		Spec: pineconeIndexSpec{Serverless: pineconeServerlessSpec{
			Cloud:  r.cfg.PineconeCloud,
			Region: r.cfg.PineconeRegion,
		}},
	}
	if err := r.controlPost(ctx, "/indexes", payload, nil); err != nil {
		return fmt.Errorf("vector_store_repo.createIndex: %w", err)
	}
	log.Info().
		Str("index", r.cfg.PineconeIndex).
		Int("dimensions", r.dimensions).
		Str("cloud", r.cfg.PineconeCloud).
		Str("region", r.cfg.PineconeRegion).
		Msg("vector_store_repo: Pinecone index creation requested")
	return nil
}

func (r *VectorStoreRepository) controlPost(ctx context.Context, path string, payload any, out any) error {
	return r.doJSON(ctx, http.MethodPost, "https://api.pinecone.io"+path, payload, out, true)
}

func (r *VectorStoreRepository) dataPost(ctx context.Context, path string, payload any, out any) error {
	if r.host == "" {
		if err := r.Ensure(ctx); err != nil {
			return err
		}
	}
	return r.doJSON(ctx, http.MethodPost, r.host+path, payload, out, false)
}

func (r *VectorStoreRepository) dataGet(ctx context.Context, path string, out any) error {
	if r.host == "" {
		if err := r.Ensure(ctx); err != nil {
			return err
		}
	}
	return r.doJSON(ctx, http.MethodGet, r.host+path, nil, out, false)
}

func (r *VectorStoreRepository) doJSON(ctx context.Context, method, endpoint string, payload any, out any, control bool) error {
	var body io.Reader
	if payload != nil {
		buf := new(bytes.Buffer)
		if err := json.NewEncoder(buf).Encode(payload); err != nil {
			return err
		}
		body = buf
	}
	req, err := http.NewRequestWithContext(ctx, method, endpoint, body)
	if err != nil {
		return err
	}
	if control {
		r.setControlHeaders(req)
	} else {
		r.setDataHeaders(req)
	}
	resp, err := r.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return err
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("status %d: %s", resp.StatusCode, truncateBody(respBody))
	}
	if out != nil && len(respBody) > 0 {
		if err := json.Unmarshal(respBody, out); err != nil {
			return err
		}
	}
	return nil
}

func (r *VectorStoreRepository) setControlHeaders(req *http.Request) {
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Api-Key", r.cfg.PineconeAPIKey)
	req.Header.Set("X-Pinecone-Api-Version", pineconeAPIVersion)
}

func (r *VectorStoreRepository) setDataHeaders(req *http.Request) {
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Api-Key", r.cfg.PineconeAPIKey)
	req.Header.Set("X-Pinecone-Api-Version", pineconeAPIVersion)
}

func normalizePineconeHost(host string) string {
	if strings.HasPrefix(host, "http://") || strings.HasPrefix(host, "https://") {
		return strings.TrimRight(host, "/")
	}
	return "https://" + strings.TrimRight(host, "/")
}

func metadataString(metadata map[string]any, key string) string {
	if metadata == nil {
		return ""
	}
	if v, ok := metadata[key].(string); ok {
		return v
	}
	return ""
}

func metadataToStringMap(metadata map[string]any) map[string]string {
	if len(metadata) == 0 {
		return nil
	}
	result := make(map[string]string, len(metadata))
	for k, v := range metadata {
		switch typed := v.(type) {
		case string:
			result[k] = typed
		case float64:
			result[k] = fmt.Sprintf("%g", typed)
		case bool:
			result[k] = fmt.Sprintf("%t", typed)
		}
	}
	return result
}

func truncateBody(body []byte) string {
	const max = 512
	s := string(body)
	if len(s) <= max {
		return s
	}
	return s[:max] + "..."
}

type pineconeCreateIndexRequest struct {
	Name               string            `json:"name"`
	VectorType         string            `json:"vector_type"`
	Dimension          int               `json:"dimension"`
	Metric             string            `json:"metric"`
	Spec               pineconeIndexSpec `json:"spec"`
	DeletionProtection string            `json:"deletion_protection"`
}

type pineconeIndexSpec struct {
	Serverless pineconeServerlessSpec `json:"serverless"`
}

type pineconeServerlessSpec struct {
	Cloud  string `json:"cloud"`
	Region string `json:"region"`
}

type pineconeIndex struct {
	Name   string `json:"name"`
	Host   string `json:"host"`
	Status struct {
		Ready bool `json:"ready"`
	} `json:"status"`
}

type pineconeVector struct {
	ID       string         `json:"id"`
	Values   []float32      `json:"values"`
	Metadata map[string]any `json:"metadata,omitempty"`
}

type pineconeUpsertRequest struct {
	Namespace string           `json:"namespace"`
	Vectors   []pineconeVector `json:"vectors"`
}

type pineconeQueryRequest struct {
	Namespace       string    `json:"namespace"`
	Vector          []float32 `json:"vector"`
	TopK            int       `json:"topK"`
	IncludeValues   bool      `json:"includeValues"`
	IncludeMetadata bool      `json:"includeMetadata"`
}

type pineconeQueryResponse struct {
	Matches []struct {
		ID       string         `json:"id"`
		Score    float64        `json:"score"`
		Metadata map[string]any `json:"metadata"`
	} `json:"matches"`
}

type pineconeFetchResponse struct {
	Vectors map[string]struct {
		ID       string         `json:"id"`
		Metadata map[string]any `json:"metadata"`
	} `json:"vectors"`
}

type pineconeDeleteRequest struct {
	Namespace string         `json:"namespace"`
	DeleteAll bool           `json:"deleteAll,omitempty"`
	Filter    map[string]any `json:"filter,omitempty"`
}

type pineconeStatsResponse struct {
	Namespaces map[string]struct {
		VectorCount int64 `json:"vectorCount"`
	} `json:"namespaces"`
}
