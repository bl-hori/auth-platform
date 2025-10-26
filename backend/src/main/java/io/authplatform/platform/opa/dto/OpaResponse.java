package io.authplatform.platform.opa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * Response DTO from OPA (Open Policy Agent) policy evaluation.
 *
 * <p>This represents the output returned from OPA's data API after policy evaluation.
 * The structure follows OPA's standard result format:
 * <pre>{@code
 * {
 *   "result": {
 *     "allow": true,
 *     "reasons": ["User has required permission"],
 *     "matched_policies": ["policy-123", "policy-456"]
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * OpaResponse response = opaClient.evaluate(request);
 *
 * if (response.getResult().isAllow()) {
 *     System.out.println("Access granted: " + response.getResult().getReasons());
 * } else {
 *     System.out.println("Access denied");
 * }
 * }</pre>
 *
 * @see OpaRequest
 */
@Value
@Builder
@Jacksonized
public class OpaResponse {

    /**
     * The result of policy evaluation.
     */
    @JsonProperty("result")
    OpaResult result;

    /**
     * Result data structure from OPA policy evaluation.
     */
    @Value
    @Builder
    @Jacksonized
    public static class OpaResult {
        /**
         * Whether access is allowed (true) or denied (false).
         */
        @JsonProperty("allow")
        boolean allow;

        /**
         * Reasons for the decision (human-readable explanations).
         */
        @JsonProperty("reasons")
        List<String> reasons;

        /**
         * List of policy IDs that matched and contributed to the decision.
         */
        @JsonProperty("matched_policies")
        List<String> matchedPolicies;

        /**
         * Additional metadata or context from policy evaluation.
         */
        @JsonProperty("metadata")
        Map<String, Object> metadata;
    }
}
