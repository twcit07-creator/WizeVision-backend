package com.thewizecompany.wizevision.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/*
 * JWT AUTHENTICATION FILTER
 *
 * This filter runs on EVERY incoming HTTP request — exactly once.
 * OncePerRequestFilter guarantees no duplicate execution.
 *
 * WHAT IT DOES:
 * 1. Looks for "Authorization: Bearer <token>" header
 * 2. If no header → continues chain (Spring Security
 *    will block the request later if endpoint needs auth)
 * 3. If header exists → validates the token
 * 4. If valid → checks Redis blacklist (logout protection)
 * 5. If not blacklisted → sets Authentication in SecurityContext
 * 6. Spring Security sees the Authentication and allows access
 *
 * WHAT IT DOES NOT DO:
 * - Does NOT block requests (that is SecurityConfig's job)
 * - Does NOT throw exceptions to the client
 *   (logs quietly and lets the chain continue unauthenticated)
 * - Does NOT load the user from the database
 *   (all info is in the token — that is the point of JWT)
 *
 * TOKEN BLACKLIST (Redis):
 * Key pattern: "token:blacklist:{jti}"
 * When user logs out, we store their token's JTI in Redis.
 * This filter checks Redis before accepting any token.
 * TTL = remaining token validity (auto-expires from Redis).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // ── STEP 1: Extract token from header ─────────────────
        String token = extractTokenFromRequest(request);

        if (token == null) {
            /*
             * No token present.
             * This is fine — public endpoints don't need one.
             * Protected endpoints will be blocked by SecurityConfig.
             * We just continue the filter chain.
             */
            filterChain.doFilter(request, response);
            return;
        }

        // ── STEP 2: Validate the token ─────────────────────────
        Claims claims;
        try {
            claims = jwtUtil.validateAccessToken(token);
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired for request: {}",
                    request.getRequestURI());
            /*
             * Token is expired. Continue chain unauthenticated.
             * The client should use the refresh token to get
             * a new access token.
             * We do not write an error response here —
             * SecurityConfig will return 401 for protected endpoints.
             */
            filterChain.doFilter(request, response);
            return;
        } catch (JwtException e) {
            log.warn("Invalid JWT token for request {}: {}",
                    request.getRequestURI(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── STEP 3: Check Redis blacklist ──────────────────────
        String jti = jwtUtil.extractJti(claims);
        String blacklistKey = BLACKLIST_PREFIX + jti;

        Boolean isBlacklisted = redisTemplate
                .hasKey(blacklistKey);

        if (isBlacklisted) {
            /*
             * Token has been blacklisted — user logged out.
             * Even though the token signature is valid,
             * we reject it by not setting authentication.
             */
            log.warn("Blacklisted token used from IP: {}",
                    request.getRemoteAddr());
            filterChain.doFilter(request, response);
            return;
        }

        // ── STEP 4: Set authentication in SecurityContext ──────
        /*
         * We only do this if no authentication exists yet.
         * Prevents overwriting an existing authentication
         * (important in test scenarios).
         */
        if (SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            String role = jwtUtil.extractRole(claims);
            String email = jwtUtil.extractEmail(claims);

            /*
             * ROLE_ prefix is required by Spring Security.
             * When we do hasRole('ADMIN'), Spring internally
             * looks for an authority named "ROLE_ADMIN".
             * We store roles WITHOUT the prefix in JWT,
             * and add it here when creating the authority.
             *
             * Example:
             *   JWT role claim: "ADMIN"
             *   Authority created: "ROLE_ADMIN"
             *   @PreAuthorize("hasRole('ADMIN')") → matches 
             */
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            /*
             * UsernamePasswordAuthenticationToken with 3 args
             * = authenticated token (credentials are verified).
             *
             * principal = email (this is what AuditorAware reads
             *             to populate createdBy/updatedBy)
             * credentials = null (we have the token, not a password)
             * authorities = list of roles for authorization
             */
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authToken);

            log.debug("Authenticated user: {} with role: {}",
                    email, role);
        }

        filterChain.doFilter(request, response);
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private String extractTokenFromRequest(
            HttpServletRequest request) {

        String header = request.getHeader(AUTH_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        /*
         * "Bearer eyJhbGc..."
         *  0123456
         * Substring from index 7 removes "Bearer " prefix
         * and returns the raw token string.
         */
        return header.substring(BEARER_PREFIX.length()).trim();
    }
}