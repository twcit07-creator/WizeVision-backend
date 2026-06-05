package com.thewizecompany.wizevision.auth.controller;

import com.thewizecompany.wizevision.auth.dto.LoginRequest;
import com.thewizecompany.wizevision.auth.dto.AuthResult;
import com.thewizecompany.wizevision.auth.dto.AuthResponse;
import com.thewizecompany.wizevision.auth.service.AuthService;
import com.thewizecompany.wizevision.shared.config.AppProperties;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AppProperties appProperties;

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    // ─────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        /*
         * Pass IP address and User-Agent to the service.
         * AuthService no longer takes HttpServletRequest
         * directly — it receives extracted strings only.
         */
        AuthResult result = authService.login(
                request,
                extractIpAddress(httpRequest),
                extractDeviceInfo(httpRequest)
        );

        /*
         * Set refresh token as httpOnly cookie.
         * Frontend never sees or touches this value.
         * Browser sends it automatically on every
         * request to /api/v1/auth/refresh.
         */
        httpResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                buildRefreshCookie(result.getRefreshToken()).toString()
        );

        /*
         * Return access token in JSON body.
         * Frontend stores this in memory (preferred) or localStorage.
         * Access token is short-lived (15 min) so even if XSS
         * steals it, the damage window is small.
         *
         * refreshToken is intentionally NOT included here.
         */
        return ResponseEntity.ok(
                ApiResponse.ok(
                        toAuthResponse(result),
                        "Login successful"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // REFRESH
    // ─────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        /*
         * Read refresh token from httpOnly cookie.
         * Browser sends this automatically — frontend does
         * not need to do anything special.
         */
        String refreshToken = extractRefreshCookie(request);

        if (refreshToken == null) {
            return ResponseEntity
                    .status(401)
                    .body(ApiResponse.error(
                            "Refresh token not found. Please login again.",
                            "REFRESH_TOKEN_MISSING"
                    ));
        }

        AuthResult result = authService.refresh(
                refreshToken,
                extractIpAddress(request),
                extractDeviceInfo(request)
        );

        /*
         * Rotate the refresh token cookie.
         * Old token is replaced with a new one.
         * This is refresh token rotation —
         * each refresh token can only be used once.
         */
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildRefreshCookie(result.getRefreshToken()).toString()
        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        toAuthResponse(result),
                        "Token refreshed"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal String email,
            HttpServletRequest request,
            HttpServletResponse response) {

        /*
         * Read the refresh token from the cookie
         * so we can revoke it in the database.
         */
        String refreshToken = extractRefreshCookie(request);

        /*
         * The access token comes from the Authorization header.
         * Spring Security has already validated it by this point;
         * we extract the raw value here just to blacklist it.
         */
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        authService.logout(accessToken, refreshToken);

        /*
         * Clear the refresh token cookie.
         * maxAge(0) tells the browser to delete it immediately.
         */
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                clearRefreshCookie().toString()
        );

        return ResponseEntity.ok(
                ApiResponse.ok("Logged out successfully")
        );
    }

    // ─────────────────────────────────────────────────────────
    // MAPPING HELPER
    // ─────────────────────────────────────────────────────────

    /*
     * Convert the internal AuthResult (which carries the raw
     * refresh token) into the public AuthResponse (which does
     * not). This is the single place where the refresh token
     * is deliberately dropped from the outbound JSON.
     */
    private AuthResponse toAuthResponse(AuthResult result) {
        return AuthResponse.builder()
                .accessToken(result.getAccessToken())
                .tokenType("Bearer")
                .accessTokenExpiresInMs(result.getAccessTokenExpiresInMs())
                .employeeId(result.getEmployeeId())
                .employeeCode(result.getEmployeeCode())
                .fullName(result.getFullName())
                .email(result.getEmail())
                .role(result.getRole())
                .mustChangePassword(result.isMustChangePassword())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // COOKIE HELPERS
    // ─────────────────────────────────────────────────────────

    private ResponseCookie buildRefreshCookie(String refreshToken) {

        boolean isProduction = isProductionEnvironment();

        return ResponseCookie
                .from(REFRESH_COOKIE_NAME,
                        URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                /*
                 * httpOnly = true
                 * JavaScript cannot read this cookie.
                 * document.cookie will not show it.
                 * XSS attacks cannot steal it.
                 */
                .httpOnly(true)
                /*
                 * secure = true in production (HTTPS only).
                 * secure = false in dev (HTTP localhost).
                 * In production, cookie is never sent over HTTP.
                 */
                .secure(isProduction)
                /*
                 * SameSite=Lax allows the cookie to be sent
                 * when the user navigates to your site from
                 * another site (e.g. clicking a link).
                 * It blocks cross-site POST requests (CSRF protection).
                 *
                 * SameSite=Strict is too restrictive —
                 * blocks the cookie even on normal navigation.
                 *
                 * SameSite=None requires secure=true and allows
                 * all cross-site requests — use only if your
                 * frontend and backend are on different domains.
                 */
                .sameSite("Lax")
                /*
                 * path restricts which URLs receive the cookie.
                 * Only requests to /api/v1/auth/* will include
                 * the refresh token cookie.
                 */
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(
                        appProperties.getSecurity()
                                .getJwt()
                                .getRefreshTokenExpiryMs()
                ))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie
                .from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isProductionEnvironment())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)  // tells browser to delete immediately
                .build();
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String extractIpAddress(HttpServletRequest request) {
        /*
         * X-Forwarded-For is set by nginx/load balancer
         * and contains the real client IP.
         * Without this, we would always see the proxy IP.
         */
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "Unknown";
        return userAgent.length() > 500
                ? userAgent.substring(0, 500)
                : userAgent;
    }

    private boolean isProductionEnvironment() {
        /*
         * Check active Spring profile.
         * Cookie is secure only in production.
         * In dev (localhost HTTP) secure=false is needed
         * because localhost does not have HTTPS.
         */
        String profile = System.getProperty(
                "spring.profiles.active", "dev"
        );
        return profile.contains("prod");
    }
}