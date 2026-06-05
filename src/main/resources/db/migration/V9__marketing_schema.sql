-- ================================================================
-- V9 — Marketing Schema
-- ================================================================

-- ── LEADS ─────────────────────────────────────────────────────
CREATE TABLE leads
(
    id                   UUID         NOT NULL DEFAULT uuid_generate_v4(),
    lead_number          VARCHAR(20)  NOT NULL,

    company_name         VARCHAR(255) NOT NULL,
    industry_type        VARCHAR(100),
    city                 VARCHAR(100),
    state                VARCHAR(100),
    country              VARCHAR(100),

    contact_name         VARCHAR(200),
    contact_email        VARCHAR(255),
    contact_phone        VARCHAR(20),
    contact_whatsapp     VARCHAR(20),
    contact_designation  VARCHAR(100),

    source               VARCHAR(30)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    notes                TEXT,
    lost_reason          VARCHAR(500),

    assigned_to_id       UUID         NOT NULL,
    client_id            UUID,
    converted_at         TIMESTAMPTZ,

    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ,
    deleted_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_leads PRIMARY KEY (id),
    CONSTRAINT uq_leads_number UNIQUE (lead_number),
    CONSTRAINT fk_leads_assigned_to
        FOREIGN KEY (assigned_to_id)
            REFERENCES employees (id),
    CONSTRAINT fk_leads_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_lead_status
        CHECK (status IN (
                          'NEW','CONTACTED','QUALIFIED',
                          'CONVERTED','LOST'
            )),
    CONSTRAINT chk_lead_source
        CHECK (source IN (
                          'REFERRAL','COLD_CALL','WEBSITE','EXHIBITION',
                          'LINKEDIN','EMAIL_CAMPAIGN',
                          'EXISTING_CLIENT','OTHER'
            ))
);

CREATE INDEX idx_leads_status
    ON leads (status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_leads_assigned_to
    ON leads (assigned_to_id)
    WHERE is_deleted = FALSE;

-- ── PROJECT INQUIRIES ─────────────────────────────────────────
CREATE TABLE project_inquiries
(
    id                  UUID         NOT NULL DEFAULT uuid_generate_v4(),
    inquiry_number      VARCHAR(20)  NOT NULL,

    lead_id             UUID,
    client_id           UUID,
    client_contact_id   UUID,

    project_name        VARCHAR(255) NOT NULL,
    project_location    VARCHAR(255),
    description         TEXT,
    document_references JSONB,

    status              VARCHAR(20)  NOT NULL DEFAULT 'NEW',

    forwarded_to_id     UUID,
    forwarded_by_id     UUID,
    forwarded_at        TIMESTAMPTZ,
    forwarding_notes    VARCHAR(1000),
    bid_id              UUID,
    notes               TEXT,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_project_inquiries
        PRIMARY KEY (id),
    CONSTRAINT uq_inquiry_number
        UNIQUE (inquiry_number),
    CONSTRAINT fk_inquiry_lead
        FOREIGN KEY (lead_id)
            REFERENCES leads (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_inquiry_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_inquiry_client_contact
        FOREIGN KEY (client_contact_id)
            REFERENCES client_contacts (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_inquiry_forwarded_to
        FOREIGN KEY (forwarded_to_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_inquiry_forwarded_by
        FOREIGN KEY (forwarded_by_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_inquiry_status
        CHECK (status IN (
                          'NEW','FORWARDED','BID_IN_PROGRESS',
                          'BID_CREATED','CLOSED'
            ))
);

CREATE INDEX idx_inquiries_lead
    ON project_inquiries (lead_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_inquiries_client
    ON project_inquiries (client_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_inquiries_status
    ON project_inquiries (status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_inquiries_forwarded_to
    ON project_inquiries (forwarded_to_id)
    WHERE is_deleted = FALSE;