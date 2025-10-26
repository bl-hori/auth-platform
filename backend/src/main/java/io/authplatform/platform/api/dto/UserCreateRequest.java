package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
 * User creation request DTO.
 *
 * <p>This DTO is used to create a new user in the system. It includes
 * validation constraints to ensure data integrity.
 *
 * <p><strong>Required fields:</strong>
 * <ul>
 *   <li>organizationId - The organization to which the user belongs</li>
 *   <li>email - Valid email address (unique within organization)</li>
 * </ul>
 *
 * <p><strong>Optional fields:</strong>
 * <ul>
 *   <li>username - Must be unique within organization if provided</li>
 *   <li>displayName - Human-readable name</li>
 *   <li>externalId - External IdP identifier</li>
 *   <li>attributes - Custom ABAC attributes</li>
 * </ul>
 *
 * @see UserResponse
 * @see UserUpdateRequest
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "User creation request",
        example = """
                {
                  "organizationId": "123e4567-e89b-12d3-a456-426614174001",
                  "email": "user@example.com",
                  "username": "john.doe",
                  "displayName": "John Doe",
                  "externalId": "okta:00u1234567890",
                  "attributes": {
                    "department": "Engineering",
                    "title": "Senior Developer"
                  }
                }
                """
)
public class UserCreateRequest {

    /**
     * Organization ID to which the user belongs.
     */
    @Schema(
            description = "Organization ID to which the user belongs",
            example = "123e4567-e89b-12d3-a456-426614174001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    /**
     * User's email address.
     * Must be valid email format and unique within the organization.
     */
    @Schema(
            description = "User's email address (unique within organization)",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
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
     * Custom attributes for the user (ABAC support).
     */
    @Schema(
            description = "Custom attributes for the user stored as key-value pairs",
            example = """
                    {
                      "department": "Engineering",
                      "title": "Senior Developer",
                      "location": "US-West",
                      "clearanceLevel": "confidential"
                    }
                    """
    )
    private Map<String, Object> attributes;
}
