package com.thewizecompany.wizevision.hr.domain;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "leave_applications",
        indexes = {
                @Index(
                        name = "idx_leave_apps_number",
                        columnList = "application_number",
                        unique = true
                ),
                @Index(
                        name = "idx_leave_apps_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_leave_apps_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_leave_apps_dates",
                        columnList = "from_date, to_date"
                ),
                @Index(
                        name = "idx_leave_apps_tl",
                        columnList = "tl_id"
                ),
                @Index(
                        name = "idx_leave_apps_pm",
                        columnList = "pm_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplication extends BaseEntity {

    @Column(
            name = "application_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String applicationNumber;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    /*
     * Total days = business days between from and to.
     * Calculated when application is submitted.
     * Excludes weekends.
     */
    @Column(name = "total_days", nullable = false)
    private BigDecimal totalDays;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(
            name = "supporting_document_url",
            length = 500
    )
    private String supportingDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING;

    // ── TEAM LEAD REVIEW ──────────────────────────────────────

    @Column(name = "tl_id")
    private UUID tlId;

    @Column(name = "tl_action_at")
    private Instant tlActionAt;

    @Column(name = "tl_remarks", length = 500)
    private String tlRemarks;

    // ── PROJECT MANAGER REVIEW ────────────────────────────────

    @Column(name = "pm_id")
    private UUID pmId;

    @Column(name = "pm_action_at")
    private Instant pmActionAt;

    @Column(name = "pm_remarks", length = 500)
    private String pmRemarks;

    // ── HR REVIEW ─────────────────────────────────────────────

    @Column(name = "hr_id")
    private UUID hrId;

    @Column(name = "hr_action_at")
    private Instant hrActionAt;

    @Column(name = "hr_remarks", length = 500)
    private String hrRemarks;
}