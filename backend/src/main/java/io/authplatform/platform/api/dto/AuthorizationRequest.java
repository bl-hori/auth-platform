package io.authplatform.platform.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.UUID;

/**
 * Authorization request DTO for access control decisions.
 *
 * <p>This DTO represents a request to determine whether a principal (user/service)
 * is authorized to perform a specific action on a resource within an organization.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * POST /v1/authorize
 * {
 *   "organizationId": "123e4567-e89b-12d3-a456-426614174000",
 *   "principal": {
 *     "id": "user-123",
 *     "type": "user",
 *     "attributes": {
 *       "email": "user@example.com",
 *       "department": "Engineering"
 *     }
 *   },
 *   "action": "read",
 *   "resource": {
 *     "type": "document",
 *     "id": "doc-456",
 *     "attributes": {
 *       "classification": "confidential",
 *       "owner": "user-789"
 *     }
 *   },
 *   "context": {
 *     "ipAddress": "192.168.1.1",
 *     "userAgent": "Mozilla/5.0...",
 *     "timestamp": "2025-10-26T10:30:00Z"
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Key Concepts:</strong>
 * <ul>
 *   <li><strong>Principal</strong>: The actor requesting access (user, service, etc.)</li>
 *   <li><strong>Action</strong>: The operation being performed (read, write, delete, etc.)</li>
 *   <li><strong>Resource</strong>: The target of the action (document, API, project, etc.)</li>
 *   <li><strong>Context</strong>: Additional contextual information (time, location, etc.)</li>
 * </ul>
 *
 * @see AuthorizationResponse
 * @see Principal
 * @see Resource
 */
@Value
@Builder
@Schema(description = "Authorization request for access control decision")
public class AuthorizationRequest {

    /**
     * Organization ID for multi-tenant isolation.
     *
     * <p>All authorization decisions are scoped to a specific organization.
     */
    @NotNull(message = "Organization ID is required")
    @Schema(
            description = "Organization UUID for multi-tenant isolation",
            example = "123e4567-e89b-12d3-a456-426614174000",
            required = true
    )
    UUID organizationId;

    /**
     * Principal (actor) requesting access.
     *
     * <p>The principal represents the entity attempting to perform an action.
     * This can be a user, service account, API client, etc.
     */
    @NotNull(message = "Principal is required")
    @Schema(description = "Principal (user/service) requesting access", required = true)
    Principal principal;

    /**
     * Action to be performed.
     *
     * <p>Common actions: read, write, delete, execute, admin, etc.
     * Actions are case-insensitive and should follow a consistent naming convention.
     *
     * <p>Examples:
     * <ul>
     *   <li>read - View/read access</li>
     *   <li>write - Modify/update access</li>
     *   <li>delete - Remove/delete access</li>
     *   <li>execute - Run/execute access</li>
     *   <li>admin - Administrative access</li>
     * </ul>
     */
    @NotBlank(message = "Action is required")
    @Schema(
            description = "Action to be performed on the resource",
            example = "read",
            required = true
    )
    String action;

    /**
     * Resource being accessed.
     *
     * <p>The resource represents the target of the authorization request.
     * It includes the resource type, ID, and optional attributes for ABAC.
     */
    @NotNull(message = "Resource is required")
    @Schema(description = "Resource being accessed", required = true)
    Resource resource;

    /**
     * Additional context for the authorization decision.
     *
     * <p>Context provides additional information that may influence the authorization
     * decision, such as IP address, time of day, device type, etc.
     *
     * <p>Common context attributes:
     * <ul>
     *   <li>ipAddress - Client IP address</li>
     *   <li>userAgent - HTTP User-Agent header</li>
     *   <li>timestamp - Request timestamp</li>
     *   <li>location - Geographic location</li>
     *   <li>deviceType - Device type (mobile, desktop, etc.)</li>
     * </ul>
     */
    @Schema(description = "Additional context for authorization decision")
    Map<String, Object> context;

