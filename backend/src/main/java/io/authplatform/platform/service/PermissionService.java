package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.PermissionListResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Permission management operations.
 *
 * <p>This service provides business logic for managing permissions within organizations,
 * including retrieval operations.
 *
 * <p><strong>Key responsibilities:</strong>
 * <ul>
 *   <li>Permission retrieval (paginated list)</li>
 *   <li>Organization-specific permission filtering</li>
 *   <li>Resource type and action filtering</li>
 * </ul>
 *
 * @see io.authplatform.platform.service.impl.PermissionServiceImpl
 * @since 0.1.0
 */
public interface PermissionService {

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
}
