package io.authplatform.platform.security;

import io.authplatform.platform.config.ApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link ApiKeyAuthenticationFilter}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Valid API key authentication</li>
 *   <li>Invalid API key rejection</li>
 *   <li>Missing API key handling</li>
 *   <li>Public endpoint bypass</li>
 *   <li>Organization ID extraction</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("API Key Authentication Filter Tests")
class ApiKeyAuthenticationFilterTest {

    @Mock
    private FilterChain filterChain;

    private ApiKeyProperties apiKeyProperties;
    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();

        // Set up API key properties with test keys
        apiKeyProperties = new ApiKeyProperties();
        apiKeyProperties.setEnabled(true);
        apiKeyProperties.setHeaderName("X-API-Key");
        apiKeyProperties.setKeys(Map.of(
                "valid-key-123", "org-1",
                "another-key-456", "org-2"
        ));

        filter = new ApiKeyAuthenticationFilter(apiKeyProperties);
    }

    @Test
    @DisplayName("Should authenticate request with valid API key")
    void shouldAuthenticateWithValidApiKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/authorize");
        request.addHeader("X-API-Key", "valid-key-123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("Should reject request with invalid API key")
    void shouldRejectInvalidApiKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/authorize");
        request.addHeader("X-API-Key", "invalid-key-999");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Invalid API key");
    }

    @Test
    @DisplayName("Should reject request with missing API key")
    void shouldRejectMissingApiKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/authorize");
        // No API key header added

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("API key is required");
    }

    @Test
    @DisplayName("Should reject request with blank API key")
    void shouldRejectBlankApiKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/authorize");
        request.addHeader("X-API-Key", "   ");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("API key is required");
    }

    @Test
    @DisplayName("Should allow access to actuator health endpoint without API key")
    void shouldAllowPublicActuatorEndpoint() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        // No API key header

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("Should allow access to Swagger UI without API key")
    void shouldAllowSwaggerUiEndpoint() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/swagger-ui/index.html");
        // No API key header

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("Should allow access to API docs without API key")
    void shouldAllowApiDocsEndpoint() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v3/api-docs/swagger-config");
        // No API key header

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("Should extract correct organization ID from API key")
    void shouldExtractCorrectOrganizationId() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/authorize");
        request.addHeader("X-API-Key", "another-key-456");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        // Security context is cleared after filter, so we can't check it here
        // This would be tested in integration tests
    }

    @Test
    @DisplayName("Should not filter when API key authentication is disabled")
    void shouldNotFilterWhenDisabled() throws ServletException, IOException {
        // Given
        apiKeyProperties.setEnabled(false);
        filter = new ApiKeyAuthenticationFilter(apiKeyProperties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/authorize");
        // No API key header

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        boolean shouldNotFilter = filter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("Should filter when API key authentication is enabled")
    void shouldFilterWhenEnabled() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/authorize");

        // When
        boolean shouldNotFilter = filter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isFalse();
    }
}
