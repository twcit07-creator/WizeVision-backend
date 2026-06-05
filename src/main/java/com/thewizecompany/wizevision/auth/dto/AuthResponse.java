package com.thewizecompany.wizevision.auth.dto;

import com.thewizecompany.wizevision.employee.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/*
 * AUTH RESPONSE DTO
 *
 * Returned after successful login or token refresh.
 *
 * DELIBERATELY MISSING: refreshToken
 * The refresh token is set as an httpOnly cookie by the
 * controller. It never appears in the JSON response body.
 * This prevents JavaScript (including XSS attacks) from
 * ever reading the refresh token.
 *
 * For the internal object that carries both tokens between
 * AuthService and AuthController, see AuthResult.
 */
@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final long accessTokenExpiresInMs;
    private final UUID employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String email;
    private final Role role;
    private final boolean mustChangePassword;
}