package io.authplatform.platform.api.dto.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new policy.
 *
 * <p>Policies are created in DRAFT status and must be published to become active.
 *
 * <p><strong>Example JSON:</strong>
 * <pre>{@code
 * {
 *   "name": "document-access-policy",
 *   "displayName": "Document Access Policy",
 *   "description": "Controls access to documents based on user roles",
 *   "regoCode": "package authz\ndefault allow = false\nallow { input.user.role == \"admin\" }",
 *   "organizationId": "123e4567-e89b-12d3-a456-426614174000"
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new policy")
public class PolicyCreateRequest {

    /**
     * Unique policy name within the organization.
     * Must be lowercase with hyphens.
     */
    @NotBlank(message = "Policy name is required")
    @Size(min = 3, max = 255, message = "Policy name must be between 3 and 255 characters")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$",
            message = "Policy name must be lowercase alphanumeric with hyphens, starting with a letter")
    @Schema(
            description = "Unique policy name within the organization (lowercase-with-hyphens)",
            example = "document-access-policy",
            required = true
    )
    private String name;

    /**
     * Human-readable display name.
     */
    @NotBlank(message = "Display name is required")
    @Size(min = 3, max = 255, message = "Display name must be between 3 and 255 characters")
    @Schema(
            description = "Human-readable display name",
            example = "Document Access Policy",
            required = true
    )
    @JsonProperty("displayName")
    private String displayName;

    /**
     * Optional description of the policy's purpose.
     */
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(
            description = "Optional description of the policy's purpose",
            example = "Controls access to documents based on user roles and departments"
    )
    private String description;

    /**
     * Rego policy code (OPA).
     */
    @NotBlank(message = "Rego code is required")
    @Size(min = 10, max = 100000, message = "Rego code must be between 10 and 100000 characters")
    @Schema(
            description = "Rego policy code (Open Policy Agent)",
            example = "package authz\n\ndefault allow = false\n\nallow {\n  input.user.role == \"admin\"\n}",
            required = true
    )
    @JsonProperty("regoCode")
    private String regoCode;

    /**
     * Organization ID to which this policy belongs.
     */
    @NotNull(message = "Organization ID is required")
    @Schema(
            description = "Organization ID to which this policy belongs",
            example = "123e4567-e89b-12d3-a456-426614174000",
            required = true
    )
    @JsonProperty("organizationId")
    private UUID organizationId;
}
