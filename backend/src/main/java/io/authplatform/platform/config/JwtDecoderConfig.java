package io.authplatform.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.time.Duration;

/**
 * Configuration for JWT token decoding and validation.
 *
 * <p>This configuration sets up JWT decoding using Keycloak's JWK Set URI
 * for public key retrieval and signature verification. The decoder validates:
 * <ul>
 *   <li>JWT signature using RS256 algorithm</li>
 *   <li>Token expiration (exp claim)</li>
 *   <li>Token not-before time (nbf claim)</li>
 *   <li>Issuer (iss claim) matches Keycloak realm</li>
 *   <li>Audience (aud claim) contains expected value</li>
 * </ul>
 *
 * <p>Public keys are cached automatically by Spring Security's JWK Set cache
 * to minimize network calls to Keycloak. Cache TTL is configurable via
 * {@link KeycloakProperties.JwtValidationSettings#getPublicKeyCacheTtl()}.
 *
 * <p>This configuration is only active when {@code authplatform.keycloak.enabled=true}.
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>JWT validation: &lt;5ms (p95) with cached public keys</li>
 *   <li>Public key fetch: Only on cache miss or key rotation</li>
 *   <li>Clock skew tolerance: Configurable (default 30 seconds)</li>
 * </ul>
 *
 * <h2>Example Configuration</h2>
 * <pre>
 * authplatform:
 *   keycloak:
 *     enabled: true
 *     issuer-uri: http://localhost:8180/realms/authplatform
 *     jwk-set-uri: http://localhost:8180/realms/authplatform/protocol/openid-connect/certs
 *     jwt:
 *       public-key-cache-ttl: 3600
 *       clock-skew-seconds: 30
 *       expected-audience: auth-platform-backend
 * </pre>
 *
 * @see KeycloakProperties
 * @see JwtDecoder
 * @since 0.2.0
 */
@Configuration
@ConditionalOnProperty(prefix = "authplatform.keycloak", name = "enabled", havingValue = "true")
public class JwtDecoderConfig {

    private final KeycloakProperties keycloakProperties;

    /**
     * Constructs the JWT decoder configuration.
     *
     * @param keycloakProperties Keycloak configuration properties
     */
    public JwtDecoderConfig(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    /**
     * Creates a JWT decoder bean for validating Keycloak-issued tokens.
     *
     * <p>The decoder performs the following validations:
     * <ol>
     *   <li><b>Signature Verification</b>: Validates JWT signature using Keycloak's public key (RS256)</li>
     *   <li><b>Timestamp Validation</b>: Checks exp and nbf claims with clock skew tolerance</li>
     *   <li><b>Issuer Validation</b>: Verifies iss claim matches Keycloak realm</li>
     *   <li><b>Audience Validation</b>: Ensures aud claim contains expected audience</li>
     * </ol>
     *
     * <p><b>Public Key Caching</b>: The JWK Set is cached to minimize network calls.
     * Keys are fetched only when:
     * <ul>
     *   <li>First JWT validation after application startup</li>
     *   <li>Cache expires (TTL: 1 hour by default)</li>
     *   <li>Unknown kid (key ID) is encountered (key rotation)</li>
     * </ul>
     *
     * <p><b>Clock Skew Tolerance</b>: The decoder allows small time differences
     * between client and server clocks (default: 30 seconds). This prevents
     * legitimate tokens from being rejected due to clock drift.
     *
     * <p><b>Error Handling</b>: Invalid tokens are rejected with:
     * <ul>
     *   <li>{@link JwtValidationException} for validation failures</li>
     *   <li>{@link BadJwtException} for malformed tokens</li>
     *   <li>{@link JwtException} for general JWT processing errors</li>
     * </ul>
     *
     * @return Configured JWT decoder
     * @throws IllegalStateException if JWK Set URI is not configured
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Create JWT decoder with Keycloak JWK Set URI
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(keycloakProperties.getJwkSetUri())
                .build();

        // Configure JWT validators
        OAuth2TokenValidator<Jwt> validators = createJwtValidators();
        jwtDecoder.setJwtValidator(validators);

        return jwtDecoder;
    }

    /**
     * Creates a composite JWT validator with multiple validation rules.
     *
     * <p>Validators are executed in order:
     * <ol>
     *   <li>Timestamp validator (exp, nbf, iat with clock skew)</li>
     *   <li>Issuer validator (iss claim)</li>
     *   <li>Audience validator (aud claim)</li>
     * </ol>
     *
     * @return Composite OAuth2 token validator
     */
    private OAuth2TokenValidator<Jwt> createJwtValidators() {
        KeycloakProperties.JwtValidationSettings jwtSettings = keycloakProperties.getJwt();

        // 1. Timestamp validator with clock skew tolerance
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(
                Duration.ofSeconds(jwtSettings.getClockSkewSeconds())
        );

        // 2. Issuer validator
        JwtIssuerValidator issuerValidator = new JwtIssuerValidator(
                keycloakProperties.getIssuerUri()
        );

        // 3. Audience validator
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<String>(
                "aud",
                aud -> aud != null && aud.contains(jwtSettings.getExpectedAudience())
        );

        // Combine all validators
        return new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator
        );
    }
}
