-- ================================================================
-- V6 — Attendance Schema
-- ================================================================

-- ── ATTENDANCE RECORDS (daily summary) ───────────────────────
CREATE TABLE attendance_records
(
    id                   UUID         NOT NULL DEFAULT uuid_generate_v4(),
    employee_id          UUID         NOT NULL,
    attendance_date      DATE         NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ABSENT',

    check_in_time        TIMESTAMPTZ,
    check_out_time       TIMESTAMPTZ,
    total_work_minutes   INT          NOT NULL DEFAULT 0,
    total_break_minutes  INT          NOT NULL DEFAULT 0,
    total_idle_minutes   INT          NOT NULL DEFAULT 0,
    overtime_minutes     INT          NOT NULL DEFAULT 0,

    machine_identifier   VARCHAR(100),
    machine_name         VARCHAR(100),

    calculated_pay       DECIMAL(10, 2),
    payroll_processed    BOOLEAN      NOT NULL DEFAULT FALSE,

    is_late              BOOLEAN      NOT NULL DEFAULT FALSE,
    late_by_minutes      INT          NOT NULL DEFAULT 0,
    manually_adjusted    BOOLEAN      NOT NULL DEFAULT FALSE,
    adjustment_note      VARCHAR(500),
    adjustment_by        VARCHAR(100),

    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ,
    deleted_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_attendance_records
        PRIMARY KEY (id),
    CONSTRAINT uq_attendance_employee_date
        UNIQUE (employee_id, attendance_date),
    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_attendance_status
        CHECK (status IN (
                          'PRESENT','ABSENT','HALF_DAY',
                          'ON_LEAVE','HOLIDAY','LATE','WORK_FROM_HOME'
            ))
);

CREATE INDEX idx_attendance_employee_date
    ON attendance_records (employee_id, attendance_date)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_attendance_date
    ON attendance_records (attendance_date)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_attendance_status
    ON attendance_records (status)
    WHERE is_deleted = FALSE;

-- ── ATTENDANCE EVENTS (full audit trail) ─────────────────────
CREATE TABLE attendance_events
(
    id                    UUID        NOT NULL DEFAULT uuid_generate_v4(),
    employee_id           UUID        NOT NULL,
    attendance_record_id  UUID,
    event_type            VARCHAR(20) NOT NULL,
    event_time            TIMESTAMPTZ NOT NULL,
    machine_identifier    VARCHAR(100),
    machine_name          VARCHAR(100),
    duration_minutes      INT,
    notes                 VARCHAR(500),
    is_offline            BOOLEAN     NOT NULL DEFAULT FALSE,
    synced_at             TIMESTAMPTZ,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    is_deleted            BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,
    deleted_by            VARCHAR(100),
    version               BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_attendance_events
        PRIMARY KEY (id),
    CONSTRAINT fk_att_events_employee
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_att_events_record
        FOREIGN KEY (attendance_record_id)
            REFERENCES attendance_records (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_event_type
        CHECK (event_type IN (
                              'CHECK_IN','CHECK_OUT','BREAK_START',
                              'BREAK_END','IDLE_START','IDLE_END','HEARTBEAT'
            ))
);

CREATE INDEX idx_att_events_employee_time
    ON attendance_events (employee_id, event_time);

CREATE INDEX idx_att_events_record
    ON attendance_events (attendance_record_id);

-- ── MACHINE SESSIONS (live presence) ─────────────────────────
CREATE TABLE machine_sessions
(
    id                   UUID        NOT NULL DEFAULT uuid_generate_v4(),
    machine_identifier   VARCHAR(100) NOT NULL,
    machine_name         VARCHAR(100),
    current_employee_id  UUID,
    session_start_time   TIMESTAMPTZ,
    last_heartbeat       TIMESTAMPTZ,
    current_status       VARCHAR(20),
    app_version          VARCHAR(20),

    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ,
    deleted_by           VARCHAR(100),
    version              BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_machine_sessions
        PRIMARY KEY (id),
    CONSTRAINT uq_machine_identifier
        UNIQUE (machine_identifier),
    CONSTRAINT fk_machine_employee
        FOREIGN KEY (current_employee_id)
            REFERENCES employees (id)
            ON DELETE SET NULL
);

CREATE INDEX idx_machine_sessions_employee
    ON machine_sessions (current_employee_id)
    WHERE is_deleted = FALSE;