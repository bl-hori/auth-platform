package io.authplatform.platform.security;

import io.authplatform.platform.config.ApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authentication filter for API key-based authentication.
 *
 * <p>This filter intercepts incoming HTTP requests and validates the API key
 * provided in the request header. If valid, it creates an authentication token
 * with the associated organization ID and sets it in the security context.
 *
 * <p>The filter processes all requests except:
 * <ul>
 *   <li>Actuator endpoints (/actuator/**)</li>
 *   <li>API documentation endpoints (/v3/api-docs/**, /swagger-ui/**)</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final ApiKeyProperties apiKeyProperties;

    /**
     * Constructs a new API key authentication filter.
     *
     * @param apiKeyProperties Configuration properties for API keys
     */
    public ApiKeyAuthenticationFilter(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication for public endpoints
        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String apiKey = extractApiKey(request);

            if (apiKey == null || apiKey.isBlank()) {
                logger.warn("Missing API key in request to {}", requestPath);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key is required");
                return;
            }

            String organizationId = apiKeyProperties.getOrganizationId(apiKey);

            if (organizationId == null) {
                logger.warn("Invalid API key attempted for {}", requestPath);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
                return;
            }

            // Create authentication token with organization context
            ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                    apiKey,
                    organizationId,
                    List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("Authenticated request for organization: {}", organizationId);

            filterChain.doFilter(request, response);

        } catch (BadCredentialsException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
        } finally {
            // Clear security context after request processing
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Extracts the API key from the request header.
     *
     * @param request The HTTP request
     * @return The API key string, or null if not present
     */
    private String extractApiKey(HttpServletRequest request) {
        return request.getHeader(apiKeyProperties.getHeaderName());
    }

    /**
     * Determines if the request path is a public endpoint that doesn't require authentication.
     *
     * @param requestPath The request URI path
     * @return true if the endpoint is public, false otherwise
     */
    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/actuator/")
                || requestPath.startsWith("/v3/api-docs")
                || requestPath.startsWith("/swagger-ui");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Filter is disabled if API key authentication is disabled in configuration
        return !apiKeyProperties.isEnabled();
    }
}
