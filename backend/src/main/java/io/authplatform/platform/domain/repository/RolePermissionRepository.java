package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link RolePermission} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for role-permission assignments.
 * This repository supports queries to find permissions assigned to roles and roles
 * that have specific permissions.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private RolePermissionRepository rolePermissionRepository;
 *
 * // Find all permissions for a role
 * List<RolePermission> permissions = rolePermissionRepository.findByRoleId(roleId);
 *
 * // Find all roles that have a specific permission
 * List<RolePermission> roles = rolePermissionRepository.findByPermissionId(permissionId);
 *
 * // Check if a role has a specific permission
 * boolean hasPermission = rolePermissionRepository.existsByRoleIdAndPermissionId(
 *     roleId, permissionId);
 * }</pre>
 *
 * @see RolePermission
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    /**
     * Find all permission assignments for a role.
     *
     * @param roleId the role ID
     * @return list of role-permission assignments
     */
    List<RolePermission> findByRoleId(UUID roleId);

    /**
     * Find all role assignments for a permission.
     *
     * @param permissionId the permission ID
     * @return list of role-permission assignments
     */
    List<RolePermission> findByPermissionId(UUID permissionId);

    /**
     * Find specific role-permission assignment.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @return Optional containing the assignment if found
     */
    Optional<RolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    /**
     * Find all permissions for roles in a specific organization.
     *
     * @param organizationId the organization ID
     * @return list of role-permission assignments
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.organization.id = :organizationId")
    List<RolePermission> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all role-permission assignments for a specific resource type.
     *
     * @param organizationId the organization ID
     * @param resourceType the resource type
     * @return list of role-permission assignments
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.organization.id = :organizationId " +
           "AND rp.permission.resourceType = :resourceType")
    List<RolePermission> findByOrganizationIdAndResourceType(
            @Param("organizationId") UUID organizationId,
            @Param("resourceType") String resourceType);

    /**
     * Find all role-permission assignments for a specific action.
     *
     * @param organizationId the organization ID
     * @param action the action
     * @return list of role-permission assignments
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.organization.id = :organizationId " +
           "AND rp.permission.action = :action")
    List<RolePermission> findByOrganizationIdAndAction(
            @Param("organizationId") UUID organizationId,
            @Param("action") String action);

    /**
     * Find all permissions assigned to system roles in an organization.
     *
     * @param organizationId the organization ID
     * @return list of role-permission assignments for system roles
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.organization.id = :organizationId " +
           "AND rp.role.isSystem = true")
    List<RolePermission> findSystemRolePermissions(@Param("organizationId") UUID organizationId);

    /**
     * Find all ALLOW permissions for a role.
     *
     * @param roleId the role ID
     * @return list of role-permission assignments with ALLOW effect
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId " +
           "AND rp.permission.effect = io.authplatform.platform.domain.entity.Permission$PermissionEffect.ALLOW")
    List<RolePermission> findAllowPermissionsByRoleId(@Param("roleId") UUID roleId);

    /**
     * Find all DENY permissions for a role.
     *
     * @param roleId the role ID
     * @return list of role-permission assignments with DENY effect
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId " +
           "AND rp.permission.effect = io.authplatform.platform.domain.entity.Permission$PermissionEffect.DENY")
    List<RolePermission> findDenyPermissionsByRoleId(@Param("roleId") UUID roleId);

    /**
     * Find all permissions for roles at a specific hierarchy level.
     *
     * @param organizationId the organization ID
     * @param level the role hierarchy level (0-10)
     * @return list of role-permission assignments
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.organization.id = :organizationId " +
           "AND rp.role.level = :level")
    List<RolePermission> findByOrganizationIdAndRoleLevel(
            @Param("organizationId") UUID organizationId,
            @Param("level") Integer level);

    /**
     * Check if a role has a specific permission.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @return true if the role has the permission
     */
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    /**
     * Count permission assignments for a role.
     *
     * @param roleId the role ID
     * @return number of permission assignments
     */
    long countByRoleId(UUID roleId);

    /**
     * Count role assignments for a permission.
     *
     * @param permissionId the permission ID
     * @return number of role assignments
     */
    long countByPermissionId(UUID permissionId);

    /**
     * Delete all permission assignments for a role.
     *
     * @param roleId the role ID
     */
    void deleteByRoleId(UUID roleId);

    /**
     * Delete all role assignments for a permission.
     *
     * @param permissionId the permission ID
     */
    void deleteByPermissionId(UUID permissionId);

    /**
     * Delete specific role-permission assignment.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     */
    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    /**
     * Find all distinct permission resource types assigned to a role.
     *
     * @param roleId the role ID
     * @return list of distinct resource types
     */
    @Query("SELECT DISTINCT rp.permission.resourceType FROM RolePermission rp " +
           "WHERE rp.role.id = :roleId AND rp.permission.resourceType IS NOT NULL " +
           "ORDER BY rp.permission.resourceType")
    List<String> findDistinctResourceTypesByRoleId(@Param("roleId") UUID roleId);

    /**
     * Find all distinct actions assigned to a role.
     *
     * @param roleId the role ID
     * @return list of distinct actions
     */
    @Query("SELECT DISTINCT rp.permission.action FROM RolePermission rp " +
           "WHERE rp.role.id = :roleId ORDER BY rp.permission.action")
    List<String> findDistinctActionsByRoleId(@Param("roleId") UUID roleId);

    /**
     * Find all permissions for a role by permission name pattern.
     *
     * @param roleId the role ID
     * @param namePattern the permission name pattern (use % for wildcards)
     * @return list of matching role-permission assignments
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId " +
           "AND LOWER(rp.permission.name) LIKE LOWER(:namePattern)")
    List<RolePermission> findByRoleIdAndPermissionNamePattern(
            @Param("roleId") UUID roleId,
            @Param("namePattern") String namePattern);
}
