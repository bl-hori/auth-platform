package io.authplatform.platform.api.dto;

import io.authplatform.platform.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for user-role assignment information.
 *
 * <p>Contains complete information about a role assignment including the role details,
 * resource scope, and expiration information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User role assignment information")
public class UserRoleResponse {

    /**
     * Unique identifier for this user-role assignment.
     */
    @Schema(
            description = "Unique identifier for the user-role assignment",
            example = "660e8400-e29b-41d4-a716-446655440001"
    )
    private UUID id;

    /**
     * ID of the user who has this role.
     */
    @Schema(
            description = "ID of the user",
            example = "770e8400-e29b-41d4-a716-446655440002"
    )
    private UUID userId;

    /**
     * ID of the assigned role.
     */
    @Schema(
            description = "ID of the assigned role",
            example = "880e8400-e29b-41d4-a716-446655440003"
    )
    private UUID roleId;

    /**
     * Name of the assigned role.
     */
    @Schema(
            description = "Name of the assigned role",
            example = "project-manager"
    )
    private String roleName;

    /**
     * Display name of the assigned role.
     */
    @Schema(
            description = "Display name of the assigned role",
            example = "Project Manager"
    )
    private String roleDisplayName;

    /**
     * Optional resource scope for the role assignment.
     */
    @Schema(
            description = "Optional resource scope. If null, role applies organization-wide",
            example = "project:acme-webapp",
            nullable = true
    )
    private String resourceId;

    /**
     * Optional expiration timestamp.
     */
    @Schema(
            description = "Optional expiration time. If null, assignment does not expire",
            example = "2024-12-31T23:59:59Z",
            nullable = true
    )
    private OffsetDateTime expiresAt;

    /**
     * Timestamp when the role was assigned.
     */
    @Schema(
            description = "Timestamp when the role was assigned",
            example = "2024-01-15T10:30:00Z"
    )
    private OffsetDateTime assignedAt;

    /**
     * ID of the user who assigned this role.
     */
    @Schema(
            description = "ID of the user who assigned this role",
            example = "990e8400-e29b-41d4-a716-446655440004",
            nullable = true
    )
    private UUID assignedBy;

    /**
     * Converts a UserRole entity to a response DTO.
     *
     * @param userRole the user role entity
     * @return the response DTO
     */
    public static UserRoleResponse fromEntity(UserRole userRole) {
        return UserRoleResponse.builder()
                .id(userRole.getId())
                .userId(userRole.getUser().getId())
                .roleId(userRole.getRole().getId())
                .roleName(userRole.getRole().getName())
                .roleDisplayName(userRole.getRole().getDisplayName())
                .resourceId(userRole.getResourceId())
                .expiresAt(userRole.getExpiresAt())
                .assignedAt(userRole.getGrantedAt())
                .assignedBy(userRole.getGrantedBy() != null ? userRole.getGrantedBy().getId() : null)
                .build();
    }
}
