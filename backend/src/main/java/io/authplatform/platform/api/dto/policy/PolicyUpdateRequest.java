package io.authplatform.platform.api.dto.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a policy.
 *
 * <p>Updating a policy creates a new version and increments the version number.
 *
 * <p><strong>Example JSON:</strong>
 * <pre>{@code
 * {
 *   "displayName": "Updated Document Access Policy",
 *   "description": "Enhanced access control with attribute-based rules",
 *   "regoCode": "package authz\ndefault allow = false\nallow { ... new rules ... }"
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update a policy (creates new version)")
public class PolicyUpdateRequest {

    /**
     * Updated display name (optional).
     */
    @Size(min = 3, max = 255, message = "Display name must be between 3 and 255 characters")
    @Schema(
            description = "Updated human-readable display name",
            example = "Updated Document Access Policy"
    )
    @JsonProperty("displayName")
    private String displayName;

    /**
     * Updated description (optional).
     */
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(
            description = "Updated description of the policy's purpose",
            example = "Enhanced access control with attribute-based rules"
    )
    private String description;

    /**
     * Updated Rego code (required).
     */
    @NotBlank(message = "Rego code is required")
    @Size(min = 10, max = 100000, message = "Rego code must be between 10 and 100000 characters")
    @Schema(
            description = "Updated Rego policy code",
            example = "package authz\n\ndefault allow = false\n\nallow {\n  input.user.role == \"admin\"\n  input.resource.owner == input.user.id\n}",
            required = true
    )
    @JsonProperty("regoCode")
    private String regoCode;
}
