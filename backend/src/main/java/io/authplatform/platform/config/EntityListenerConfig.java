package io.authplatform.platform.config;

import io.authplatform.platform.domain.listener.PolicyCacheInvalidationListener;
import io.authplatform.platform.domain.listener.RolePermissionCacheInvalidationListener;
import io.authplatform.platform.domain.listener.UserCacheInvalidationListener;
import io.authplatform.platform.domain.listener.UserRoleCacheInvalidationListener;
import io.authplatform.platform.service.AuthorizationCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JPA entity listeners.
 *
 * <p>This configuration initializes JPA entity listeners with Spring-managed dependencies.
 * Since JPA creates entity listeners outside of Spring's context, we need to manually
 * inject dependencies through static setters.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EntityListenerConfig {

    private final AuthorizationCacheService cacheService;

    /**
     * Initialize entity listeners with Spring-managed dependencies.
     *
     * <p>This method is called after Spring context initialization to inject
     * the cache service into all cache invalidation listeners.
     */
    @PostConstruct
    public void initializeEntityListeners() {
        log.info("Initializing JPA entity listeners for cache invalidation");

        UserRoleCacheInvalidationListener.setCacheService(cacheService);
        RolePermissionCacheInvalidationListener.setCacheService(cacheService);
        PolicyCacheInvalidationListener.setCacheService(cacheService);
        UserCacheInvalidationListener.setCacheService(cacheService);

        log.info("All cache invalidation listeners initialized successfully");
    }
}
