-- ================================================================
-- V11 — Project Schema
-- ================================================================
-- projects            = main project records
-- project_assignments = team assignment history
-- change_orders       = additional work requests
-- ================================================================

-- ── PROJECTS ──────────────────────────────────────────────────
CREATE TABLE projects
(
    id                    UUID          NOT NULL
                                                 DEFAULT uuid_generate_v4(),

    -- Identity
    project_number        VARCHAR(25)   NOT NULL,

    -- Links
    bid_id                UUID,
    client_id             UUID          NOT NULL,
    client_contact_id     UUID,

    -- Project details
    project_name          VARCHAR(255)  NOT NULL,
    project_location      VARCHAR(255),
    scope_of_work         TEXT,
    inclusions            JSONB,
    exclusions            JSONB,

    -- Financials
    contract_amount       DECIMAL(12,2),
    change_orders_total   DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_invoiced        DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_paid            DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- Status
    status                VARCHAR(20)   NOT NULL DEFAULT 'PLANNING',
    current_phase         VARCHAR(20)   NOT NULL DEFAULT 'MODELLING',
    progress_percentage   INT           NOT NULL DEFAULT 0,

    -- Team
    pm_id                 UUID          NOT NULL,
    modeler_id            UUID,
    editor_id             UUID,
    checker_id            UUID,

    -- Timeline
    estimated_start_date  DATE,
    estimated_end_date    DATE,
    actual_start_date     DATE,
    actual_end_date       DATE,
    estimated_weeks       INT,

    -- Notes
    pm_notes              TEXT,
    on_hold_reason        VARCHAR(500),
    on_hold_at            TIMESTAMPTZ,

    -- Audit
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,
    deleted_by            VARCHAR(100),
    version               BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT uq_projects_number UNIQUE (project_number),
    CONSTRAINT fk_projects_bid
        FOREIGN KEY (bid_id)
            REFERENCES bids (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_projects_client
        FOREIGN KEY (client_id)
            REFERENCES clients (id),
    CONSTRAINT fk_projects_pm
        FOREIGN KEY (pm_id)
            REFERENCES employees (id),
    CONSTRAINT fk_projects_modeler
        FOREIGN KEY (modeler_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_projects_editor
        FOREIGN KEY (editor_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_projects_checker
        FOREIGN KEY (checker_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_project_status
        CHECK (status IN (
                          'PLANNING','ACTIVE','ON_HOLD',
                          'DELIVERED','COMPLETED','CANCELLED'
            )),
    CONSTRAINT chk_project_phase
        CHECK (current_phase IN (
                                 'MODELLING','DRAFTING','CHECKING',
                                 'CORRECTIONS','FINAL_REVIEW',
                                 'DELIVERED','COMPLETED'
            )),
    CONSTRAINT chk_progress
        CHECK (progress_percentage BETWEEN 0 AND 100)
);

CREATE INDEX idx_projects_status
    ON projects (status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_projects_client
    ON projects (client_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_projects_pm
    ON projects (pm_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_projects_modeler
    ON projects (modeler_id)
    WHERE is_deleted = FALSE;

-- ── PROJECT ASSIGNMENTS ───────────────────────────────────────
CREATE TABLE project_assignments
(
    id               UUID        NOT NULL DEFAULT uuid_generate_v4(),
    project_id       UUID        NOT NULL,
    employee_id      UUID        NOT NULL,
    role_in_project  VARCHAR(20) NOT NULL,
    assigned_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by      VARCHAR(100),
    removed_at       TIMESTAMPTZ,
    removed_by       VARCHAR(100),
    notes            VARCHAR(500),

    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    deleted_by       VARCHAR(100),
    version          BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_project_assignments PRIMARY KEY (id),
    CONSTRAINT fk_assignments_project
        FOREIGN KEY (project_id)
            REFERENCES projects (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_assignments_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id),
    CONSTRAINT chk_assignment_role
        CHECK (role_in_project IN (
                                   'PROJECT_MANAGER','MODELER','EDITOR','CHECKER'
            ))
);

CREATE INDEX idx_proj_assignments_project
    ON project_assignments (project_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_proj_assignments_employee
    ON project_assignments (employee_id)
    WHERE is_deleted = FALSE;

-- ── CHANGE ORDERS ─────────────────────────────────────────────
CREATE TABLE change_orders
(
    id                    UUID          NOT NULL
                                                 DEFAULT uuid_generate_v4(),

    /*
     * Format: J-TWC-2026-001-COR-001
     * Directly traceable to parent project.
     */
    change_order_number   VARCHAR(35)   NOT NULL,
    project_id            UUID          NOT NULL,
    description           TEXT          NOT NULL,
    scope_of_change       TEXT,
    status                VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    amount                DECIMAL(12,2),
    created_by_pm_id      UUID          NOT NULL,
    submitted_at          TIMESTAMPTZ,
    approved_by_id        UUID,
    approved_at           TIMESTAMPTZ,
    rejection_reason      VARCHAR(500),
    notes                 TEXT,

    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,
    deleted_by            VARCHAR(100),
    version               BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_change_orders PRIMARY KEY (id),
    CONSTRAINT uq_change_order_number
        UNIQUE (change_order_number),
    CONSTRAINT fk_change_orders_project
        FOREIGN KEY (project_id)
            REFERENCES projects (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_change_orders_pm
        FOREIGN KEY (created_by_pm_id)
            REFERENCES employees (id),
    CONSTRAINT fk_change_orders_approver
        FOREIGN KEY (approved_by_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_co_status
        CHECK (status IN (
                          'DRAFT','SUBMITTED','APPROVED','REJECTED'
            ))
);

CREATE INDEX idx_change_orders_project
    ON change_orders (project_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_change_orders_status
    ON change_orders (status)
    WHERE is_deleted = FALSE;

-- ── UPDATE BIDS TABLE ─────────────────────────────────────────
-- Add FK now that projects table exists
ALTER TABLE bids
    ADD CONSTRAINT fk_bids_converted_project
        FOREIGN KEY (converted_project_id)
            REFERENCES projects (id)
            ON DELETE SET NULL;