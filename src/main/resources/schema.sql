-- =============================================
-- Phase 1: URL Shortener tables
-- =============================================
CREATE TABLE IF NOT EXISTS urls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    short_code VARCHAR(20) NOT NULL,
    long_url TEXT NOT NULL,
    custom_alias VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_short_code UNIQUE (short_code),
    CONSTRAINT uq_custom_alias UNIQUE (custom_alias)
);

CREATE INDEX IF NOT EXISTS idx_short_code ON urls(short_code);
CREATE INDEX IF NOT EXISTS idx_custom_alias ON urls(custom_alias);
CREATE INDEX IF NOT EXISTS idx_expires_at ON urls(expires_at) WHERE is_active = TRUE;

-- =============================================
-- Phase 2: Authentication tables
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);

-- Add user_id to urls table for ownership tracking
ALTER TABLE urls ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_url_user ON urls(user_id);

-- =============================================
-- Phase 3: Click Analytics tables
-- =============================================
CREATE TABLE IF NOT EXISTS click_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url_id UUID NOT NULL,
    short_code VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    country VARCHAR(100),
    city VARCHAR(100),
    device_type VARCHAR(50),
    browser VARCHAR(100),
    operating_system VARCHAR(100),
    referrer TEXT,
    user_agent TEXT,
    clicked_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_click_event_url FOREIGN KEY (url_id)
        REFERENCES urls(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_click_short_code
    ON click_events(short_code);

CREATE INDEX IF NOT EXISTS idx_click_clicked_at
    ON click_events(clicked_at DESC);

CREATE INDEX IF NOT EXISTS idx_click_url_id
    ON click_events(url_id);

CREATE INDEX IF NOT EXISTS idx_click_short_code_clicked_at
    ON click_events(short_code, clicked_at DESC);
