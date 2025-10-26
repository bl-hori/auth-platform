package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Role creation request DTO.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Role creation request")
public class RoleCreateRequest {

    @Schema(description = "Organization ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @Schema(description = "Role name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Schema(description = "Display name")
    @Size(max = 255)
    private String displayName;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Parent role ID for hierarchy")
    private UUID parentRoleId;

    @Schema(description = "Level in hierarchy (0-10)")
    @Min(0)
    @Max(10)
    private Integer level;

    @Schema(description = "Custom metadata")
    private Map<String, Object> metadata;
}
