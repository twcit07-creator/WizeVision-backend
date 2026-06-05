-- ================================================================
-- V2 — Employee Schema
-- ================================================================
-- Creates the employees table.
-- This is the central table — every other module references it.
-- ================================================================

CREATE TABLE employees
(
    -- ── PRIMARY KEY ───────────────────────────────────────────
    id                    UUID        NOT NULL DEFAULT uuid_generate_v4(),

    -- ── IDENTITY ──────────────────────────────────────────────
    employee_code         VARCHAR(20)  NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(255) NOT NULL,
    phone                 VARCHAR(20),

    -- ── SECURITY ──────────────────────────────────────────────
    password_hash         VARCHAR(255) NOT NULL,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    account_locked_until  TIMESTAMPTZ,
    must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at         TIMESTAMPTZ,
    last_login_ip         VARCHAR(45),

    -- ── ROLE & STATUS ─────────────────────────────────────────
    role                  VARCHAR(30)  NOT NULL,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,

    -- ── HR INFORMATION ────────────────────────────────────────
    department            VARCHAR(100),
    designation           VARCHAR(100),
    joining_date          DATE,
    profile_photo_url     TEXT,

    -- ── AUDIT FIELDS (from BaseEntity) ────────────────────────
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,
    deleted_by            VARCHAR(100),
    version               BIGINT       NOT NULL DEFAULT 0,

    -- ── CONSTRAINTS ───────────────────────────────────────────
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uq_employees_email
        UNIQUE (email),
    CONSTRAINT uq_employees_code
        UNIQUE (employee_code),
    CONSTRAINT chk_employees_role
        CHECK (role IN (
                        'SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER',
                        'MARKETING_EXECUTIVE', 'HR_MANAGER', 'IT_ADMIN',
                        'FINANCE', 'CHECKER', 'EDITOR', 'MODELER', 'EMPLOYEE'
            ))
);

-- ── INDEXES ───────────────────────────────────────────────────
CREATE INDEX idx_employees_email
    ON employees (email)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_employees_code
    ON employees (employee_code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_employees_role
    ON employees (role)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_employees_is_active
    ON employees (is_active)
    WHERE is_deleted = FALSE;

-- ── SEED: DEFAULT SUPER ADMIN ─────────────────────────────────
/*
 * Creates the first super admin account.
 * Password: Admin@12345
 * BCrypt hash generated with strength 12.
 *
 * IMPORTANT: Change this password immediately after
 * first login in any environment.
 *
 * mustChangePassword = TRUE forces password change on login.
 */
INSERT INTO employees (
    id,
    employee_code,
    first_name,
    last_name,
    email,
    password_hash,
    role,
    is_active,
    must_change_password,
    department,
    designation,
    joining_date,
    created_by
) VALUES (
             uuid_generate_v4(),
             'WV-2025-001',
             'Super',
             'Admin',
             'admin@wizevision.com',
             -- BCrypt hash of: Admin@12345
             '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
             'SUPER_ADMIN',
             TRUE,
             TRUE,
             'Management',
             'System Administrator',
             CURRENT_DATE,
             'system'
         );