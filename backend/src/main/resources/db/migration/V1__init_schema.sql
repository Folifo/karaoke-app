-- Schéma initial (CLAUDE.md §5), plus la table des refresh tokens.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Les emails sont normalisés en minuscules par l'application ; l'index garantit l'unicité.
CREATE UNIQUE INDEX users_email_uk ON users (email);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- SHA-256 du token (hexadécimal) : le token lui-même n'est jamais stocké.
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX refresh_tokens_hash_uk ON refresh_tokens (token_hash);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);

CREATE TABLE songs (
    id               UUID PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    artist           VARCHAR(200) NOT NULL,
    duration_ms      BIGINT       NOT NULL CHECK (duration_ms > 0),
    instrumental_key VARCHAR(500) NOT NULL,
    lyrics_lrc       TEXT         NOT NULL,
    language         VARCHAR(8)   NOT NULL,
    cover_url        VARCHAR(1000),
    is_free          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Recherche « contient » insensible à la casse sur titre et artiste.
CREATE INDEX songs_title_trgm_idx ON songs USING gin (lower(title) gin_trgm_ops);
CREATE INDEX songs_artist_trgm_idx ON songs USING gin (lower(artist) gin_trgm_ops);
CREATE INDEX songs_artist_title_idx ON songs (artist, title);

CREATE TABLE recordings (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    song_id     UUID         NOT NULL REFERENCES songs (id),
    voice_key   VARCHAR(500) NOT NULL,
    mixed_key   VARCHAR(500) NOT NULL,
    duration_ms BIGINT       NOT NULL CHECK (duration_ms > 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX recordings_user_created_idx ON recordings (user_id, created_at DESC);

CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY,
    user_id                 UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    platform                VARCHAR(10)  NOT NULL CHECK (platform IN ('APPLE', 'GOOGLE')),
    product_id              VARCHAR(200) NOT NULL,
    original_transaction_id VARCHAR(500) NOT NULL,
    status                  VARCHAR(10)  NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELED')),
    expires_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Une transaction d'origine identifie un abonnement chez Apple/Google.
CREATE UNIQUE INDEX subscriptions_platform_tx_uk ON subscriptions (platform, original_transaction_id);
CREATE INDEX subscriptions_user_idx ON subscriptions (user_id);

CREATE TABLE usage_daily (
    user_id      UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day          DATE    NOT NULL,
    songs_played INTEGER NOT NULL DEFAULT 0 CHECK (songs_played >= 0),
    PRIMARY KEY (user_id, day)
);
