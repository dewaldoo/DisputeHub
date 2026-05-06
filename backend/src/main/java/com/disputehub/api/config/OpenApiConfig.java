package com.disputehub.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration
 *
 * EXPLANATION:
 * Configures SpringDoc OpenAPI to generate API documentation and Swagger UI.
 * This provides interactive API documentation accessible at /swagger-ui.html
 *
 * FEATURES:
 * - Auto-generates API docs from controllers and DTOs
 * - Interactive UI to test API endpoints
 * - JWT authentication integration
 * - Export OpenAPI spec as JSON/YAML
 *
 * ACCESS:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 * - OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure OpenAPI specification
     *
     * COMPONENTS:
     * - Info: API metadata (title, description, version)
     * - Security: JWT Bearer token authentication
     * - Contact: API maintainer information
     * - License: API license information
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DisputeHub API")
                .version("1.0.0")
                .description("""
                    # DisputeHub Transaction Dispute Management API

                    RESTful API for managing transaction disputes in a banking system.

                    ## Features
                    - **Authentication**: JWT-based authentication with role-based access control
                    - **Transactions**: View and manage bank transactions
                    - **Disputes**: Create and track transaction disputes
                    - **Admin Panel**: Manage disputes and view audit logs
                    - **Audit Trail**: Complete history of all dispute actions

                    ## Authentication
                    1. Register or login to get JWT token
                    2. Click "Authorize" button and enter: `Bearer <your-token>`
                    3. All authenticated endpoints will include the token automatically

                    ## Roles
                    - **CUSTOMER**: Can view own transactions, create and track disputes
                    - **ADMIN**: Can view all disputes, update dispute status, access audit logs

                    ## Status Flow
                    ```
                    PENDING → UNDER_REVIEW → MERCHANT_CONTACTED → RESOLVED/REJECTED
                    ```
                    """)
                .contact(new Contact()
                    .name("DisputeHub Support")
                    .email("support@disputehub.com")
                    .url("https://github.com/disputehub/api"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                            Enter JWT token obtained from login/register endpoints.
                            Format: Just paste the token without 'Bearer' prefix.
                            Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                            """)));
    }
}
