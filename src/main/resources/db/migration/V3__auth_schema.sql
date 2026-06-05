-- ================================================================
-- V3 — Auth Schema
-- ================================================================
-- Creates the refresh tokens table.
-- ================================================================

CREATE TABLE auth_refresh_tokens
(
    -- ── PRIMARY KEY ───────────────────────────────────────────
    id            UUID         NOT NULL DEFAULT uuid_generate_v4(),

    -- ── TOKEN DATA ────────────────────────────────────────────
    employee_id   UUID         NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    is_revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    device_info   VARCHAR(500),
    ip_address    VARCHAR(45),

    -- ── AUDIT FIELDS ──────────────────────────────────────────
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    deleted_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0,

    -- ── CONSTRAINTS ───────────────────────────────────────────
    CONSTRAINT pk_auth_refresh_tokens
        PRIMARY KEY (id),
    CONSTRAINT uq_auth_refresh_tokens_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_auth_refresh_tokens_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE
);

-- ── INDEXES ───────────────────────────────────────────────────

-- Fast lookup when validating a refresh token
CREATE INDEX idx_refresh_tokens_hash
    ON auth_refresh_tokens (token_hash)
    WHERE is_revoked = FALSE;

-- Fast lookup for "show all sessions for this employee"
CREATE INDEX idx_refresh_tokens_employee
    ON auth_refresh_tokens (employee_id)
    WHERE is_revoked = FALSE;

-- Used by cleanup job to delete expired tokens
CREATE INDEX idx_refresh_tokens_expires
    ON auth_refresh_tokens (expires_at)
    WHERE is_revoked = FALSE;