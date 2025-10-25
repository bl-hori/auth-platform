package io.authplatform.platform.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Authorization response DTO containing the access control decision.
 *
 * <p>This DTO represents the result of an authorization request, including
 * the decision (allow/deny), reasoning, and optional metadata.
 *
 * <p><strong>Example Response:</strong>
 * <pre>{@code
 * {
 *   "decision": "allow",
 *   "reason": "User has 'document:read' permission via 'viewer' role",
 *   "timestamp": "2025-10-26T10:30:00.123Z",
 *   "evaluationTimeMs": 5,
 *   "appliedPolicies": [
 *     {
 *       "policyId": "policy-123",
 *       "policyName": "document-access-policy",
 *       "version": 3,
 *       "effect": "allow"
 *     }
 *   ],
 *   "context": {
 *     "matchedRoles": ["viewer", "team-member"],
 *     "matchedPermissions": ["document:read"],
 *     "cacheHit": true
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Decision Values:</strong>
 * <ul>
 *   <li><strong>allow</strong>: Access is granted</li>
 *   <li><strong>deny</strong>: Access is explicitly denied</li>
 *   <li><strong>error</strong>: An error occurred during evaluation</li>
 * </ul>
 *
 * @see AuthorizationRequest
 * @see Decision
 * @see AppliedPolicy
 */
@Value
@Builder
@Schema(description = "Authorization response containing the access control decision")
public class AuthorizationResponse {

    /**
     * Authorization decision.
     *
     * <p>Indicates whether access is allowed, denied, or if an error occurred.
     */
    @NotNull
    @Schema(
            description = "Authorization decision (allow, deny, or error)",
            example = "allow",
            required = true
    )
    Decision decision;

    /**
     * Human-readable reason for the decision.
     *
     * <p>Provides transparency into why access was granted or denied.
     * This helps with debugging, auditing, and user feedback.
     *
     * <p>Example reasons:
     * <ul>
     *   <li>"User has 'document:read' permission via 'viewer' role"</li>
     *   <li>"Access denied: user lacks required permission"</li>
     *   <li>"Access denied: resource classification requires 'confidential' clearance"</li>
     *   <li>"Error: policy evaluation timeout"</li>
     * </ul>
     */
    @Schema(
            description = "Human-readable explanation for the decision",
            example = "User has 'document:read' permission via 'viewer' role"
    )
    String reason;

    /**
     * Timestamp when the authorization decision was made.
     */
    @NotNull
    @Schema(description = "Decision timestamp", example = "2025-10-26T10:30:00.123Z", required = true)
    OffsetDateTime timestamp;

    /**
     * Policy evaluation time in milliseconds.
     *
     * <p>Useful for performance monitoring and identifying slow policy evaluations.
     */
    @Schema(description = "Policy evaluation time in milliseconds", example = "5")
    Long evaluationTimeMs;

    /**
     * List of policies that were applied during evaluation.
     *
     * <p>Shows which policies contributed to the final decision.
     * Useful for debugging and audit trails.
     */
    @Schema(description = "Policies that were evaluated and contributed to the decision")
    List<AppliedPolicy> appliedPolicies;

    /**
     * Additional context and metadata about the authorization decision.
     *
     * <p>Can include:
     * <ul>
     *   <li>matchedRoles - Roles that matched the principal</li>
     *   <li>matchedPermissions - Permissions that were evaluated</li>
     *   <li>cacheHit - Whether the decision was served from cache</li>
     *   <li>warnings - Any warnings during evaluation</li>
     * </ul>
     */
    @Schema(description = "Additional context and metadata")
    Map<String, Object> context;

    @JsonCreator
    public AuthorizationResponse(
            @JsonProperty("decision") Decision decision,
            @JsonProperty("reason") String reason,
            @JsonProperty("timestamp") OffsetDateTime timestamp,
            @JsonProperty("evaluationTimeMs") Long evaluationTimeMs,
            @JsonProperty("appliedPolicies") List<AppliedPolicy> appliedPolicies,
            @JsonProperty("context") Map<String, Object> context) {
        this.decision = decision;
        this.reason = reason;
        this.timestamp = timestamp;
        this.evaluationTimeMs = evaluationTimeMs;
        this.appliedPolicies = appliedPolicies;
        this.context = context;
    }

    /**
     * Authorization decision enum.
     *
     * <p>Represents the possible outcomes of an authorization request.
     */
    @Schema(description = "Authorization decision")
    public enum Decision {
        /**
         * Access is granted.
         */
        @Schema(description = "Access is granted")
        ALLOW,

        /**
         * Access is denied.
         */
        @Schema(description = "Access is denied")
        DENY,

        /**
         * An error occurred during evaluation.
         *
         * <p>This typically results in access being denied for safety,
         * but the error should be logged and investigated.
         */
        @Schema(description = "Error occurred during authorization evaluation")
        ERROR
    }

    /**
     * Information about a policy that was applied during authorization.
     *
     * <p>Tracks which policies contributed to the final decision.
     */
    @Value
    @Builder
    @Schema(description = "Policy that was applied during authorization")
    public static class AppliedPolicy {

        /**
         * Policy identifier.
         */
        @Schema(description = "Policy UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        String policyId;

        /**
         * Policy name.
         */
        @Schema(description = "Human-readable policy name", example = "document-access-policy")
        String policyName;

        /**
         * Policy version that was evaluated.
         */
        @Schema(description = "Policy version number", example = "3")
        Integer version;

        /**
         * Effect of this policy (allow/deny).
         */
        @Schema(description = "Policy effect (allow or deny)", example = "allow")
        String effect;

        /**
         * Conditions that were matched (if any).
         *
         * <p>Shows which specific conditions in the policy were satisfied.
         */
        @Schema(description = "Conditions that were matched in the policy")
        List<String> matchedConditions;

        @JsonCreator
        public AppliedPolicy(
                @JsonProperty("policyId") String policyId,
                @JsonProperty("policyName") String policyName,
                @JsonProperty("version") Integer version,
                @JsonProperty("effect") String effect,
                @JsonProperty("matchedConditions") List<String> matchedConditions) {
            this.policyId = policyId;
            this.policyName = policyName;
            this.version = version;
            this.effect = effect;
            this.matchedConditions = matchedConditions;
        }
    }

    /**
     * Helper method to check if access was allowed.
     *
     * @return true if decision is ALLOW, false otherwise
     */
    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }

    /**
     * Helper method to check if access was denied.
     *
     * @return true if decision is DENY, false otherwise
     */
    public boolean isDenied() {
        return decision == Decision.DENY;
    }

    /**
     * Helper method to check if an error occurred.
     *
     * @return true if decision is ERROR, false otherwise
     */
    public boolean isError() {
        return decision == Decision.ERROR;
    }
}
