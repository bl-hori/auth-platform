package io.authplatform.platform.config;

import io.authplatform.platform.security.ApiKeyAuthenticationFilter;
import io.authplatform.platform.security.JwtAuthenticationFilter;
import io.authplatform.platform.security.RateLimitFilter;
import io.authplatform.platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the Authorization Platform.
 *
 * <p>This configuration sets up hybrid authentication supporting both:
 * <ul>
 *   <li><b>JWT Authentication</b>: Bearer tokens from Keycloak (Phase 2)</li>
 *   <li><b>API Key Authentication</b>: X-API-Key header (backward compatibility)</li>
 * </ul>
 *
 * <p>The security model follows these principles:
 * <ul>
 *   <li>Stateless authentication (no sessions)</li>
 *   <li>Multi-tenant isolation via organization ID</li>
 *   <li>Public access to actuator health endpoints and API documentation</li>
 *   <li>JWT authentication takes precedence over API Key</li>
 * </ul>
 *
 * <p>Authentication Filter Chain (Phase 2):
 * <ol>
 *   <li>RateLimitFilter - Rate limiting by IP</li>
 *   <li>JwtAuthenticationFilter - JWT validation (if enabled)</li>
 *   <li>ApiKeyAuthenticationFilter - API Key validation (fallback)</li>
 * </ol>
 *
 * <p>Security features:
 * <ul>
 *   <li>CSRF protection disabled (stateless API, not browser-based)</li>
 *   <li>CORS configured for cross-origin requests</li>
 *   <li>Method-level security enabled with @PreAuthorize annotations</li>
 *   <li>Conditional JWT authentication (configurable via properties)</li>
 * </ul>
 *
 * @since 0.1.0 (API Key authentication)
 * @since 0.2.0 (JWT authentication)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ApiKeyProperties apiKeyProperties;
    private final RateLimitFilter rateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    // JWT-related dependencies (optional, only injected if JWT is enabled)
    @Autowired(required = false)
    private JwtDecoder jwtDecoder;

    @Autowired(required = false)
    private UserService userService;

    @Autowired(required = false)
    private KeycloakProperties keycloakProperties;

    /**
     * Constructs the security configuration.
     *
     * @param apiKeyProperties        API key configuration properties
     * @param rateLimitFilter         Rate limit filter
     * @param corsConfigurationSource CORS configuration source
     */
    public SecurityConfig(
            ApiKeyProperties apiKeyProperties,
            RateLimitFilter rateLimitFilter,
            CorsConfigurationSource corsConfigurationSource) {
        this.apiKeyProperties = apiKeyProperties;
        this.rateLimitFilter = rateLimitFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Configures the security filter chain.
     *
     * <p>This method sets up:
     * <ul>
     *   <li>Authorization rules for different endpoint patterns</li>
     *   <li>JWT authentication filter (Phase 2, conditional)</li>
     *   <li>API key authentication filter (backward compatibility)</li>
     *   <li>Stateless session management</li>
     *   <li>CSRF and CORS configuration</li>
     * </ul>
     *
     * <p><b>Filter Order</b> (Phase 2):
     * <ol>
     *   <li>RateLimitFilter</li>
     *   <li>JwtAuthenticationFilter (if JWT enabled)</li>
     *   <li>ApiKeyAuthenticationFilter</li>
     * </ol>
     *
     * @param http The HttpSecurity to configure
     * @return The configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS using the CorsConfigurationSource bean
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Stateless session management (no session cookies)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Add rate limit filter before authentication
            .addFilterBefore(
                rateLimitFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        // Add JWT authentication filter if enabled (Phase 2)
        if (jwtDecoder != null && userService != null && keycloakProperties != null &&
                keycloakProperties.isEnabled()) {
            http.addFilterAfter(
                jwtAuthenticationFilter(),
                RateLimitFilter.class
            );
        }

        // Add API key authentication filter after JWT (or after rate limit if JWT disabled)
        if (jwtDecoder != null && userService != null && keycloakProperties != null &&
                keycloakProperties.isEnabled()) {
            http.addFilterAfter(
                apiKeyAuthenticationFilter(),
                JwtAuthenticationFilter.class
            );
        } else {
            http.addFilterAfter(
                apiKeyAuthenticationFilter(),
                RateLimitFilter.class
            );
        }

        // Disable form login and HTTP basic auth
        http
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Creates the JWT authentication filter bean (Phase 2).
     *
     * <p>This bean is only created if JWT authentication is enabled via
     * {@code authplatform.keycloak.enabled=true}.
     *
     * @return The configured JwtAuthenticationFilter
     */
    @Bean
    @ConditionalOnProperty(prefix = "authplatform.keycloak", name = "enabled", havingValue = "true")
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtDecoder, userService, keycloakProperties);
    }

    /**
     * Creates the API key authentication filter bean.
     *
     * @return The configured ApiKeyAuthenticationFilter
     */
    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter() {
        return new ApiKeyAuthenticationFilter(apiKeyProperties);
    }
}
