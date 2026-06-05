-- ================================================================
-- V14 — Document Template Schema
-- ================================================================

CREATE TABLE document_templates
(
    id               UUID         NOT NULL
                                           DEFAULT uuid_generate_v4(),
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(500),
    template_type    VARCHAR(30)  NOT NULL,
    content          TEXT         NOT NULL,
    template_version VARCHAR(20),
    is_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    preview_image_url VARCHAR(500),

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_document_templates PRIMARY KEY (id),
    CONSTRAINT chk_template_type
        CHECK (template_type IN (
                                 'INVOICE','BID','PAYSLIP','PROJECT_SUMMARY'
            ))
);

CREATE INDEX idx_templates_type
    ON document_templates (template_type)
    WHERE is_deleted = FALSE;

/*
 * Only one default template per type.
 * Partial unique index enforces this at DB level.
 */
CREATE UNIQUE INDEX uq_default_template_per_type
    ON document_templates (template_type)
    WHERE is_default = TRUE
    AND is_deleted = FALSE
    AND is_active = TRUE;