package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.UserCreateRequest;
import io.authplatform.platform.api.dto.UserListResponse;
import io.authplatform.platform.api.dto.UserResponse;
import io.authplatform.platform.api.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;

import io.authplatform.platform.api.dto.UserRoleAssignRequest;
import io.authplatform.platform.api.dto.UserRoleResponse;
import io.authplatform.platform.api.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for User management operations.
 *
 * <p>This service provides business logic for managing users within organizations,
 * including CRUD operations, search, and status management.
 *
 * <p><strong>Key responsibilities:</strong>
 * <ul>
 *   <li>User creation with validation and uniqueness checks</li>
 *   <li>User retrieval (single and paginated list)</li>
 *   <li>User updates with partial update support</li>
 *   <li>User deactivation (soft delete)</li>
 *   <li>Email and username uniqueness enforcement</li>
 *   <li>Organization isolation (multi-tenancy)</li>
 * </ul>
 *
 * @see io.authplatform.platform.service.impl.UserServiceImpl
 * @since 0.1.0
 */
public interface UserService {

    /**
     * Create a new user in the specified organization.
     *
     * <p>This method validates:
     * <ul>
     *   <li>Email is unique within the organization</li>
     *   <li>Username is unique within the organization (if provided)</li>
     *   <li>Organization exists and is active</li>
     *   <li>All validation constraints are met</li>
     * </ul>
     *
     * @param request the user creation request
     * @return the created user response
     * @throws IllegalArgumentException if organization does not exist
     * @throws IllegalStateException if email or username already exists
     */
    UserResponse createUser(UserCreateRequest request);

    /**
     * Get a user by ID.
     *
     * <p>This method returns both active and inactive users.
     * Soft-deleted users are also returned with deletedAt timestamp.
     *
     * @param userId the user ID
     * @return the user response
     * @throws IllegalArgumentException if user does not exist
     */
    UserResponse getUserById(UUID userId);

    /**
     * Get all users for an organization with pagination and search.
     *
     * <p>This method supports:
     * <ul>
     *   <li>Pagination via {@link Pageable}</li>
     *   <li>Search by email, username, or displayName (case-insensitive)</li>
     *   <li>Status filtering (active, inactive, suspended)</li>
     *   <li>Excludes soft-deleted users by default</li>
     * </ul>
     *
     * @param organizationId the organization ID
     * @param search optional search query (searches email, username, displayName)
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return paginated list of users
     */
    UserListResponse getUsersByOrganization(
            UUID organizationId,
            String search,
            String status,
            Pageable pageable
    );

    /**
     * Update a user's information.
     *
     * <p>This method supports partial updates - only provided fields are updated.
     * Null values are ignored.
     *
     * <p>Validation:
     * <ul>
     *   <li>Email uniqueness (if email is being changed)</li>
     *   <li>Username uniqueness (if username is being changed)</li>
     *   <li>Cannot change organizationId</li>
     * </ul>
     *
     * @param userId the user ID
     * @param request the update request with optional fields
     * @return the updated user response
     * @throws IllegalArgumentException if user does not exist
     * @throws IllegalStateException if email or username conflict
     */
    UserResponse updateUser(UUID userId, UserUpdateRequest request);

    /**
     * Deactivate a user (soft delete).
     *
     * <p>This method:
     * <ul>
     *   <li>Sets deletedAt timestamp to current time</li>
     *   <li>Changes status to DELETED</li>
     *   <li>Preserves user data for audit purposes</li>
     *   <li>Removes user from authorization decisions</li>
     *   <li>Invalidates user's cache entries</li>
     * </ul>
     *
     * @param userId the user ID
     * @throws IllegalArgumentException if user does not exist
     * @throws IllegalStateException if user is already deleted
     */
    void deactivateUser(UUID userId);

    /**
     * Activate a previously deactivated user.
     *
     * <p>This method:
     * <ul>
     *   <li>Clears deletedAt timestamp</li>
     *   <li>Changes status to ACTIVE</li>
     *   <li>Restores user access</li>
     * </ul>
     *
     * @param userId the user ID
     * @throws IllegalArgumentException if user does not exist
     * @throws IllegalStateException if user is not deleted
     */
    void activateUser(UUID userId);

    /**
     * Assign a role to a user.
     *
     * <p>This method:
     * <ul>
     *   <li>Validates the user and role exist in the same organization</li>
     *   <li>Checks for duplicate role assignments (same user, role, resource)</li>
     *   <li>Supports optional resource scoping</li>
     *   <li>Supports optional expiration time</li>
     *   <li>Invalidates authorization cache for the user</li>
     * </ul>
     *
     * @param userId the user ID
     * @param request the role assignment request
     * @return the created user-role assignment
     * @throws IllegalArgumentException if user or role does not exist
     * @throws IllegalStateException if role is already assigned
     * @throws IllegalStateException if user and role are in different organizations
     */
    UserRoleResponse assignRole(UUID userId, UserRoleAssignRequest request);

    /**
     * Remove a role from a user.
     *
     * <p>This method:
     * <ul>
     *   <li>Removes the role assignment</li>
     *   <li>Handles resource-scoped and global assignments</li>
     *   <li>Invalidates authorization cache for the user</li>
     * </ul>
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @throws IllegalArgumentException if user or role does not exist
     * @throws IllegalStateException if role is not assigned to user
     */
    void removeRole(UUID userId, UUID roleId);

    /**
     * Get all roles assigned to a user.
     *
     * <p>This method returns all active role assignments, including:
     * <ul>
     *   <li>Resource-scoped roles</li>
     *   <li>Global roles</li>
     *   <li>Non-expired roles</li>
     * </ul>
     *
     * <p>Expired roles are excluded from the results.
     *
     * @param userId the user ID
     * @return list of user role assignments
     * @throws IllegalArgumentException if user does not exist
     */
    List<UserRoleResponse> getUserRoles(UUID userId);
}
