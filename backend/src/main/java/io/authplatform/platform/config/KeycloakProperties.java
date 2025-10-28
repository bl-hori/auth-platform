package io.authplatform.platform.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Keycloak integration and JWT authentication.
 *
 * <p>This class centralizes all Keycloak-related configuration including:
 * <ul>
 *   <li>Keycloak server URLs and realm settings</li>
 *   <li>OIDC discovery endpoints</li>
 *   <li>JWT validation settings (cache TTL, clock skew, audience)</li>
 * </ul>
 *
 * <p>Configuration can be enabled/disabled via {@code authplatform.keycloak.enabled} property.
 *
 * <p>Example configuration in {@code application.yml}:
 * <pre>
 * authplatform:
 *   keycloak:
 *     enabled: true
 *     base-url: http://localhost:8180
 *     realm: authplatform
 *     issuer-uri: http://localhost:8180/realms/authplatform
 *     jwk-set-uri: http://localhost:8180/realms/authplatform/protocol/openid-connect/certs
 *     jwt:
 *       public-key-cache-ttl: 3600
 *       clock-skew-seconds: 30
 *       expected-audience: auth-platform-backend
 * </pre>
 *
 * @since 0.2.0
 */
@Configuration
@ConfigurationProperties(prefix = "authplatform.keycloak")
@Validated
public class KeycloakProperties {

    /**
     * Enable or disable JWT authentication with Keycloak.
     * When false, JWT authentication filter is not initialized.
     */
    private boolean enabled = false;

    /**
     * Base URL of the Keycloak server (e.g., http://localhost:8180).
     */
    @NotEmpty(message = "Keycloak base-url must not be empty when enabled")
    private String baseUrl;

    /**
     * Keycloak realm name (e.g., authplatform).
     */
    @NotEmpty(message = "Keycloak realm must not be empty when enabled")
    private String realm;

    /**
     * OIDC issuer URI for JWT validation.
     * Format: {base-url}/realms/{realm}
     */
    @NotEmpty(message = "Keycloak issuer-uri must not be empty when enabled")
    private String issuerUri;

    /**
     * JWK Set URI for fetching Keycloak's public keys.
     * Format: {base-url}/realms/{realm}/protocol/openid-connect/certs
     */
    @NotEmpty(message = "Keycloak jwk-set-uri must not be empty when enabled")
    private String jwkSetUri;

    /**
     * Admin console URL for documentation purposes.
     * Format: {base-url}/admin
     */
    private String adminConsoleUrl;

    /**
     * JWT validation settings.
     */
    @NotNull
    private JwtValidationSettings jwt = new JwtValidationSettings();

    /**
     * JWT validation configuration.
     */
    public static class JwtValidationSettings {

        /**
         * Public key cache TTL in seconds.
         * Default: 3600 (1 hour).
         * Keycloak's public keys are cached to minimize network calls.
         */
        private int publicKeyCacheTtl = 3600;

        /**
         * Clock skew tolerance in seconds for JWT expiration validation.
         * Default: 30 seconds.
         * Allows small time differences between client and server clocks.
         */
        private int clockSkewSeconds = 30;

        /**
         * Expected audience (aud claim) in JWT tokens.
         * Default: auth-platform-backend.
         */
        private String expectedAudience = "auth-platform-backend";

        public int getPublicKeyCacheTtl() {
            return publicKeyCacheTtl;
        }

        public void setPublicKeyCacheTtl(int publicKeyCacheTtl) {
            this.publicKeyCacheTtl = publicKeyCacheTtl;
        }

        public int getClockSkewSeconds() {
            return clockSkewSeconds;
        }

        public void setClockSkewSeconds(int clockSkewSeconds) {
            this.clockSkewSeconds = clockSkewSeconds;
        }

        public String getExpectedAudience() {
            return expectedAudience;
        }

        public void setExpectedAudience(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getAdminConsoleUrl() {
        return adminConsoleUrl;
    }

    public void setAdminConsoleUrl(String adminConsoleUrl) {
        this.adminConsoleUrl = adminConsoleUrl;
    }

    public JwtValidationSettings getJwt() {
        return jwt;
    }

    public void setJwt(JwtValidationSettings jwt) {
        this.jwt = jwt;
    }
}
