package com.thewizecompany.wizevision.hr.domain;

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
import java.util.UUID;

/*
 * PAYROLL RUN
 *
 * Represents the monthly payroll processing cycle.
 * One PayrollRun per month.
 * Contains all payslips for that month.
 *
 * WORKFLOW:
 * HR initiates run for a month  → DRAFT
 * System calculates all payslips → PROCESSING
 * HR reviews                     → DRAFT (can modify)
 * HR finalizes                   → FINALIZED
 * Salaries paid                  → PAID
 */
@Entity
@Table(
        name = "payroll_runs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payroll_run_month_year",
                        columnNames = {"month", "year"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_payroll_runs_year_month",
                        columnList = "year, month"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRun extends BaseEntity {

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "run_by_id")
    private UUID runById;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(
            name = "total_employees",
            nullable = false
    )
    @Builder.Default
    private Integer totalEmployees = 0;

    @Column(
            name = "total_gross",
            precision = 14,
            scale = 2
    )
    @Builder.Default
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(
            name = "total_deductions",
            precision = 14,
            scale = 2
    )
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(
            name = "total_net_pay",
            precision = 14,
            scale = 2
    )
    @Builder.Default
    private BigDecimal totalNetPay = BigDecimal.ZERO;

    @Column(name = "notes", length = 1000)
    private String notes;

    public enum PayrollStatus {
        DRAFT,
        PROCESSING,
        PROCESSED,
        FINALIZED,
        PAID
    }

    public String getPeriodDisplay() {
        String[] months = {
                "January", "February", "March", "April",
                "May", "June", "July", "August",
                "September", "October", "November", "December"
        };
        return months[month - 1] + " " + year;
    }
}