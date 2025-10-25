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
 * Permission entity representing a fine-grained permission for resource access control.
 *
 * <p>Permissions define what actions can be performed on specific resource types.
 * Each permission belongs to a specific organization for multi-tenancy isolation.
 *
 * <p>Key features:
 * <ul>
 *   <li>Multi-tenant: Each permission belongs to one organization</li>
 *   <li>Resource-action model: Defines action on resource type</li>
 *   <li>Effect: Allow or deny the action</li>
 *   <li>JSONB conditions field for ABAC (Attribute-Based Access Control)</li>
 *   <li>Unique constraint on (organization, name) and (organization, resource_type, action)</li>
 *   <li>Automatic timestamp management for created/updated dates</li>
 * </ul>
 *
 * <p>Example permissions:
 * <ul>
 *   <li>document:read - Allow reading documents</li>
 *   <li>document:write - Allow writing documents</li>
 *   <li>project:delete - Allow deleting projects</li>
 *   <li>api:invoke - Allow invoking API endpoints</li>
 * </ul>
 *
 * @see Organization
 * @see io.authplatform.platform.domain.repository.PermissionRepository
 */
@Entity
@Table(
    name = "permissions",
    uniqueConstraints = {
        @UniqueConstraint(name = "permissions_org_name_unique", columnNames = {"organization_id", "name"}),
        @UniqueConstraint(name = "permissions_org_resource_action_unique", columnNames = {"organization_id", "resource_type", "action"})
    },
    indexes = {
        @Index(name = "idx_permissions_organization_id", columnList = "organization_id"),
        @Index(name = "idx_permissions_resource_type", columnList = "resource_type"),
        @Index(name = "idx_permissions_action", columnList = "action")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    /**
     * Unique identifier for the permission.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Organization to which this permission belongs.
     * Required for multi-tenancy isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, foreignKey = @ForeignKey(name = "permissions_organization_id_fkey"))
    private Organization organization;

    /**
     * Permission name identifier.
     * Must be unique within the organization.
     *
     * <p>Naming convention: resource_type:action
     * <p>Examples: "document:read", "project:write", "api:invoke"
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Human-readable display name for the permission.
     *
     * <p>Example: "Read Documents", "Write Projects", "Invoke API"
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Optional description of the permission's purpose and scope.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Type of resource this permission applies to.
     *
     * <p>Resource types represent the entities or objects in the system.
     * <p>Examples:
     * <ul>
     *   <li>document - Document resources</li>
     *   <li>project - Project resources</li>
     *   <li>user - User management</li>
     *   <li>api - API endpoints</li>
     *   <li>billing - Billing operations</li>
     * </ul>
     */
    @Column(name = "resource_type", nullable = false, length = 255)
    private String resourceType;

    /**
     * Action that can be performed on the resource.
     *
     * <p>Actions represent the operations allowed on resources.
     * <p>Common actions:
     * <ul>
     *   <li>read - View/read the resource</li>
     *   <li>write - Create/update the resource</li>
     *   <li>delete - Delete the resource</li>
     *   <li>execute - Execute/invoke the resource</li>
     *   <li>manage - Full management access</li>
     *   <li>share - Share the resource with others</li>
     * </ul>
     */
    @Column(name = "action", nullable = false, length = 255)
    private String action;

    /**
     * Effect of the permission (allow or deny).
     *
     * <p>Valid values:
     * <ul>
     *   <li>allow - Grant the action on the resource</li>
     *   <li>deny - Explicitly deny the action (takes precedence over allow)</li>
     * </ul>
     *
     * <p>Deny permissions take precedence over allow permissions in policy evaluation.
     */
    @Column(name = "effect", nullable = false, length = 50)
    @Convert(converter = PermissionEffectConverter.class)
    @Builder.Default
    private PermissionEffect effect = PermissionEffect.ALLOW;

    /**
     * Additional conditions for ABAC (Attribute-Based Access Control).
     *
     * <p>This field enables fine-grained access control by specifying conditions
     * that must be met for the permission to be granted. Used in Phase 2 for ABAC.
     *
     * <p>Common condition types:
     * <ul>
     *   <li>Resource attributes (owner, status, tags)</li>
     *   <li>User attributes (department, clearance level)</li>
     *   <li>Environmental attributes (time, location, IP)</li>
     *   <li>Request attributes (method, headers)</li>
     * </ul>
     *
     * <p>Example conditions:
     * <pre>{@code
     * {
     *   "resource": {
     *     "owner": "${user.id}",
     *     "status": "active"
     *   },
     *   "user": {
     *     "department": "Engineering",
     *     "clearanceLevel": "confidential"
     *   },
     *   "environment": {
     *     "timeRange": {
     *       "start": "09:00",
     *       "end": "17:00"
     *     },
     *     "allowedIPs": ["10.0.0.0/8"]
     *   }
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> conditions = Map.of();

    /**
     * Timestamp when the permission was created.
     * Automatically set by the database on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the permission was last updated.
     * Automatically updated by database trigger on each update.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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
     * Check if this is an allow permission.
     *
     * @return true if effect is ALLOW, false otherwise
     */
    public boolean isAllow() {
        return effect == PermissionEffect.ALLOW;
    }

    /**
     * Check if this is a deny permission.
     *
     * @return true if effect is DENY, false otherwise
     */
    public boolean isDeny() {
        return effect == PermissionEffect.DENY;
    }

    /**
     * Check if this permission has conditions.
     *
     * @return true if conditions map is not empty, false otherwise
     */
    public boolean hasConditions() {
        return conditions != null && !conditions.isEmpty();
    }

    /**
     * Get the full permission identifier (resource_type:action).
     *
     * @return permission identifier string
     */
    public String getFullIdentifier() {
        return resourceType + ":" + action;
    }

    /**
     * Permission effect enumeration.
     *
     * <p>Note: Values are stored in lowercase in the database.
     */
    public enum PermissionEffect {
        /**
         * Grant the action on the resource.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("allow")
        ALLOW("allow"),

        /**
         * Explicitly deny the action.
         * Deny permissions take precedence over allow permissions.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("deny")
        DENY("deny");

        private final String value;

        PermissionEffect(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
