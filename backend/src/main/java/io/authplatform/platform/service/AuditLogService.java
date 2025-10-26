package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.auditlog.AuditLogQueryParams;
import io.authplatform.platform.api.dto.auditlog.AuditLogResponse;
import io.authplatform.platform.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service interface for audit logging operations.
 *
 * <p>Handles async logging of authorization decisions and administrative actions.
 * Provides query capabilities with time-range filtering and CSV export.
 *
 * <p><strong>Audit Event Types:</strong>
 * <ul>
 *   <li><strong>authorization:</strong> Authorization decision events</li>
 *   <li><strong>admin_action:</strong> Administrative operations (user/role changes)</li>
 *   <li><strong>policy_change:</strong> Policy create/update/publish events</li>
 *   <li><strong>role_assignment:</strong> Role assignment/revocation</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * @Autowired
 * private AuditLogService auditLogService;
 *
 * // Log an authorization decision asynchronously
 * auditLogService.logAuthorizationDecision(
 *     orgId, userId, "document", "doc-123", "read", "allow", "User has viewer role");
 *
 * // Query audit logs
 * AuditLogQueryParams params = AuditLogQueryParams.builder()
 *     .organizationId(orgId)
 *     .eventType("authorization")
 *     .startDate(startDate)
 *     .endDate(endDate)
 *     .build();
 * Page<AuditLogResponse> logs = auditLogService.queryAuditLogs(params, pageable);
 * }</pre>
 *
 * @see AuditLog
 */
public interface AuditLogService {

    /**
     * Log an authorization decision (async).
     *
     * <p>Records authorization evaluation results for compliance and security monitoring.
     *
     * @param organizationId the organization ID
     * @param userId the user ID making the request
     * @param resourceType the resource type (e.g., "document", "user")
     * @param resourceId the resource ID (optional)
     * @param action the action (e.g., "read", "write", "delete")
     * @param decision the authorization decision ("allow" or "deny")
     * @param decisionReason the reason for the decision
     */
    void logAuthorizationDecision(
            UUID organizationId,
            UUID userId,
            String resourceType,
            String resourceId,
            String action,
            String decision,
            String decisionReason
    );

    /**
     * Log an administrative action (async).
     *
     * <p>Records administrative operations such as user creation, role assignment, etc.
     *
     * @param organizationId the organization ID
     * @param actorId the user ID performing the action
     * @param action the action performed (e.g., "user.create", "role.assign")
     * @param resourceType the resource type affected
     * @param resourceId the resource ID affected
     * @param details additional details about the action
     */
    void logAdministrativeAction(
            UUID organizationId,
            UUID actorId,
            String action,
            String resourceType,
            String resourceId,
            String details
    );

    /**
     * Log a policy change event (async).
     *
     * <p>Records policy create/update/publish/delete events.
     *
     * @param organizationId the organization ID
     * @param actorId the user ID performing the change
     * @param policyId the policy ID
     * @param action the action (e.g., "policy.create", "policy.publish")
     * @param details change details
     */
    void logPolicyChange(
            UUID organizationId,
            UUID actorId,
            UUID policyId,
            String action,
            String details
    );

    /**
     * Query audit logs with filtering and pagination.
     *
     * @param params query parameters (filters)
     * @param pageable pagination parameters
     * @return page of audit log responses
     */
    Page<AuditLogResponse> queryAuditLogs(AuditLogQueryParams params, Pageable pageable);

    /**
     * Get a specific audit log by ID.
     *
     * @param logId the audit log ID
     * @return the audit log
     * @throws AuditLogNotFoundException if not found
     */
    AuditLogResponse getAuditLogById(UUID logId);

    /**
     * Export audit logs to CSV format.
     *
     * @param params query parameters (filters)
     * @return CSV content as string
     */
    String exportAuditLogsToCsv(AuditLogQueryParams params);

    /**
     * Delete audit logs older than the retention period.
     *
     * <p>Default retention: 90 days. This method should be called by a scheduled job.
     *
     * @param retentionDays number of days to retain logs
     * @return number of logs deleted
     */
    long deleteLogsOlderThan(int retentionDays);

    /**
     * Exception thrown when an audit log is not found.
     */
    class AuditLogNotFoundException extends RuntimeException {
        public AuditLogNotFoundException(String message) {
            super(message);
        }
    }
}
