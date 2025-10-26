package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request DTO for assigning a role to a user.
 *
 * <p>This DTO is used when assigning roles to users with optional resource scoping
 * and expiration time.
 *
 * @see io.authplatform.platform.domain.entity.UserRole
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to assign a role to a user")
public class UserRoleAssignRequest {

    /**
     * The unique identifier of the role to assign.
     *
     * <p>The role must exist in the same organization as the user.
     */
    @NotNull(message = "Role ID is required")
    @Schema(
            description = "ID of the role to assign",
            example = "550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private UUID roleId;

    /**
     * Optional resource identifier for scoped role assignment.
     *
     * <p>When specified, the role is only effective for this specific resource.
     * For example, a "Manager" role might be scoped to a specific project or team.
     *
     * <p>If null or empty, the role applies globally within the organization.
     *
     * <p>Examples:
     * <ul>
     *   <li>"project:12345" - Role applies only to project 12345</li>
     *   <li>"team:engineering" - Role applies only to engineering team</li>
     *   <li>null - Role applies organization-wide</li>
     * </ul>
     */
    @Schema(
            description = "Optional resource scope for the role assignment. "
                    + "If null, role applies organization-wide",
            example = "project:acme-webapp",
            nullable = true
    )
    private String resourceId;

    /**
     * Optional expiration timestamp for the role assignment.
     *
     * <p>After this time, the role assignment becomes inactive and should not
     * be considered during authorization checks.
     *
     * <p>If null, the role assignment does not expire.
     *
     * <p>Use cases:
     * <ul>
     *   <li>Temporary elevated permissions</li>
     *   <li>Trial periods</li>
     *   <li>Time-limited access grants</li>
     * </ul>
     */
    @Schema(
            description = "Optional expiration time for the role assignment. "
                    + "If null, assignment does not expire",
            example = "2024-12-31T23:59:59Z",
            nullable = true
    )
    private OffsetDateTime expiresAt;
}
