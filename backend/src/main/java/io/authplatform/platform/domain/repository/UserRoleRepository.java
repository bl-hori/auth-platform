package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link UserRole} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for user-role assignments with resource scoping.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private UserRoleRepository userRoleRepository;
 *
 * // Find all roles for a user
 * List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
 *
 * // Find roles for a user on a specific resource
 * List<UserRole> docRoles = userRoleRepository.findByUserIdAndResourceTypeAndResourceId(
 *     userId, "document", "doc-123");
 * }</pre>
 *
 * @see UserRole
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Find all role assignments for a user.
     *
     * @param userId the user ID
     * @return list of role assignments
     */
    List<UserRole> findByUserId(UUID userId);

    /**
     * Find all users with a specific role.
     *
     * @param roleId the role ID
     * @return list of user-role assignments
     */
    List<UserRole> findByRoleId(UUID roleId);

    /**
     * Find specific user-role assignment.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return Optional containing the assignment if found
     */
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Find all role assignments for a user on a specific resource type.
     *
     * @param userId the user ID
     * @param resourceType the resource type
     * @return list of role assignments
     */
    List<UserRole> findByUserIdAndResourceType(UUID userId, String resourceType);

    /**
     * Find all role assignments for a user on a specific resource instance.
     *
     * @param userId the user ID
     * @param resourceType the resource type
     * @param resourceId the resource instance ID
     * @return list of role assignments
     */
    List<UserRole> findByUserIdAndResourceTypeAndResourceId(UUID userId, String resourceType, String resourceId);

    /**
     * Find all global role assignments for a user (not scoped to any resource).
     *
     * @param userId the user ID
     * @return list of global role assignments
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.resourceType IS NULL AND ur.resourceId IS NULL")
    List<UserRole> findGlobalRolesByUserId(@Param("userId") UUID userId);

    /**
     * Find all non-expired role assignments for a user.
     *
     * @param userId the user ID
     * @param now current timestamp for expiration check
     * @return list of non-expired role assignments
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findNonExpiredByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    /**
     * Find all expired role assignments for a user.
     *
     * @param userId the user ID
     * @param now current timestamp for expiration check
     * @return list of expired role assignments
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.expiresAt IS NOT NULL AND ur.expiresAt <= :now")
    List<UserRole> findExpiredByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    /**
     * Find all role assignments granted by a specific user.
     *
     * @param grantedById the ID of the user who granted the roles
     * @return list of role assignments
     */
    List<UserRole> findByGrantedById(UUID grantedById);

    /**
     * Find all role assignments expiring before a certain date.
     *
     * @param expirationDate the expiration date threshold
     * @return list of role assignments expiring before the date
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt IS NOT NULL AND ur.expiresAt <= :expirationDate")
    List<UserRole> findExpiringBefore(@Param("expirationDate") OffsetDateTime expirationDate);

    /**
     * Check if a user has a specific role (globally or scoped to resource).
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return true if the user has the role
     */
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Check if a user has a specific role on a specific resource.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @param resourceType the resource type
     * @param resourceId the resource instance ID
     * @return true if the user has the role on the resource
     */
    boolean existsByUserIdAndRoleIdAndResourceTypeAndResourceId(
            UUID userId, UUID roleId, String resourceType, String resourceId);

    /**
     * Count role assignments for a user.
     *
     * @param userId the user ID
     * @return number of role assignments
     */
    long countByUserId(UUID userId);

    /**
     * Count users with a specific role.
     *
     * @param roleId the role ID
     * @return number of users with the role
     */
    long countByRoleId(UUID roleId);

    /**
     * Delete all role assignments for a user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Delete all assignments of a specific role.
     *
     * @param roleId the role ID
     */
    void deleteByRoleId(UUID roleId);

    /**
     * Delete a specific user-role assignment.
     *
     * @param userId the user ID
     * @param roleId the role ID
     */
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Delete expired role assignments.
     *
     * @param now current timestamp
     * @return number of deleted assignments
     */
    @Query("DELETE FROM UserRole ur WHERE ur.expiresAt IS NOT NULL AND ur.expiresAt <= :now")
    int deleteExpiredAssignments(@Param("now") OffsetDateTime now);

    /**
     * Find all role assignments for users in a specific organization.
     *
     * @param organizationId the organization ID
     * @return list of role assignments
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.organization.id = :organizationId")
    List<UserRole> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all distinct resource types for a user's role assignments.
     *
     * @param userId the user ID
     * @return list of distinct resource types
     */
    @Query("SELECT DISTINCT ur.resourceType FROM UserRole ur WHERE ur.user.id = :userId AND ur.resourceType IS NOT NULL ORDER BY ur.resourceType")
    List<String> findDistinctResourceTypesByUserId(@Param("userId") UUID userId);
}
