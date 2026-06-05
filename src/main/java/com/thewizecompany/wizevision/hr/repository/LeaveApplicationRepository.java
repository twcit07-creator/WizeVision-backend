package com.thewizecompany.wizevision.hr.repository;

import com.thewizecompany.wizevision.hr.domain.LeaveApplication;
import com.thewizecompany.wizevision.hr.domain.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveApplicationRepository
        extends JpaRepository<LeaveApplication, UUID> {

    Optional<LeaveApplication> findByIdAndIsDeletedFalse(
            UUID id
    );

    Page<LeaveApplication> findByEmployeeIdAndIsDeletedFalse(
            UUID employeeId,
            Pageable pageable
    );

    /*
     * Pending applications for a TL to review.
     */
    List<LeaveApplication> findByTlIdAndStatusAndIsDeletedFalse(
            UUID tlId,
            LeaveStatus status
    );

    /*
     * Pending applications for a PM to review
     * (already approved by TL).
     */
    List<LeaveApplication> findByPmIdAndStatusAndIsDeletedFalse(
            UUID pmId,
            LeaveStatus status
    );

    /*
     * All pending applications for HR.
     */
    List<LeaveApplication> findByStatusAndIsDeletedFalse(
            LeaveStatus status
    );

    /*
     * Check for overlapping leave.
     * Employee cannot apply for the same dates twice.
     */
    @Query("""
        SELECT COUNT(a) FROM LeaveApplication a
        WHERE a.employeeId = :employeeId
        AND a.isDeleted = FALSE
        AND a.status NOT IN ('REJECTED', 'CANCELLED')
        AND a.fromDate <= :toDate
        AND a.toDate >= :fromDate
        """)
    long countOverlappingLeaves(
            @Param("employeeId") UUID employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    long countByIsDeletedFalse();
}