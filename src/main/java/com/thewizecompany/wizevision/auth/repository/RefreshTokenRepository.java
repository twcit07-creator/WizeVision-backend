package com.thewizecompany.wizevision.auth.repository;

import com.thewizecompany.wizevision.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    /*
     * Primary lookup — used during token refresh.
     * Only finds valid (not revoked, not deleted) tokens.
     */
    Optional<RefreshToken> findByTokenHashAndIsRevokedFalseAndIsDeletedFalse(
            String tokenHash
    );

    /*
     * Revokes ALL active tokens for an employee.
     * Used when:
     * - Employee changes password
     * - Admin force-logs-out an employee
     * - Account is locked or disabled
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
        SET r.isRevoked = TRUE
        WHERE r.employeeId = :employeeId
        AND r.isRevoked = FALSE
        """)
    void revokeAllForEmployee(@Param("employeeId") UUID employeeId);

    /*
     * Cleanup job — deletes expired tokens nightly.
     * Keeps the table clean without accumulating old data.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        DELETE FROM RefreshToken r
        WHERE r.expiresAt < :now
        OR r.isRevoked = TRUE
        """)
    void deleteExpiredAndRevoked(@Param("now") Instant now);

    /*
     * Count active sessions for an employee.
     * Used in the "active sessions" feature
     * where employee can see all their logged-in devices.
     */
    long countByEmployeeIdAndIsRevokedFalseAndIsDeletedFalse(
            UUID employeeId
    );
}