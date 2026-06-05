-- ================================================================
-- V7 — Add idle tracking fields to attendance_events
-- ================================================================
-- idle_start_actual: the real idle start time
--   = detection_time - 3 minutes
--   This is what gets logged, not the detection time
--
-- idle_reason: why the employee was away
--   Selected by employee on the lock screen
--
-- absence_duration_minutes: calculated gap duration
--   Shown to employee on the resume screen
--   "You were away for X minutes"
-- ================================================================

ALTER TABLE attendance_events
    ADD COLUMN idle_reason              VARCHAR(30),
    ADD COLUMN idle_start_actual        TIMESTAMPTZ,
    ADD COLUMN absence_duration_minutes INT,
    ADD COLUMN is_auto_checkout         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_resume_event          BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE attendance_events
    ADD CONSTRAINT chk_idle_reason
        CHECK (idle_reason IS NULL OR idle_reason IN (
                                                      'LUNCH_BREAK', 'TEA_BREAK', 'WASHROOM_BREAK',
                                                      'IN_MEETING', 'PROJECT_DISCUSSION',
                                                      'TEAM_GUIDANCE', 'OTHER'
            ));

-- Track day_ended flag on attendance record
-- so we know re-login should be blocked
ALTER TABLE attendance_records
    ADD COLUMN day_ended                BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN day_ended_at             TIMESTAMPTZ,
    ADD COLUMN day_ended_reason         VARCHAR(100);

-- Index for finding records where day has not ended
-- Used when checking if employee can resume
CREATE INDEX idx_attendance_day_ended
    ON attendance_records (employee_id, attendance_date)
    WHERE day_ended = FALSE AND is_deleted = FALSE;