package com.thewizecompany.wizevision.auth.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

/*
 * REFRESH TOKEN ENTITY
 *
 * Stores hashed refresh tokens in the database.
 *
 * WHY STORE REFRESH TOKENS IN DATABASE?
 *
 * Access tokens are stateless — validated by signature only.
 * We do NOT store them (except blacklist in Redis on logout).
 *
 * Refresh tokens are stored because:
 * 1. TOKEN ROTATION — when refresh token is used,
 *    old one is invalidated and new one is issued.
 *    Database tracks which tokens are valid.
 *
 * 2. FORCED LOGOUT — admin can revoke all refresh tokens
 *    for an employee (e.g., account compromised).
 *    Cannot do this with stateless tokens.
 *
 * 3. DEVICE TRACKING — each device gets its own refresh token.
 *    User can see and revoke specific device sessions.
 *
 * WHY STORE HASH NOT PLAIN TOKEN?
 * If database is compromised, attacker cannot use the tokens.
 * We hash with SHA-256 before storing.
 * When refresh request comes in, we hash the provided token
 * and compare with stored hash — same pattern as passwords.
 *
 * TABLE: auth_refresh_tokens
 */
@Entity
@Table(
        name = "auth_refresh_tokens",
        indexes = {
                @Index(
                        name = "idx_refresh_tokens_token_hash",
                        columnList = "token_hash",
                        unique = true
                ),
                @Index(
                        name = "idx_refresh_tokens_employee_id",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_refresh_tokens_expires_at",
                        columnList = "expires_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity implements Persistable<UUID> {

    /*
     * The employee this token belongs to.
     * Not a @ManyToOne relationship intentionally —
     * we use UUID directly to avoid lazy loading issues
     * and keep the auth module self-contained.
     */
    @Column(
            name = "employee_id",
            nullable = false
    )
    private UUID employeeId;

    /*
     * SHA-256 hash of the actual refresh token.
     * We never store the plain token.
     */
    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    /*
     * Soft revocation — set to true when:
     * - Token is used (rotation: old invalidated, new issued)
     * - User logs out from this device
     * - Admin force-revokes all sessions
     * - Password is changed
     */
    @Column(
            name = "is_revoked",
            nullable = false
    )
    @Builder.Default
    private boolean isRevoked = false;

    /*
     * Device/browser identification for session management.
     * Allows user to see "logged in from Chrome on Windows"
     * in their active sessions list.
     */
    @Column(
            name = "device_info",
            length = 500
    )
    private String deviceInfo;

    @Column(
            name = "ip_address",
            length = 45
    )
    private String ipAddress;

    // ── Persistable ──────────────────────────────────────────────

    /*
     * CRITICAL: Tells Spring Data JPA whether to call persist() or merge().
     *
     * Without this, SimpleJpaRepository.save() looks at the @Version field
     * inherited from BaseEntity. BaseEntity initialises version = 0L, and
     * Lombok's @AllArgsConstructor (generated for @Builder) carries that 0L
     * into every new RefreshToken instance. A non-null version makes Spring
     * Data think the entity already exists, so it calls merge() instead of
     * persist(). merge() on an entity with id=null does nothing — no INSERT,
     * no error, silent skip. The token is never written to the database.
     *
     * Implementing Persistable and returning id == null bypasses that check
     * entirely. New entities always get persist(), existing ones get merge().
     */
    @Override
    public boolean isNew() {
        return getId() == null;
    }

    // ── HELPER METHODS ────────────────────────────────────────

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isRevoked && !isExpired();
    }

    public void revoke() {
        this.isRevoked = true;
    }
}