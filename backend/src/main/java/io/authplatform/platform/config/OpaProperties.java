package io.authplatform.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OPA (Open Policy Agent) integration.
 *
 * <p>These properties control how the application connects to and interacts with OPA.
 *
 * <p><strong>Configuration Example (application.yml):</strong>
 * <pre>{@code
 * opa:
 *   enabled: true
 *   base-url: http://localhost:8181
 *   policy-path: /v1/data/authz/allow
 *   timeout-ms: 5000
 *   connect-timeout-ms: 2000
 *   retry-attempts: 3
 * }</pre>
 *
 * <p><strong>Environment Variables:</strong>
 * <ul>
 *   <li>OPA_ENABLED: Enable/disable OPA integration (default: false)</li>
 *   <li>OPA_BASE_URL: OPA server base URL (default: http://localhost:8181)</li>
 *   <li>OPA_POLICY_PATH: Policy evaluation endpoint path (default: /v1/data/authz/allow)</li>
 *   <li>OPA_TIMEOUT_MS: Request timeout in milliseconds (default: 5000)</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "opa")
@Data
public class OpaProperties {

    /**
     * Enable or disable OPA integration.
     * When disabled, only RBAC evaluation will be performed.
     * Default: false
     */
    private boolean enabled = false;

    /**
     * Base URL of the OPA server.
     * Default: http://localhost:8181
     */
    private String baseUrl = "http://localhost:8181";

    /**
     * Policy evaluation endpoint path.
     * This should match your OPA policy package structure.
     * Default: /v1/data/authz/allow
     */
    private String policyPath = "/v1/data/authz/allow";

    /**
     * Request timeout in milliseconds.
     * Default: 5000ms (5 seconds)
     */
    private int timeoutMs = 5000;

    /**
     * Connection timeout in milliseconds.
     * Default: 2000ms (2 seconds)
     */
    private int connectTimeoutMs = 2000;

    /**
     * Number of retry attempts for failed requests.
     * Default: 3
     */
    private int retryAttempts = 3;

    /**
     * Get the full policy evaluation URL.
     *
     * @return Complete URL for OPA policy evaluation
     */
    public String getPolicyUrl() {
        return baseUrl + policyPath;
    }
}
