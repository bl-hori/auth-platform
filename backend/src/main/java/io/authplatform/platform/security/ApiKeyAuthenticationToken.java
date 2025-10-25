package io.authplatform.platform.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication token for API key-based authentication.
 *
 * <p>This token represents an authenticated API client with an associated
 * organization ID for multi-tenant isolation. The organization ID is stored
 * in the principal field and can be used for authorization decisions.
 *
 * <p>Example usage:
 * <pre>
 * ApiKeyAuthenticationToken auth = (ApiKeyAuthenticationToken)
 *     SecurityContextHolder.getContext().getAuthentication();
 * String orgId = auth.getOrganizationId();
 * </pre>
 *
 * @since 0.1.0
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final String organizationId;

    /**
     * Creates an authenticated API key token.
     *
     * @param apiKey The API key string
     * @param organizationId The organization ID associated with this API key
     * @param authorities The granted authorities for this authentication
     */
    public ApiKeyAuthenticationToken(
            String apiKey,
            String organizationId,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.organizationId = organizationId;
        setAuthenticated(true);
    }

    /**
     * Returns the API key (credentials).
     *
     * @return The API key string
     */
    @Override
    public Object getCredentials() {
        return apiKey;
    }

    /**
     * Returns the organization ID (principal).
     *
     * @return The organization ID string
     */
    @Override
    public Object getPrincipal() {
        return organizationId;
    }

    /**
     * Gets the organization ID for this authenticated request.
     *
     * @return The organization ID
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Gets the API key used for authentication.
     *
     * @return The API key string
     */
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public String toString() {
        return "ApiKeyAuthenticationToken{" +
                "organizationId='" + organizationId + '\'' +
                ", authenticated=" + isAuthenticated() +
                '}';
    }
}
