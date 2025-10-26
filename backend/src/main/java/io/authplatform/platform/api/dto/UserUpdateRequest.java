package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * User update request DTO.
 *
 * <p>This DTO is used to update an existing user. All fields are optional,
 * allowing partial updates. Only provided fields will be updated.
 *
 * <p><strong>Update behavior:</strong>
 * <ul>
 *   <li>null values are ignored (field not updated)</li>
 *   <li>empty strings are treated as clearing the field (where applicable)</li>
 *   <li>email and username must remain unique within the organization</li>
 *   <li>organizationId cannot be changed (use user transfer API if needed)</li>
 * </ul>
 *
 * @see UserResponse
 * @see UserCreateRequest
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "User update request (all fields optional for partial updates)",
        example = """
                {
                  "email": "newemail@example.com",
                  "displayName": "John Smith",
                  "attributes": {
                    "department": "Product",
                    "title": "Staff Engineer"
                  }
                }
                """
)
public class UserUpdateRequest {

    /**
     * User's email address.
     * Must be valid email format and unique within the organization.
     */
    @Schema(
            description = "User's email address (unique within organization)",
            example = "user@example.com"
    )
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * Optional username for the user.
     * Must be unique within the organization if provided.
     */
    @Schema(
            description = "Optional username for the user (unique within organization)",
            example = "john.doe"
    )
    @Size(max = 255, message = "Username must not exceed 255 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "Username must contain only alphanumeric characters, dots, underscores, and hyphens"
    )
    private String username;

    /**
     * Human-readable display name for the user.
     */
    @Schema(
            description = "Human-readable display name for the user",
            example = "John Doe"
    )
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;

    /**
     * External identity provider user ID.
     */
    @Schema(
            description = "External identity provider user ID for SSO integration",
            example = "okta:00u1234567890"
    )
    @Size(max = 255, message = "External ID must not exceed 255 characters")
    private String externalId;

    /**
     * User status.
     * Use separate deactivation/activation endpoints for status changes when possible.
     */
    @Schema(
            description = "User status (use activation/deactivation endpoints when possible)",
            example = "active",
            allowableValues = {"active", "inactive", "suspended"}
    )
    @Pattern(
            regexp = "^(active|inactive|suspended)$",
            message = "Status must be one of: active, inactive, suspended"
    )
    private String status;

    /**
     * Custom attributes for the user (ABAC support).
     * This replaces all existing attributes - use PATCH for partial updates.
     */
    @Schema(
            description = "Custom attributes for the user (replaces all existing attributes)",
            example = """
                    {
                      "department": "Product",
                      "title": "Staff Engineer",
                      "location": "US-East"
                    }
                    """
    )
    private Map<String, Object> attributes;
}
