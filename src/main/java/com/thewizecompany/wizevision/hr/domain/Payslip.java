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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "payslips",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payslip_employee_month_year",
                        columnNames = {
                                "employee_id", "month", "year"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_payslips_run",
                        columnList = "payroll_run_id"
                ),
                @Index(
                        name = "idx_payslips_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_payslips_month_year",
                        columnList = "employee_id, month, year"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip extends BaseEntity {

    @Column(name = "payroll_run_id", nullable = false)
    private UUID payrollRunId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    // ── ATTENDANCE SUMMARY ────────────────────────────────────

    @Column(name = "total_working_days", nullable = false)
    @Builder.Default
    private Integer totalWorkingDays = 0;

    @Column(name = "present_days", nullable = false)
    @Builder.Default
    private BigDecimal presentDays = BigDecimal.ZERO;

    @Column(name = "paid_leave_days", nullable = false)
    @Builder.Default
    private BigDecimal paidLeaveDays = BigDecimal.ZERO;

    @Column(name = "unpaid_leave_days", nullable = false)
    @Builder.Default
    private BigDecimal unpaidLeaveDays = BigDecimal.ZERO;

    @Column(name = "overtime_hours", nullable = false)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    // ── SALARY COMPONENTS ─────────────────────────────────────

    /*
     * Full breakdown of earnings and deductions.
     * Same structure as EmployeeSalaryStructure.components
     * but with actual calculated amounts for this month.
     *
     * [
     *   { "code": "BASIC", "name": "Basic Salary",
     *     "type": "EARNING", "amount": 30000 },
     *   { "code": "HRA", "name": "HRA",
     *     "type": "EARNING", "amount": 12000 },
     *   { "code": "PF_EMP", "name": "PF (Employee)",
     *     "type": "DEDUCTION", "amount": 3600 }
     * ]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "earnings",
            columnDefinition = "jsonb"
    )
    private String earnings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "deductions",
            columnDefinition = "jsonb"
    )
    private String deductions;

    // ── TOTALS ────────────────────────────────────────────────

    @Column(
            name = "gross_earnings",
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal grossEarnings = BigDecimal.ZERO;

    @Column(
            name = "total_deductions",
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(
            name = "net_pay",
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal netPay = BigDecimal.ZERO;

    // ── STATUTORY BREAKDOWN ───────────────────────────────────

    @Column(
            name = "pf_employee",
            precision = 10,
            scale = 2
    )
    @Builder.Default
    private BigDecimal pfEmployee = BigDecimal.ZERO;

    @Column(
            name = "pf_employer",
            precision = 10,
            scale = 2
    )
    @Builder.Default
    private BigDecimal pfEmployer = BigDecimal.ZERO;

    @Column(
            name = "esi_employee",
            precision = 10,
            scale = 2
    )
    @Builder.Default
    private BigDecimal esiEmployee = BigDecimal.ZERO;

    @Column(
            name = "esi_employer",
            precision = 10,
            scale = 2
    )
    @Builder.Default
    private BigDecimal esiEmployer = BigDecimal.ZERO;

    @Column(
            name = "professional_tax",
            precision = 10,
            scale = 2
    )
    @Builder.Default
    private BigDecimal professionalTax = BigDecimal.ZERO;

    @Column(
            name = "tds",
            precision = 10,
            scale = 2
    )
    @Builder.Default
    private BigDecimal tds = BigDecimal.ZERO;

    // ── STATUS ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private PayslipStatus status = PayslipStatus.DRAFT;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    private com.thewizecompany.wizevision
            .invoicing.domain.PaymentMode paymentMode;

    @Column(
            name = "payment_reference",
            length = 100
    )
    private String paymentReference;

    @Column(name = "notes", length = 500)
    private String notes;

    public enum PayslipStatus {
        DRAFT,
        FINALIZED,
        PAID
    }
}