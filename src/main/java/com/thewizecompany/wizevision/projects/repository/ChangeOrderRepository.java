package com.thewizecompany.wizevision.projects.repository;

import com.thewizecompany.wizevision.projects.domain.ChangeOrder;
import com.thewizecompany.wizevision.projects.domain.ChangeOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeOrderRepository
        extends JpaRepository<ChangeOrder, UUID> {

    Optional<ChangeOrder> findByIdAndIsDeletedFalse(UUID id);

    List<ChangeOrder> findByProjectIdAndIsDeletedFalse(
            UUID projectId
    );

    long countByProjectIdAndIsDeletedFalse(UUID projectId);

    /*
     * Sum of all APPROVED change orders for a project.
     * Used to update project.changeOrdersTotal.
     */
    @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM ChangeOrder c
        WHERE c.projectId = :projectId
        AND c.status = 'APPROVED'
        AND c.isDeleted = FALSE
        """)
    BigDecimal sumApprovedAmountByProject(
            @Param("projectId") UUID projectId
    );

}