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

@Entity
@Table(
        name = "salary_components",
        indexes = {
                @Index(
                        name = "idx_salary_components_code",
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
public class SalaryComponent extends BaseEntity {

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    /*
     * Unique code used in formulas and calculations.
     * Examples: BASIC, HRA, DA, CONVEYANCE, PF_EMP, ESI_EMP
     */
    @Column(
            name = "code",
            nullable = false,
            unique = true,
            length = 30
    )
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            length = 20
    )
    private ComponentType type;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "calculation_type",
            nullable = false,
            length = 30
    )
    private CalculationType calculationType;

    /*
     * For FIXED: this is the default amount
     * For PERCENTAGE: this is the percentage value
     * Example: HRA = 40% of basic → value = 40
     */
    @Column(
            name = "default_value",
            precision = 10,
            scale = 4
    )
    private BigDecimal defaultValue;

    /*
     * Display order on payslip.
     * Earnings shown first (ascending order),
     * then deductions.
     */
    @Column(
            name = "display_order",
            nullable = false
    )
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_taxable", nullable = false)
    @Builder.Default
    private boolean isTaxable = false;

    @Column(
            name = "is_active",
            nullable = false
    )
    @Builder.Default
    private boolean isActive = true;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    public enum ComponentType {
        EARNING,
        DEDUCTION
    }

    public enum CalculationType {
        /*
         * FIXED              → Fixed amount per month
         * PERCENTAGE_OF_BASIC → X% of basic salary
         * PERCENTAGE_OF_GROSS → X% of gross earnings
         * ATTENDANCE_BASED   → Calculated from
         *                      attendance records
         */
        FIXED,
        PERCENTAGE_OF_BASIC,
        PERCENTAGE_OF_GROSS,
        ATTENDANCE_BASED
    }
}