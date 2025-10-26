package io.authplatform.platform.domain.listener;

import io.authplatform.platform.domain.entity.Policy;
import io.authplatform.platform.service.AuthorizationCacheService;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Entity Listener for Policy cache invalidation.
 *
 * <p>Automatically invalidates authorization cache for entire organization
 * when policies are modified.
 */
@Slf4j
public class PolicyCacheInvalidationListener {

    private static AuthorizationCacheService cacheService;

    public static void setCacheService(AuthorizationCacheService authorizationCacheService) {
        PolicyCacheInvalidationListener.cacheService = authorizationCacheService;
    }

    @PostUpdate
    public void onUpdate(Policy policy) {
        if (cacheService != null && policy.getOrganization() != null) {
            log.debug("Policy updated: policyId={}, name={}", policy.getId(), policy.getName());

            cacheService.invalidateOrganization(policy.getOrganization().getId());
        }
    }

    @PostRemove
    public void onRemove(Policy policy) {
        if (cacheService != null && policy.getOrganization() != null) {
            log.debug("Policy removed: policyId={}, name={}", policy.getId(), policy.getName());

            cacheService.invalidateOrganization(policy.getOrganization().getId());
        }
    }
}
