package io.authplatform.platform.api.dto.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for policy data.
 *
 * <p><strong>Example JSON:</strong>
 * <pre>{@code
 * {
 *   "id": "123e4567-e89b-12d3-a456-426614174000",
 *   "organizationId": "org-001",
 *   "name": "document-access-policy",
 *   "displayName": "Document Access Policy",
 *   "description": "Controls access to documents",
 *   "status": "active",
 *   "currentVersion": 2,
 *   "createdAt": "2025-10-26T10:00:00Z",
 *   "updatedAt": "2025-10-26T11:00:00Z",
 *   "publishedAt": "2025-10-26T11:00:00Z"
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Policy response data")
public class PolicyResponse {

    @Schema(description = "Policy ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Organization ID", example = "org-001")
    @JsonProperty("organizationId")
    private UUID organizationId;

    @Schema(description = "Unique policy name", example = "document-access-policy")
    private String name;

    @Schema(description = "Human-readable display name", example = "Document Access Policy")
    @JsonProperty("displayName")
    private String displayName;

    @Schema(description = "Policy description", example = "Controls access to documents based on user roles")
    private String description;

    @Schema(description = "Policy status", example = "active", allowableValues = {"draft", "active", "archived"})
    private String status;

    @Schema(description = "Policy type", example = "rego", allowableValues = {"rego", "cedar"})
    @JsonProperty("policyType")
    private String policyType;

    @Schema(description = "Current version number", example = "2")
    @JsonProperty("currentVersion")
    private Integer currentVersion;

    @Schema(description = "Creation timestamp", example = "2025-10-26T10:00:00Z")
    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2025-10-26T11:00:00Z")
    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;

    @Schema(description = "Publication timestamp (when policy became active)", example = "2025-10-26T11:00:00Z")
    @JsonProperty("publishedAt")
    private OffsetDateTime publishedAt;

    @Schema(description = "User ID who published the policy", example = "user-001")
    @JsonProperty("publishedBy")
    private String publishedBy;

    @Schema(description = "User ID who created the policy", example = "user-001")
    @JsonProperty("createdBy")
    private String createdBy;
}
