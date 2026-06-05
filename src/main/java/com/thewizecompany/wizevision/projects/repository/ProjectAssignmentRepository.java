package com.thewizecompany.wizevision.projects.repository;

import com.thewizecompany.wizevision.projects.domain.ProjectAssignment;
import com.thewizecompany.wizevision.projects.domain.ProjectRoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectAssignmentRepository
        extends JpaRepository<ProjectAssignment, UUID> {

    List<ProjectAssignment> findByProjectIdAndIsDeletedFalse(
            UUID projectId
    );

    List<ProjectAssignment> findByProjectIdAndRemovedAtIsNullAndIsDeletedFalse(
            UUID projectId
    );

    Optional<ProjectAssignment> findByProjectIdAndEmployeeIdAndRoleInProjectAndRemovedAtIsNullAndIsDeletedFalse(
            UUID projectId,
            UUID employeeId,
            ProjectRoleType role
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProjectAssignment a
        SET a.removedAt = :removedAt,
            a.removedBy = :removedBy
        WHERE a.projectId = :projectId
        AND a.roleInProject = :role
        AND a.removedAt IS NULL
        AND a.isDeleted = FALSE
        """)
    void removeCurrentAssignment(
            @Param("projectId") UUID projectId,
            @Param("role") ProjectRoleType role,
            @Param("removedAt") Instant removedAt,
            @Param("removedBy") String removedBy
    );

    /*
     * Closes the active assignment for a specific employee + role
     * on a project. Used when reassigning the same role to someone
     * else, or explicitly removing one person without affecting
     * others in the same role.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProjectAssignment a
        SET a.removedAt = :removedAt,
            a.removedBy = :removedBy
        WHERE a.projectId = :projectId
        AND a.employeeId = :employeeId
        AND a.roleInProject = :role
        AND a.removedAt IS NULL
        AND a.isDeleted = FALSE
        """)
    void removeCurrentAssignmentForEmployee(
            @Param("projectId") UUID projectId,
            @Param("employeeId") UUID employeeId,
            @Param("role") ProjectRoleType role,
            @Param("removedAt") Instant removedAt,
            @Param("removedBy") String removedBy
    );
}