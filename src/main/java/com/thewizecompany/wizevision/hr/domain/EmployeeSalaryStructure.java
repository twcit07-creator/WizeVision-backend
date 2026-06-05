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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

/*
 * EMPLOYEE SALARY STRUCTURE
 *
 * Defines the salary breakdown for a specific employee
 * from a specific date.
 *
 * components JSONB:
 * [
 *   {
 *     "componentId": "uuid",
 *     "componentCode": "BASIC",
 *     "componentName": "Basic Salary",
 *     "type": "EARNING",
 *     "calculationType": "FIXED",
 *     "value": 30000,
 *     "amount": 30000
 *   },
 *   {
 *     "componentId": "uuid",
 *     "componentCode": "HRA",
 *     "componentName": "HRA",
 *     "type": "EARNING",
 *     "calculationType": "PERCENTAGE_OF_BASIC",
 *     "value": 40,
 *     "amount": 12000
 *   },
 *   {
 *     "componentId": "uuid",
 *     "componentCode": "PF_EMP",
 *     "componentName": "PF (Employee)",
 *     "type": "DEDUCTION",
 *     "calculationType": "PERCENTAGE_OF_BASIC",
 *     "value": 12,
 *     "amount": 3600
 *   }
 * ]
 */
@Entity
@Table(
        name = "employee_salary_structures",
        indexes = {
                @Index(
                        name = "idx_salary_structures_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_salary_structures_active",
                        columnList = "employee_id, is_active"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSalaryStructure extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(
            name = "effective_from",
            nullable = false
    )
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "components",
            columnDefinition = "jsonb",
            nullable = false
    )
    private String components;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "notes", length = 500)
    private String notes;
}