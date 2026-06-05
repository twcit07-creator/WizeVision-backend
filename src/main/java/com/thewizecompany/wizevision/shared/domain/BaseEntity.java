package com.thewizecompany.wizevision.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /*
     * PRIMARY KEY
     * UUID instead of Long for three reasons:
     * 1. Security — attackers cannot enumerate records
     *    (GET /invoices/1, /invoices/2 reveals your business volume)
     * 2. Distributed-ready — IDs never collide if you split later
     * 3. Professional standard for modern enterprise applications
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            updatable = false,
            nullable = false,
            columnDefinition = "UUID"
    )
    private UUID id;

    /*
     * AUDIT TIMESTAMPS
     * Instant instead of LocalDateTime for one critical reason:
     * Instant is timezone-aware (UTC internally).
     * LocalDateTime has no timezone — causes bugs when
     * your server timezone differs from your database timezone.
     * Always store time as Instant, display as local in frontend.
     */
    @CreatedDate
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @LastModifiedDate
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    /*
     * AUDIT USER TRACKING
     * These are auto-populated by AuditorAware bean (JpaConfig)
     * which reads the current logged-in user from SecurityContext.
     * Every create/update automatically records who did it.
     */
    @CreatedBy
    @Column(
            name = "created_by",
            updatable = false,
            length = 100
    )
    private String createdBy;

    @LastModifiedBy
    @Column(
            name = "updated_by",
            length = 100
    )
    private String updatedBy;

    /*
     * SOFT DELETE
     * We never physically delete records in production.
     * Reasons:
     * 1. Audit trail — you need to know what existed
     * 2. Recovery — accidental deletes can be undone
     * 3. Referential integrity — deleting a client that
     *    has invoices would break foreign keys
     *
     * How it works:
     * isDeleted = false → normal record, appears in queries
     * isDeleted = true  → logically deleted, hidden from queries
     *
     * Your repositories will use:
     * findByIdAndIsDeletedFalse() instead of findById()
     */
    @Column(
            name = "is_deleted",
            nullable = false
    )
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    /*
     * OPTIMISTIC LOCKING
     * Prevents the "lost update" problem:
     *
     * Without version:
     * User A reads record (name = "ABC")
     * User B reads same record (name = "ABC")
     * User A saves (name = "XYZ") → version stays 1
     * User B saves (name = "DEF") → overwrites User A silently
     * User A's change is LOST with no error
     *
     * With version:
     * User A reads record (version = 1)
     * User B reads same record (version = 1)
     * User A saves → version becomes 2
     * User B tries to save with version 1
     * Database rejects → OptimisticLockException thrown
     * User B gets an error, not a silent data loss
     *
     * This is critical for bid amounts, invoice totals, etc.
     */
    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /*
     * SOFT DELETE HELPER METHOD
     * Call this instead of repository.delete()
     * Sets isDeleted = true and records who deleted it and when.
     */
    public void markAsDeleted(String deletedByUser) {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUser;
    }
}