package com.thewizecompany.wizevision.shared.security;

import com.thewizecompany.wizevision.shared.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/*
 * JWT UTILITY
 *
 * Responsible for three things only:
 * 1. Generating access tokens
 * 2. Generating refresh tokens
 * 3. Validating and parsing tokens
 *
 * TWO SEPARATE SECRETS — WHY?
 *
 * Access token secret:
 *   Signs short-lived access tokens (15 min)
 *   Used on every API request
 *
 * Refresh token secret:
 *   Signs long-lived refresh tokens (7 days)
 *   Used ONLY at /api/v1/auth/refresh
 *
 * If both tokens used the same secret:
 *   An attacker who steals a refresh token could
 *   use it as an access token (same signature = valid)
 *   Two secrets = refresh token cannot be used as access token
 *
 * WHAT IS A JTI (JWT ID)?
 *   A unique UUID inside every token.
 *   Used for the token blacklist on logout.
 *   When user logs out, we store the JTI in Redis.
 *   JwtAuthFilter checks Redis before accepting any token.
 *   Without JTI, logout is impossible with stateless JWT.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;
    private final AppProperties appProperties;

    /*
     * Keys are built once at construction time.
     * Building a key is expensive — do it once, reuse forever.
     * Keys.hmacShaKeyFor requires minimum 256 bits (32 bytes).
     * Our secrets in application.yml are longer than 32 chars.
     */
    public JwtUtil(AppProperties appProperties) {
        this.appProperties = appProperties;

        this.accessTokenKey = Keys.hmacShaKeyFor(
                appProperties.getSecurity()
                        .getJwt()
                        .getAccessTokenSecret()
                        .getBytes(StandardCharsets.UTF_8)
        );

        this.refreshTokenKey = Keys.hmacShaKeyFor(
                appProperties.getSecurity()
                        .getJwt()
                        .getRefreshTokenSecret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    // ─────────────────────────────────────────────────────────
    // ACCESS TOKEN
    // ─────────────────────────────────────────────────────────

    /*
     * Generates an access token containing:
     *   sub   = employeeId (UUID as string) — who this token belongs to
     *   email = employee email — for auditing and display
     *   role  = employee role — for authorization decisions
     *   jti   = unique token ID — for blacklist on logout
     *   iat   = issued at timestamp
     *   exp   = expiry timestamp (now + 15 minutes)
     *
     * We store employeeId as subject (sub) not email because:
     * - Email can change (employee updates profile)
     * - UUID never changes
     * - Services that receive the token use UUID to query DB
     */
    public String generateAccessToken(
            UUID employeeId,
            String email,
            String role) {

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())       // jti
                .subject(employeeId.toString())          // sub
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(
                        now + appProperties.getSecurity()
                                .getJwt()
                                .getAccessTokenExpiryMs()
                ))
                .signWith(accessTokenKey)
                .compact();
    }

    /*
     * Generates a refresh token containing:
     *   sub = employeeId
     *   jti = unique token ID (stored in DB for rotation tracking)
     *
     * Refresh tokens contain MINIMAL claims.
     * They are only used to get new access tokens.
     * They never flow through regular API endpoints.
     */
    public String generateRefreshToken(UUID employeeId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(employeeId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(
                        now + appProperties.getSecurity()
                                .getJwt()
                                .getRefreshTokenExpiryMs()
                ))
                .signWith(refreshTokenKey)
                .compact();
    }

    // ─────────────────────────────────────────────────────────
    // VALIDATION AND PARSING
    // ─────────────────────────────────────────────────────────

    /*
     * Validates and parses an access token.
     * Returns Claims (the payload) if valid.
     * Throws specific exceptions for different failure modes
     * so callers can handle expired vs invalid differently.
     */
    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(accessTokenKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /*
     * Validates and parses a refresh token.
     * Uses the REFRESH key — not the access key.
     * An access token presented here will fail validation.
     */
    public Claims validateRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(refreshTokenKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ─────────────────────────────────────────────────────────
    // CLAIM EXTRACTORS
    // ─────────────────────────────────────────────────────────

    /*
     * These convenience methods extract specific values
     * from the Claims object returned by validation.
     *
     * Usage in JwtAuthFilter:
     *   Claims claims = jwtUtil.validateAccessToken(token);
     *   UUID employeeId = jwtUtil.extractEmployeeId(claims);
     *   String role = jwtUtil.extractRole(claims);
     */

    public UUID extractEmployeeId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    public Date extractExpiration(Claims claims) {
        return claims.getExpiration();
    }

    /*
     * Checks if token is expired WITHOUT throwing an exception.
     * Useful for refresh token logic where you want to
     * check expiry gracefully rather than catching exceptions.
     */
    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /*
     * Returns remaining validity in milliseconds.
     * Used when blacklisting a token — we set the Redis TTL
     * to this value so the blacklist entry expires exactly
     * when the token would have expired anyway.
     * No point keeping blacklist entries for expired tokens.
     */
    public long getRemainingValidityMs(Claims claims) {
        long expiry = claims.getExpiration().getTime();
        long now = System.currentTimeMillis();
        return Math.max(0, expiry - now);
    }
}