    /**
     * JSON creator for deserialization.
     */
    @JsonCreator
    public AuthorizationRequest(
            @JsonProperty("organizationId") UUID organizationId,
            @JsonProperty("principal") Principal principal,
            @JsonProperty("action") String action,
            @JsonProperty("resource") Resource resource,
            @JsonProperty("context") Map<String, Object> context) {
        this.organizationId = organizationId;
        this.principal = principal;
        this.action = action;
        this.resource = resource;
        this.context = context;
    }

    /**
     * Principal (actor) in an authorization request.
     *
     * <p>Represents the entity requesting access to a resource.
     */
    @Value
    @Builder
    @Schema(description = "Principal (user/service) requesting access")
    public static class Principal {

        /**
         * Principal identifier.
         *
         * <p>This can be a user ID, service account ID, API key ID, etc.
         */
        @NotBlank(message = "Principal ID is required")
        @Schema(description = "Principal identifier (user ID, service ID, etc.)", example = "user-123", required = true)
        String id;

        /**
         * Principal type.
         *
         * <p>Indicates the type of principal making the request.
         *
         * <p>Common types:
         * <ul>
         *   <li>user - Human user</li>
         *   <li>service - Service account</li>
         *   <li>api_client - API client/application</li>
         *   <li>system - System/internal process</li>
         * </ul>
         */
        @NotBlank(message = "Principal type is required")
        @Schema(description = "Principal type (user, service, api_client, etc.)", example = "user", required = true)
        String type;

        /**
         * Principal attributes for ABAC.
         *
         * <p>Additional attributes that can be used in attribute-based access control policies.
         *
         * <p>Common attributes:
         * <ul>
         *   <li>email - User email address</li>
         *   <li>department - User department</li>
         *   <li>role - User role(s)</li>
         *   <li>groups - User group memberships</li>
         *   <li>securityLevel - Security clearance level</li>
         * </ul>
         */
        @Schema(description = "Principal attributes for ABAC policies")
        Map<String, Object> attributes;

        @JsonCreator
        public Principal(
                @JsonProperty("id") String id,
                @JsonProperty("type") String type,
                @JsonProperty("attributes") Map<String, Object> attributes) {
            this.id = id;
            this.type = type;
            this.attributes = attributes;
        }
    }

    /**
     * Resource in an authorization request.
     *
     * <p>Represents the target resource being accessed.
     */
    @Value
    @Builder
    @Schema(description = "Resource being accessed")
    public static class Resource {

        /**
         * Resource type.
         *
         * <p>Categorizes the resource for policy evaluation.
         *
         * <p>Examples: document, project, api, database, repository, etc.
         */
        @NotBlank(message = "Resource type is required")
        @Schema(description = "Resource type (document, project, api, etc.)", example = "document", required = true)
        String type;

        /**
         * Resource identifier.
         *
         * <p>Unique identifier for the specific resource instance.
         */
        @NotBlank(message = "Resource ID is required")
        @Schema(description = "Resource identifier", example = "doc-456", required = true)
        String id;

        /**
         * Resource attributes for ABAC.
         *
         * <p>Additional attributes that can be used in attribute-based access control policies.
         *
         * <p>Common attributes:
         * <ul>
         *   <li>owner - Resource owner ID</li>
         *   <li>classification - Data classification (public, confidential, secret)</li>
         *   <li>tags - Resource tags/labels</li>
         *   <li>createdAt - Resource creation timestamp</li>
         *   <li>visibility - Visibility level (public, private, restricted)</li>
         * </ul>
         */
        @Schema(description = "Resource attributes for ABAC policies")
        Map<String, Object> attributes;

        @JsonCreator
        public Resource(
                @JsonProperty("type") String type,
                @JsonProperty("id") String id,
                @JsonProperty("attributes") Map<String, Object> attributes) {
            this.type = type;
            this.id = id;
            this.attributes = attributes;
        }
    }
}
