package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.PermissionCreateRequest;
import io.authplatform.platform.api.dto.PermissionListResponse;
import io.authplatform.platform.api.dto.PermissionResponse;
import io.authplatform.platform.api.dto.PermissionUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Permission management operations.
 *
 * <p>This service provides business logic for managing permissions within organizations,
 * including CRUD operations and retrieval.
 *
 * <p><strong>Key responsibilities:</strong>
 * <ul>
 *   <li>Permission creation with validation</li>
 *   <li>Permission retrieval (single and paginated list)</li>
 *   <li>Permission updates</li>
 *   <li>Permission deletion</li>
 *   <li>Organization-specific permission filtering</li>
 *   <li>Resource type and action filtering</li>
 * </ul>
 *
 * @see io.authplatform.platform.service.impl.PermissionServiceImpl
 * @since 0.1.0
 */
public interface PermissionService {

    /**
     * Create a new permission in the specified organization.
     *
     * <p>This method validates:
     * <ul>
     *   <li>Permission name is unique within the organization</li>
     *   <li>Organization exists</li>
     *   <li>Resource type and action combination is unique</li>
     * </ul>
     *
     * @param request the permission creation request
     * @return the created permission response
     * @throws IllegalArgumentException if organization does not exist
     * @throws IllegalStateException if permission name or resource:action already exists
     */
    PermissionResponse createPermission(PermissionCreateRequest request);

    /**
     * Get a permission by ID.
     *
     * @param permissionId the permission ID
     * @return the permission response
     * @throws IllegalArgumentException if permission does not exist
     */
    PermissionResponse getPermissionById(UUID permissionId);

    /**
     * Get all permissions for the specified organization with pagination.
     *
     * <p>This method retrieves permissions belonging to the organization,
     * with support for pagination to handle large permission sets efficiently.
     *
     * @param organizationId the organization ID
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of permissions
     * @throws IllegalArgumentException if organization does not exist
     */
    PermissionListResponse getPermissions(UUID organizationId, Pageable pageable);

    /**
     * Update an existing permission.
     *
     * <p>Note: Resource type, action, and name cannot be changed after creation.
     * Only displayName, description, effect, and conditions can be updated.
     *
     * @param permissionId the permission ID
     * @param request the permission update request
     * @return the updated permission response
     * @throws IllegalArgumentException if permission does not exist
     */
    PermissionResponse updatePermission(UUID permissionId, PermissionUpdateRequest request);

    /**
     * Delete a permission by ID.
     *
     * <p>This operation is permanent and will remove all role-permission associations.
     *
     * @param permissionId the permission ID
     * @throws IllegalArgumentException if permission does not exist
     */
    void deletePermission(UUID permissionId);
}
