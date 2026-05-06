package repository

import (
	"context"
	"fmt"

	"github.com/ZRishu/smart-portfolio/internal/config"
)

type GitHubEmbeddingDocument struct {
	EntityKey    string
	Username     string
	EntityType   string
	GitHubRepoID *int64
	Content      string
	Embedding    []float32
	Metadata     map[string]string
}

type GitHubEmbeddingRepository struct {
	vectorStore *VectorStoreRepository
}

func NewGitHubEmbeddingRepository(cfg config.VectorStoreConfig, dimensions int) (*GitHubEmbeddingRepository, error) {
	base, err := NewVectorStoreRepository(cfg, dimensions)
	if err != nil {
		return nil, err
	}
	namespace := cfg.PineconeGitHubNS
	if namespace == "" {
		namespace = "github"
	}
	return &GitHubEmbeddingRepository{vectorStore: base.WithNamespace(namespace)}, nil
}

func (r *GitHubEmbeddingRepository) UpsertMany(ctx context.Context, docs []GitHubEmbeddingDocument) error {
	if len(docs) == 0 {
		return nil
	}

	vectorDocs := make([]EmbeddingDocument, 0, len(docs))
	for _, doc := range docs {
		metadata := map[string]string{
			"username":    doc.Username,
			"entity_type": doc.EntityType,
		}
		if doc.GitHubRepoID != nil {
			metadata["repo_id"] = fmt.Sprintf("%d", *doc.GitHubRepoID)
		}
		for k, v := range doc.Metadata {
			metadata[k] = v
		}
		vectorDocs = append(vectorDocs, EmbeddingDocument{
			ID:        doc.EntityKey,
			Content:   doc.Content,
			Embedding: doc.Embedding,
			Metadata:  metadata,
		})
	}

	return r.vectorStore.UpsertDocuments(ctx, vectorDocs)
}

func (r *GitHubEmbeddingRepository) DeleteMissingByUsername(ctx context.Context, username string, keepKeys []string) error {
	// Pinecone upserts use stable IDs, so unchanged repositories are overwritten
	// and duplicate vectors are not created. Avoid metadata deletes here because
	// they can consume extra write units and risk deleting freshly upserted data
	// on low-tier resources.
	return nil
}
