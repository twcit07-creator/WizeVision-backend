package com.thewizecompany.wizevision.invoicing.repository;

import com.thewizecompany.wizevision.invoicing.domain.Invoice;
import com.thewizecompany.wizevision.invoicing.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository
        extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndIsDeletedFalse(UUID id);

    List<Invoice> findByProjectIdAndIsDeletedFalse(
            UUID projectId
    );

    List<Invoice> findByClientIdAndIsDeletedFalse(
            UUID clientId
    );

    /*
     * Overdue invoices — due date passed and not fully paid.
     * Used for daily overdue check job.
     */
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.isDeleted = FALSE
        AND i.dueDate < :today
        AND i.status NOT IN ('PAID', 'CANCELLED')
        AND i.amountPaid < i.totalAmount
        """)
    List<Invoice> findOverdueInvoices(
            @Param("today") LocalDate today
    );

    @Query(
            value = """
            SELECT * FROM invoices
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(invoice_number)
                    LIKE LOWER(CONCAT('%',:search,'%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:clientId AS VARCHAR) IS NULL
                 OR client_id = CAST(:clientId AS UUID))
            AND (CAST(:projectId AS VARCHAR) IS NULL
                 OR project_id = CAST(:projectId AS UUID))
            ORDER BY invoice_date DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM invoices
            WHERE is_deleted = FALSE
            AND (CAST(:search AS VARCHAR) IS NULL
                 OR LOWER(invoice_number)
                    LIKE LOWER(CONCAT('%',:search,'%')))
            AND (CAST(:status AS VARCHAR) IS NULL
                 OR status = :status)
            AND (CAST(:clientId AS VARCHAR) IS NULL
                 OR client_id = CAST(:clientId AS UUID))
            AND (CAST(:projectId AS VARCHAR) IS NULL
                 OR project_id = CAST(:projectId AS UUID))
            """,
            nativeQuery = true
    )
    Page<Invoice> searchInvoices(
            @Param("search") String search,
            @Param("status") String status,
            @Param("clientId") String clientId,
            @Param("projectId") String projectId,
            Pageable pageable
    );

    /*
     * Total outstanding across all active invoices.
     * Used for finance dashboard.
     */
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount - i.amountPaid), 0)
        FROM Invoice i
        WHERE i.isDeleted = FALSE
        AND i.status NOT IN ('PAID', 'CANCELLED')
        """)
    BigDecimal getTotalOutstanding();

    long countByIsDeletedFalse();
}