package io.authplatform.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for API key authentication.
 *
 * <p>This class holds the configuration for API keys used to authenticate
 * requests to the authorization platform. API keys are mapped to organization IDs
 * to provide multi-tenant isolation.
 *
 * <p>Example configuration in application.yml:
 * <pre>
 * auth-platform:
 *   security:
 *     api-keys:
 *       enabled: true
 *       header-name: X-API-Key
 *       keys:
 *         dev-key-123: org-1
 *         test-key-456: org-2
 * </pre>
 *
 * @since 0.1.0
 */
@Component
@ConfigurationProperties(prefix = "auth-platform.security.api-keys")
public class ApiKeyProperties {

    /**
     * Whether API key authentication is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * The HTTP header name containing the API key.
     * Default: X-API-Key
     */
    private String headerName = "X-API-Key";

    /**
     * Map of API keys to organization IDs.
     * Key: API key string
     * Value: Organization ID
     */
    private Map<String, String> keys = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }

    /**
     * Validates an API key and returns the associated organization ID.
     *
     * @param apiKey The API key to validate
     * @return The organization ID if the key is valid, null otherwise
     */
    public String getOrganizationId(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return keys.get(apiKey);
    }

    /**
     * Checks if an API key is valid.
     *
     * @param apiKey The API key to check
     * @return true if the key exists in the configuration, false otherwise
     */
    public boolean isValidApiKey(String apiKey) {
        return getOrganizationId(apiKey) != null;
    }
}
