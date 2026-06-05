-- ================================================================
-- V12 — Invoicing Schema
-- ================================================================

-- ── INVOICES ──────────────────────────────────────────────────
CREATE TABLE invoices
(
    id                   UUID          NOT NULL
                                                DEFAULT uuid_generate_v4(),
    invoice_number       VARCHAR(20)   NOT NULL,

    project_id           UUID          NOT NULL,
    client_id            UUID          NOT NULL,
    client_contact_id    UUID,

    invoice_date         DATE          NOT NULL,
    due_date             DATE,

    subtotal             DECIMAL(12,2) NOT NULL,
    tax_percentage       DECIMAL(5,2)  NOT NULL DEFAULT 0,
    tax_amount           DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount         DECIMAL(12,2) NOT NULL,
    amount_paid          DECIMAL(12,2) NOT NULL DEFAULT 0,

    status               VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',

    line_items           JSONB,
    notes                TEXT,
    terms_and_conditions TEXT,

    sent_at              TIMESTAMPTZ,
    paid_at              TIMESTAMPTZ,

    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ,
    deleted_by           VARCHAR(100),
    version              BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT uq_invoice_number UNIQUE (invoice_number),
    CONSTRAINT fk_invoices_project
        FOREIGN KEY (project_id)
            REFERENCES projects (id),
    CONSTRAINT fk_invoices_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id),
    CONSTRAINT chk_invoice_status
        CHECK (status IN (
                          'DRAFT','SENT','PARTIALLY_PAID',
                          'PAID','OVERDUE','CANCELLED'
            )),
    CONSTRAINT chk_invoice_amounts
        CHECK (amount_paid >= 0
            AND total_amount >= 0
            AND subtotal >= 0)
);

CREATE INDEX idx_invoices_project
    ON invoices (project_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_invoices_client
    ON invoices (client_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_invoices_status
    ON invoices (status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_invoices_due_date
    ON invoices (due_date)
    WHERE is_deleted = FALSE AND status NOT IN
    ('PAID','CANCELLED');

-- ── PAYMENTS ──────────────────────────────────────────────────
CREATE TABLE payments
(
    id               UUID          NOT NULL
                                            DEFAULT uuid_generate_v4(),
    payment_number   VARCHAR(20)   NOT NULL,

    invoice_id       UUID          NOT NULL,
    project_id       UUID          NOT NULL,
    client_id        UUID          NOT NULL,

    amount           DECIMAL(12,2) NOT NULL,
    payment_date     DATE          NOT NULL,
    payment_mode     VARCHAR(20)   NOT NULL,
    reference_number VARCHAR(100),
    notes            VARCHAR(1000),
    recorded_by_id   UUID,

    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    deleted_by       VARCHAR(100),
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payment_number UNIQUE (payment_number),
    CONSTRAINT fk_payments_invoice
        FOREIGN KEY (invoice_id)
            REFERENCES invoices (id),
    CONSTRAINT fk_payments_project
        FOREIGN KEY (project_id)
            REFERENCES projects (id),
    CONSTRAINT fk_payments_recorder
        FOREIGN KEY (recorded_by_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_payment_amount
        CHECK (amount > 0),
    CONSTRAINT chk_payment_mode
        CHECK (payment_mode IN (
                                'BANK_TRANSFER','CHEQUE','CASH',
                                'ONLINE','UPI','OTHER'
            ))
);

CREATE INDEX idx_payments_invoice
    ON payments (invoice_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_payments_project
    ON payments (project_id)
    WHERE is_deleted = FALSE;