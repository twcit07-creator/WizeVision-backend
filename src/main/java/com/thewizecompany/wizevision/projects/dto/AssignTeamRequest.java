package com.thewizecompany.wizevision.projects.dto;

import com.thewizecompany.wizevision.projects.domain.ProjectMemberRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/*
 * Assigns multiple members to a project.
 * Each member has a role.
 *
 * A project can have:
 *   1 PROJECT_MANAGER (required)
 *   1+ TEAM_LEAD      (optional)
 *   1+ MODELER        (required)
 *   1+ EDITOR         (required)
 *   1+ CHECKER        (required)
 *
 * Replacing an existing role member:
 *   Send the new member with the same role.
 *   Previous assignment is closed (removedAt set).
 */
@Getter
@Setter
public class AssignTeamRequest {

    @NotEmpty(message = "At least one team member is required")
    private List<TeamMemberAssignment> members;

    private String notes;

    @Getter
    @Setter
    public static class TeamMemberAssignment {

        @NotNull(message = "Employee ID is required")
        private UUID employeeId;

        @NotNull(message = "Role is required")
        private ProjectMemberRole role;
    }
}