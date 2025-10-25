package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Permission;
import io.authplatform.platform.domain.entity.Permission.PermissionEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Permission} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for permission management.
 * Queries are scoped to organizations for multi-tenancy support.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private PermissionRepository permissionRepository;
 *
 * // Find permission by name in organization
 * Optional<Permission> permission = permissionRepository.findByOrganizationIdAndName(
 *     orgId, "document:read");
 *
 * // Get all permissions for a resource type
 * List<Permission> docPermissions = permissionRepository.findByOrganizationIdAndResourceType(
 *     orgId, "document");
 * }</pre>
 *
 * @see Permission
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    /**
     * Find permission by name within an organization.
     *
     * @param organizationId the organization ID
     * @param name the permission name
     * @return Optional containing the permission if found
     */
    Optional<Permission> findByOrganizationIdAndName(UUID organizationId, String name);

    /**
     * Find permission by resource type and action within an organization.
     *
     * @param organizationId the organization ID
     * @param resourceType the resource type
     * @param action the action
     * @return Optional containing the permission if found
     */
    Optional<Permission> findByOrganizationIdAndResourceTypeAndAction(
            UUID organizationId, String resourceType, String action);

    /**
     * Find all permissions in an organization.
     *
     * @param organizationId the organization ID
     * @return list of all permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.organization.id = :organizationId")
    List<Permission> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all permissions for a specific resource type in an organization.
     *
     * @param organizationId the organization ID
     * @param resourceType the resource type
     * @return list of permissions for the resource type
     */
    List<Permission> findByOrganizationIdAndResourceType(UUID organizationId, String resourceType);

    /**
     * Find all permissions for a specific action in an organization.
     *
     * @param organizationId the organization ID
     * @param action the action
     * @return list of permissions for the action
     */
    List<Permission> findByOrganizationIdAndAction(UUID organizationId, String action);

    /**
     * Find all permissions with a specific effect in an organization.
     *
     * @param organizationId the organization ID
     * @param effect the permission effect (ALLOW or DENY)
     * @return list of permissions with the specified effect
     */
    List<Permission> findByOrganizationIdAndEffect(UUID organizationId, PermissionEffect effect);

    /**
     * Find all allow permissions in an organization.
     *
     * @param organizationId the organization ID
     * @return list of allow permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.organization.id = :organizationId AND p.effect = io.authplatform.platform.domain.entity.Permission$PermissionEffect.ALLOW")
    List<Permission> findAllowPermissionsByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all deny permissions in an organization.
     *
     * @param organizationId the organization ID
     * @return list of deny permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.organization.id = :organizationId AND p.effect = io.authplatform.platform.domain.entity.Permission$PermissionEffect.DENY")
    List<Permission> findDenyPermissionsByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Check if a permission with the given name exists in an organization.
     *
     * @param organizationId the organization ID
     * @param name the permission name
     * @return true if a permission with the name exists
     */
    boolean existsByOrganizationIdAndName(UUID organizationId, String name);

    /**
     * Check if a permission with the given resource type and action exists in an organization.
     *
     * @param organizationId the organization ID
     * @param resourceType the resource type
     * @param action the action
     * @return true if a permission exists
     */
    boolean existsByOrganizationIdAndResourceTypeAndAction(
            UUID organizationId, String resourceType, String action);

    /**
     * Count all permissions in an organization.
     *
     * @param organizationId the organization ID
     * @return number of permissions
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.organization.id = :organizationId")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Count allow permissions in an organization.
     *
     * @param organizationId the organization ID
     * @return number of allow permissions
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.organization.id = :organizationId AND p.effect = io.authplatform.platform.domain.entity.Permission$PermissionEffect.ALLOW")
    long countAllowPermissionsByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Count deny permissions in an organization.
     *
     * @param organizationId the organization ID
     * @return number of deny permissions
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.organization.id = :organizationId AND p.effect = io.authplatform.platform.domain.entity.Permission$PermissionEffect.DENY")
    long countDenyPermissionsByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find permissions by name pattern (case-insensitive) within an organization.
     *
     * @param organizationId the organization ID
     * @param namePattern the name pattern to search for (use % for wildcards)
     * @return list of matching permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.organization.id = :organizationId AND LOWER(p.name) LIKE LOWER(:namePattern)")
    List<Permission> findByNameContainingIgnoreCase(@Param("organizationId") UUID organizationId, @Param("namePattern") String namePattern);

    /**
     * Find all unique resource types in an organization.
     *
     * @param organizationId the organization ID
     * @return list of distinct resource types
     */
    @Query("SELECT DISTINCT p.resourceType FROM Permission p WHERE p.organization.id = :organizationId ORDER BY p.resourceType")
    List<String> findDistinctResourceTypesByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all unique actions in an organization.
     *
     * @param organizationId the organization ID
     * @return list of distinct actions
     */
    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.organization.id = :organizationId ORDER BY p.action")
    List<String> findDistinctActionsByOrganizationId(@Param("organizationId") UUID organizationId);
}
