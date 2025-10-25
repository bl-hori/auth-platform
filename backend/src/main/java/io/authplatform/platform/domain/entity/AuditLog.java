package io.authplatform.platform.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AuditLog entity for comprehensive audit trail tracking.
 *
 * <p>This entity records all authorization decisions, admin actions, and policy changes
 * for compliance, security monitoring, and troubleshooting purposes.
 *
 * <p>Key features:
 * <ul>
 *   <li>Partitioned by timestamp (monthly) for performance</li>
 *   <li>Composite primary key: (id, timestamp)</li>
 *   <li>Multi-tenant: Each log belongs to one organization</li>
 *   <li>Comprehensive event tracking: authorization, admin actions, policy changes</li>
 *   <li>Request/response data in JSONB format</li>
 *   <li>IP address and user agent tracking</li>
 *   <li>Immutable: Once created, logs should not be modified</li>
 * </ul>
 *
 * <p>Event types:
 * <ul>
 *   <li>authorization: Authorization decision events</li>
 *   <li>admin_action: Administrative operations</li>
 *   <li>policy_change: Policy create/update/delete events</li>
 *   <li>role_assignment: Role assignment/revocation</li>
 *   <li>permission_change: Permission modifications</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Log an authorization decision
 * AuditLog log = AuditLog.builder()
 *     .organization(org)
 *     .eventType("authorization")
 *     .actor(user)
 *     .actorEmail(user.getEmail())
 *     .resourceType("document")
 *     .resourceId("doc-123")
 *     .action("read")
 *     .decision("allow")
 *     .decisionReason("User has viewer role")
 *     .ipAddress("192.168.1.1")
 *     .userAgent("Mozilla/5.0...")
 *     .build();
 * auditLogRepository.save(log);
 * }</pre>
 *
 * @see Organization
 * @see User
 * @see io.authplatform.platform.domain.repository.AuditLogRepository
 */
@Entity
@Table(name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_organization_id", columnList = "organization_id"),
        @Index(name = "idx_audit_logs_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /**
     * Unique identifier for the audit log entry.
     * Note: In the database, this is part of a composite primary key with timestamp,
     * but for JPA/Hibernate compatibility with partitioned tables, we use a single ID.
     * Generated using UUID v4.
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    /**
     * Timestamp of the event.
     * Part of composite primary key in the database (required for partitioning).
     * Automatically set on creation.
     * Note: This is NOT marked as @Id in JPA to avoid composite key complexity.
     */
    @Column(name = "timestamp", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    /**
     * Organization to which this audit log belongs.
     * Required for multi-tenancy isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Type of event being logged.
     *
     * <p>Common event types:
     * <ul>
     *   <li>authorization - Authorization decision</li>
     *   <li>admin_action - Administrative operation</li>
     *   <li>policy_change - Policy modification</li>
     *   <li>role_assignment - Role assignment/revocation</li>
     *   <li>permission_change - Permission modification</li>
     *   <li>user_login - User authentication</li>
     *   <li>user_logout - User session termination</li>
     * </ul>
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * User who performed the action.
     * Can be null for system-initiated events or unauthenticated requests.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /**
     * Email of the actor at the time of the event.
     * Stored separately in case the user is deleted later.
     */
    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    /**
     * Type of resource being accessed or modified.
     *
     * <p>Examples: "document", "project", "policy", "role", "user"
     */
    @Column(name = "resource_type", length = 255)
    private String resourceType;

    /**
     * Identifier of the specific resource instance.
     *
     * <p>Examples: "doc-123", "project-456", "policy-789"
     */
    @Column(name = "resource_id", length = 255)
    private String resourceId;

    /**
     * Action being performed or requested.
     *
     * <p>Examples:
     * <ul>
     *   <li>Authorization: "read", "write", "delete"</li>
     *   <li>Admin: "create_user", "update_role", "delete_policy"</li>
     *   <li>Auth: "login", "logout", "refresh_token"</li>
     * </ul>
     */
    @Column(name = "action", nullable = false, length = 255)
    private String action;

    /**
     * Authorization decision result.
     *
     * <p>Valid values:
     * <ul>
     *   <li>allow - Access granted</li>
     *   <li>deny - Access denied</li>
     *   <li>error - Error during evaluation</li>
     * </ul>
     *
     * <p>Null for non-authorization events.
     */
    @Column(name = "decision", length = 50)
    private String decision;

    /**
     * Reason for the authorization decision.
     *
     * <p>Examples:
     * <ul>
     *   <li>"User has admin role"</li>
     *   <li>"Policy document-access-policy denied access"</li>
     *   <li>"No matching policy found"</li>
     * </ul>
     */
    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    /**
     * Request data in JSONB format.
     * Contains context about the authorization request or admin action.
     *
     * <p>Example structure:
     * <pre>{@code
     * {
     *   "user": {"id": "user-123", "roles": ["editor"]},
     *   "resource": {"type": "document", "id": "doc-123"},
     *   "context": {"time": "2025-01-15T10:30:00Z"}
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", columnDefinition = "jsonb")
    private Map<String, Object> requestData;

    /**
     * Response data in JSONB format.
     * Contains the result of the operation.
     *
     * <p>Example structure:
     * <pre>{@code
     * {
     *   "decision": "allow",
     *   "policies_evaluated": ["policy-1", "policy-2"],
     *   "duration_ms": 15
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_data", columnDefinition = "jsonb")
    private Map<String, Object> responseData;

    /**
     * IP address of the client making the request.
     * Stored as VARCHAR for simplicity and Hibernate compatibility.
     * Supports both IPv4 and IPv6 addresses.
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * User agent string from the HTTP request.
     * Useful for identifying client type and browser.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Lifecycle callback to set timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = OffsetDateTime.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    /**
     * Check if this is an authorization event.
     *
     * @return true if event type is "authorization"
     */
    public boolean isAuthorizationEvent() {
        return "authorization".equals(eventType);
    }

    /**
     * Check if this is an admin action event.
     *
     * @return true if event type is "admin_action"
     */
    public boolean isAdminAction() {
        return "admin_action".equals(eventType);
    }

    /**
     * Check if this is a policy change event.
     *
     * @return true if event type is "policy_change"
     */
    public boolean isPolicyChange() {
        return "policy_change".equals(eventType);
    }

    /**
     * Check if the decision was "allow".
     *
     * @return true if decision is "allow", false otherwise
     */
    public boolean isAllowed() {
        return "allow".equals(decision);
    }

    /**
     * Check if the decision was "deny".
     *
     * @return true if decision is "deny", false otherwise
     */
    public boolean isDenied() {
        return "deny".equals(decision);
    }

    /**
     * Check if the decision resulted in an error.
     *
     * @return true if decision is "error", false otherwise
     */
    public boolean isError() {
        return "error".equals(decision);
    }

    /**
     * Get a human-readable description of the event.
     *
     * @return event description string
     */
    public String getEventDescription() {
        StringBuilder desc = new StringBuilder();
        if (actor != null) {
            desc.append(actorEmail != null ? actorEmail : actor.getEmail());
        } else {
            desc.append("System");
        }
        desc.append(" performed ").append(action);
        if (resourceType != null) {
            desc.append(" on ").append(resourceType);
            if (resourceId != null) {
                desc.append(":").append(resourceId);
            }
        }
        if (decision != null) {
            desc.append(" - ").append(decision);
        }
        return desc.toString();
    }
}
