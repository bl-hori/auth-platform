package io.authplatform.platform.config;

import io.authplatform.platform.security.ApiKeyAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Authorization Platform.
 *
 * <p>This configuration sets up API key-based authentication for all API endpoints.
 * The security model follows these principles:
 * <ul>
 *   <li>Stateless authentication using API keys (no sessions)</li>
 *   <li>Multi-tenant isolation via organization ID extracted from API key</li>
 *   <li>Public access to actuator health endpoints and API documentation</li>
 *   <li>All other endpoints require valid API key authentication</li>
 * </ul>
 *
 * <p>Security features:
 * <ul>
 *   <li>CSRF protection disabled (stateless API, not browser-based)</li>
 *   <li>CORS configured for cross-origin requests</li>
 *   <li>Custom authentication filter for API key validation</li>
 *   <li>Method-level security enabled with @PreAuthorize annotations</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ApiKeyProperties apiKeyProperties;

    /**
     * Constructs the security configuration.
     *
     * @param apiKeyProperties API key configuration properties
     */
    public SecurityConfig(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    /**
     * Configures the security filter chain.
     *
     * <p>This method sets up:
     * <ul>
     *   <li>Authorization rules for different endpoint patterns</li>
     *   <li>API key authentication filter</li>
     *   <li>Stateless session management</li>
     *   <li>CSRF and CORS configuration</li>
     * </ul>
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

            // Configure CORS (allow all origins for development, restrict in production)
            .cors(cors -> cors.configure(http))

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

            // Add API key authentication filter before username/password filter
            .addFilterBefore(
                apiKeyAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
            )

            // Disable form login and HTTP basic auth (API key only)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
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
