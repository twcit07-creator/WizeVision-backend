package com.thewizecompany.wizevision.projects.domain;

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
 * PROJECT ASSIGNMENT
 *
 * Audit trail of team assignments.
 * Every time PM assigns or changes a team member,
 * a record is created here.
 *
 * This allows:
 * - Seeing full history of who worked on a project
 * - Tracking when someone was assigned/removed
 * - Performance reports per employee per project
 *
 * The current team is on the Project entity itself
 * (modeler_id, editor_id, checker_id).
 * This table is the historical record.
 */
@Entity
@Table(
        name = "project_assignments",
        indexes = {
                @Index(
                        name = "idx_proj_assignments_project",
                        columnList = "project_id"
                ),
                @Index(
                        name = "idx_proj_assignments_employee",
                        columnList = "employee_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAssignment extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role_in_project",
            nullable = false,
            length = 20
    )
    private ProjectRoleType roleInProject;

    @Column(
            name = "assigned_at",
            nullable = false
    )
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "removed_by", length = 100)
    private String removedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    public boolean isActive() {
        return removedAt == null;
    }
}