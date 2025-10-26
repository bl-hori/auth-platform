package io.authplatform.platform.api.dto;

import io.authplatform.platform.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * User response DTO containing user information.
 *
 * <p>This DTO is used to return user data from the API, excluding sensitive
 * information and providing a clean interface for clients.
 *
 * <p><strong>Usage:</strong>
 * <ul>
 *   <li>GET /v1/users/{id} - Single user retrieval</li>
 *   <li>GET /v1/users - User list (as part of pagination response)</li>
 *   <li>POST /v1/users - After user creation</li>
 *   <li>PUT /v1/users/{id} - After user update</li>
 * </ul>
 *
 * @see User
 * @see UserCreateRequest
 * @see UserUpdateRequest
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "User information response",
        example = """
                {
                  "id": "123e4567-e89b-12d3-a456-426614174000",
                  "organizationId": "123e4567-e89b-12d3-a456-426614174001",
                  "email": "user@example.com",
                  "username": "john.doe",
                  "displayName": "John Doe",
                  "externalId": "okta:00u1234567890",
                  "status": "active",
                  "attributes": {
                    "department": "Engineering",
                    "title": "Senior Developer"
                  },
                  "createdAt": "2024-01-15T10:30:00Z",
                  "updatedAt": "2024-01-20T14:45:00Z",
                  "deletedAt": null
                }
                """
)
public class UserResponse {

    /**
     * Unique identifier for the user.
     */
    @Schema(
            description = "Unique identifier for the user",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID id;

    /**
     * Organization ID to which the user belongs.
     */
    @Schema(
            description = "Organization ID to which the user belongs",
            example = "123e4567-e89b-12d3-a456-426614174001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID organizationId;

    /**
     * User's email address.
     */
    @Schema(
            description = "User's email address",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    /**
     * Optional username for the user.
     */
    @Schema(
            description = "Optional username for the user",
            example = "john.doe"
    )
    private String username;

    /**
     * Human-readable display name for the user.
     */
    @Schema(
            description = "Human-readable display name for the user",
            example = "John Doe"
    )
    private String displayName;

    /**
     * External identity provider user ID.
     */
    @Schema(
            description = "External identity provider user ID",
            example = "okta:00u1234567890"
    )
    private String externalId;

    /**
     * Current status of the user.
     */
    @Schema(
            description = "Current status of the user",
            example = "active",
            allowableValues = {"active", "inactive", "suspended", "deleted"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String status;

    /**
     * Custom attributes for the user (ABAC support).
     */
    @Schema(
            description = "Custom attributes for the user stored as key-value pairs",
            example = """
                    {
                      "department": "Engineering",
                      "title": "Senior Developer",
                      "location": "US-West"
                    }
                    """
    )
    private Map<String, Object> attributes;

    /**
     * Timestamp when the user was created.
     */
    @Schema(
            description = "Timestamp when the user was created",
            example = "2024-01-15T10:30:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the user was last updated.
     */
    @Schema(
            description = "Timestamp when the user was last updated",
            example = "2024-01-20T14:45:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime updatedAt;

    /**
     * Timestamp when the user was soft-deleted (null if not deleted).
     */
    @Schema(
            description = "Timestamp when the user was soft-deleted (null if not deleted)",
            example = "null"
    )
    private OffsetDateTime deletedAt;

    /**
     * Create a UserResponse from a User entity.
     *
     * @param user the user entity
     * @return the user response DTO
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .organizationId(user.getOrganization().getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .externalId(user.getExternalId())
                .status(user.getStatus().getValue())
                .attributes(user.getAttributes())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}
