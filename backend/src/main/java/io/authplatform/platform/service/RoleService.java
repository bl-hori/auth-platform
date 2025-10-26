package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.RoleCreateRequest;
import io.authplatform.platform.api.dto.RoleListResponse;
import io.authplatform.platform.api.dto.RoleResponse;
import io.authplatform.platform.api.dto.RoleUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Role management operations.
 *
 * <p>This service provides business logic for managing roles within organizations,
 * including CRUD operations, hierarchy management, and validation.
 *
 * <p><strong>Key responsibilities:</strong>
 * <ul>
 *   <li>Role creation with hierarchy validation</li>
 *   <li>Role retrieval (single and paginated list)</li>
 *   <li>Role updates with hierarchy consistency checks</li>
 *   <li>Role deletion with dependency checks</li>
 *   <li>Role hierarchy resolution (max depth 10)</li>
 *   <li>Circular dependency prevention</li>
 *   <li>System role protection</li>
 * </ul>
 *
 * @see io.authplatform.platform.service.impl.RoleServiceImpl
 * @since 0.1.0
 */
public interface RoleService {

    /**
     * Create a new role in the specified organization.
     *
     * <p>This method validates:
     * <ul>
     *   <li>Role name is unique within the organization</li>
     *   <li>Organization exists and is active</li>
     *   <li>Parent role exists (if specified)</li>
     *   <li>Hierarchy level does not exceed 10</li>
     *   <li>No circular dependencies in hierarchy</li>
     * </ul>
     *
     * @param request the role creation request
     * @return the created role response
     * @throws IllegalArgumentException if organization or parent role does not exist
     * @throws IllegalStateException if role name already exists or hierarchy is invalid
     */
    RoleResponse createRole(RoleCreateRequest request);

    /**
     * Get a role by ID.
     *
     * @param roleId the role ID
     * @return the role response
     * @throws IllegalArgumentException if role does not exist
     */
    RoleResponse getRoleById(UUID roleId);

    /**
     * Get all roles for an organization with pagination.
     *
     * <p>This method supports:
     * <ul>
     *   <li>Pagination via {@link Pageable}</li>
     *   <li>Excludes soft-deleted roles by default</li>
     *   <li>Sorts by hierarchy level and name</li>
     * </ul>
     *
     * @param organizationId the organization ID
     * @param pageable pagination parameters
     * @return paginated list of roles
     */
    RoleListResponse getRolesByOrganization(UUID organizationId, Pageable pageable);

    /**
     * Get role hierarchy path from root to the specified role.
     *
     * <p>Returns the full hierarchy chain, e.g.:
     * ["admin", "developer", "junior-developer"]
     *
     * @param roleId the role ID
     * @return list of role names from root to current role
     * @throws IllegalArgumentException if role does not exist
     */
    List<String> getRoleHierarchy(UUID roleId);

    /**
     * Update a role's information.
     *
     * <p>This method supports partial updates. System roles cannot be updated.
     *
     * <p>Validation:
     * <ul>
     *   <li>Cannot modify system roles</li>
     *   <li>Hierarchy changes must not create cycles</li>
     *   <li>Level must be consistent with parent</li>
     * </ul>
     *
     * @param roleId the role ID
     * @param request the update request
     * @return the updated role response
     * @throws IllegalArgumentException if role does not exist
     * @throws IllegalStateException if role is system role or update violates hierarchy rules
     */
    RoleResponse updateRole(UUID roleId, RoleUpdateRequest request);

    /**
     * Delete a role (soft delete).
     *
     * <p>This method:
     * <ul>
     *   <li>Cannot delete system roles</li>
     *   <li>Cannot delete roles that are parents of other roles</li>
     *   <li>Sets deletedAt timestamp</li>
     *   <li>Removes role from active user assignments</li>
     * </ul>
     *
     * @param roleId the role ID
     * @throws IllegalArgumentException if role does not exist
     * @throws IllegalStateException if role is system role or has child roles
     */
    void deleteRole(UUID roleId);
}
