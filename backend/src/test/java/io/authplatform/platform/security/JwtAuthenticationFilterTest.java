package io.authplatform.platform.security;

import io.authplatform.platform.config.KeycloakProperties;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * <p>Tests JWT extraction, validation, JIT provisioning, and SecurityContext setup.
 *
 * @since 0.2.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private UserService userService;

    @Mock
    private KeycloakProperties keycloakProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtDecoder, userService, keycloakProperties);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should extract JWT from Authorization header")
    void shouldExtractJwtFromAuthorizationHeader() throws Exception {
        // Given
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/users");

        Jwt jwt = createMockJwt("user-sub-123", "user@example.com", "org-uuid-456");
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        User mockUser = createMockUser();
        when(userService.findOrCreateFromJwt(anyString(), anyString(), anyString())).thenReturn(mockUser);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder).decode(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when no Authorization header present")
    void shouldContinueWhenNoAuthorizationHeader() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/users");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder, never()).decode(anyString());
        verify(userService, never()).findOrCreateFromJwt(anyString(), anyString(), anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header is not Bearer token")
    void shouldContinueWhenNotBearerToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNzd29yZA==");
        when(request.getRequestURI()).thenReturn("/api/users");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder, never()).decode(anyString());
        verify(userService, never()).findOrCreateFromJwt(anyString(), anyString(), anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should validate JWT and provision user successfully")
    void shouldValidateJwtAndProvisionUser() throws Exception {
        // Given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/users");

        String keycloakSub = "keycloak-sub-123";
        String email = "user@example.com";
        String organizationId = "org-uuid-456";

        Jwt jwt = createMockJwt(keycloakSub, email, organizationId);
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        User mockUser = createMockUser();
        when(userService.findOrCreateFromJwt(keycloakSub, email, organizationId)).thenReturn(mockUser);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder).decode(token);
        verify(userService).findOrCreateFromJwt(keycloakSub, email, organizationId);
        verify(filterChain).doFilter(request, response);

        // Verify SecurityContext is set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(JwtAuthenticationToken.class);

        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getUserId()).isEqualTo(mockUser.getId());
        assertThat(auth.getOrganizationId()).isEqualTo(organizationId);
        assertThat(auth.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should handle JWT validation failure")
    void shouldHandleJwtValidationFailure() throws Exception {
        // Given
        String token = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/users");
        when(jwtDecoder.decode(token)).thenThrow(new JwtException("Invalid JWT"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder).decode(token);
        verify(userService, never()).findOrCreateFromJwt(anyString(), anyString(), anyString());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);

        // SecurityContext should not be set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle missing email claim in JWT (email is optional)")
    void shouldHandleMissingEmailClaim() throws Exception {
        // Given
        String token = "jwt.without.email";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/users");

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("keycloak-sub-123")
                .claim("organization_id", "org-uuid-456")
                // Missing email claim - but should still work with null
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtDecoder.decode(token)).thenReturn(jwt);

        User mockUser = createMockUser();
        when(userService.findOrCreateFromJwt(anyString(), isNull(), anyString())).thenReturn(mockUser);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder).decode(token);
        verify(userService).findOrCreateFromJwt("keycloak-sub-123", null, "org-uuid-456");
        verify(filterChain).doFilter(request, response);

        // SecurityContext should be set even without email
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("Should handle missing organization_id claim in JWT")
    void shouldHandleMissingOrganizationIdClaim() throws Exception {
        // Given
        String token = "jwt.without.org";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/users");

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("keycloak-sub-123")
                .claim("email", "user@example.com")
                // Missing organization_id claim
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder).decode(token);
        verify(userService, never()).findOrCreateFromJwt(anyString(), anyString(), anyString());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);

        // SecurityContext should not be set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle JIT provisioning failure")
    void shouldHandleJitProvisioningFailure() throws Exception {
        // Given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/users");

        Jwt jwt = createMockJwt("keycloak-sub-123", "user@example.com", "org-uuid-456");
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        when(userService.findOrCreateFromJwt(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Organization not found"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtDecoder).decode(token);
        verify(userService).findOrCreateFromJwt(anyString(), anyString(), anyString());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);

        // SecurityContext should not be set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should set JwtAuthenticationToken with correct authorities")
    void shouldSetJwtAuthenticationTokenWithAuthorities() throws Exception {
        // Given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/users");

        Jwt jwt = createMockJwt("keycloak-sub-123", "user@example.com", "org-uuid-456");
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        User mockUser = createMockUser();
        when(userService.findOrCreateFromJwt(anyString(), anyString(), anyString())).thenReturn(mockUser);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty(); // No roles assigned yet
        assertThat(auth.getPrincipal()).isEqualTo(mockUser.getId());
        assertThat(auth.getJwt()).isEqualTo(jwt);
    }

    // Helper methods

    private Jwt createMockJwt(String subject, String email, String organizationId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("email", email)
                .claim("organization_id", organizationId)
                .claim("preferred_username", "user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private User createMockUser() {
        Organization org = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Organization")
                .displayName("Test Organization")
                .status(Organization.OrganizationStatus.ACTIVE)
                .build();

        return User.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .email("user@example.com")
                .displayName("Test User")
                .keycloakSub("keycloak-sub-123")
                .status(User.UserStatus.ACTIVE)
                .attributes(Map.of())
                .build();
    }
}
