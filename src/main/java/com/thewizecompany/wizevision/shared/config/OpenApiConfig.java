package com.thewizecompany.wizevision.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/*
 * OPENAPI CONFIGURATION
 *
 * Configures Swagger UI to:
 * 1. Show app name and version
 * 2. Add "Authorize" button in Swagger UI
 *    so you can paste your JWT token and test
 *    protected endpoints directly from the browser
 *
 * After adding this, go to swagger-ui.html,
 * click "Authorize", paste your Bearer token,
 * and all protected endpoints become testable.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "WizeVision Portal API",
                version = "v1.0",
                description = "Enterprise Management Portal — " +
                        "Steel Detailing Company"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter your JWT access token here"
)
public class OpenApiConfig {
    // Configuration is done via annotations above
}