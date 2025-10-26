package io.authplatform.platform.domain.entity;

import io.authplatform.platform.domain.listener.CacheInvalidationListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * UserRole entity representing the assignment of roles to users with optional resource scoping.
 *
 * <p>This is a join table entity that links users to roles with additional features:
 * <ul>
 *   <li>Resource scoping: Limit role to specific resource type/instance</li>
 *   <li>Time-limited assignments: Optional expiration date</li>
 *   <li>Audit trail: Track who granted the role and when</li>
 * </ul>
 *
 * <p>Resource scoping examples:
 * <ul>
 *   <li>Global role: user has "admin" role across entire organization (resource_type=null, resource_id=null)</li>
 *   <li>Type-scoped role: user has "editor" role for all "documents" (resource_type="document", resource_id=null)</li>
 *   <li>Instance-scoped role: user has "owner" role for document "doc-123" (resource_type="document", resource_id="doc-123")</li>
 * </ul>
 *
 * @see User
 * @see Role
 * @see io.authplatform.platform.domain.repository.UserRoleRepository
 */
@Entity
@Table(
    name = "user_roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "user_roles_unique", columnNames = {"user_id", "role_id", "resource_type", "resource_id"})
    },
    indexes = {
        @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
        @Index(name = "idx_user_roles_role_id", columnList = "role_id"),
        @Index(name = "idx_user_roles_resource", columnList = "resource_type,resource_id"),
        @Index(name = "idx_user_roles_expires_at", columnList = "expires_at")
    }
)
@EntityListeners(CacheInvalidationListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    /**
     * Unique identifier for the user-role assignment.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * User to whom the role is assigned.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "user_roles_user_id_fkey"))
    private User user;

    /**
     * Role that is assigned to the user.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "user_roles_role_id_fkey"))
    private Role role;

    /**
     * Optional resource type to limit the role scope.
     *
     * <p>When specified, this role assignment only applies to resources of this type.
     * When null, the role applies globally within the organization.
     *
     * <p>Examples:
     * <ul>
     *   <li>null - Global role (applies to all resources)</li>
     *   <li>"document" - Role applies to document resources</li>
     *   <li>"project" - Role applies to project resources</li>
     *   <li>"api" - Role applies to API resources</li>
     * </ul>
     */
    @Column(name = "resource_type", length = 255)
    private String resourceType;

    /**
     * Optional resource instance ID to limit the role scope.
     *
     * <p>When specified together with resource_type, this role assignment only applies
     * to a specific resource instance. When null but resource_type is set, the role
     * applies to all resources of that type.
     *
     * <p>Examples:
     * <ul>
     *   <li>null - Applies to all resources of the type (or globally if type is also null)</li>
     *   <li>"doc-123" - Role applies only to document with ID "doc-123"</li>
     *   <li>"project-456" - Role applies only to project with ID "project-456"</li>
     * </ul>
     *
     * <p>Note: resource_id should not be set if resource_type is null.
     */
    @Column(name = "resource_id", length = 255)
    private String resourceId;

    /**
     * User who granted this role assignment.
     *
     * <p>This provides an audit trail of who assigned the role.
     * Can be null for system-initiated assignments or initial setup.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", foreignKey = @ForeignKey(name = "user_roles_granted_by_fkey"))
    private User grantedBy;

    /**
     * Timestamp when the role was granted.
     * Automatically set by the database on insert.
     */
    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt;

    /**
     * Optional expiration timestamp for time-limited role assignments.
     *
     * <p>When set, the role assignment automatically becomes invalid after this time.
     * When null, the role assignment has no expiration.
     *
     * <p>Use cases:
     * <ul>
     *   <li>Temporary elevated access (e.g., admin for maintenance)</li>
     *   <li>Trial periods</li>
     *   <li>Time-bound project access</li>
     *   <li>Temporary delegation</li>
     * </ul>
     */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /**
     * Lifecycle callback to set grant timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.grantedAt = OffsetDateTime.now();
    }

    /**
     * Check if this role assignment has expired.
     *
     * @return true if expiresAt is set and in the past, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    /**
     * Check if this is a global role assignment (not scoped to any resource).
     *
     * @return true if both resourceType and resourceId are null
     */
    public boolean isGlobalScope() {
        return resourceType == null && resourceId == null;
    }

    /**
     * Check if this role is scoped to a resource type (but not a specific instance).
     *
     * @return true if resourceType is set but resourceId is null
     */
    public boolean isTypeScopedOnly() {
        return resourceType != null && resourceId == null;
    }

    /**
     * Check if this role is scoped to a specific resource instance.
     *
     * @return true if both resourceType and resourceId are set
     */
    public boolean isInstanceScoped() {
        return resourceType != null && resourceId != null;
    }

    /**
     * Get a human-readable description of the scope.
     *
     * @return scope description string
     */
    public String getScopeDescription() {
        if (isGlobalScope()) {
            return "Global (all resources)";
        } else if (isTypeScopedOnly()) {
            return "All " + resourceType + " resources";
        } else {
            return resourceType + ":" + resourceId;
        }
    }
}
