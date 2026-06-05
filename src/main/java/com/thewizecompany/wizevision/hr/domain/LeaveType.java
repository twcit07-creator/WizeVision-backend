package com.thewizecompany.wizevision.hr.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "leave_types",
        indexes = {
                @Index(
                        name = "idx_leave_types_code",
                        columnList = "code",
                        unique = true
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType extends BaseEntity {

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    /*
     * Short code: ANNUAL, SICK, CASUAL, MATERNITY,
     * PATERNITY, COMPENSATORY, UNPAID
     */
    @Column(
            name = "code",
            nullable = false,
            unique = true,
            length = 30
    )
    private String code;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean isPaid = true;

    @Column(
            name = "default_days_per_year",
            nullable = false
    )
    private Integer defaultDaysPerYear;

    @Column(
            name = "carry_forward_allowed",
            nullable = false
    )
    @Builder.Default
    private boolean carryForwardAllowed = false;

    @Column(name = "max_carry_forward_days")
    @Builder.Default
    private Integer maxCarryForwardDays = 0;

    /*
     * Does this leave type require a supporting document?
     * Sick leave > 2 days typically requires a certificate.
     */
    @Column(
            name = "requires_document",
            nullable = false
    )
    @Builder.Default
    private boolean requiresDocument = false;

    /*
     * How many days in advance must the application be made?
     * Annual leave: 7 days notice
     * Casual leave: 1 day notice
     * Sick leave: 0 (can apply same day)
     */
    @Column(name = "min_days_notice")
    @Builder.Default
    private Integer minDaysNotice = 0;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}