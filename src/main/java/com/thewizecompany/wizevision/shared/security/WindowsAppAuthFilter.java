package com.thewizecompany.wizevision.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thewizecompany.wizevision.shared.config.AppProperties;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
 * WINDOWS APP AUTHENTICATION FILTER
 *
 * Guards all /api/v1/attendance/** endpoints.
 *
 * Every request to attendance endpoints MUST include:
 * Header: X-App-Key: <secret>
 *
 * This secret is:
 * - Stored in application config (environment variable)
 * - Embedded in the Windows app at build time
 * - Known only to the Windows app and the backend
 *
 * PURPOSE:
 * Prevents any other client (browser, Postman, other apps)
 * from calling attendance endpoints directly.
 * Only the genuine Windows attendance app can call these.
 *
 * This filter does NOT replace JWT auth.
 * Both must pass:
 * 1. X-App-Key header must be valid (this filter)
 * 2. JWT token must be valid (JwtAuthFilter)
 *
 * EXCEPTION: /attendance/auth/pin-login
 * This endpoint does not have a JWT yet (it's the login endpoint).
 * But it still requires X-App-Key.
 */
@Slf4j
@RequiredArgsConstructor
public class WindowsAppAuthFilter extends OncePerRequestFilter {

    private static final String APP_KEY_HEADER = "X-App-Key";
    private static final String ATTENDANCE_PATH =
            "/api/v1/attendance";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String providedKey = request.getHeader(APP_KEY_HEADER);
        String expectedKey = appProperties
                .getSecurity()
                .getWindowsApp()
                .getApiKey();

        /*
         * Compare keys using constant-time comparison.
         * Regular .equals() is vulnerable to timing attacks:
         * Attacker measures response time to guess key length.
         * MessageDigest.isEqual uses constant time regardless
         * of where characters differ.
         */
        if (providedKey == null
                || !constantTimeEquals(providedKey, expectedKey)) {

            log.warn(
                    "Invalid or missing X-App-Key for attendance " +
                            "endpoint from IP: {}",
                    request.getRemoteAddr()
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<?> body = ApiResponse.error(
                    "This endpoint is only accessible from " +
                            "the WizeVision Attendance application.",
                    "INVALID_APP_KEY"
            );

            response.getWriter().write(
                    objectMapper.writeValueAsString(body)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        /*
         * Only filter attendance endpoints.
         * All other endpoints are unaffected.
         */
        return !request.getServletPath()
                .startsWith(ATTENDANCE_PATH);
    }

    /*
     * Constant-time string comparison.
     * Prevents timing attacks where attacker
     * measures how long comparison takes to
     * determine how many characters match.
     */
    private boolean constantTimeEquals(
            String a,
            String b) {

        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}