package io.authplatform.platform.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;
import io.authplatform.platform.api.dto.BatchAuthorizationRequest;
import io.authplatform.platform.api.dto.BatchAuthorizationResponse;
import io.authplatform.platform.config.ApiKeyProperties;
import io.authplatform.platform.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthorizationController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authorization API Tests")
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyProperties apiKeyProperties;

    @MockBean
    private AuthorizationService authorizationService;

    private String validApiKey;
    private UUID testOrgId;
    private AuthorizationRequest validRequest;
    private AuthorizationResponse allowResponse;

    @BeforeEach
    void setUp() {
        // Use the test API key from application-test.yml
        validApiKey = "test-key";
        testOrgId = UUID.randomUUID();

        validRequest = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        allowResponse = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("User has required permissions")
                .evaluationTimeMs(5L)
                .build();
    }

    @Test
    @DisplayName("Should authorize successfully with valid request")
    void shouldAuthorizeSuccessfully() throws Exception {
        // Given
        when(authorizationService.authorize(any(AuthorizationRequest.class)))
                .thenReturn(allowResponse);

        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.decision").value("ALLOW"))
                .andExpect(jsonPath("$.reason").value("User has required permissions"))
                .andExpect(jsonPath("$.evaluationTimeMs").value(5));
    }

    @Test
    @DisplayName("Should return DENY when authorization is denied")
    void shouldReturnDenyWhenDenied() throws Exception {
        // Given
        AuthorizationResponse denyResponse = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.DENY)
                .reason("User lacks required permissions")
                .evaluationTimeMs(3L)
                .build();

        when(authorizationService.authorize(any(AuthorizationRequest.class)))
                .thenReturn(denyResponse);

        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reason").value("User lacks required permissions"));
    }

    @Test
    @DisplayName("Should reject request without API key")
    void shouldRejectRequestWithoutApiKey() throws Exception {
        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with invalid API key")
    void shouldRejectRequestWithInvalidApiKey() throws Exception {
        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with missing organization ID")
    void shouldRejectRequestWithMissingOrganizationId() throws Exception {
        // Given
        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.organizationId").exists());
    }

    @Test
    @DisplayName("Should reject request with missing principal")
    void shouldRejectRequestWithMissingPrincipal() throws Exception {
        // Given
        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.principal").exists());
    }

    @Test
    @DisplayName("Should reject request with missing action")
    void shouldRejectRequestWithMissingAction() throws Exception {
        // Given
        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.action").exists());
    }

    @Test
    @DisplayName("Should reject request with missing resource")
    void shouldRejectRequestWithMissingResource() throws Exception {
        // Given
        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.resource").exists());
    }

    @Test
    @DisplayName("Should reject request with invalid JSON")
    void shouldRejectRequestWithInvalidJson() throws Exception {
        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject request with empty body")
    void shouldRejectRequestWithEmptyBody() throws Exception {
        // When/Then
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    // ===== Batch Authorization Tests =====

    @Test
    @DisplayName("Should process batch authorization successfully")
    void shouldProcessBatchAuthorizationSuccessfully() throws Exception {
        // Given
        AuthorizationRequest request1 = AuthorizationRequest.builder()
                .organizationId(testOrgId)
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

        AuthorizationRequest request2 = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("write")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-2")
                        .build())
                .build();

        BatchAuthorizationRequest batchRequest = BatchAuthorizationRequest.builder()
                .requests(Arrays.asList(request1, request2))
                .build();

        AuthorizationResponse response1 = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("User has read permission")
                .evaluationTimeMs(3L)
                .build();

        AuthorizationResponse response2 = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.DENY)
                .reason("User lacks write permission")
                .evaluationTimeMs(2L)
                .build();

        when(authorizationService.authorizeBatch(anyList()))
                .thenReturn(Arrays.asList(response1, response2));

        // When/Then
        mockMvc.perform(post("/v1/authorize/batch")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responses").isArray())
                .andExpect(jsonPath("$.responses.length()").value(2))
                .andExpect(jsonPath("$.responses[0].decision").value("ALLOW"))
                .andExpect(jsonPath("$.responses[0].reason").value("User has read permission"))
                .andExpect(jsonPath("$.responses[1].decision").value("DENY"))
                .andExpect(jsonPath("$.responses[1].reason").value("User lacks write permission"))
                .andExpect(jsonPath("$.totalEvaluationTimeMs").isNumber());
    }

    @Test
    @DisplayName("Should reject batch request with empty requests list")
    void shouldRejectBatchRequestWithEmptyList() throws Exception {
        // Given
        BatchAuthorizationRequest batchRequest = BatchAuthorizationRequest.builder()
                .requests(List.of())
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize/batch")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.requests").exists());
    }

    @Test
    @DisplayName("Should reject batch request exceeding size limit")
    void shouldRejectBatchRequestExceedingSizeLimit() throws Exception {
        // Given - Create 101 requests (exceeds 100 limit)
        List<AuthorizationRequest> requests = new java.util.ArrayList<>();
        for (int i = 0; i < 101; i++) {
            requests.add(AuthorizationRequest.builder()
                    .organizationId(testOrgId)
                    .principal(AuthorizationRequest.Principal.builder()
                            .id("user-" + i)
                            .type("user")
                            .build())
                    .action("read")
                    .resource(AuthorizationRequest.Resource.builder()
                            .type("document")
                            .id("doc-" + i)
                            .build())
                    .build());
        }

        BatchAuthorizationRequest batchRequest = BatchAuthorizationRequest.builder()
                .requests(requests)
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize/batch")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.requests").exists());
    }

    @Test
    @DisplayName("Should reject batch request with invalid nested request")
    void shouldRejectBatchRequestWithInvalidNestedRequest() throws Exception {
        // Given - Missing action in second request
        AuthorizationRequest validRequest = AuthorizationRequest.builder()
                .organizationId(testOrgId)
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

        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                // Missing action
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-2")
                        .build())
                .build();

        BatchAuthorizationRequest batchRequest = BatchAuthorizationRequest.builder()
                .requests(Arrays.asList(validRequest, invalidRequest))
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize/batch")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors['requests[1].action']").exists());
    }

    @Test
    @DisplayName("Should reject batch request without API key")
    void shouldRejectBatchRequestWithoutApiKey() throws Exception {
        // Given
        BatchAuthorizationRequest batchRequest = BatchAuthorizationRequest.builder()
                .requests(List.of(validRequest))
                .build();

        // When/Then
        mockMvc.perform(post("/v1/authorize/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should process large valid batch")
    void shouldProcessLargeValidBatch() throws Exception {
        // Given - Create 50 valid requests
        List<AuthorizationRequest> requests = new java.util.ArrayList<>();
        List<AuthorizationResponse> responses = new java.util.ArrayList<>();

        for (int i = 0; i < 50; i++) {
            requests.add(AuthorizationRequest.builder()
                    .organizationId(testOrgId)
                    .principal(AuthorizationRequest.Principal.builder()
                            .id("user-" + i)
                            .type("user")
                            .build())
                    .action("read")
                    .resource(AuthorizationRequest.Resource.builder()
                            .type("document")
                            .id("doc-" + i)
                            .build())
                    .build());

            responses.add(AuthorizationResponse.builder()
                    .decision(i % 2 == 0 ? AuthorizationResponse.Decision.ALLOW : AuthorizationResponse.Decision.DENY)
                    .reason("Test decision " + i)
                    .evaluationTimeMs(2L)
                    .build());
        }

        BatchAuthorizationRequest batchRequest = BatchAuthorizationRequest.builder()
                .requests(requests)
                .build();

        when(authorizationService.authorizeBatch(anyList()))
                .thenReturn(responses);

        // When/Then
        mockMvc.perform(post("/v1/authorize/batch")
                        .header("X-API-Key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responses").isArray())
                .andExpect(jsonPath("$.responses.length()").value(50))
                .andExpect(jsonPath("$.totalEvaluationTimeMs").isNumber());
    }
}
