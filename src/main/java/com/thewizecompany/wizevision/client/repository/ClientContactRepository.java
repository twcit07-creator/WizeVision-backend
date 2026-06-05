package com.thewizecompany.wizevision.client.repository;

import com.thewizecompany.wizevision.client.domain.ClientContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientContactRepository
        extends JpaRepository<ClientContact, UUID> {

    List<ClientContact> findByClient_IdAndIsDeletedFalse(
            UUID clientId
    );

    List<ClientContact> findByClient_IdAndIsActiveTrueAndIsDeletedFalse(
            UUID clientId
    );

    Optional<ClientContact> findByIdAndIsDeletedFalse(UUID id);

    Optional<ClientContact> findByClient_IdAndIsPrimaryTrueAndIsDeletedFalse(
            UUID clientId
    );

    boolean existsByEmailAndIsDeletedFalse(String email);

    /*
     * Before setting a new primary contact,
     * we demote the existing primary.
     * This ensures only one primary contact per client.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ClientContact c
        SET c.isPrimary = FALSE
        WHERE c.client.id = :clientId
        AND c.isPrimary = TRUE
        AND c.isDeleted = FALSE
        """)
    void demotePrimaryContact(
            @Param("clientId") UUID clientId
    );
}