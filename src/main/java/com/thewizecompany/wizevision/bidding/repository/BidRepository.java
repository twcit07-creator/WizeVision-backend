package com.thewizecompany.wizevision.bidding.repository;

import com.thewizecompany.wizevision.bidding.domain.Bid;
import com.thewizecompany.wizevision.bidding.domain.BidStatus;
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
public interface BidRepository
        extends JpaRepository<Bid, UUID> {

    Optional<Bid> findByIdAndIsDeletedFalse(UUID id);

    Optional<Bid> findByInquiryIdAndIsDeletedFalse(
            UUID inquiryId
    );

    List<Bid> findByCreatedByPmIdAndStatusAndIsDeletedFalse(
            UUID pmId,
            BidStatus status
    );

    /*
     * Admin view — bids waiting for review.
     */
    List<Bid> findByStatusAndIsDeletedFalse(BidStatus status);

    @Query(
            value = """
            SELECT * FROM bids
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(project_name)
                    LIKE LOWER(CONCAT('%',:search,'%'))
                 OR LOWER(bid_number)
                    LIKE LOWER(CONCAT('%',:search,'%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:clientId AS VARCHAR) IS NULL
                 OR client_id = CAST(:clientId AS UUID))
            AND (CAST(:pmId AS VARCHAR) IS NULL
                 OR created_by_pm_id = CAST(:pmId AS UUID))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM bids
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(project_name)
                    LIKE LOWER(CONCAT('%',:search,'%'))
                 OR LOWER(bid_number)
                    LIKE LOWER(CONCAT('%',:search,'%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:clientId AS VARCHAR) IS NULL
                 OR client_id = CAST(:clientId AS UUID))
            AND (CAST(:pmId AS VARCHAR) IS NULL
                 OR created_by_pm_id = CAST(:pmId AS UUID))
            """,
            nativeQuery = true
    )
    Page<Bid> searchBids(
            @Param("search") String search,
            @Param("status") String status,
            @Param("clientId") String clientId,
            @Param("pmId") String pmId,
            Pageable pageable
    );

    long countByStatusAndIsDeletedFalse(BidStatus status);

    long countByIsDeletedFalse();
}