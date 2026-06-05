package com.thewizecompany.wizevision.marketing.repository;

import com.thewizecompany.wizevision.marketing.domain.ProjectInquiry;
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
public interface ProjectInquiryRepository
        extends JpaRepository<ProjectInquiry, UUID> {

    Optional<ProjectInquiry> findByIdAndIsDeletedFalse(UUID id);

    /*
     * All inquiries forwarded to a specific PM.
     * Used in PM dashboard: "Inquiries waiting for my bid"
     */
    List<ProjectInquiry> findByForwardedToIdAndStatusAndIsDeletedFalse(
            UUID forwardedToId,
            com.thewizecompany.wizevision.marketing.domain.InquiryStatus status
    );

    @Query(
            value = """
            SELECT * FROM project_inquiries
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(project_name)
                    LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(inquiry_number)
                    LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:forwardedToId AS VARCHAR) IS NULL
                 OR forwarded_to_id =
                    CAST(:forwardedToId AS UUID))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM project_inquiries
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(project_name)
                    LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(inquiry_number)
                    LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:forwardedToId AS VARCHAR) IS NULL
                 OR forwarded_to_id =
                    CAST(:forwardedToId AS UUID))
            """,
            nativeQuery = true
    )
    Page<ProjectInquiry> searchInquiries(
            @Param("search") String search,
            @Param("status") String status,
            @Param("forwardedToId") String forwardedToId,
            Pageable pageable
    );

    long countByIsDeletedFalse();
}