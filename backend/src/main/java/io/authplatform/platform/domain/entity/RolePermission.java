package io.authplatform.platform.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * RolePermission entity representing the assignment of permissions to roles.
 *
 * <p>This is a join table entity that links roles to permissions, defining which
 * permissions are granted when a role is assigned to a user.
 *
 * <p>Key features:
 * <ul>
 *   <li>Many-to-many relationship between Role and Permission</li>
 *   <li>Unique constraint ensures no duplicate assignments</li>
 *   <li>Cascade delete when role or permission is deleted</li>
 *   <li>Creation timestamp for audit purposes</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Assign "document:read" permission to "editor" role
 * RolePermission rolePermission = RolePermission.builder()
 *     .role(editorRole)
 *     .permission(documentReadPermission)
 *     .build();
 * rolePermissionRepository.save(rolePermission);
 * }</pre>
 *
 * @see Role
 * @see Permission
 * @see io.authplatform.platform.domain.repository.RolePermissionRepository
 */
@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = {
        @UniqueConstraint(name = "role_permissions_unique", columnNames = {"role_id", "permission_id"})
    },
    indexes = {
        @Index(name = "idx_role_permissions_role_id", columnList = "role_id"),
        @Index(name = "idx_role_permissions_permission_id", columnList = "permission_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    /**
     * Unique identifier for the role-permission assignment.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Role to which the permission is assigned.
     * This relationship cascades on delete - when a role is deleted,
     * all its permission assignments are automatically removed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "role_permissions_role_id_fkey"))
    private Role role;

    /**
     * Permission that is assigned to the role.
     * This relationship cascades on delete - when a permission is deleted,
     * all role assignments are automatically removed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @ForeignKey(name = "role_permissions_permission_id_fkey"))
    private Permission permission;

    /**
     * Timestamp when the permission was assigned to the role.
     * Automatically set by the database on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Lifecycle callback to set creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    /**
     * Get the full permission identifier (resource_type:action) from the assigned permission.
     *
     * @return permission identifier string, or null if permission is not loaded
     */
    public String getPermissionIdentifier() {
        return permission != null ? permission.getFullIdentifier() : null;
    }

    /**
     * Get the role name from the assigned role.
     *
     * @return role name, or null if role is not loaded
     */
    public String getRoleName() {
        return role != null ? role.getName() : null;
    }

    /**
     * Check if this assignment links to a specific role by name.
     *
     * @param roleName the role name to check
     * @return true if the role matches, false otherwise
     */
    public boolean hasRole(String roleName) {
        return role != null && role.getName().equals(roleName);
    }

    /**
     * Check if this assignment links to a specific permission by identifier.
     *
     * @param permissionName the permission name to check
     * @return true if the permission matches, false otherwise
     */
    public boolean hasPermission(String permissionName) {
        return permission != null && permission.getName().equals(permissionName);
    }

    /**
     * Get a human-readable description of this role-permission assignment.
     *
     * @return assignment description string
     */
    public String getAssignmentDescription() {
        if (role == null || permission == null) {
            return "Incomplete assignment";
        }
        return String.format("Role '%s' has permission '%s' (%s)",
            role.getName(),
            permission.getName(),
            permission.getFullIdentifier());
    }
}
