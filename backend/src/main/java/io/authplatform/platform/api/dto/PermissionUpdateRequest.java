package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Permission update request DTO.
 *
 * <p>Used to update an existing permission. All fields are optional.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Permission update request (all fields optional)",
        example = """
                {
                  "displayName": "Read All Documents",
                  "description": "Permission to read all documents in the system",
                  "effect": "allow",
                  "conditions": {
                    "resource": {
                      "status": "published"
                    }
                  }
                }
                """
)
public class PermissionUpdateRequest {

    @Schema(description = "Display name", example = "Read All Documents")
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;

    @Schema(
            description = "Description",
            example = "Permission to read all documents in the system"
    )
    private String description;

    @Schema(
            description = "Effect (allow or deny)",
            example = "allow",
            allowableValues = {"allow", "deny"}
    )
    @Pattern(regexp = "^(allow|deny)$", message = "Effect must be 'allow' or 'deny'")
    private String effect;

    @Schema(
            description = "Conditions for ABAC (Attribute-Based Access Control)",
            example = """
                    {
                      "resource": {
                        "status": "published"
                      }
                    }
                    """
    )
    private Map<String, Object> conditions;
}
