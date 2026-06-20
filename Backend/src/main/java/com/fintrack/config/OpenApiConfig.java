package com.fintrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI 3.0 configuration.
 *
 * springdoc-openapi auto-generates the API spec from our @RestController classes.
 * This config adds:
 *   - API metadata (title, version, description, contact)
 *   - JWT Bearer auth scheme — adds the "Authorize" button to Swagger UI
 *   - Server base URL
 *
 * Access Swagger UI at: http://localhost:8080/api/v1/swagger-ui.html
 * Access raw OpenAPI JSON at: http://localhost:8080/api/v1/api-docs
 *
 * The JWT Bearer scheme means:
 *   1. You click "Authorize" in Swagger UI
 *   2. Paste your JWT token
 *   3. All subsequent API calls in Swagger will include: Authorization: Bearer <token>
 *
 * WHY Swagger in production?
 * It's the "always up-to-date" API documentation — generated from code,
 * never out of sync. Interviewers can explore the API without Postman.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("/api/v1").description("Default server")
                ))
                // Apply Bearer auth globally — every endpoint shows a lock icon
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("FinTrack API")
                .description("Personal Expense Tracker & Finance Dashboard REST API. " +
                             "Register/Login first to get a Bearer token, then click 'Authorize'.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Prachi Bhari")
                        .email("prachichoudharybhari@gmail.com"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT access token. Obtain it from POST /auth/login.");
    }
}
