package com.thewizecompany.wizevision.attendance.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/*
 * ATTENDANCE RECORD
 *
 * One record per employee per day.
 * Stores the summary of the day:
 * total work minutes, break minutes, idle minutes, etc.
 *
 * Detailed events (each check-in, break-start, etc.)
 * are stored in AttendanceEvent table.
 *
 * Think of this as the "daily summary" row
 * and AttendanceEvent as the "audit trail" rows.
 *
 * WHY TWO TABLES?
 * AttendanceRecord = fast queries for reports
 *   "Show me all employees absent this week" → query this table
 *   No need to scan thousands of events
 *
 * AttendanceEvent = full audit trail
 *   "Show me exactly when John was idle today" → query events
 */
@Entity
@Table(
        name = "attendance_records",
        uniqueConstraints = {
                /*
                 * One record per employee per day.
                 * Database enforces this — no duplicate days possible.
                 */
                @UniqueConstraint(
                        name = "uq_attendance_employee_date",
                        columnNames = {"employee_id", "attendance_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_attendance_employee_date",
                        columnList = "employee_id, attendance_date"
                ),
                @Index(
                        name = "idx_attendance_date",
                        columnList = "attendance_date"
                ),
                @Index(
                        name = "idx_attendance_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(
            name = "attendance_date",
            nullable = false
    )
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    // ── TIME TRACKING ─────────────────────────────────────────

    @Column(name = "check_in_time")
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    /*
     * All durations stored in MINUTES.
     * Easier for payroll calculations:
     * (workMinutes / 60) * hourlyRate = pay
     */
    @Column(name = "total_work_minutes")
    @Builder.Default
    private Integer totalWorkMinutes = 0;

    @Column(name = "total_break_minutes")
    @Builder.Default
    private Integer totalBreakMinutes = 0;

    @Column(name = "total_idle_minutes")
    @Builder.Default
    private Integer totalIdleMinutes = 0;

    @Column(name = "overtime_minutes")
    @Builder.Default
    private Integer overtimeMinutes = 0;

    // ── MACHINE TRACKING ──────────────────────────────────────

    /*
     * Which PC the employee used today.
     * Machine identifier = hostname of the Windows machine.
     * Example: "DESKTOP-ABC123" or "JOHN-PC"
     * Set on first check-in of the day.
     */
    @Column(
            name = "machine_identifier",
            length = 100
    )
    private String machineIdentifier;

    @Column(name = "machine_name", length = 100)
    private String machineName;

    // ── PAYROLL FIELDS ────────────────────────────────────────

    /*
     * Calculated daily pay based on attendance.
     * Populated during payroll processing.
     * null = payroll not yet calculated for this day.
     */
    @Column(
            name = "calculated_pay",
            precision = 10,
            scale = 2
    )
    private BigDecimal calculatedPay;

    @Column(name = "payroll_processed", nullable = false)
    @Builder.Default
    private boolean payrollProcessed = false;

    // ── FLAGS ─────────────────────────────────────────────────

    @Column(name = "is_late", nullable = false)
    @Builder.Default
    private boolean late = false;

    @Column(name = "late_by_minutes")
    @Builder.Default
    private Integer lateByMinutes = 0;

    /*
     * Manual override by HR/Admin.
     * Example: HR marks someone as "On Leave"
     * after they forget to check in.
     */
    @Column(
            name = "manually_adjusted",
            nullable = false
    )
    @Builder.Default
    private boolean manuallyAdjusted = false;

    @Column(name = "adjustment_note", length = 500)
    private String adjustmentNote;

    @Column(
            name = "adjustment_by",
            length = 100
    )
    private String adjustmentBy;

    /*
     * DAY ENDED FLAG
     *
     * Set to true when a single continuous absence
     * exceeds 4 hours (the business threshold).
     *
     * Once true:
     * - Employee cannot resume work for this day
     * - Windows app shows "Day ended" screen
     * - Re-login creates awareness but no new work session
     *
     * Can be manually overridden by HR/Admin
     * if the employee had a legitimate reason.
     */
    @Column(name = "day_ended", nullable = false)
    @Builder.Default
    private boolean dayEnded = false;

    @Column(name = "day_ended_at")
    private Instant dayEndedAt;

    @Column(name = "day_ended_reason", length = 100)
    private String dayEndedReason;
}