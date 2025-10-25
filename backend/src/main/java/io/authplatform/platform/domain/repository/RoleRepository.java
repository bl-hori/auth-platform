package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Role} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for role management with hierarchy support.
 * All queries automatically exclude soft-deleted roles unless explicitly included.
 * Queries are scoped to organizations for multi-tenancy support.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private RoleRepository roleRepository;
 *
 * // Find role by name in organization
 * Optional<Role> role = roleRepository.findByOrganizationIdAndNameAndDeletedAtIsNull(
 *     orgId, "admin");
 *
 * // Get all root roles (no parent)
 * List<Role> rootRoles = roleRepository.findRootRolesByOrganizationId(orgId);
 *
 * // Get child roles
 * List<Role> childRoles = roleRepository.findByParentRoleIdAndDeletedAtIsNull(parentId);
 * }</pre>
 *
 * @see Role
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find role by name within an organization, excluding soft-deleted roles.
     *
     * @param organizationId the organization ID
     * @param name the role name
     * @return Optional containing the role if found and not deleted
     */
    Optional<Role> findByOrganizationIdAndNameAndDeletedAtIsNull(UUID organizationId, String name);

    /**
     * Find role by ID, excluding soft-deleted roles.
     *
     * @param id the role ID
     * @return Optional containing the role if found and not deleted
     */
    @Query("SELECT r FROM Role r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Role> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Find all roles in an organization, excluding soft-deleted.
     *
     * @param organizationId the organization ID
     * @return list of roles that are not soft-deleted
     */
    @Query("SELECT r FROM Role r WHERE r.organization.id = :organizationId AND r.deletedAt IS NULL")
    List<Role> findAllNotDeletedByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all root roles (no parent) in an organization, excluding soft-deleted.
     *
     * <p>Root roles have {@code level = 0} and {@code parentRole = null}.
     *
     * @param organizationId the organization ID
     * @return list of root roles
     */
    @Query("SELECT r FROM Role r WHERE r.organization.id = :organizationId AND r.parentRole IS NULL AND r.deletedAt IS NULL")
    List<Role> findRootRolesByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all child roles of a parent role, excluding soft-deleted.
     *
     * @param parentRoleId the parent role ID
     * @return list of child roles
     */
    List<Role> findByParentRoleIdAndDeletedAtIsNull(UUID parentRoleId);

    /**
     * Find all system roles in an organization, excluding soft-deleted.
     *
     * <p>System roles have {@code isSystem = true} and cannot be deleted or modified.
     *
     * @param organizationId the organization ID
     * @return list of system roles
     */
    @Query("SELECT r FROM Role r WHERE r.organization.id = :organizationId AND r.isSystem = true AND r.deletedAt IS NULL")
    List<Role> findSystemRolesByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all non-system roles in an organization, excluding soft-deleted.
     *
     * @param organizationId the organization ID
     * @return list of non-system roles
     */
    @Query("SELECT r FROM Role r WHERE r.organization.id = :organizationId AND r.isSystem = false AND r.deletedAt IS NULL")
    List<Role> findNonSystemRolesByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all roles at a specific hierarchy level in an organization, excluding soft-deleted.
     *
     * @param organizationId the organization ID
     * @param level the hierarchy level (0-10)
     * @return list of roles at the specified level
     */
    @Query("SELECT r FROM Role r WHERE r.organization.id = :organizationId AND r.level = :level AND r.deletedAt IS NULL")
    List<Role> findByOrganizationIdAndLevel(@Param("organizationId") UUID organizationId, @Param("level") Integer level);

    /**
     * Check if a role with the given name exists in an organization (excluding deleted).
     *
     * @param organizationId the organization ID
     * @param name the role name
     * @return true if a role with the name exists and is not deleted
     */
    boolean existsByOrganizationIdAndNameAndDeletedAtIsNull(UUID organizationId, String name);

    /**
     * Count all non-deleted roles in an organization.
     *
     * @param organizationId the organization ID
     * @return number of non-deleted roles
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.organization.id = :organizationId AND r.deletedAt IS NULL")
    long countNotDeletedByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Count system roles in an organization.
     *
     * @param organizationId the organization ID
     * @return number of system roles
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.organization.id = :organizationId AND r.isSystem = true AND r.deletedAt IS NULL")
    long countSystemRolesByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find roles by name pattern (case-insensitive) within an organization, excluding deleted.
     *
     * @param organizationId the organization ID
     * @param namePattern the name pattern to search for (use % for wildcards)
     * @return list of matching roles
     */
    @Query("SELECT r FROM Role r WHERE r.organization.id = :organizationId AND LOWER(r.name) LIKE LOWER(:namePattern) AND r.deletedAt IS NULL")
    List<Role> findByNameContainingIgnoreCaseAndNotDeleted(@Param("organizationId") UUID organizationId, @Param("namePattern") String namePattern);

    /**
     * Find all descendants (children, grandchildren, etc.) of a role recursively.
     *
     * <p>This uses a recursive CTE to traverse the role hierarchy tree.
     *
     * @param roleId the parent role ID
     * @return list of all descendant roles
     */
    @Query(value = """
        WITH RECURSIVE role_descendants AS (
            SELECT id, organization_id, name, display_name, description, parent_role_id, level, is_system, metadata, created_at, updated_at, deleted_at
            FROM roles
            WHERE id = :roleId AND deleted_at IS NULL

            UNION ALL

            SELECT r.id, r.organization_id, r.name, r.display_name, r.description, r.parent_role_id, r.level, r.is_system, r.metadata, r.created_at, r.updated_at, r.deleted_at
            FROM roles r
            INNER JOIN role_descendants rd ON r.parent_role_id = rd.id
            WHERE r.deleted_at IS NULL
        )
        SELECT * FROM role_descendants WHERE id != :roleId
        """, nativeQuery = true)
    List<Role> findAllDescendants(@Param("roleId") UUID roleId);

    /**
     * Find all ancestors (parent, grandparent, etc.) of a role recursively.
     *
     * <p>This uses a recursive CTE to traverse the role hierarchy tree upwards.
     *
     * @param roleId the child role ID
     * @return list of all ancestor roles
     */
    @Query(value = """
        WITH RECURSIVE role_ancestors AS (
            SELECT id, organization_id, name, display_name, description, parent_role_id, level, is_system, metadata, created_at, updated_at, deleted_at
            FROM roles
            WHERE id = :roleId AND deleted_at IS NULL

            UNION ALL

            SELECT r.id, r.organization_id, r.name, r.display_name, r.description, r.parent_role_id, r.level, r.is_system, r.metadata, r.created_at, r.updated_at, r.deleted_at
            FROM roles r
            INNER JOIN role_ancestors ra ON r.id = ra.parent_role_id
            WHERE r.deleted_at IS NULL
        )
        SELECT * FROM role_ancestors WHERE id != :roleId
        """, nativeQuery = true)
    List<Role> findAllAncestors(@Param("roleId") UUID roleId);
}
