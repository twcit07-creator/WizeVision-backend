package com.thewizecompany.wizevision.attendance.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/*
 * ATTENDANCE EVENT
 *
 * Every single action from the Windows app is stored here.
 * This is the raw, immutable audit trail.
 *
 * Examples of records in this table:
 * employee_id | event_type  | event_time           | machine
 * ────────────────────────────────────────────────────────────
 * john-uuid   | CHECK_IN    | 2025-01-15 09:02:00  | DESK-01
 * john-uuid   | BREAK_START | 2025-01-15 13:00:00  | DESK-01
 * john-uuid   | BREAK_END   | 2025-01-15 13:45:00  | DESK-01
 * john-uuid   | IDLE_START  | 2025-01-15 15:30:00  | DESK-01
 * john-uuid   | IDLE_END    | 2025-01-15 15:42:00  | DESK-01
 * john-uuid   | CHECK_OUT   | 2025-01-15 18:05:00  | DESK-01
 *
 * OFFLINE SYNC SUPPORT:
 * is_synced = false means event came from offline queue
 * synced_at = when it arrived at the server
 * client_timestamp = when it actually happened on the PC
 */
@Entity
@Table(
        name = "attendance_events",
        indexes = {
                @Index(
                        name = "idx_att_events_employee_time",
                        columnList = "employee_id, event_time"
                ),
                @Index(
                        name = "idx_att_events_record",
                        columnList = "attendance_record_id"
                ),
                @Index(
                        name = "idx_att_events_type",
                        columnList = "event_type"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEvent extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "attendance_record_id")
    private UUID attendanceRecordId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            length = 20
    )
    private AttendanceEventType eventType;

    /*
     * event_time = the ACTUAL time the event happened.
     * For online events: same as created_at
     * For offline events: the time it happened on the PC
     *                     before internet was lost
     */
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(
            name = "machine_identifier",
            length = 100
    )
    private String machineIdentifier;

    @Column(name = "machine_name", length = 100)
    private String machineName;

    /*
     * Duration in minutes.
     * Calculated when the NEXT event arrives.
     * Example: IDLE_START at 15:30, IDLE_END at 15:42
     * → IDLE_START event gets duration = 12 minutes
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "notes", length = 500)
    private String notes;

    /*
     * IDLE TRACKING FIELDS
     *
     * idle_start_actual:
     *   The real time idle began.
     *   Windows app detects idle at T+3mins.
     *   It subtracts 3 mins to get actual idle start.
     *   This field stores that corrected time.
     *   Example: Detected idle at 13:03 → stored as 13:00
     *
     * idleReason:
     *   Why the employee was away.
     *   Selected by employee on lock screen when returning.
     *   Null for non-idle events.
     *
     * absenceDurationMinutes:
     *   How long the employee was away.
     *   Calculated as: idle_end_time - idle_start_actual
     *   Shown to employee: "You were away for 23 minutes"
     *
     * isAutoCheckout:
     *   True when this CHECK_OUT was triggered automatically
     *   by the 30-minute no-return rule.
     *   Not a manual checkout by the employee.
     *
     * isResumeEvent:
     *   True when employee re-logged in after auto-checkout.
     *   Marks the point where session resumed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "idle_reason", length = 30)
    private IdleReason idleReason;

    @Column(name = "idle_start_actual")
    private Instant idleStartActual;

    @Column(name = "absence_duration_minutes")
    private Integer absenceDurationMinutes;

    @Column(name = "is_auto_checkout", nullable = false)
    @Builder.Default
    private boolean autoCheckout = false;

    @Column(name = "is_resume_event", nullable = false)
    @Builder.Default
    private boolean resumeEvent = false;



    /*
     * Offline sync tracking.
     * is_offline = true means this event was queued
     * on the PC while internet was unavailable.
     */
    @Column(name = "is_offline", nullable = false)
    @Builder.Default
    private boolean offline = false;

    @Column(name = "synced_at")
    private Instant syncedAt;
}