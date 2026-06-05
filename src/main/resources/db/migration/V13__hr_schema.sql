-- ================================================================
-- V13 — HR Schema
-- ================================================================

-- ── LEAVE TYPES ───────────────────────────────────────────────
CREATE TABLE leave_types
(
    id                    UUID         NOT NULL
                                                DEFAULT uuid_generate_v4(),
    name                  VARCHAR(100) NOT NULL,
    code                  VARCHAR(30)  NOT NULL,
    is_paid               BOOLEAN      NOT NULL DEFAULT TRUE,
    default_days_per_year INT          NOT NULL,
    carry_forward_allowed BOOLEAN      NOT NULL DEFAULT FALSE,
    max_carry_forward_days INT         NOT NULL DEFAULT 0,
    requires_document     BOOLEAN      NOT NULL DEFAULT FALSE,
    min_days_notice       INT          NOT NULL DEFAULT 0,
    description           VARCHAR(500),
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_leave_types PRIMARY KEY (id),
    CONSTRAINT uq_leave_type_code UNIQUE (code)
);

-- ── LEAVE BALANCES ────────────────────────────────────────────
CREATE TABLE leave_balances
(
    id                    UUID    NOT NULL
                                                DEFAULT uuid_generate_v4(),
    employee_id           UUID    NOT NULL,
    leave_type_id         UUID    NOT NULL,
    year                  INT     NOT NULL,
    total_days            DECIMAL(6,1) NOT NULL DEFAULT 0,
    used_days             DECIMAL(6,1) NOT NULL DEFAULT 0,
    pending_days          DECIMAL(6,1) NOT NULL DEFAULT 0,
    carried_forward_days  DECIMAL(6,1) NOT NULL DEFAULT 0,

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_leave_balances PRIMARY KEY (id),
    CONSTRAINT uq_leave_balance_emp_type_year
        UNIQUE (employee_id, leave_type_id, year),
    CONSTRAINT fk_leave_balance_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_leave_balance_type
        FOREIGN KEY (leave_type_id)
            REFERENCES leave_types (id)
);

CREATE INDEX idx_leave_balances_employee
    ON leave_balances (employee_id)
    WHERE is_deleted = FALSE;

