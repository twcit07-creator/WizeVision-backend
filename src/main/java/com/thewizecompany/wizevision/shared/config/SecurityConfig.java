package com.thewizecompany.wizevision.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import com.thewizecompany.wizevision.shared.security.JwtAuthFilter;
import com.thewizecompany.wizevision.shared.security.WindowsAppAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
 * SECURITY CONFIGURATION
 *
 * This class replaces Spring Security's default behavior
 * with our JWT-based stateless security.
 *
 * KEY DECISIONS EXPLAINED:
 *
 * 1. STATELESS SESSION
 *    No HttpSession is created or used.
 *    Every request must carry a JWT token.
 *    This is required for REST APIs consumed by
 *    React frontends and mobile apps.
 *
 * 2. CSRF DISABLED
 *    CSRF attacks target cookie-based sessions.
 *    We use JWT in Authorization header — not cookies.
 *    Therefore CSRF protection is not needed and
 *    would only block legitimate requests.
 *
 * 3. CORS CONFIGURED
 *    React dev server runs on localhost:3000.
 *    Our API runs on localhost:8080.
 *    Different ports = different origin = CORS required.
 *
 * 4. @EnableMethodSecurity
 *    Enables @PreAuthorize on individual controller methods.
 *    Example: @PreAuthorize("hasRole('ADMIN')")
 *    This gives fine-grained control per endpoint.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    /*
     * PUBLIC ENDPOINTS — no token required.
     * Everything else requires a valid JWT.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/attendance/auth/pin-login",
            "/api/v1/attendance/idle-reasons",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // ── Disable CSRF (JWT-based API, not session-based) ──
                .csrf(AbstractHttpConfigurer::disable)

                // ── Configure CORS ────────────────────────────────────
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()))

                // ── Stateless — no sessions ───────────────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                // ── Secure HTTP headers ───────────────────────────────
                .headers(headers -> headers
                        /*
                         * X-Content-Type-Options: nosniff
                         * Prevents browser from MIME-sniffing responses.
                         * A JSON response will never be executed as JS.
                         */
                        .contentTypeOptions(contentType -> {})
                        /*
                         * X-Frame-Options: DENY
                         * Prevents your app from being embedded in iframes.
                         * Protects against clickjacking attacks.
                         */
                        .frameOptions(frame -> frame.deny())
                )

                // ── Authorization rules ───────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no token needed
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // OPTIONS requests — needed for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // ── Custom error responses (JSON not HTML) ────────────
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(
                        windowsAppAuthFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )

                // ── Add JWT filter before Spring's auth filter ────────
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /*
     * AUTHENTICATION ENTRY POINT
     *
     * Called when an unauthenticated user tries to access
     * a protected endpoint.
     *
     * DEFAULT Spring behavior: redirect to /login (HTML page)
     * OUR behavior: return JSON with 401 status
     *
     * This is critical for React frontend — it cannot handle
     * HTML redirects, it needs JSON error responses.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                org.springframework.security.core.AuthenticationException ex) -> {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> body = ApiResponse.error(
                    "Authentication required. Please login.",
                    "AUTH_REQUIRED"
            );

            response.getWriter().write(
                    objectMapper.writeValueAsString(body)
            );
        };
    }

    /*
     * ACCESS DENIED HANDLER
     *
     * Called when an AUTHENTICATED user tries to access
     * an endpoint they don't have permission for.
     *
     * Example: A MODELER trying to access /api/v1/admin/users
     *
     * Returns 403 Forbidden with JSON body.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException ex) -> {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> body = ApiResponse.error(
                    "You do not have permission to access this resource.",
                    "ACCESS_DENIED"
            );

            response.getWriter().write(
                    objectMapper.writeValueAsString(body)
            );
        };
    }

    /*
     * CORS CONFIGURATION
     *
     * Allows the React frontend to call this API
     * from a different port/domain.
     *
     * Allowed origins come from application.yml so they
     * can be different in dev vs production without
     * changing code.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                appProperties.getSecurity()
                        .getCors()
                        .getAllowedOrigins()
        );

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH",
                "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "X-Correlation-ID"
        ));

        config.setExposedHeaders(List.of(
                "X-Correlation-ID"
        ));

        // Required when frontend sends Authorization header
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /*
     * PASSWORD ENCODER
     *
     * BCrypt with strength 12.
     * Strength 12 = ~250ms to hash on modern hardware.
     * This is intentional — makes brute force impractical.
     *
     * Why not strength 10 (Spring default)?
     * 10 is fast enough for most apps but 12 is better
     * for a company portal where security matters.
     *
     * Why not strength 14+?
     * Too slow for login endpoints under load.
     * 12 is the industry sweet spot.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /*
     * AUTHENTICATION MANAGER
     *
     * Required by AuthService to authenticate
     * username/password during login.
     * Spring Boot auto-configures this — we just expose it as a Bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public WindowsAppAuthFilter windowsAppAuthFilter() {
        return new WindowsAppAuthFilter(appProperties, objectMapper);
    }
}