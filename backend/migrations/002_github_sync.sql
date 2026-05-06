-- Smart Portfolio: GitHub works sync
-- Stores a compact GitHub-backed works catalog. Embeddings live in Pinecone.

CREATE TABLE IF NOT EXISTS github_profiles (
    username            VARCHAR(255) PRIMARY KEY,
    display_name        VARCHAR(255),
    bio                 TEXT,
    profile_url         VARCHAR(255) NOT NULL,
    repositories_url    VARCHAR(255) NOT NULL,
    avatar_url          VARCHAR(255),
    last_synced_at      TIMESTAMPTZ,
    last_error          TEXT,
    rate_limit_remaining INT,
    rate_limit_reset_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS github_repositories (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_repo_id    BIGINT       NOT NULL UNIQUE,
    username          VARCHAR(255) NOT NULL,
    owner_login       VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    description       TEXT         NOT NULL,
    readme_summary    TEXT,
    tech_stack        VARCHAR(512),
    github_url        VARCHAR(255) NOT NULL,
    homepage_url      VARCHAR(255),
    primary_language  VARCHAR(100),
    stars             INT          NOT NULL DEFAULT 0,
    forks             INT          NOT NULL DEFAULT 0,
    watchers          INT          NOT NULL DEFAULT 0,
    is_pinned         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_archived       BOOLEAN      NOT NULL DEFAULT FALSE,
    github_updated_at TIMESTAMPTZ  NOT NULL,
    pushed_at         TIMESTAMPTZ,
    readme_sha        VARCHAR(255),
    synced_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_github_repositories_username_rank
    ON github_repositories (username, is_pinned DESC, stars DESC, pushed_at DESC, github_updated_at DESC);
