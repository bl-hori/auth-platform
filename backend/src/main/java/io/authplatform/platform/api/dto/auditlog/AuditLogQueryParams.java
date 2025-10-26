package io.authplatform.platform.api.dto.auditlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Query parameters for filtering audit logs.
 *
 * <p>All parameters are optional and can be combined for complex filtering.
 *
 * <p><strong>Example Query:</strong>
 * <pre>{@code
 * GET /v1/audit-logs?organizationId=org-001&eventType=authorization
 *     &decision=deny&startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Query parameters for filtering audit logs")
public class AuditLogQueryParams {

    @Schema(description = "Organization ID (required)", example = "org-001", required = true)
    private UUID organizationId;

    @Schema(description = "Event type filter", example = "authorization",
            allowableValues = {"authorization", "admin_action", "policy_change", "role_assignment"})
    private String eventType;

    @Schema(description = "Actor (user) ID filter", example = "user-001")
    private UUID actorId;

    @Schema(description = "Resource type filter", example = "document")
    private String resourceType;

    @Schema(description = "Resource ID filter", example = "doc-123")
    private String resourceId;

    @Schema(description = "Action filter", example = "read")
    private String action;

    @Schema(description = "Decision filter (for authorization events)", example = "allow",
            allowableValues = {"allow", "deny"})
    private String decision;

    @Schema(description = "Start date for time range filter", example = "2025-01-01T00:00:00Z")
    private OffsetDateTime startDate;

    @Schema(description = "End date for time range filter", example = "2025-12-31T23:59:59Z")
    private OffsetDateTime endDate;
}
