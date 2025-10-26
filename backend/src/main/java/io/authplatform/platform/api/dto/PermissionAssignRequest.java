package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for assigning a permission to a role.
 *
 * <p>This request is used to create a role-permission assignment,
 * granting the specified permission to the specified role.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to assign a permission to a role")
public class PermissionAssignRequest {

    /**
     * Permission ID to assign.
     *
     * <p>The permission must:
     * <ul>
     *   <li>Exist in the database</li>
     *   <li>Belong to the same organization as the role</li>
     *   <li>Not already be assigned to the role</li>
     * </ul>
     */
    @NotNull(message = "Permission ID is required")
    @Schema(
            description = "ID of the permission to assign to the role",
            example = "550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private UUID permissionId;
}
