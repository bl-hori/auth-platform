package io.authplatform.platform.api.dto;

import io.authplatform.platform.domain.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Role response DTO containing role information.
 *
 * <p>This DTO is used to return role data from the API, providing a clean
 * interface for clients including hierarchy information.
 *
 * <p><strong>Usage:</strong>
 * <ul>
 *   <li>GET /v1/roles/{id} - Single role retrieval</li>
 *   <li>GET /v1/roles - Role list</li>
 *   <li>POST /v1/roles - After role creation</li>
 *   <li>PUT /v1/roles/{id} - After role update</li>
 * </ul>
 *
 * @see Role
 * @see RoleCreateRequest
 * @see RoleUpdateRequest
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Role information response",
        example = """
                {
                  "id": "123e4567-e89b-12d3-a456-426614174000",
                  "organizationId": "123e4567-e89b-12d3-a456-426614174001",
                  "name": "developer",
                  "displayName": "Developer",
                  "description": "Software developers with code access",
                  "parentRoleId": "123e4567-e89b-12d3-a456-426614174002",
                  "level": 1,
                  "isSystem": false,
                  "metadata": {
                    "ui": {
                      "icon": "code",
                      "color": "#10B981"
                    }
                  },
                  "createdAt": "2024-01-15T10:30:00Z",
                  "updatedAt": "2024-01-20T14:45:00Z",
                  "deletedAt": null
                }
                """
)
public class RoleResponse {

    /**
     * Unique identifier for the role.
     */
    @Schema(
            description = "Unique identifier for the role",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID id;

    /**
     * Organization ID to which the role belongs.
     */
    @Schema(
            description = "Organization ID to which the role belongs",
            example = "123e4567-e89b-12d3-a456-426614174001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID organizationId;

    /**
     * Role name identifier.
     */
    @Schema(
            description = "Role name identifier (unique within organization)",
            example = "developer",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    /**
     * Human-readable display name for the role.
     */
    @Schema(
            description = "Human-readable display name for the role",
            example = "Developer"
    )
    private String displayName;

    /**
     * Optional description of the role's purpose.
     */
    @Schema(
            description = "Optional description of the role's purpose",
            example = "Software developers with code access"
    )
    private String description;

    /**
     * Parent role ID for hierarchy (null if root role).
     */
    @Schema(
            description = "Parent role ID for inheritance (null if root role)",
            example = "123e4567-e89b-12d3-a456-426614174002"
    )
    private UUID parentRoleId;

    /**
     * Depth level in role hierarchy (0 = root).
     */
    @Schema(
            description = "Depth level in role hierarchy (0 = root, max 10)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer level;

    /**
     * System role flag (cannot be deleted/modified if true).
     */
    @Schema(
            description = "System role flag (cannot be deleted/modified if true)",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean isSystem;

    /**
     * Custom metadata for the role.
     */
    @Schema(
            description = "Custom metadata for the role stored as key-value pairs",
            example = """
                    {
                      "ui": {
                        "icon": "code",
                        "color": "#10B981"
                      },
                      "features": {
                        "canAccessProd": false
                      }
                    }
                    """
    )
    private Map<String, Object> metadata;

    /**
     * Timestamp when the role was created.
     */
    @Schema(
            description = "Timestamp when the role was created",
            example = "2024-01-15T10:30:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the role was last updated.
     */
    @Schema(
            description = "Timestamp when the role was last updated",
            example = "2024-01-20T14:45:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime updatedAt;

    /**
     * Timestamp when the role was soft-deleted (null if not deleted).
     */
    @Schema(
            description = "Timestamp when the role was soft-deleted (null if not deleted)",
            example = "null"
    )
    private OffsetDateTime deletedAt;

    /**
     * List of permissions assigned to this role.
     */
    @Schema(
            description = "List of permissions assigned to this role",
            example = "[]"
    )
    private List<PermissionResponse> permissions;

    /**
     * Create a RoleResponse from a Role entity.
     *
     * @param role the role entity
     * @return the role response DTO
     */
    public static RoleResponse fromEntity(Role role) {
        return fromEntity(role, null);
    }

    /**
     * Create a RoleResponse from a Role entity with permissions.
     *
     * @param role the role entity
     * @param permissions the list of permission responses (optional)
     * @return the role response DTO
     */
    public static RoleResponse fromEntity(Role role, List<PermissionResponse> permissions) {
        return RoleResponse.builder()
                .id(role.getId())
                .organizationId(role.getOrganization().getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .parentRoleId(role.getParentRole() != null ? role.getParentRole().getId() : null)
                .level(role.getLevel())
                .isSystem(role.getIsSystem())
                .metadata(role.getMetadata())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .deletedAt(role.getDeletedAt())
                .permissions(permissions)
                .build();
    }
}
