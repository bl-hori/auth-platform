package io.authplatform.platform.config;

import io.authplatform.platform.domain.listener.CacheInvalidationListener;
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
     * the cache service into the {@link CacheInvalidationListener}.
     */
    @PostConstruct
    public void initializeEntityListeners() {
        log.info("Initializing JPA entity listeners");
        CacheInvalidationListener.setCacheService(cacheService);
        log.info("CacheInvalidationListener initialized with cache service");
    }
}
