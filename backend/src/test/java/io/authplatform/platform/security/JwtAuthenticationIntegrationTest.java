package io.authplatform.platform.security;

import io.authplatform.platform.config.KeycloakProperties;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for JWT authentication.
 *
 * <p>Tests end-to-end JWT authentication flow including:
 * <ul>
 *   <li>JWT validation and user provisioning</li>
 *   <li>Hybrid authentication (JWT + API Key)</li>
 *   <li>Public endpoint access</li>
 *   <li>JIT provisioning scenarios</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * @since 0.2.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("JWT Authentication Integration Tests")
class JwtAuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeycloakProperties keycloakProperties;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Organization testOrganization;
    private String organizationId;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = Organization.builder()
                .name("test-org")
                .displayName("Test Organization")
                .status(Organization.OrganizationStatus.ACTIVE)
                .build();
        testOrganization = organizationRepository.save(testOrganization);
        organizationId = testOrganization.getId().toString();
    }

    @Test
    @DisplayName("Should authenticate successfully with valid JWT")
    void shouldAuthenticateWithValidJwt() throws Exception {
        // Given
        String jwt = createValidJwtToken("user-sub-123", "user@example.com", organizationId);

        // When/Then
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // Verify user was created via JIT provisioning
        var user = userRepository.findByKeycloakSubAndDeletedAtIsNull("user-sub-123");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("user@example.com");
        assertThat(user.get().getOrganization().getId()).isEqualTo(testOrganization.getId());
    }

    @Test
    @DisplayName("Should link existing user on first JWT authentication")
    void shouldLinkExistingUserOnFirstJwtAuth() throws Exception {
        // Given: Create user without keycloak_sub
        User existingUser = User.builder()
                .organization(testOrganization)
                .email("existing@example.com")
                .displayName("Existing User")
                .status(User.UserStatus.ACTIVE)
                .attributes(Map.of())
                .build();
        existingUser = userRepository.save(existingUser);

        String jwt = createValidJwtToken("new-keycloak-sub", "existing@example.com", organizationId);

        // When
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // Then: User should be linked to Keycloak
        var linkedUser = userRepository.findById(existingUser.getId());
        assertThat(linkedUser).isPresent();
        assertThat(linkedUser.get().getKeycloakSub()).isEqualTo("new-keycloak-sub");
        assertThat(linkedUser.get().getKeycloakSyncedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject request with invalid JWT")
    void shouldRejectInvalidJwt() throws Exception {
        // Given
        String invalidJwt = "invalid.jwt.token";

        // When/Then
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + invalidJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject expired JWT")
    void shouldRejectExpiredJwt() throws Exception {
        // Given: Create expired JWT
        String expiredJwt = createExpiredJwtToken("user-sub-123", "user@example.com", organizationId);

        // When/Then
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + expiredJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject JWT with missing organization_id")
    void shouldRejectJwtWithoutOrganizationId() throws Exception {
        // Given: JWT without organization_id claim
        String jwt = createJwtTokenWithoutOrganizationId("user-sub-123", "user@example.com");

        // When/Then
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject JWT with non-existent organization")
    void shouldRejectJwtWithInvalidOrganization() throws Exception {
        // Given: JWT with non-existent organization
        String invalidOrgId = UUID.randomUUID().toString();
        String jwt = createValidJwtToken("user-sub-123", "user@example.com", invalidOrgId);

        // When/Then
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow public endpoint access without JWT")
    void shouldAllowPublicEndpointAccess() throws Exception {
        // When/Then: Health endpoint should be accessible without authentication
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Should fallback to API Key when JWT not present")
    void shouldFallbackToApiKeyAuth() throws Exception {
        // Given: Valid API key (from test configuration)
        String apiKey = "test-api-key-12345";

        // When/Then: Should authenticate with API Key
        mockMvc.perform(get("/v1/organizations")
                        .header("X-API-Key", apiKey))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should prefer JWT over API Key when both present")
    void shouldPreferJwtOverApiKey() throws Exception {
        // Given: Both JWT and API Key
        String jwt = createValidJwtToken("user-sub-123", "user@example.com", organizationId);
        String apiKey = "test-api-key-12345";

        // When
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt)
                        .header("X-API-Key", apiKey))
                .andExpect(status().isOk());

        // Then: JWT should be used (verify user was created via JIT)
        var user = userRepository.findByKeycloakSubAndDeletedAtIsNull("user-sub-123");
        assertThat(user).isPresent();
    }

    @Test
    @DisplayName("Should update keycloak_synced_at on subsequent authentications")
    void shouldUpdateSyncedAtTimestamp() throws Exception {
        // Given: Create user with JWT
        String jwt = createValidJwtToken("user-sub-123", "user@example.com", organizationId);

        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        var user = userRepository.findByKeycloakSubAndDeletedAtIsNull("user-sub-123").orElseThrow();
        var firstSyncTime = user.getKeycloakSyncedAt();

        // Wait a bit
        Thread.sleep(100);

        // When: Authenticate again
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // Then: Synced timestamp should be updated
        user = userRepository.findByKeycloakSubAndDeletedAtIsNull("user-sub-123").orElseThrow();
        assertThat(user.getKeycloakSyncedAt()).isAfter(firstSyncTime);
    }

    @Test
    @DisplayName("Should handle email as optional claim in JWT")
    void shouldHandleOptionalEmailClaim() throws Exception {
        // Given: JWT without email claim
        String jwt = createJwtTokenWithoutEmail("user-sub-456", organizationId);

        // When/Then: Should still authenticate successfully
        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // Verify user was created with null email
        var user = userRepository.findByKeycloakSubAndDeletedAtIsNull("user-sub-456");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isNull();
    }

    // Helper methods

    /**
     * Creates a valid JWT token for testing and mocks JwtDecoder to return it.
     */
    private String createValidJwtToken(String subject, String email, String organizationId) {
        String token = "valid.jwt.token." + subject;

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject(subject)
                .claim("email", email)
                .claim("organization_id", organizationId)
                .claim("preferred_username", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(jwtDecoder.decode(token)).thenReturn(jwt);
        return token;
    }

    private String createExpiredJwtToken(String subject, String email, String organizationId) {
        String token = "expired.jwt.token." + subject;

        // Mock decoder to throw exception for expired token
        when(jwtDecoder.decode(token)).thenThrow(new JwtException("JWT expired"));
        return token;
    }

    private String createJwtTokenWithoutOrganizationId(String subject, String email) {
        String token = "jwt.without.org." + subject;

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject(subject)
                .claim("email", email)
                // Missing organization_id
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(jwtDecoder.decode(token)).thenReturn(jwt);
        return token;
    }

    private String createJwtTokenWithoutEmail(String subject, String organizationId) {
        String token = "jwt.without.email." + subject;

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject(subject)
                .claim("organization_id", organizationId)
                // Missing email
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(jwtDecoder.decode(token)).thenReturn(jwt);
        return token;
    }
}
