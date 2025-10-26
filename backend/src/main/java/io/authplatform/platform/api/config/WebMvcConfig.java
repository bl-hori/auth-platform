package io.authplatform.platform.api.config;

import io.authplatform.platform.api.security.OrganizationIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC configuration for custom argument resolvers.
 *
 * <p>This configuration registers custom {@link HandlerMethodArgumentResolver}s
 * that enable automatic injection of values from the security context into
 * controller method parameters.
 *
 * @see OrganizationIdArgumentResolver
 * @since 0.1.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final OrganizationIdArgumentResolver organizationIdArgumentResolver;

    /**
     * Adds custom argument resolvers to the Spring MVC framework.
     *
     * @param resolvers the list of resolvers to add custom resolvers to
     */
    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(organizationIdArgumentResolver);
    }
}
