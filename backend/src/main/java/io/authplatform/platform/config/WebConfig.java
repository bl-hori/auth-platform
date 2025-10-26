package io.authplatform.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Web configuration for CORS (Cross-Origin Resource Sharing).
 *
 * <p>This configuration allows the frontend application to make requests
 * to the backend API from a different origin (e.g., http://localhost:3000).
 *
 * <p><strong>Development vs Production:</strong>
 * <ul>
 *   <li>Development: Allows localhost:3000 for Next.js dev server</li>
 *   <li>Production: Should be configured via application.yml with specific allowed origins</li>
 * </ul>
 *
 * <p><strong>Security Notes:</strong>
 * <ul>
 *   <li>Credentials are allowed (cookies, authorization headers)</li>
 *   <li>Preflight requests are cached for 1 hour (3600 seconds)</li>
 *   <li>All standard HTTP methods are allowed</li>
 *   <li>Custom headers (X-API-Key) are explicitly allowed</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Configuration
public class WebConfig {

    /**
     * Configures CORS for all endpoints.
     *
     * <p>This configuration:
     * <ul>
     *   <li>Allows requests from http://localhost:3000 (frontend dev server)</li>
     *   <li>Permits all standard HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)</li>
     *   <li>Allows all headers including custom X-API-Key header</li>
     *   <li>Enables credentials (cookies, authorization headers)</li>
     *   <li>Caches preflight responses for 1 hour</li>
     * </ul>
     *
     * @return The configured CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow frontend origin (localhost:3000 for development)
        // In production, this should be configured via application.yml
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        ));

        // Allow all standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow all headers (including custom X-API-Key header)
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
