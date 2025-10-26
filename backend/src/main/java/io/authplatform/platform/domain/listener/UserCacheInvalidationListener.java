package io.authplatform.platform.domain.listener;

import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.service.AuthorizationCacheService;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Entity Listener for User cache invalidation.
 *
 * <p>Automatically invalidates authorization cache when users are modified.
 */
@Slf4j
public class UserCacheInvalidationListener {

    private static AuthorizationCacheService cacheService;

    public static void setCacheService(AuthorizationCacheService authorizationCacheService) {
        UserCacheInvalidationListener.cacheService = authorizationCacheService;
    }

    @PostUpdate
    public void onUpdate(User user) {
        if (cacheService != null && user.getOrganization() != null) {
            log.debug("User updated: userId={}, externalId={}", user.getId(), user.getExternalId());

            cacheService.invalidate(
                    user.getOrganization().getId(),
                    user.getExternalId()
            );
        }
    }

    @PostRemove
    public void onRemove(User user) {
        if (cacheService != null && user.getOrganization() != null) {
            log.debug("User removed: userId={}, externalId={}", user.getId(), user.getExternalId());

            cacheService.invalidate(
                    user.getOrganization().getId(),
                    user.getExternalId()
            );
        }
    }
}
