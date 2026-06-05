package com.thewizecompany.wizevision.auth.dto;

import com.thewizecompany.wizevision.employee.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/*
 * INTERNAL AUTH RESULT
 *
 * Returned by AuthService.login() and AuthService.refresh()
 * to the controller. Carries BOTH tokens so the controller
 * can:
 *   - set the refresh token as an httpOnly cookie
 *   - put the access token in the JSON response body
 *
 * This object NEVER leaves the server. It is never
 * serialized to JSON. Only AuthResponse (which has no
 * refreshToken field) is sent to the client.
 */
@Getter
@Builder
public class AuthResult {

    // ── Tokens ───────────────────────────────────────────────

    /** Short-lived JWT. Goes in the JSON response body. */
    private final String accessToken;

    /**
     * Long-lived opaque token.
     * Controller sets this as an httpOnly cookie.
     * Never written into AuthResponse.
     */
    private final String refreshToken;

    // ── Employee data ────────────────────────────────────────

    private final long accessTokenExpiresInMs;
    private final UUID employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String email;
    private final Role role;
    private final boolean mustChangePassword;
}