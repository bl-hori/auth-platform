package io.authplatform.platform.api.dto.auditlog;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for audit log data.
 *
 * <p><strong>Example JSON:</strong>
 * <pre>{@code
 * {
 *   "id": "log-001",
 *   "organizationId": "org-001",
 *   "timestamp": "2025-10-26T10:30:00Z",
 *   "eventType": "authorization",
 *   "actorId": "user-001",
 *   "actorEmail": "admin@example.com",
 *   "resourceType": "document",
 *   "resourceId": "doc-123",
 *   "action": "read",
 *   "decision": "allow",
 *   "decisionReason": "User has viewer role",
 *   "ipAddress": "192.168.1.100",
 *   "userAgent": "Mozilla/5.0",
 *   "metadata": {}
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Audit log response data")
public class AuditLogResponse {

    @Schema(description = "Audit log ID", example = "log-001")
    private UUID id;

    @Schema(description = "Organization ID", example = "org-001")
    @JsonProperty("organizationId")
    private UUID organizationId;

    @Schema(description = "Event timestamp", example = "2025-10-26T10:30:00Z")
    private OffsetDateTime timestamp;

    @Schema(description = "Event type", example = "authorization",
            allowableValues = {"authorization", "admin_action", "policy_change", "role_assignment"})
    @JsonProperty("eventType")
    private String eventType;

    @Schema(description = "Actor (user) ID", example = "user-001")
    @JsonProperty("actorId")
    private UUID actorId;

    @Schema(description = "Actor email", example = "admin@example.com")
    @JsonProperty("actorEmail")
    private String actorEmail;

    @Schema(description = "Resource type", example = "document")
    @JsonProperty("resourceType")
    private String resourceType;

    @Schema(description = "Resource ID", example = "doc-123")
    @JsonProperty("resourceId")
    private String resourceId;

    @Schema(description = "Action performed", example = "read")
    private String action;

    @Schema(description = "Authorization decision (for authorization events)", example = "allow")
    private String decision;

    @Schema(description = "Reason for the decision", example = "User has viewer role")
    @JsonProperty("decisionReason")
    private String decisionReason;

    @Schema(description = "IP address of the requester", example = "192.168.1.100")
    @JsonProperty("ipAddress")
    private String ipAddress;

    @Schema(description = "User agent string", example = "Mozilla/5.0")
    @JsonProperty("userAgent")
    private String userAgent;

    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;
}
