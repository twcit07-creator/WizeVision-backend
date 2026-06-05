package com.thewizecompany.wizevision.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.thewizecompany.wizevision"
)
public class JpaConfig {

    /*
     * AUDITOR AWARE BEAN
     *
     * This bean answers one question:
     * "Who is currently logged in?"
     *
     * Spring JPA Auditing calls this method automatically
     * every time an entity is created or updated.
     * The returned value is stored in:
     *   - createdBy  (on first save)
     *   - updatedBy  (on every save)
     *
     * How it works:
     * 1. Request comes in with JWT token
     * 2. JwtAuthFilter validates token and sets Authentication
     *    into SecurityContextHolder
     * 3. When entity is saved, Spring calls getCurrentAuditor()
     * 4. We read the email/userId from SecurityContext
     * 5. That value is stored in createdBy / updatedBy
     *
     * Why Optional?
     * During startup, migration, or anonymous requests,
     * there is no logged-in user.
     * Optional.empty() tells Spring to leave the field null
     * rather than throwing a NullPointerException.
     *
     * Bean name "auditorAware" must match exactly what
     * WizeVisionApplication.java declares:
     * @EnableJpaAuditing(auditorAwareRef = "auditorAware")
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            // No authentication context exists
            // (app startup, Flyway migrations, public endpoints)
            if (authentication == null
                    || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            // "anonymousUser" is what Spring Security sets
            // for unauthenticated requests that pass through
            // the filter chain without a token.
            // We treat this the same as no user.
            String principal = authentication.getName();
            if ("anonymousUser".equals(principal)) {
                return Optional.empty();
            }

            // For authenticated requests, getName() returns
            // whatever we set as the principal in JwtAuthFilter.
            // We will set it to the employee's email address
            // so createdBy shows "john.doe@wizevision.com"
            // which is human-readable in audit queries.
            return Optional.of(principal);
        };
    }
}