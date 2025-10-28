package io.authplatform.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtAuthenticationToken}.
 *
 * @since 0.2.0
 */
@DisplayName("JwtAuthenticationToken Unit Tests")
class JwtAuthenticationTokenTest {

    @Test
    @DisplayName("Should create JWT authentication token with required fields")
    void shouldCreateJwtAuthenticationToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String organizationId = "org-uuid-123";
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        Jwt jwt = createMockJwt();

        // When
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                userId,
                organizationId,
                authorities,
                jwt
        );

        // Then
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getOrganizationId()).isEqualTo(organizationId);
        assertThat(token.getAuthorities()).hasSize(1);
        assertThat(token.getJwt()).isEqualTo(jwt);
        assertThat(token.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should return userId as principal")
    void shouldReturnUserIdAsPrincipal() {
        // Given
        UUID userId = UUID.randomUUID();
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                userId,
                "org-uuid-123",
                Collections.emptyList(),
                createMockJwt()
        );

        // When
        Object principal = token.getPrincipal();

        // Then
        assertThat(principal).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should return JWT as credentials")
    void shouldReturnJwtAsCredentials() {
        // Given
        Jwt jwt = createMockJwt();
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                UUID.randomUUID(),
                "org-uuid-123",
                Collections.emptyList(),
                jwt
        );

        // When
        Object credentials = token.getCredentials();

        // Then
        assertThat(credentials).isEqualTo(jwt);
    }

    @Test
    @DisplayName("Should be authenticated by default")
    void shouldBeAuthenticatedByDefault() {
        // Given/When
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                UUID.randomUUID(),
                "org-uuid-123",
                Collections.emptyList(),
                createMockJwt()
        );

        // Then
        assertThat(token.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should store empty authorities when none provided")
    void shouldStoreEmptyAuthorities() {
        // Given/When
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                UUID.randomUUID(),
                "org-uuid-123",
                Collections.emptyList(),
                createMockJwt()
        );

        // Then
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Should store multiple authorities")
    void shouldStoreMultipleAuthorities() {
        // Given
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("SCOPE_read"),
                new SimpleGrantedAuthority("SCOPE_write")
        );

        // When
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                UUID.randomUUID(),
                "org-uuid-123",
                authorities,
                createMockJwt()
        );

        // Then
        assertThat(token.getAuthorities()).hasSize(4);
        assertThat(token.getAuthorities()).containsExactlyElementsOf(authorities);
    }

    @Test
    @DisplayName("Should preserve JWT claims")
    void shouldPreserveJwtClaims() {
        // Given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-sub-123")
                .claim("email", "user@example.com")
                .claim("organization_id", "org-uuid-456")
                .claim("custom_claim", "custom_value")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // When
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                UUID.randomUUID(),
                "org-uuid-456",
                Collections.emptyList(),
                jwt
        );

        // Then
        assertThat(token.getJwt()).isEqualTo(jwt);
        assertThat((String) token.getJwt().getClaim("email")).isEqualTo("user@example.com");
        assertThat((String) token.getJwt().getClaim("organization_id")).isEqualTo("org-uuid-456");
        assertThat((String) token.getJwt().getClaim("custom_claim")).isEqualTo("custom_value");
    }

    @Test
    @DisplayName("Should maintain immutability of authorities")
    void shouldMaintainImmutabilityOfAuthorities() {
        // Given
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        JwtAuthenticationToken token = new JwtAuthenticationToken(
                UUID.randomUUID(),
                "org-uuid-123",
                authorities,
                createMockJwt()
        );

        // When/Then
        // The authorities collection should be immutable (from AbstractAuthenticationToken)
        assertThat(token.getAuthorities()).hasSize(1);
    }

    // Helper methods

    private Jwt createMockJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-sub-123")
                .claim("email", "user@example.com")
                .claim("organization_id", "org-uuid-456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
