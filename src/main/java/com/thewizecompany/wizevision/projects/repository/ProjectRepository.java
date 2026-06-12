package com.thewizecompany.wizevision.projects.repository;

import com.thewizecompany.wizevision.projects.domain.Project;
import com.thewizecompany.wizevision.projects.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository
        extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndIsDeletedFalse(UUID id);

    Optional<Project> findByBidIdAndIsDeletedFalse(UUID bidId);

    Optional<Project> findByProjectNumberAndIsDeletedFalse(
            String projectNumber
    );

    /*
     * PM's project list.
     */
    List<Project> findByPmIdAndStatusAndIsDeletedFalse(
            UUID pmId,
            ProjectStatus status
    );

    /*
     * Employee's assigned projects.
     * Used in employee dashboard.
     * Joins through ProjectAssignment for team members
     * (modeler, editor, checker, team lead),
     * and also includes projects where the employee is PM.
     */
    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN ProjectAssignment pa
            ON pa.projectId = p.id
            AND pa.employeeId = :employeeId
            AND pa.removedAt IS NULL
            AND pa.isDeleted = FALSE
        WHERE p.isDeleted = FALSE
        AND (pa.employeeId = :employeeId
             OR p.pmId = :employeeId)
        AND p.status NOT IN (
            com.thewizecompany.wizevision.projects.domain.ProjectStatus.COMPLETED,
            com.thewizecompany.wizevision.projects.domain.ProjectStatus.CANCELLED
        )
        ORDER BY p.createdAt DESC
        """)
    List<Project> findActiveProjectsForEmployee(
            @Param("employeeId") UUID employeeId
    );

    @Query(
            value = """
            SELECT * FROM projects
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(project_name)
                    LIKE LOWER(CONCAT('%',:search,'%'))
                 OR LOWER(project_number)
                    LIKE LOWER(CONCAT('%',:search,'%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:clientId AS VARCHAR) IS NULL
                 OR client_id = CAST(:clientId AS UUID))
            AND (CAST(:pmId AS VARCHAR) IS NULL
                 OR pm_id = CAST(:pmId AS UUID))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM projects
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(project_name)
                    LIKE LOWER(CONCAT('%',:search,'%'))
                 OR LOWER(project_number)
                    LIKE LOWER(CONCAT('%',:search,'%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:clientId AS VARCHAR) IS NULL
                 OR client_id = CAST(:clientId AS UUID))
            AND (CAST(:pmId AS VARCHAR) IS NULL
                 OR pm_id = CAST(:pmId AS UUID))
            """,
            nativeQuery = true
    )
    Page<Project> searchProjects(
            @Param("search") String search,
            @Param("status") String status,
            @Param("clientId") String clientId,
            @Param("pmId") String pmId,
            Pageable pageable
    );

    long countByStatusAndIsDeletedFalse(ProjectStatus status);

    long countByIsDeletedFalse();

    long countByClientIdAndIsDeletedFalse(UUID id);
}