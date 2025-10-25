package io.authplatform.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiKeyAuthenticationToken}.
 *
 * <p>Tests verify the authentication token correctly stores and exposes
 * API key credentials and organization ID principal.
 */
@DisplayName("API Key Authentication Token Tests")
class ApiKeyAuthenticationTokenTest {

    @Test
    @DisplayName("Should create authenticated token with correct values")
    void shouldCreateAuthenticatedToken() {
        // Given
        String apiKey = "test-key-123";
        String orgId = "org-1";
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"));

        // When
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(apiKey, orgId, authorities);

        // Then
        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getApiKey()).isEqualTo(apiKey);
        assertThat(token.getOrganizationId()).isEqualTo(orgId);
        assertThat(token.getPrincipal()).isEqualTo(orgId);
        assertThat(token.getCredentials()).isEqualTo(apiKey);
        assertThat(token.getAuthorities()).containsExactlyElementsOf(authorities);
    }

    @Test
    @DisplayName("Should return organization ID as principal")
    void shouldReturnOrganizationIdAsPrincipal() {
        // Given
        String orgId = "test-organization";
        var token = new ApiKeyAuthenticationToken(
                "api-key",
                orgId,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );

        // When
        Object principal = token.getPrincipal();

        // Then
        assertThat(principal).isEqualTo(orgId);
        assertThat(token.getOrganizationId()).isEqualTo(orgId);
    }

    @Test
    @DisplayName("Should return API key as credentials")
    void shouldReturnApiKeyAsCredentials() {
        // Given
        String apiKey = "secret-api-key";
        var token = new ApiKeyAuthenticationToken(
                apiKey,
                "org-id",
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );

        // When
        Object credentials = token.getCredentials();

        // Then
        assertThat(credentials).isEqualTo(apiKey);
        assertThat(token.getApiKey()).isEqualTo(apiKey);
    }

    @Test
    @DisplayName("Should include organization ID in toString")
    void shouldIncludeOrgIdInToString() {
        // Given
        var token = new ApiKeyAuthenticationToken(
                "api-key",
                "my-organization",
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );

        // When
        String tokenString = token.toString();

        // Then
        assertThat(tokenString).contains("my-organization");
        assertThat(tokenString).contains("authenticated=true");
        assertThat(tokenString).doesNotContain("api-key"); // Don't expose API key in logs
    }

    @Test
    @DisplayName("Should handle multiple authorities")
    void shouldHandleMultipleAuthorities() {
        // Given
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_API_CLIENT"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        // When
        var token = new ApiKeyAuthenticationToken("key", "org", authorities);

        // Then
        assertThat(token.getAuthorities()).hasSize(2);
        assertThat(token.getAuthorities()).containsExactlyElementsOf(authorities);
    }
}
