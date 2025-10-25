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
 * Role entity representing a role within an organization with hierarchy support.
 *
 * <p>Roles are hierarchical and can inherit permissions from parent roles.
 * Each role belongs to a specific organization for multi-tenancy isolation.
 *
 * <p>Key features:
 * <ul>
 *   <li>Multi-tenant: Each role belongs to one organization</li>
 *   <li>Hierarchy support: Roles can have parent roles for inheritance</li>
 *   <li>Level tracking: Depth in hierarchy (0=root, max 10)</li>
 *   <li>System roles: Cannot be deleted or modified</li>
 *   <li>Soft delete support via {@code deletedAt} timestamp</li>
 *   <li>JSONB metadata field for flexible configuration</li>
 *   <li>Automatic timestamp management for created/updated dates</li>
 * </ul>
 *
 * @see Organization
 * @see io.authplatform.platform.domain.repository.RoleRepository
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "roles_org_name_unique", columnNames = {"organization_id", "name"})
    },
    indexes = {
        @Index(name = "idx_roles_organization_id", columnList = "organization_id"),
        @Index(name = "idx_roles_parent_role_id", columnList = "parent_role_id"),
        @Index(name = "idx_roles_is_system", columnList = "is_system")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    /**
     * Unique identifier for the role.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Organization to which this role belongs.
     * Required for multi-tenancy isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, foreignKey = @ForeignKey(name = "roles_organization_id_fkey"))
    private Organization organization;

    /**
     * Role name identifier.
     * Must be unique within the organization.
     *
     * <p>Example: "admin", "developer", "viewer"
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Human-readable display name for the role.
     *
     * <p>Example: "Administrator", "Developer", "Viewer"
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Optional description of the role's purpose and permissions.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Parent role for inheritance in role hierarchy.
     *
     * <p>When set, this role inherits all permissions from the parent role.
     * This enables hierarchical RBAC where child roles automatically receive
     * parent role permissions.
     *
     * <p>Example hierarchy:
     * <pre>{@code
     * admin (level 0)
     *   └─ developer (level 1)
     *       └─ junior-developer (level 2)
     * }</pre>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id", foreignKey = @ForeignKey(name = "roles_parent_role_id_fkey"))
    private Role parentRole;

    /**
     * Depth level in the role hierarchy.
     *
     * <p>Valid range: 0 (root) to 10 (maximum depth)
     * <ul>
     *   <li>0: Root role with no parent</li>
     *   <li>1-10: Child roles at various depths</li>
     * </ul>
     *
     * <p>The level must be consistent with the parent role:
     * {@code level = parentRole.level + 1}
     */
    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 0;

    /**
     * System role flag.
     *
     * <p>When true, this role is system-defined and cannot be:
     * <ul>
     *   <li>Deleted</li>
     *   <li>Renamed</li>
     *   <li>Modified in certain ways (enforced by business logic)</li>
     * </ul>
     *
     * <p>System roles are typically created during database initialization
     * or application bootstrap (e.g., "superadmin", "system-viewer").
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    /**
     * Custom metadata for the role stored as JSONB.
     *
     * <p>This field allows flexible configuration without schema changes.
     * Common use cases:
     * <ul>
     *   <li>UI configuration (icon, color, badge)</li>
     *   <li>Feature flags for role-specific features</li>
     *   <li>Integration settings</li>
     *   <li>Custom business metadata</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * {
     *   "ui": {
     *     "icon": "shield",
     *     "color": "#3B82F6",
     *     "badge": "ADMIN"
     *   },
     *   "features": {
     *     "canManageBilling": true,
     *     "canAccessAuditLogs": true
     *   },
     *   "maxUsers": 100
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    /**
     * Timestamp when the role was created.
     * Automatically set by the database on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the role was last updated.
     * Automatically updated by database trigger on each update.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Timestamp when the role was soft-deleted.
     *
     * <p>When not null, the role is considered deleted and should be
     * excluded from normal queries. Use {@link #isDeleted()} to check deletion status.
     *
     * <p>Note: System roles cannot be soft-deleted.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Lifecycle callback to set timestamps before persisting.
     */
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Lifecycle callback to update timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Check if the role is deleted.
     *
     * @return true if deletedAt is not null, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if the role is a root role (no parent).
     *
     * @return true if parentRole is null, false otherwise
     */
    public boolean isRootRole() {
        return parentRole == null;
    }

    /**
     * Soft delete the role by setting deletedAt timestamp.
     *
     * <p>Note: This method does not check if the role is a system role.
     * The caller should validate that system roles are not deleted.
     *
     * @throws IllegalStateException if the role is a system role (checked by caller)
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    /**
     * Restore a soft-deleted role.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Get the hierarchy path from root to this role.
     *
     * <p>Example: For a role hierarchy admin → developer → junior-developer,
     * calling this on junior-developer would return "admin/developer/junior-developer"
     *
     * @return hierarchy path as a string
     */
    public String getHierarchyPath() {
        if (parentRole == null) {
            return name;
        }
        return parentRole.getHierarchyPath() + "/" + name;
    }
}
