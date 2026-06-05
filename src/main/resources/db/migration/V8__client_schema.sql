-- ================================================================
-- V8 — Client Schema
-- ================================================================
-- clients      = fabricator companies
-- client_contacts = persons at those companies
-- ================================================================

-- ── CLIENTS ───────────────────────────────────────────────────
CREATE TABLE clients
(
    id            UUID         NOT NULL DEFAULT uuid_generate_v4(),
    company_code  VARCHAR(20)  NOT NULL,
    company_name  VARCHAR(255) NOT NULL,
    email         VARCHAR(255),
    phone         VARCHAR(20),
    website       VARCHAR(255),

    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city          VARCHAR(100),
    state         VARCHAR(100),
    country       VARCHAR(100),
    pincode       VARCHAR(20),

    gst_number    VARCHAR(30),
    industry_type VARCHAR(100),
    notes         TEXT,

    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    deleted_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_clients
        PRIMARY KEY (id),
    CONSTRAINT uq_clients_code
        UNIQUE (company_code)
);

CREATE INDEX idx_clients_company_name
    ON clients (company_name)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_clients_is_active
    ON clients (is_active)
    WHERE is_deleted = FALSE;

-- ── CLIENT CONTACTS ───────────────────────────────────────────
CREATE TABLE client_contacts
(
    id          UUID         NOT NULL DEFAULT uuid_generate_v4(),
    client_id   UUID         NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    designation VARCHAR(100),
    email       VARCHAR(255),
    phone       VARCHAR(20),
    whatsapp    VARCHAR(20),
    notes       VARCHAR(1000),
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    deleted_by  VARCHAR(100),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_client_contacts
        PRIMARY KEY (id),
    CONSTRAINT fk_client_contacts_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_client_contacts_client
    ON client_contacts (client_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_client_contacts_email
    ON client_contacts (email)
    WHERE is_deleted = FALSE;

/*
 * Partial unique index:
 * Only ONE primary contact per client.
 * Cannot have two contacts with is_primary = TRUE
 * for the same client.
 *
 * This is enforced at DB level, not just service level.
 */
CREATE UNIQUE INDEX uq_client_primary_contact
    ON client_contacts (client_id)
    WHERE is_primary = TRUE
    AND is_deleted = FALSE;