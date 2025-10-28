package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.entity.User.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link User} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for user management.
 * All queries automatically exclude soft-deleted users unless explicitly
 * included. Queries are scoped to organizations for multi-tenancy support.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private UserRepository userRepository;
 *
 * // Find active user by email in organization
 * Optional<User> user = userRepository.findByOrganizationIdAndEmailAndDeletedAtIsNull(
 *     orgId, "user@example.com");
 *
 * // Get all active users in organization
 * List<User> activeUsers = userRepository.findAllActiveByOrganizationId(orgId);
 * }</pre>
 *
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email within an organization, excluding soft-deleted users.
     *
     * @param organizationId the organization ID
     * @param email the user email
     * @return Optional containing the user if found and not deleted
     */
    Optional<User> findByOrganizationIdAndEmailAndDeletedAtIsNull(UUID organizationId, String email);

    /**
     * Find user by username within an organization, excluding soft-deleted users.
     *
     * @param organizationId the organization ID
     * @param username the username
     * @return Optional containing the user if found and not deleted
     */
    Optional<User> findByOrganizationIdAndUsernameAndDeletedAtIsNull(UUID organizationId, String username);

    /**
     * Find user by external ID, excluding soft-deleted users.
     *
     * @param externalId the external IdP user ID
     * @return Optional containing the user if found and not deleted
     */
    Optional<User> findByExternalIdAndDeletedAtIsNull(String externalId);

    /**
     * Find user by ID, excluding soft-deleted users.
     *
     * @param id the user ID
     * @return Optional containing the user if found and not deleted
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Find all users in an organization with a specific status, excluding soft-deleted.
     *
     * @param organizationId the organization ID
     * @param status the user status
     * @return list of users with the given status
     */
    List<User> findByOrganizationIdAndStatusAndDeletedAtIsNull(UUID organizationId, UserStatus status);

    /**
     * Find all active users in an organization (status = ACTIVE and not deleted).
     *
     * @param organizationId the organization ID
     * @return list of active users
     */
    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId AND u.status = io.authplatform.platform.domain.entity.User$UserStatus.ACTIVE AND u.deletedAt IS NULL")
    List<User> findAllActiveByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all non-deleted users in an organization.
     *
     * @param organizationId the organization ID
     * @return list of users that are not soft-deleted
     */
    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId AND u.deletedAt IS NULL")
    List<User> findAllNotDeletedByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Check if a user with the given email exists in an organization (excluding deleted).
     *
     * @param organizationId the organization ID
     * @param email the user email
     * @return true if a user with the email exists and is not deleted
     */
    boolean existsByOrganizationIdAndEmailAndDeletedAtIsNull(UUID organizationId, String email);

    /**
     * Check if a user with the given username exists in an organization (excluding deleted).
     *
     * @param organizationId the organization ID
     * @param username the username
     * @return true if a user with the username exists and is not deleted
     */
    boolean existsByOrganizationIdAndUsernameAndDeletedAtIsNull(UUID organizationId, String username);

    /**
     * Count active users in an organization.
     *
     * @param organizationId the organization ID
     * @return number of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :organizationId AND u.status = io.authplatform.platform.domain.entity.User$UserStatus.ACTIVE AND u.deletedAt IS NULL")
    long countActiveByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Count all non-deleted users in an organization.
     *
     * @param organizationId the organization ID
     * @return number of non-deleted users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :organizationId AND u.deletedAt IS NULL")
    long countNotDeletedByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find users by email pattern (case-insensitive) within an organization, excluding deleted.
     *
     * @param organizationId the organization ID
     * @param emailPattern the email pattern to search for (use % for wildcards)
     * @return list of matching users
     */
    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId AND LOWER(u.email) LIKE LOWER(:emailPattern) AND u.deletedAt IS NULL")
    List<User> findByEmailContainingIgnoreCaseAndNotDeleted(@Param("organizationId") UUID organizationId, @Param("emailPattern") String emailPattern);

    /**
     * Find users by display name pattern (case-insensitive) within an organization, excluding deleted.
     *
     * @param organizationId the organization ID
     * @param displayNamePattern the display name pattern to search for
     * @return list of matching users
     */
    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId AND LOWER(u.displayName) LIKE LOWER(:displayNamePattern) AND u.deletedAt IS NULL")
    List<User> findByDisplayNameContainingIgnoreCaseAndNotDeleted(@Param("organizationId") UUID organizationId, @Param("displayNamePattern") String displayNamePattern);

    // ===== Keycloak Integration (Phase 2) =====

    /**
     * Find user by Keycloak subject (sub claim from JWT), excluding soft-deleted users.
     *
     * <p>This method is used for JWT authentication to quickly find users by their
     * Keycloak ID. The keycloak_sub field is indexed for fast lookups.
     *
     * @param keycloakSub the Keycloak user ID (JWT sub claim)
     * @return Optional containing the user if found and not deleted
     * @since 0.2.0
     */
    Optional<User> findByKeycloakSubAndDeletedAtIsNull(String keycloakSub);

    /**
     * Find user by email across all organizations, excluding soft-deleted users.
     *
     * <p>This method is used during JWT authentication for user linking.
     * When a user authenticates with JWT for the first time, we search for
     * an existing user by email to link them with their Keycloak identity.
     *
     * @param email the user email
     * @return Optional containing the user if found and not deleted
     * @since 0.2.0
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
}
