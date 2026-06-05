-- ================================================================
-- V10 — Bid Schema
-- ================================================================

CREATE TABLE bids
(
    id                    UUID          NOT NULL
                                                 DEFAULT uuid_generate_v4(),
    bid_number            VARCHAR(20)   NOT NULL,

    -- Links
    inquiry_id            UUID,
    client_id             UUID          NOT NULL,
    client_contact_id     UUID,

    -- PM fills
    project_name          VARCHAR(255)  NOT NULL,
    project_location      VARCHAR(255),
    scope_of_work         TEXT,
    inclusions            JSONB,
    exclusions            JSONB,
    reference_documents   JSONB,
    estimated_weeks       INT,
    proposed_start_date   DATE,
    proposed_end_date     DATE,
    notes                 TEXT,

    -- Admin fills (hidden from PM)
    bid_amount            DECIMAL(12,2),
    internal_notes        TEXT,

    -- Status & workflow
    status                VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_by_pm_id      UUID          NOT NULL,
    reviewed_by_admin_id  UUID,
    submitted_at          TIMESTAMPTZ,
    sent_to_client_at     TIMESTAMPTZ,
    decided_at            TIMESTAMPTZ,
    rejection_reason      VARCHAR(1000),
    revision_number       INT           NOT NULL DEFAULT 0,

    -- Outcome
    converted_project_id  UUID,

    -- Audit
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,
    deleted_by            VARCHAR(100),
    version               BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_bids
        PRIMARY KEY (id),
    CONSTRAINT uq_bids_number
        UNIQUE (bid_number),
    CONSTRAINT fk_bids_inquiry
        FOREIGN KEY (inquiry_id)
            REFERENCES project_inquiries (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_bids_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id),
    CONSTRAINT fk_bids_client_contact
        FOREIGN KEY (client_contact_id)
            REFERENCES client_contacts (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_bids_pm
        FOREIGN KEY (created_by_pm_id)
            REFERENCES employees (id),
    CONSTRAINT fk_bids_admin
        FOREIGN KEY (reviewed_by_admin_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_bid_status
        CHECK (status IN (
                          'DRAFT','SUBMITTED','UNDER_REVIEW',
                          'SENT_TO_CLIENT','NEGOTIATING',
                          'ACCEPTED','REJECTED','CANCELLED'
            ))
);

CREATE INDEX idx_bids_status
    ON bids (status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_bids_client
    ON bids (client_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_bids_created_by_pm
    ON bids (created_by_pm_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_bids_inquiry
    ON bids (inquiry_id)
    WHERE is_deleted = FALSE;