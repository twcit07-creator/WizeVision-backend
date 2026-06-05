package com.thewizecompany.wizevision.client.repository;

import com.thewizecompany.wizevision.client.domain.Client;
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
public interface ClientRepository
        extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByCompanyNameIgnoreCaseAndIsDeletedFalse(
            String companyName
    );

    boolean existsByCompanyCodeAndIsDeletedFalse(
            String companyCode
    );

    /*
     * Native SQL query instead of JPQL.
     * Reason: PostgreSQL cannot infer the type of a
     * nullable JPQL parameter (:search IS NULL).
     * It treats null as bytea instead of varchar.
     * lower(bytea) does not exist → error.
     *
     * Fix: Cast the parameter explicitly using
     * CAST(:search AS VARCHAR) in native SQL.
     * PostgreSQL now knows the type regardless of null.
     *
     * countQuery is required when using native SQL
     * with Spring Data pagination.
     */
    @Query(
            value = """
        SELECT * FROM clients
        WHERE is_deleted = FALSE
        AND (
            CAST(:search AS VARCHAR) IS NULL
            OR LOWER(company_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(company_code) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(city, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
        ORDER BY company_name ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM clients
        WHERE is_deleted = FALSE
        AND (
            CAST(:search AS VARCHAR) IS NULL
            OR LOWER(company_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(company_code) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(city, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
        """,
            nativeQuery = true
    )
    Page<Client> searchClients(
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    /*
     * Count active clients — used for dashboard stats.
     */
    long countByIsActiveTrueAndIsDeletedFalse();

    /*
     * Used for generating next company code.
     * WC-001, WC-002, etc.
     */
    long countByIsDeletedFalse();

    @Query("""
    SELECT c FROM Client c
    WHERE c.isDeleted = FALSE
    AND c.isActive = TRUE
    ORDER BY c.companyName ASC
    """)
    List<Client> findAllActiveForDropdown();
}