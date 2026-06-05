package com.thewizecompany.wizevision.hr.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_leave_balance_employee_type_year",
                        columnNames = {
                                "employee_id",
                                "leave_type_id",
                                "year"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_leave_balances_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_leave_balances_year",
                        columnList = "employee_id, year"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "year", nullable = false)
    private Integer year;

    /*
     * total_days = allocated for this year
     *              (default days + carried forward)
     * used_days  = approved and taken
     * pending_days = approved but future dates
     * remaining  = total - used - pending
     */
    @Column(
            name = "total_days",
            nullable = false
    )
    @Builder.Default
    private BigDecimal totalDays = BigDecimal.ZERO;

    @Column(
            name = "used_days",
            nullable = false
    )
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(
            name = "pending_days",
            nullable = false
    )
    @Builder.Default
    private BigDecimal pendingDays = BigDecimal.ZERO;

    @Column(
            name = "carried_forward_days",
            nullable = false
    )
    @Builder.Default
    private BigDecimal carriedForwardDays = BigDecimal.ZERO;

    public BigDecimal getRemainingDays() {
        return totalDays
                .subtract(usedDays)
                .subtract(pendingDays);
    }
}