-- ── LEAVE APPLICATIONS ────────────────────────────────────────
CREATE TABLE leave_applications
(
    id                     UUID         NOT NULL
                                                 DEFAULT uuid_generate_v4(),
    application_number     VARCHAR(20)  NOT NULL,
    employee_id            UUID         NOT NULL,
    leave_type_id          UUID         NOT NULL,
    from_date              DATE         NOT NULL,
    to_date                DATE         NOT NULL,
    total_days             DECIMAL(6,1) NOT NULL,
    reason                 VARCHAR(1000),
    supporting_document_url VARCHAR(500),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    tl_id                  UUID,
    tl_action_at           TIMESTAMPTZ,
    tl_remarks             VARCHAR(500),

    pm_id                  UUID,
    pm_action_at           TIMESTAMPTZ,
    pm_remarks             VARCHAR(500),

    hr_id                  UUID,
    hr_action_at           TIMESTAMPTZ,
    hr_remarks             VARCHAR(500),

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_leave_applications PRIMARY KEY (id),
    CONSTRAINT uq_leave_app_number
        UNIQUE (application_number),
    CONSTRAINT fk_leave_app_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id),
    CONSTRAINT fk_leave_app_type
        FOREIGN KEY (leave_type_id)
            REFERENCES leave_types (id),
    CONSTRAINT fk_leave_app_tl
        FOREIGN KEY (tl_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_leave_app_pm
        FOREIGN KEY (pm_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_leave_app_hr
        FOREIGN KEY (hr_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_leave_status
        CHECK (status IN (
                          'PENDING','TL_APPROVED','PM_APPROVED',
                          'APPROVED','REJECTED','CANCELLED'
            ))
);

CREATE INDEX idx_leave_apps_employee
    ON leave_applications (employee_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_leave_apps_status
    ON leave_applications (status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_leave_apps_tl
    ON leave_applications (tl_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_leave_apps_pm
    ON leave_applications (pm_id)
    WHERE is_deleted = FALSE;

-- ── SALARY COMPONENTS ─────────────────────────────────────────
CREATE TABLE salary_components
(
    id                UUID         NOT NULL
                                            DEFAULT uuid_generate_v4(),
    name              VARCHAR(100) NOT NULL,
    code              VARCHAR(30)  NOT NULL,
    type              VARCHAR(20)  NOT NULL,
    calculation_type  VARCHAR(30)  NOT NULL,
    default_value     DECIMAL(10,4),
    display_order     INT          NOT NULL DEFAULT 0,
    is_taxable        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    description       VARCHAR(500),

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_salary_components PRIMARY KEY (id),
    CONSTRAINT uq_salary_component_code UNIQUE (code),
    CONSTRAINT chk_component_type
        CHECK (type IN ('EARNING','DEDUCTION')),
    CONSTRAINT chk_calculation_type
        CHECK (calculation_type IN (
                                    'FIXED','PERCENTAGE_OF_BASIC',
                                    'PERCENTAGE_OF_GROSS','ATTENDANCE_BASED'
            ))
);

-- ── EMPLOYEE SALARY STRUCTURES ────────────────────────────────
CREATE TABLE employee_salary_structures
(
    id             UUID    NOT NULL DEFAULT uuid_generate_v4(),
    employee_id    UUID    NOT NULL,
    effective_from DATE    NOT NULL,
    effective_to   DATE,
    components     JSONB   NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    notes          VARCHAR(500),

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_employee_salary_structures PRIMARY KEY (id),
    CONSTRAINT fk_salary_structure_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_salary_structures_employee
    ON employee_salary_structures (employee_id)
    WHERE is_deleted = FALSE;

-- ── PAYROLL RUNS ──────────────────────────────────────────────
CREATE TABLE payroll_runs
(
    id                UUID          NOT NULL
                                             DEFAULT uuid_generate_v4(),
    month             INT           NOT NULL,
    year              INT           NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    run_by_id         UUID,
    processed_at      TIMESTAMPTZ,
    finalized_at      TIMESTAMPTZ,
    total_employees   INT           NOT NULL DEFAULT 0,
    total_gross       DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_deductions  DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_net_pay     DECIMAL(14,2) NOT NULL DEFAULT 0,
    notes             VARCHAR(1000),

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_payroll_runs PRIMARY KEY (id),
    CONSTRAINT uq_payroll_run_month_year UNIQUE (month, year),
    CONSTRAINT fk_payroll_run_by
        FOREIGN KEY (run_by_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_payroll_status
        CHECK (status IN (
                          'DRAFT','PROCESSING','PROCESSED',
                          'FINALIZED','PAID'
            ))
);

-- ── PAYSLIPS ──────────────────────────────────────────────────
CREATE TABLE payslips
(
    id                UUID          NOT NULL
                                             DEFAULT uuid_generate_v4(),
    payroll_run_id    UUID          NOT NULL,
    employee_id       UUID          NOT NULL,
    month             INT           NOT NULL,
    year              INT           NOT NULL,

    total_working_days INT          NOT NULL DEFAULT 0,
    present_days      DECIMAL(6,1)  NOT NULL DEFAULT 0,
    paid_leave_days   DECIMAL(6,1)  NOT NULL DEFAULT 0,
    unpaid_leave_days DECIMAL(6,1)  NOT NULL DEFAULT 0,
    overtime_hours    DECIMAL(6,1)  NOT NULL DEFAULT 0,

    earnings          JSONB,
    deductions        JSONB,

    gross_earnings    DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_deductions  DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_pay           DECIMAL(12,2) NOT NULL DEFAULT 0,

    pf_employee       DECIMAL(10,2) NOT NULL DEFAULT 0,
    pf_employer       DECIMAL(10,2) NOT NULL DEFAULT 0,
    esi_employee      DECIMAL(10,2) NOT NULL DEFAULT 0,
    esi_employer      DECIMAL(10,2) NOT NULL DEFAULT 0,
    professional_tax  DECIMAL(10,2) NOT NULL DEFAULT 0,
    tds               DECIMAL(10,2) NOT NULL DEFAULT 0,

    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    payment_date      DATE,
    payment_mode      VARCHAR(20),
    payment_reference VARCHAR(100),
    notes             VARCHAR(500),

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_payslips PRIMARY KEY (id),
    CONSTRAINT uq_payslip_emp_month_year
        UNIQUE (employee_id, month, year),
    CONSTRAINT fk_payslip_run
        FOREIGN KEY (payroll_run_id)
            REFERENCES payroll_runs (id),
    CONSTRAINT fk_payslip_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id),
    CONSTRAINT chk_payslip_status
        CHECK (status IN ('DRAFT','FINALIZED','PAID'))
);

CREATE INDEX idx_payslips_run
    ON payslips (payroll_run_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_payslips_employee
    ON payslips (employee_id)
    WHERE is_deleted = FALSE;

-- ── SEED DEFAULT LEAVE TYPES ──────────────────────────────────
INSERT INTO leave_types (
    id, name, code, is_paid,
    default_days_per_year,
    carry_forward_allowed,
    max_carry_forward_days,
    requires_document,
    min_days_notice,
    description, is_active, created_by
) VALUES
      (
          uuid_generate_v4(), 'Annual Leave', 'ANNUAL',
          TRUE, 18, TRUE, 30, FALSE, 7,
          'Yearly paid leave entitlement', TRUE, 'system'
      ),
      (
          uuid_generate_v4(), 'Sick Leave', 'SICK',
          TRUE, 12, FALSE, 0, TRUE, 0,
          'Medical illness or injury. Certificate required for > 2 days',
          TRUE, 'system'
      ),
      (
          uuid_generate_v4(), 'Casual Leave', 'CASUAL',
          TRUE, 6, FALSE, 0, FALSE, 1,
          'Short unplanned absence', TRUE, 'system'
      ),
      (
          uuid_generate_v4(), 'Maternity Leave', 'MATERNITY',
          TRUE, 182, FALSE, 0, TRUE, 30,
          '26 weeks maternity benefit as per law',
          TRUE, 'system'
      ),
      (
          uuid_generate_v4(), 'Paternity Leave', 'PATERNITY',
          TRUE, 15, FALSE, 0, FALSE, 7,
          'Leave for fathers after childbirth',
          TRUE, 'system'
      ),
      (
          uuid_generate_v4(), 'Compensatory Leave', 'COMPENSATORY',
          TRUE, 0, TRUE, 10, FALSE, 1,
          'Earned by working on holidays or weekends',
          TRUE, 'system'
      ),
      (
          uuid_generate_v4(), 'Unpaid Leave', 'UNPAID',
          FALSE, 999, FALSE, 0, FALSE, 0,
          'Leave without pay when all paid leaves exhausted',
          TRUE, 'system'
      );

-- ── SEED DEFAULT SALARY COMPONENTS ───────────────────────────
INSERT INTO salary_components (
    id, name, code, type, calculation_type,
    default_value, display_order,
    is_taxable, is_active, description, created_by
) VALUES
-- EARNINGS
(
    uuid_generate_v4(), 'Basic Salary', 'BASIC',
    'EARNING', 'FIXED',
    0, 1, TRUE, TRUE,
    'Base salary — foundation of all calculations',
    'system'
),
(
    uuid_generate_v4(), 'HRA', 'HRA',
    'EARNING', 'PERCENTAGE_OF_BASIC',
    40.00, 2, TRUE, TRUE,
    'House Rent Allowance — 40% of basic',
    'system'
),
(
    uuid_generate_v4(), 'Dearness Allowance', 'DA',
    'EARNING', 'PERCENTAGE_OF_BASIC',
    10.00, 3, TRUE, TRUE,
    'Dearness Allowance — 10% of basic',
    'system'
),
(
    uuid_generate_v4(), 'Conveyance Allowance', 'CONVEYANCE',
    'EARNING', 'FIXED',
    1600.00, 4, FALSE, TRUE,
    'Fixed conveyance allowance',
    'system'
),
(
    uuid_generate_v4(), 'Medical Allowance', 'MEDICAL',
    'EARNING', 'FIXED',
    1250.00, 5, FALSE, TRUE,
    'Fixed medical allowance',
    'system'
),
(
    uuid_generate_v4(), 'Special Allowance', 'SPECIAL',
    'EARNING', 'FIXED',
    0, 6, TRUE, TRUE,
    'Remaining amount to make up CTC',
    'system'
),
-- DEDUCTIONS
(
    uuid_generate_v4(), 'PF (Employee)', 'PF_EMP',
    'DEDUCTION', 'PERCENTAGE_OF_BASIC',
    12.00, 1, FALSE, TRUE,
    'Provident Fund — 12% of basic (Employee share)',
    'system'
),
(
    uuid_generate_v4(), 'ESI (Employee)', 'ESI_EMP',
    'DEDUCTION', 'PERCENTAGE_OF_GROSS',
    0.75, 2, FALSE, TRUE,
    'ESI — 0.75% of gross (if gross < 21000)',
    'system'
),
(
    uuid_generate_v4(), 'Professional Tax', 'PT',
    'DEDUCTION', 'FIXED',
    200.00, 3, FALSE, TRUE,
    'Professional Tax — Maharashtra: 200/month',
    'system'
),
(
    uuid_generate_v4(), 'TDS', 'TDS',
    'DEDUCTION', 'FIXED',
    0, 4, FALSE, TRUE,
    'Tax Deducted at Source — set manually per employee',
    'system'
),
(
    uuid_generate_v4(), 'Leave Deduction', 'LEAVE_DED',
    'DEDUCTION', 'ATTENDANCE_BASED',
    0, 5, FALSE, TRUE,
    'Deduction for unpaid leave days',
    'system'
);