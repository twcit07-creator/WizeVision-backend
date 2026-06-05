package com.thewizecompany.wizevision.invoicing.repository;

import com.thewizecompany.wizevision.invoicing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceIdAndIsDeletedFalseOrderByPaymentDateDesc(
            UUID invoiceId
    );

    List<Payment> findByProjectIdAndIsDeletedFalse(
            UUID projectId
    );

    /*
     * Total payments received for an invoice.
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.invoiceId = :invoiceId
        AND p.isDeleted = FALSE
        """)
    BigDecimal sumByInvoiceId(
            @Param("invoiceId") UUID invoiceId
    );

    /*
     * Total payments received for a project.
     * Used in project financial summary.
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.projectId = :projectId
        AND p.isDeleted = FALSE
        """)
    BigDecimal sumByProjectId(
            @Param("projectId") UUID projectId
    );

    long countByIsDeletedFalse();
}