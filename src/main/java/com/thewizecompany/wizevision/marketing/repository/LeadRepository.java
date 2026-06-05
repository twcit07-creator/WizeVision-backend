package com.thewizecompany.wizevision.marketing.repository;

import com.thewizecompany.wizevision.marketing.domain.Lead;
import com.thewizecompany.wizevision.marketing.domain.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository
        extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByLeadNumberAndIsDeletedFalse(
            String leadNumber
    );

    /*
     * Search with native SQL to avoid PostgreSQL
     * bytea type inference issue with nullable params.
     */
    @Query(
            value = """
            SELECT * FROM leads
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(company_name)
                    LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(contact_name)
                    LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(lead_number)
                    LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:assignedToId AS VARCHAR) IS NULL
                 OR assigned_to_id = CAST(:assignedToId AS UUID))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM leads
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(company_name)
                    LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(contact_name)
                    LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(lead_number)
                    LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:assignedToId AS VARCHAR) IS NULL
                 OR assigned_to_id = CAST(:assignedToId AS UUID))
            """,
            nativeQuery = true
    )
    Page<Lead> searchLeads(
            @Param("search") String search,
            @Param("status") String status,
            @Param("assignedToId") String assignedToId,
            Pageable pageable
    );

    long countByStatusAndIsDeletedFalse(LeadStatus status);

    long countByAssignedToIdAndIsDeletedFalse(UUID assignedToId);

    long countByIsDeletedFalse();
}