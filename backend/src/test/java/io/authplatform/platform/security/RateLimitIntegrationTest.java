package io.authplatform.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;
import io.authplatform.platform.integration.BaseIntegrationTest;
import io.authplatform.platform.service.AuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for rate limiting functionality.
 *
 * <p>These tests verify that the rate limiting filter correctly enforces
 * request limits per API key using the Bucket4j token bucket algorithm.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "auth-platform.rate-limit.enabled=true",
        "auth-platform.rate-limit.capacity=5",
        "auth-platform.rate-limit.refill-tokens=5",
        "auth-platform.rate-limit.refill-period=1m",
        "auth-platform.security.api-keys.keys.test-key-for-rate-limit=test-org",
        "auth-platform.security.api-keys.keys.test-key-exceed-limit=test-org",
        "auth-platform.security.api-keys.keys.test-key-isolation-1=test-org",
        "auth-platform.security.api-keys.keys.test-key-isolation-2=test-org"
})
@DisplayName("Rate Limit Integration Tests")
class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() throws Exception {
        // Given
        UUID orgId = UUID.randomUUID();
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(orgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Test allow")
                .evaluationTimeMs(5L)
                .build();

        when(authorizationService.authorize(any())).thenReturn(response);

        // When/Then - First 5 requests should succeed (capacity=5)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/authorize")
                            .header("X-API-Key", "test-key-for-rate-limit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.decision").value("ALLOW"));
        }
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit")
    void shouldRejectRequestsExceedingRateLimit() throws Exception {
        // Given
        UUID orgId = UUID.randomUUID();
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(orgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-456")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-2")
                        .build())
                .build();

        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Test allow")
                .evaluationTimeMs(5L)
                .build();

        when(authorizationService.authorize(any())).thenReturn(response);

        String apiKey = "test-key-exceed-limit";

        // When/Then - First 5 requests should succeed
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/authorize")
                            .header("X-API-Key", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // 6th request should be rate limited
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.detail").value("Rate limit exceeded. Too many requests from this API key."))
                .andExpect(jsonPath("$.retryAfterSeconds").exists())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().exists("X-Rate-Limit-Retry-After-Seconds"));
    }

    @Test
    @DisplayName("Should isolate rate limits per API key")
    void shouldIsolateRateLimitsPerApiKey() throws Exception {
        // Given
        UUID orgId = UUID.randomUUID();
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(orgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-789")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-3")
                        .build())
                .build();

        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Test allow")
                .evaluationTimeMs(5L)
                .build();

        when(authorizationService.authorize(any())).thenReturn(response);

        String apiKey1 = "test-key-isolation-1";
        String apiKey2 = "test-key-isolation-2";

        // When/Then - Exhaust rate limit for API key 1
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/authorize")
                            .header("X-API-Key", apiKey1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // API key 1 should be rate limited
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", apiKey1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());

        // API key 2 should still work (separate bucket)
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", apiKey2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should not rate limit actuator endpoints")
    void shouldNotRateLimitActuatorEndpoints() throws Exception {
        // When/Then - Actuator endpoints should never be rate limited
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should not rate limit swagger endpoints")
    void shouldNotRateLimitSwaggerEndpoints() throws Exception {
        // When/Then - Swagger endpoints should never be rate limited
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/swagger-ui.html"))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
