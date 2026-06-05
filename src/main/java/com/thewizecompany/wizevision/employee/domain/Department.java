package com.thewizecompany.wizevision.employee.domain;

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

import java.util.UUID;

@Entity
@Table(
        name = "departments",
        indexes = {
                @Index(
                        name = "idx_departments_name",
                        columnList = "name",
                        unique = true
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {

    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 100
    )
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /*
     * UUID reference to employees table.
     * Not a @ManyToOne to avoid circular dependency.
     * Department head is an employee, employee belongs to department.
     * We keep this as a plain UUID to break the cycle.
     */
    @Column(name = "head_employee_id")
    private UUID headEmployeeId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}