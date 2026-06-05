-- ================================================================
-- V5 — Expand Employee Schema
-- ================================================================
-- Adds departments, designations tables.
-- Expands employees table with full Frappe-level fields.
-- ================================================================

-- ── DEPARTMENTS ───────────────────────────────────────────────
CREATE TABLE departments
(
    id               UUID         NOT NULL DEFAULT uuid_generate_v4(),
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(500),
    head_employee_id UUID,

    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    deleted_by       VARCHAR(100),
    version          BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uq_departments_name UNIQUE (name)
);

-- ── DESIGNATIONS ──────────────────────────────────────────────
CREATE TABLE designations
(
    id            UUID         NOT NULL DEFAULT uuid_generate_v4(),
    title         VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    department_id UUID,

    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    deleted_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_designations PRIMARY KEY (id),
    CONSTRAINT fk_designations_department
        FOREIGN KEY (department_id)
            REFERENCES departments (id)
            ON DELETE SET NULL
);

-- ── EXPAND EMPLOYEES TABLE ────────────────────────────────────

-- Personal information
ALTER TABLE employees
    ADD COLUMN middle_name               VARCHAR(100),
    ADD COLUMN date_of_birth             DATE,
    ADD COLUMN gender                    VARCHAR(20),
    ADD COLUMN blood_group               VARCHAR(5),
    ADD COLUMN personal_email            VARCHAR(255),
    ADD COLUMN current_address           VARCHAR(500),
    ADD COLUMN permanent_address         VARCHAR(500),
    ADD COLUMN emergency_contact_name    VARCHAR(200),
    ADD COLUMN emergency_contact_phone   VARCHAR(20),
    ADD COLUMN emergency_contact_relation VARCHAR(50);

-- Employment details
ALTER TABLE employees
    ADD COLUMN employment_type      VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME',
    ADD COLUMN department_id        UUID,
    ADD COLUMN designation_id       UUID,
    ADD COLUMN reporting_manager_id UUID,
    ADD COLUMN confirmation_date    DATE,
    ADD COLUMN notice_period_days   INT         DEFAULT 30,
    ADD COLUMN relieving_date       DATE,
    ADD COLUMN contract_start_date  DATE,
    ADD COLUMN contract_end_date    DATE;

-- Payroll
ALTER TABLE employees
    ADD COLUMN salary_structure     VARCHAR(20) DEFAULT 'MONTHLY',
    ADD COLUMN basic_salary         DECIMAL(12, 2),
    ADD COLUMN bank_account_number  VARCHAR(30),
    ADD COLUMN bank_name            VARCHAR(100),
    ADD COLUMN bank_ifsc_code       VARCHAR(20),
    ADD COLUMN pan_number           VARCHAR(20),
    ADD COLUMN pf_number            VARCHAR(30),
    ADD COLUMN esi_number           VARCHAR(30),
    ADD COLUMN pf_applicable        BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN esi_applicable       BOOLEAN     NOT NULL DEFAULT FALSE;

-- Identity documents
ALTER TABLE employees
    ADD COLUMN national_id_number   VARCHAR(30),
    ADD COLUMN passport_number      VARCHAR(20),
    ADD COLUMN passport_expiry      DATE,
    ADD COLUMN driving_license      VARCHAR(30);

-- Attendance (Windows app)
ALTER TABLE employees
    ADD COLUMN attendance_pin_hash   VARCHAR(255),
    ADD COLUMN attendance_pin_set_by VARCHAR(100),
    ADD COLUMN attendance_pin_set_at TIMESTAMPTZ,
    ADD COLUMN default_shift         VARCHAR(50);

-- Add foreign key constraints
ALTER TABLE employees
    ADD CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id)
            REFERENCES departments (id)
            ON DELETE SET NULL,
    ADD CONSTRAINT fk_employees_designation
        FOREIGN KEY (designation_id)
        REFERENCES designations (id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_employees_reporting_manager
        FOREIGN KEY (reporting_manager_id)
        REFERENCES employees (id)
        ON DELETE SET NULL;

-- Add constraint for departments head
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head
        FOREIGN KEY (head_employee_id)
            REFERENCES employees (id)
            ON DELETE SET NULL;

-- Add constraint for employment type
ALTER TABLE employees
    ADD CONSTRAINT chk_employment_type
        CHECK (employment_type IN (
                                   'FULL_TIME', 'CONTRACT', 'FREELANCE'
            ));

ALTER TABLE employees
    ADD CONSTRAINT chk_salary_structure
        CHECK (salary_structure IN (
                                    'MONTHLY', 'DAILY', 'HOURLY'
            ));

-- New indexes
CREATE INDEX idx_employees_department_id
    ON employees (department_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_employees_reporting_manager
    ON employees (reporting_manager_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_employees_employment_type
    ON employees (employment_type)
    WHERE is_deleted = FALSE;

-- ── SEED DEFAULT DEPARTMENTS ──────────────────────────────────
INSERT INTO departments (id, name, description, is_active, created_by)
VALUES
    (uuid_generate_v4(), 'Management',
     'Company management and administration',
     TRUE, 'system'),
    (uuid_generate_v4(), 'Detailing',
     'Steel detailing team - modelers, editors, checkers',
     TRUE, 'system'),
    (uuid_generate_v4(), 'Projects',
     'Project management team',
     TRUE, 'system'),
    (uuid_generate_v4(), 'Marketing',
     'Marketing and client acquisition team',
     TRUE, 'system'),
    (uuid_generate_v4(), 'HR',
     'Human resources team',
     TRUE, 'system'),
    (uuid_generate_v4(), 'IT',
     'Information technology team',
     TRUE, 'system'),
    (uuid_generate_v4(), 'Finance',
     'Finance and accounts team',
     TRUE, 'system');

-- ── SEED DEFAULT DESIGNATIONS ─────────────────────────────────
INSERT INTO designations (id, title, is_active, created_by)
VALUES
    (uuid_generate_v4(), 'Super Administrator', TRUE, 'system'),
    (uuid_generate_v4(), 'Administrator',       TRUE, 'system'),
    (uuid_generate_v4(), 'Project Manager',     TRUE, 'system'),
    (uuid_generate_v4(), 'Team Lead',           TRUE, 'system'),
    (uuid_generate_v4(), 'Senior Modeler',      TRUE, 'system'),
    (uuid_generate_v4(), 'Modeler',             TRUE, 'system'),
    (uuid_generate_v4(), 'Senior Editor',       TRUE, 'system'),
    (uuid_generate_v4(), 'Editor',              TRUE, 'system'),
    (uuid_generate_v4(), 'Senior Checker',      TRUE, 'system'),
    (uuid_generate_v4(), 'Checker',             TRUE, 'system'),
    (uuid_generate_v4(), 'Marketing Executive', TRUE, 'system'),
    (uuid_generate_v4(), 'HR Manager',          TRUE, 'system'),
    (uuid_generate_v4(), 'IT Administrator',    TRUE, 'system'),
    (uuid_generate_v4(), 'Finance Manager',     TRUE, 'system'),
    (uuid_generate_v4(), 'Accountant',          TRUE, 'system');