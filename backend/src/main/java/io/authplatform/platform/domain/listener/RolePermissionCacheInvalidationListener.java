package io.authplatform.platform.domain.listener;

import io.authplatform.platform.domain.entity.RolePermission;
import io.authplatform.platform.service.AuthorizationCacheService;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Entity Listener for RolePermission cache invalidation.
 *
 * <p>Automatically invalidates authorization cache for entire organization
 * when role permissions are modified.
 */
@Slf4j
public class RolePermissionCacheInvalidationListener {

    private static AuthorizationCacheService cacheService;

    public static void setCacheService(AuthorizationCacheService authorizationCacheService) {
        RolePermissionCacheInvalidationListener.cacheService = authorizationCacheService;
    }

    @PostUpdate
    public void onUpdate(RolePermission rolePermission) {
        if (cacheService != null && rolePermission.getRole() != null) {
            log.debug("RolePermission updated: roleId={}, permissionId={}",
                    rolePermission.getRole().getId(), rolePermission.getPermission().getId());

            cacheService.invalidateOrganization(
                    rolePermission.getRole().getOrganization().getId()
            );
        }
    }

    @PostRemove
    public void onRemove(RolePermission rolePermission) {
        if (cacheService != null && rolePermission.getRole() != null) {
            log.debug("RolePermission removed: roleId={}, permissionId={}",
                    rolePermission.getRole().getId(), rolePermission.getPermission().getId());

            cacheService.invalidateOrganization(
                    rolePermission.getRole().getOrganization().getId()
            );
        }
    }
}
