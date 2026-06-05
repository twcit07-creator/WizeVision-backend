package com.thewizecompany.wizevision.shared.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.datadog.DatadogProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/*
 * TYPE-SAFE CONFIGURATION PROPERTIES
 *
 * This class binds everything under "app:" in application.yml
 * into a strongly-typed Java object.
 *
 * WHY NOT USE @Value ANNOTATIONS?
 *
 * Bad approach (avoid):
 *   @Value("${app.security.jwt.access-token-secret}")
 *   private String secret;
 *
 *   Problems:
 *   - Scattered across 10 different classes
 *   - No validation at startup
 *   - Typo in property name = silent null at runtime
 *   - Hard to test
 *
 * Good approach (what we do):
 *   @Autowired AppProperties properties;
 *   properties.getSecurity().getJwt().getAccessTokenSecret();
 *
 *   Benefits:
 *   - All config in one place
 *   - Validated at startup — missing secrets = app won't start
 *   - IDE autocomplete works
 *   - Easy to mock in tests
 *
 * @ConfigurationPropertiesScan in WizeVisionApplication.java
 * automatically picks this up — no manual @Bean needed.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Ai ai = new Ai();


    @Getter
    @Setter
    public static class Security {

        private final Jwt jwt = new Jwt();
        private final Cors cors = new Cors();
        private final RateLimit rateLimit = new RateLimit();
        private final WindowsApp windowsApp = new WindowsApp();

        @Getter
        @Setter
        public static class Jwt {

            /*
             * @NotBlank ensures the app FAILS AT STARTUP
             * if these environment variables are not set.
             * This is intentional — you never want an app
             * running in production with no JWT secret.
             * Fail fast is always better than a silent bug.
             */
            @NotBlank(message = "JWT access token secret must be configured")
            private String accessTokenSecret;

            @NotBlank(message = "JWT refresh token secret must be configured")
            private String refreshTokenSecret;

            /*
             * Default values are set here as fallback.
             * Production MUST override via environment variables.
             * These defaults are intentionally usable for dev
             * so the app starts without setting every variable.
             */
            @Positive
            private long accessTokenExpiryMs = 900_000L; // 15 minutes

            @Positive
            private long refreshTokenExpiryMs = 604_800_000L; // 7 days
        }

        @Getter
        @Setter
        public static class Cors {

            /*
             * Comma-separated origins allowed to call this API.
             * In dev: http://localhost:3000 (React dev server)
             * In prod: https://app.wizevision.com
             */
            private List<String> allowedOrigins = List.of(
                    "http://localhost:3000",
                    "http://localhost:5173"
            );
        }

        @Getter
        @Setter
        public static class RateLimit {

            private int requestsPerMinute = 60;
            private int maxLoginAttempts = 5;
        }

        @Getter
        @Setter
        public static class WindowsApp {
            @NotBlank(message = "Windows app API key must be configured")
            private String apiKey;
        }
    }

    @Getter
    @Setter
    public static class Ai {

        private String anthropicApiKey = "";
        private String model = "claude-sonnet-4-20250514";
    }


}