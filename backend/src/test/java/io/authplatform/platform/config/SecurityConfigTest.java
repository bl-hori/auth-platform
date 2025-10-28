package io.authplatform.platform.config;

import io.authplatform.platform.security.ApiKeyAuthenticationFilter;
import io.authplatform.platform.security.JwtAuthenticationFilter;
import io.authplatform.platform.security.RateLimitFilter;
import io.authplatform.platform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SecurityConfig}.
 *
 * <p>Tests security configuration including:
 * <ul>
 *   <li>Filter chain configuration</li>
 *   <li>Conditional JWT filter creation</li>
 *   <li>Hybrid authentication setup</li>
 * </ul>
 *
 * @since 0.2.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Unit Tests")
class SecurityConfigTest {

    @Mock
    private ApiKeyProperties apiKeyProperties;

    @Mock
    private RateLimitFilter rateLimitFilter;

    @Mock
    private CorsConfigurationSource corsConfigurationSource;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private UserService userService;

    @Mock
    private KeycloakProperties keycloakProperties;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                apiKeyProperties,
                rateLimitFilter,
                corsConfigurationSource
        );
    }

    @Test
    @DisplayName("Should create API key authentication filter bean")
    void shouldCreateApiKeyAuthenticationFilterBean() {
        // When
        ApiKeyAuthenticationFilter filter = securityConfig.apiKeyAuthenticationFilter();

        // Then
        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("Should create JWT authentication filter bean when enabled")
    void shouldCreateJwtAuthenticationFilterWhenEnabled() {
        // Given
        SecurityConfig configWithJwt = new SecurityConfig(
                apiKeyProperties,
                rateLimitFilter,
                corsConfigurationSource
        );

        // Inject JWT dependencies via reflection (simulating @Autowired)
        setFieldValue(configWithJwt, "jwtDecoder", jwtDecoder);
        setFieldValue(configWithJwt, "userService", userService);
        setFieldValue(configWithJwt, "keycloakProperties", keycloakProperties);

        // When
        JwtAuthenticationFilter filter = configWithJwt.jwtAuthenticationFilter();

        // Then
        assertThat(filter).isNotNull();
    }

    // Note: Security filter chain configuration tests are done in SecurityConfigIntegrationTest
    // Unit testing HttpSecurity configuration requires deep mocking which doesn't add much value
    // Integration tests verify the actual filter chain configuration

    /**
     * Sets a private field value using reflection (for testing @Autowired fields).
     */
    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
