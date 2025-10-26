package io.authplatform.platform.domain.listener;

import io.authplatform.platform.domain.entity.UserRole;
import io.authplatform.platform.service.AuthorizationCacheService;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Entity Listener for UserRole cache invalidation.
 *
 * <p>Automatically invalidates authorization cache when user roles are modified.
 */
@Slf4j
public class UserRoleCacheInvalidationListener {

    private static AuthorizationCacheService cacheService;

    public static void setCacheService(AuthorizationCacheService authorizationCacheService) {
        UserRoleCacheInvalidationListener.cacheService = authorizationCacheService;
    }

    @PostUpdate
    public void onUpdate(UserRole userRole) {
        if (cacheService != null && userRole.getUser() != null) {
            log.debug("UserRole updated: userId={}, roleId={}",
                    userRole.getUser().getId(), userRole.getRole().getId());

            cacheService.invalidate(
                    userRole.getUser().getOrganization().getId(),
                    userRole.getUser().getExternalId()
            );
        }
    }

    @PostRemove
    public void onRemove(UserRole userRole) {
        if (cacheService != null && userRole.getUser() != null) {
            log.debug("UserRole removed: userId={}, roleId={}",
                    userRole.getUser().getId(), userRole.getRole().getId());

            cacheService.invalidate(
                    userRole.getUser().getOrganization().getId(),
                    userRole.getUser().getExternalId()
            );
        }
    }
}
