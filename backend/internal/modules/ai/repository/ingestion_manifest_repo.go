package repository

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type IngestionManifest struct {
	SourceName          string
	DocumentHash        string
	ChunkCount          int
	EmbeddingModel      string
	EmbeddingDimensions int
	VectorProvider      string
}

type IngestionManifestRepository struct {
	pool *pgxpool.Pool
}

func NewIngestionManifestRepository(pool *pgxpool.Pool) *IngestionManifestRepository {
	return &IngestionManifestRepository{pool: pool}
}

func (r *IngestionManifestRepository) Get(ctx context.Context, sourceName string) (*IngestionManifest, error) {
	const query = `
		SELECT source_name, document_hash, chunk_count, embedding_model, embedding_dimensions, vector_provider
		FROM vector_ingestion_manifests
		WHERE source_name = $1
	`
	var manifest IngestionManifest
	err := r.pool.QueryRow(ctx, query, sourceName).Scan(
		&manifest.SourceName,
		&manifest.DocumentHash,
		&manifest.ChunkCount,
		&manifest.EmbeddingModel,
		&manifest.EmbeddingDimensions,
		&manifest.VectorProvider,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, nil
		}
		return nil, fmt.Errorf("ingestion_manifest_repo.Get: %w", err)
	}
	return &manifest, nil
}

func (r *IngestionManifestRepository) Upsert(ctx context.Context, manifest IngestionManifest) error {
	const query = `
		INSERT INTO vector_ingestion_manifests (
			source_name, document_hash, chunk_count, embedding_model,
			embedding_dimensions, vector_provider, updated_at
		)
		VALUES ($1, $2, $3, $4, $5, $6, now())
		ON CONFLICT (source_name) DO UPDATE
		SET document_hash = EXCLUDED.document_hash,
		    chunk_count = EXCLUDED.chunk_count,
		    embedding_model = EXCLUDED.embedding_model,
		    embedding_dimensions = EXCLUDED.embedding_dimensions,
		    vector_provider = EXCLUDED.vector_provider,
		    updated_at = now()
	`
	if _, err := r.pool.Exec(ctx, query,
		manifest.SourceName,
		manifest.DocumentHash,
		manifest.ChunkCount,
		manifest.EmbeddingModel,
		manifest.EmbeddingDimensions,
		manifest.VectorProvider,
	); err != nil {
		return fmt.Errorf("ingestion_manifest_repo.Upsert: %w", err)
	}
	return nil
}
