package io.authplatform.platform.api.dto;

import io.authplatform.platform.domain.entity.Permission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Permission response DTO containing permission information.
 *
 * <p>This DTO is used to return permission data from the API, providing a clean
 * interface for clients including resource-action information.
 *
 * <p><strong>Usage:</strong>
 * <ul>
 *   <li>GET /v1/permissions/{id} - Single permission retrieval</li>
 *   <li>GET /v1/permissions - Permission list</li>
 *   <li>POST /v1/permissions - After permission creation</li>
 *   <li>PUT /v1/permissions/{id} - After permission update</li>
 * </ul>
 *
 * @see Permission
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Permission information response",
        example = """
                {
                  "id": "123e4567-e89b-12d3-a456-426614174000",
                  "organizationId": "123e4567-e89b-12d3-a456-426614174001",
                  "name": "document:read",
                  "displayName": "Read Documents",
                  "description": "Permission to read documents",
                  "resourceType": "document",
                  "action": "read",
                  "effect": "allow",
                  "conditions": {},
                  "createdAt": "2024-01-15T10:30:00Z",
                  "updatedAt": "2024-01-20T14:45:00Z"
                }
                """
)
public class PermissionResponse {

    /**
     * Unique identifier for the permission.
     */
    @Schema(
            description = "Unique identifier for the permission",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID id;

    /**
     * Organization ID to which the permission belongs.
     */
    @Schema(
            description = "Organization ID to which the permission belongs",
            example = "123e4567-e89b-12d3-a456-426614174001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID organizationId;

    /**
     * Permission name identifier (resource_type:action).
     */
    @Schema(
            description = "Permission name identifier (unique within organization)",
            example = "document:read",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    /**
     * Human-readable display name for the permission.
     */
    @Schema(
            description = "Human-readable display name for the permission",
            example = "Read Documents"
    )
    private String displayName;

    /**
     * Optional description of the permission's purpose.
     */
    @Schema(
            description = "Optional description of the permission's purpose",
            example = "Permission to read documents"
    )
    private String description;

    /**
     * Type of resource this permission applies to.
     */
    @Schema(
            description = "Type of resource this permission applies to",
            example = "document",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String resourceType;

    /**
     * Action that can be performed on the resource.
     */
    @Schema(
            description = "Action that can be performed on the resource",
            example = "read",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String action;

    /**
     * Effect of the permission (allow or deny).
     */
    @Schema(
            description = "Effect of the permission",
            example = "allow",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"allow", "deny"}
    )
    private String effect;

    /**
     * Additional conditions for ABAC.
     */
    @Schema(
            description = "Additional conditions for Attribute-Based Access Control",
            example = """
                    {
                      "resource": {
                        "owner": "${user.id}"
                      }
                    }
                    """
    )
    private Map<String, Object> conditions;

    /**
     * Timestamp when the permission was created.
     */
    @Schema(
            description = "Timestamp when the permission was created",
            example = "2024-01-15T10:30:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the permission was last updated.
     */
    @Schema(
            description = "Timestamp when the permission was last updated",
            example = "2024-01-20T14:45:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime updatedAt;

    /**
     * Create a PermissionResponse from a Permission entity.
     *
     * @param permission the permission entity
     * @return the permission response DTO
     */
    public static PermissionResponse fromEntity(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .organizationId(permission.getOrganization().getId())
                .name(permission.getName())
                .displayName(permission.getDisplayName())
                .description(permission.getDescription())
                .resourceType(permission.getResourceType())
                .action(permission.getAction())
                .effect(permission.getEffect().getValue())
                .conditions(permission.getConditions())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}
