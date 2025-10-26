package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Role update request DTO (all fields optional for partial updates).
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Role update request")
public class RoleUpdateRequest {

    @Schema(description = "Display name")
    @Size(max = 255)
    private String displayName;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Parent role ID for hierarchy")
    private UUID parentRoleId;

    @Schema(description = "Custom metadata")
    private Map<String, Object> metadata;
}
