package io.authplatform.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * This configuration sets up comprehensive API documentation with:
 * - API information (title, description, version, license)
 * - API key authentication scheme
 * - Server definitions for different environments
 * - Contact information
 *
 * The Swagger UI is available at: /swagger-ui.html
 * The OpenAPI specification is available at: /v3/api-docs
 *
 * @see <a href="https://springdoc.org/">Springdoc OpenAPI Documentation</a>
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Auth Platform}")
    private String applicationName;

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * Configures the OpenAPI specification for the Auth Platform API.
     *
     * This method creates a comprehensive OpenAPI 3.0 specification that includes:
     * - API metadata (title, description, version)
     * - Contact information for the API team
     * - License information (MIT License)
     * - Server definitions for development, staging, and production
     * - Security schemes (API Key authentication)
     *
     * @return OpenAPI instance with complete API documentation configuration
     */
    @Bean
    public OpenAPI authPlatformOpenAPI() {
        // API Information
        Info info = new Info()
                .title("Auth Platform API")
                .description("""
                        # Auth Platform - Fine-Grained Authorization System

                        A comprehensive, policy-based authorization platform that provides:

                        ## Core Features
                        - **Policy-Based Access Control**: Define authorization policies using Open Policy Agent (OPA) and Rego
                        - **Role-Based Access Control (RBAC)**: Hierarchical role management with inheritance
                        - **User Management**: Complete user lifecycle management with soft delete support
                        - **Multi-Tenancy**: Organization-scoped isolation for secure multi-tenant deployments
                        - **High Performance**: Multi-layer caching (Caffeine L1 + Redis L2) for sub-10ms response times
                        - **Audit Logging**: Comprehensive audit trail for all authorization decisions
                        - **Rate Limiting**: Per-organization and per-API-key rate limiting using Bucket4j

                        ## Authentication
                        All API endpoints require an API key to be included in the `X-API-Key` header.

                        ## Response Format
                        All responses follow a standard JSON format with appropriate HTTP status codes:
                        - `200 OK`: Successful request
                        - `201 Created`: Resource successfully created
                        - `400 Bad Request`: Invalid request parameters
                        - `401 Unauthorized`: Missing or invalid API key
                        - `403 Forbidden`: Insufficient permissions
                        - `404 Not Found`: Resource not found
                        - `429 Too Many Requests`: Rate limit exceeded
                        - `500 Internal Server Error`: Server error

                        ## Rate Limits
                        - Default: 1000 requests per minute per organization
                        - Burst capacity: 1200 requests
                        - Rate limit headers are included in all responses

                        ## Pagination
                        List endpoints support pagination with the following query parameters:
                        - `page`: Page number (0-indexed, default: 0)
                        - `size`: Page size (default: 20, max: 100)
                        - `sort`: Sort field and direction (e.g., `createdAt,desc`)

                        ## Support
                        For support, please contact the Auth Platform team or visit our documentation.
                        """)
                .version(applicationVersion)
                .contact(new Contact()
                        .name("Auth Platform Team")
                        .email("support@authplatform.io")
                        .url("https://authplatform.io"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        // Security Scheme - API Key
        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API key for authentication. Include this header in all requests.");

        // Security Requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("API Key");

        // Server Definitions
        String baseUrl = contextPath.isEmpty() ? "" : contextPath;
        List<Server> servers = List.of(
                new Server()
                        .url("http://localhost:8080" + baseUrl)
                        .description("Local Development Server"),
                new Server()
                        .url("https://staging.authplatform.io" + baseUrl)
                        .description("Staging Server"),
                new Server()
                        .url("https://api.authplatform.io" + baseUrl)
                        .description("Production Server")
        );

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("API Key", apiKeyScheme))
                .addSecurityItem(securityRequirement);
    }
}
