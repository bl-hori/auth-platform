package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Permission creation request DTO.
 *
 * <p>Used to create a new permission in the system.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Permission creation request",
        example = """
                {
                  "organizationId": "00000000-0000-0000-0000-000000000000",
                  "name": "document:read",
                  "displayName": "Read Documents",
                  "description": "Permission to read documents",
                  "resourceType": "document",
                  "action": "read",
                  "effect": "allow",
                  "conditions": {}
                }
                """
)
public class PermissionCreateRequest {

    @Schema(description = "Organization ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @Schema(
            description = "Permission name (format: resourceType:action)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "document:read"
    )
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Pattern(
            regexp = "^[a-z0-9_-]+:[a-z0-9_-]+$",
            message = "Name must follow pattern 'resourceType:action' (lowercase alphanumeric, underscore, hyphen)"
    )
    private String name;

    @Schema(description = "Display name", example = "Read Documents")
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;

    @Schema(description = "Description", example = "Permission to read documents")
    private String description;

    @Schema(
            description = "Resource type",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "document"
    )
    @NotBlank(message = "Resource type is required")
    @Size(max = 255, message = "Resource type must not exceed 255 characters")
    @Pattern(
            regexp = "^[a-z0-9_-]+$",
            message = "Resource type must be lowercase alphanumeric with underscore or hyphen"
    )
    private String resourceType;

    @Schema(
            description = "Action",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "read"
    )
    @NotBlank(message = "Action is required")
    @Size(max = 255, message = "Action must not exceed 255 characters")
    @Pattern(
            regexp = "^[a-z0-9_-]+$",
            message = "Action must be lowercase alphanumeric with underscore or hyphen"
    )
    private String action;

    @Schema(
            description = "Effect (allow or deny)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "allow",
            allowableValues = {"allow", "deny"}
    )
    @NotBlank(message = "Effect is required")
    @Pattern(regexp = "^(allow|deny)$", message = "Effect must be 'allow' or 'deny'")
    private String effect;

    @Schema(
            description = "Conditions for ABAC (Attribute-Based Access Control)",
            example = """
                    {
                      "resource": {
                        "owner": "${user.id}"
                      }
                    }
                    """
    )
    private Map<String, Object> conditions;
}
