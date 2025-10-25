package io.authplatform.platform.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuthorizationResponse} DTO.
 *
 * <p>Verifies response construction and helper methods.
 */
@DisplayName("AuthorizationResponse DTO Tests")
class AuthorizationResponseTest {

    @Test
    @DisplayName("Should create allow response")
    void shouldCreateAllowResponse() {
        // Given: Allow decision
        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("User has required permission")
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(5L)
                .build();

        // Then: Should be allowed
        assertThat(response.isAllowed()).isTrue();
        assertThat(response.isDenied()).isFalse();
        assertThat(response.isError()).isFalse();
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ALLOW);
    }

    @Test
    @DisplayName("Should create deny response")
    void shouldCreateDenyResponse() {
        // Given: Deny decision
        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.DENY)
                .reason("User lacks required permission")
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(3L)
                .build();

        // Then: Should be denied
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.isDenied()).isTrue();
        assertThat(response.isError()).isFalse();
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.DENY);
    }

    @Test
    @DisplayName("Should create error response")
    void shouldCreateErrorResponse() {
        // Given: Error decision
        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ERROR)
                .reason("Policy evaluation timeout")
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(null)
                .build();

        // Then: Should be error
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.isDenied()).isFalse();
        assertThat(response.isError()).isTrue();
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ERROR);
    }

    @Test
    @DisplayName("Should include applied policies")
    void shouldIncludeAppliedPolicies() {
        // Given: Response with applied policies
        AuthorizationResponse.AppliedPolicy policy1 = AuthorizationResponse.AppliedPolicy.builder()
                .policyId("policy-123")
                .policyName("document-access")
                .version(1)
                .effect("allow")
                .matchedConditions(List.of("user.department == 'Engineering'"))
                .build();

        AuthorizationResponse.AppliedPolicy policy2 = AuthorizationResponse.AppliedPolicy.builder()
                .policyId("policy-456")
                .policyName("time-restriction")
                .version(2)
                .effect("allow")
                .matchedConditions(List.of("time.hour >= 9", "time.hour <= 17"))
                .build();

        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Multiple policies matched")
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(8L)
                .appliedPolicies(List.of(policy1, policy2))
                .build();

        // Then: Should have applied policies
        assertThat(response.getAppliedPolicies()).hasSize(2);
        assertThat(response.getAppliedPolicies().get(0).getPolicyId()).isEqualTo("policy-123");
        assertThat(response.getAppliedPolicies().get(1).getPolicyId()).isEqualTo("policy-456");
    }

    @Test
    @DisplayName("Should include context metadata")
    void shouldIncludeContextMetadata() {
        // Given: Response with context
        Map<String, Object> context = Map.of(
                "matchedRoles", List.of("viewer", "team-member"),
                "matchedPermissions", List.of("document:read"),
                "cacheHit", true
        );

        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("User has viewer role")
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(2L)
                .context(context)
                .build();

        // Then: Should have context
        assertThat(response.getContext()).isNotNull();
        assertThat(response.getContext()).containsKeys("matchedRoles", "matchedPermissions", "cacheHit");
        assertThat(response.getContext().get("cacheHit")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        // Given: Response with minimal fields
        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason(null)
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(null)
                .appliedPolicies(null)
                .context(null)
                .build();

        // Then: Should not throw exceptions
        assertThat(response.getReason()).isNull();
        assertThat(response.getEvaluationTimeMs()).isNull();
        assertThat(response.getAppliedPolicies()).isNull();
        assertThat(response.getContext()).isNull();
    }

    @Test
    @DisplayName("Should create applied policy with all fields")
    void shouldCreateAppliedPolicyWithAllFields() {
        // Given: Applied policy with all fields
        AuthorizationResponse.AppliedPolicy policy = AuthorizationResponse.AppliedPolicy.builder()
                .policyId("policy-789")
                .policyName("rbac-policy")
                .version(5)
                .effect("allow")
                .matchedConditions(List.of("role == 'admin'", "resource.owner == principal.id"))
                .build();

        // Then: All fields should be set
        assertThat(policy.getPolicyId()).isEqualTo("policy-789");
        assertThat(policy.getPolicyName()).isEqualTo("rbac-policy");
        assertThat(policy.getVersion()).isEqualTo(5);
        assertThat(policy.getEffect()).isEqualTo("allow");
        assertThat(policy.getMatchedConditions()).hasSize(2);
    }

    @Test
    @DisplayName("Should create applied policy with minimal fields")
    void shouldCreateAppliedPolicyWithMinimalFields() {
        // Given: Applied policy with minimal fields
        AuthorizationResponse.AppliedPolicy policy = AuthorizationResponse.AppliedPolicy.builder()
                .policyId("policy-999")
                .policyName("basic-policy")
                .version(1)
                .effect("deny")
                .matchedConditions(null)
                .build();

        // Then: Should handle null conditions
        assertThat(policy.getMatchedConditions()).isNull();
    }

    @Test
    @DisplayName("Should represent fast evaluation time")
    void shouldRepresentFastEvaluationTime() {
        // Given: Response with very fast evaluation
        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Cache hit")
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(1L)
                .context(Map.of("cacheHit", true))
                .build();

        // Then: Should indicate fast evaluation
        assertThat(response.getEvaluationTimeMs()).isEqualTo(1L);
        assertThat(response.getContext().get("cacheHit")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should represent slow evaluation time")
    void shouldRepresentSlowEvaluationTime() {
        // Given: Response with slower evaluation
        AuthorizationResponse response = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Complex policy evaluation")
                .timestamp(OffsetDateTime.now())
                .evaluationTimeMs(50L)
                .context(Map.of("cacheHit", false, "policiesEvaluated", 5))
                .build();

        // Then: Should indicate slower evaluation
        assertThat(response.getEvaluationTimeMs()).isGreaterThan(10L);
        assertThat(response.getContext().get("cacheHit")).isEqualTo(false);
    }
}
