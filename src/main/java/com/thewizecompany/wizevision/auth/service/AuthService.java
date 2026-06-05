package com.thewizecompany.wizevision.auth.service;

import com.thewizecompany.wizevision.auth.domain.RefreshToken;
import com.thewizecompany.wizevision.auth.dto.AuthResult;
import com.thewizecompany.wizevision.auth.dto.LoginRequest;
import com.thewizecompany.wizevision.auth.repository.RefreshTokenRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.shared.config.AppProperties;
import com.thewizecompany.wizevision.shared.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/*
 * AUTH SERVICE
 *
 * Handles all authentication operations:
 * - Login (credential validation + token generation)
 * - Logout (token blacklisting + refresh revocation)
 * - Token refresh (rotation)
 *
 * KEY CHANGE: login() and refresh() now return AuthResult,
 * not AuthResponse. AuthResult carries the raw refresh token
 * internally so the controller can set it as an httpOnly
 * cookie. AuthResponse (the JSON sent to the client) never
 * contains the refresh token.
 *
 * SECURITY DECISIONS:
 *
 * 1. GENERIC ERROR MESSAGES
 *    We never tell the user whether email or password
 *    was wrong — just "Invalid credentials".
 *    If we said "email not found", attackers could enumerate
 *    valid email addresses.
 *
 * 2. FAILED ATTEMPT TRACKING
 *    Tracked in the Employee entity, not Redis.
 *    Persists across server restarts.
 *    Auto-unlocks after 30 minutes (see Employee.recordFailedLogin)
 *
 * 3. REFRESH TOKEN ROTATION
 *    Every refresh issues a NEW refresh token and invalidates
 *    the old one. If an attacker steals a refresh token,
 *    using it will invalidate it, and the legitimate user's
 *    next refresh attempt will fail — alerting them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;
    private final RedisTemplate<String, String> redisTemplate;

    // ─────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────

    /*
     * Returns AuthResult (internal DTO) — NOT AuthResponse.
     *
     * AuthResult includes the raw refresh token so the
     * controller can write it into an httpOnly cookie.
     * The controller then builds AuthResponse (no refresh
     * token) for the JSON body sent to the client.
     */
    @Transactional
    public AuthResult login(
            LoginRequest request,
            String ipAddress,
            String deviceInfo) {

        String email = request.getEmail()
                .toLowerCase()
                .trim();

        /*
         * STEP 1: Authenticate credentials
         *
         * AuthenticationManager calls CustomUserDetailsService
         * to load the user, then BCrypt-compares passwords.
         *
         * Throws:
         * - BadCredentialsException    → wrong password
         * - LockedException            → account locked
         * - DisabledException          → account inactive
         * - UsernameNotFoundException  → email not found
         *   (mapped to BadCredentialsException by Spring)
         */
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );
        } catch (LockedException e) {
            log.warn("Login attempt on locked account: {} from IP: {}",
                    email, ipAddress);
            /*
             * We throw with a generic message to avoid
             * revealing account existence to attackers.
             * The lock message is acceptable because it
             * confirms the account exists — but at this point
             * they already failed 5 times, so they know.
             */
            throw new LockedException(
                    "Account is temporarily locked due to multiple "
                            + "failed login attempts. Try again in 30 minutes."
            );
        } catch (DisabledException e) {
            log.warn("Login attempt on disabled account: {}",
                    email);
            throw new DisabledException(
                    "Your account has been deactivated. "
                            + "Please contact HR."
            );
        } catch (AuthenticationException e) {
            /*
             * Record failed attempt for brute force protection.
             * We load the employee here only to increment counter.
             * If employee doesn't exist, we do nothing
             * (still return same generic error).
             */
            employeeRepository
                    .findByEmailAndIsDeletedFalseAndIsActiveTrue(email)
                    .ifPresent(emp -> {
                        emp.recordFailedLogin();
                        employeeRepository.save(emp);
                        log.warn(
                                "Failed login attempt {}/5 for: {}",
                                emp.getFailedLoginAttempts(),
                                email
                        );
                    });

            throw new BadCredentialsException("Invalid credentials");
        }

        /*
         * STEP 2: Load full employee entity
         *
         * Authentication succeeded — load the employee
         * to get all fields needed for the response and tokens.
         */
        Employee employee = employeeRepository
                .findByEmailAndIsDeletedFalseAndIsActiveTrue(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid credentials")
                );

        /*
         * STEP 3: Generate tokens
         */
        String accessToken = jwtUtil.generateAccessToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        String rawRefreshToken = jwtUtil.generateRefreshToken(
                employee.getId()
        );

        /*
         * STEP 4: Store hashed refresh token in database
         *
         * We never store the raw token — only its SHA-256 hash.
         * If the database is breached, attackers get hashes,
         * not usable tokens.
         */
        String tokenHash = hashToken(rawRefreshToken);
        long refreshExpiryMs = appProperties.getSecurity()
                .getJwt()
                .getRefreshTokenExpiryMs();

        RefreshToken refreshToken = RefreshToken.builder()
                .employeeId(employee.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(refreshExpiryMs))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.saveAndFlush(refreshToken);
        log.debug("Refresh token saved for employee: {}", email);

        /*
         * STEP 5: Record successful login (last-login timestamp, IP, reset failed attempts).
         *
         * IMPORTANT: wrapped in try/catch so that a failure here
         * (stale version, missing column, constraint issue) does NOT
         * roll back the entire @Transactional and un-save the refresh token.
         * Login-tracking is audit data — it must never block authentication.
         */
        try {
            employeeRepository.updateLoginSuccess(
                    employee.getId(),
                    ipAddress
            );
        } catch (Exception e) {
            /*
             * Log loudly — this needs to be fixed — but do not rethrow.
             * The user has authenticated successfully and their refresh
             * token is already persisted. Rethrowing here would roll back
             * the transaction, delete the token row, and cause every
             * subsequent /refresh call to return 401.
             */
            log.error(
                    "updateLoginSuccess FAILED for employee {} — " +
                            "login tracking is broken but auth will proceed. " +
                            "Fix the underlying cause immediately. Exception: {}",
                    email, e.getMessage(), e
            );
        }

        log.info("Successful login: {} from IP: {}", email, ipAddress);

        /*
         * STEP 6: Return AuthResult (includes refresh token).
         * Controller extracts refreshToken → cookie.
         * Controller builds AuthResponse (no refreshToken) → JSON body.
         */
        return AuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)          // → httpOnly cookie in controller
                .accessTokenExpiresInMs(
                        appProperties.getSecurity()
                                .getJwt()
                                .getAccessTokenExpiryMs()
                )
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .mustChangePassword(employee.isMustChangePassword())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        /*
         * STEP 1: Blacklist the access token in Redis
         *
         * Even though it expires in 15 minutes,
         * we blacklist it so it cannot be used during
         * that remaining window after logout.
         *
         * TTL = remaining validity of the access token.
         * When token expires, Redis auto-removes the entry.
         * No manual cleanup needed.
         */
        try {
            Claims claims = jwtUtil.validateAccessToken(accessToken);
            String jti = jwtUtil.extractJti(claims);
            long remainingMs = jwtUtil.getRemainingValidityMs(claims);

            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + jti,
                        "logout",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            /*
             * If access token is already expired or invalid,
             * we still proceed to revoke the refresh token.
             * No need to blacklist an already-expired token.
             */
            log.debug("Access token invalid during logout: {}",
                    e.getMessage());
        }

        /*
         * STEP 2: Revoke the refresh token in database
         */
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = hashToken(refreshToken);
            refreshTokenRepository
                    .findByTokenHashAndIsRevokedFalseAndIsDeletedFalse(
                            tokenHash
                    )
                    .ifPresent(rt -> {
                        rt.revoke();
                        refreshTokenRepository.save(rt);
                    });
        }

        log.info("User logged out successfully");
    }

    // ─────────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────────

    /*
     * Returns AuthResult (internal DTO) — NOT AuthResponse.
     * Same pattern as login(). Controller handles cookie rotation.
     */
    @Transactional
    public AuthResult refresh(
            String rawRefreshToken,
            String ipAddress,
            String deviceInfo) {

        /*
         * STEP 1: Validate the refresh token signature and expiry
         */
        Claims claims;
        try {
            claims = jwtUtil.validateRefreshToken(rawRefreshToken);
        } catch (Exception e) {
            throw new BadCredentialsException(
                    "Invalid or expired refresh token"
            );
        }

        /*
         * STEP 2: Check database — is it valid and not revoked?
         */
        String tokenHash = hashToken(rawRefreshToken);
        log.debug("Refresh lookup — token prefix: {}, hash: {}",
                rawRefreshToken.substring(0, Math.min(10, rawRefreshToken.length())),
                tokenHash.substring(0, 16));

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndIsRevokedFalseAndIsDeletedFalse(
                        tokenHash
                )
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Refresh token not found or already revoked"
                        )
                );

        if (storedToken.isExpired()) {
            storedToken.revoke();
            refreshTokenRepository.save(storedToken);
            throw new BadCredentialsException(
                    "Refresh token has expired. Please login again."
            );
        }

        /*
         * STEP 3: Load employee
         */
        Employee employee = employeeRepository
                .findByIdAndIsDeletedFalse(storedToken.getEmployeeId())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid credentials")
                );

        if (!employee.isActive()) {
            throw new DisabledException(
                    "Your account has been deactivated."
            );
        }

        /*
         * STEP 4: ROTATE — revoke old, issue new
         *
         * This is the key security feature.
         * Old refresh token is invalidated immediately.
         * New tokens are issued.
         *
         * If attacker stole the old token and tries to use it
         * after the legitimate user already refreshed,
         * the old token will be revoked and their request fails.
         */
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        // Generate new token pair
        String newAccessToken = jwtUtil.generateAccessToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        String newRawRefreshToken = jwtUtil.generateRefreshToken(
                employee.getId()
        );

        // Store new refresh token hash
        long refreshExpiryMs = appProperties.getSecurity()
                .getJwt()
                .getRefreshTokenExpiryMs();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .employeeId(employee.getId())
                .tokenHash(hashToken(newRawRefreshToken))
                .expiresAt(
                        Instant.now().plusMillis(refreshExpiryMs)
                )
                .deviceInfo(deviceInfo != null ? deviceInfo : storedToken.getDeviceInfo())
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.saveAndFlush(newRefreshToken);

        log.debug("Token refreshed for employee: {}",
                employee.getEmail());

        return AuthResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)       // → rotated cookie in controller
                .accessTokenExpiresInMs(
                        appProperties.getSecurity()
                                .getJwt()
                                .getAccessTokenExpiryMs()
                )
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .mustChangePassword(employee.isMustChangePassword())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    /*
     * SHA-256 hash of the refresh token for safe storage.
     * Same token always produces the same hash —
     * deterministic so we can look it up later.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest
                    .getInstance("SHA-256");
            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "SHA-256 not available", e
            );
        }
    }
}