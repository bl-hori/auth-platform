package io.authplatform.platform.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Spring Security configuration.
 *
 * <p>These tests verify the entire security filter chain works correctly
 * with API key authentication in a real Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security Configuration Integration Tests")
@org.springframework.test.context.ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow access to actuator health endpoint without authentication")
    void shouldAllowHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to actuator info endpoint without authentication")
    void shouldAllowInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to Swagger UI without authentication")
    void shouldAllowSwaggerUi() throws Exception {
        // Swagger UI redirects, so we expect 3xx status instead of 200
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    void shouldRequireAuthenticationForProtectedEndpoint() throws Exception {
        mockMvc.perform(post("/v1/authorize"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should authenticate with valid API key")
    void shouldAuthenticateWithValidApiKey() throws Exception {
        // Authentication succeeds, but request fails due to missing Content-Type header
        // 415 Unsupported Media Type instead of 401 means auth passed
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "dev-key-org1-abc123"))
                .andExpect(status().isUnsupportedMediaType()); // 415 instead of 401 means auth passed
    }

    @Test
    @DisplayName("Should reject invalid API key")
    void shouldRejectInvalidApiKey() throws Exception {
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "invalid-key-999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject missing API key")
    void shouldRejectMissingApiKey() throws Exception {
        mockMvc.perform(post("/v1/authorize"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should authenticate with second valid API key")
    void shouldAuthenticateWithSecondApiKey() throws Exception {
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "dev-key-org2-def456"))
                .andExpect(status().isUnsupportedMediaType()); // Auth passes, but missing Content-Type
    }

    @Test
    @DisplayName("Should authenticate with test API key")
    void shouldAuthenticateWithTestApiKey() throws Exception {
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-key-xyz789"))
                .andExpect(status().isUnsupportedMediaType()); // Auth passes, but missing Content-Type
    }

    @Test
    @DisplayName("Should reject empty API key header")
    void shouldRejectEmptyApiKey() throws Exception {
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", ""))
                .andExpect(status().isUnauthorized());
    }
}
