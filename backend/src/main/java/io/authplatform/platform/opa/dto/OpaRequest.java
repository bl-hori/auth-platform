package io.authplatform.platform.opa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Request DTO for OPA (Open Policy Agent) policy evaluation.
 *
 * <p>This represents the input sent to OPA's data API for policy evaluation.
 * The structure follows OPA's standard input format:
 * <pre>{@code
 * {
 *   "input": {
 *     "principal": { ... },
 *     "action": "read",
 *     "resource": { ... },
 *     "context": { ... }
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * OpaInput input = OpaInput.builder()
 *     .principal(Map.of("id", "user-123", "type", "user"))
 *     .action("read")
 *     .resource(Map.of("type", "document", "id", "doc-456"))
 *     .context(Map.of("time", "2024-01-15T10:00:00Z"))
 *     .build();
 *
 * OpaRequest request = OpaRequest.builder()
 *     .input(input)
 *     .build();
 * }</pre>
 *
 * @see OpaResponse
 */
@Value
@Builder
public class OpaRequest {

    /**
     * The input data for policy evaluation.
     */
    @JsonProperty("input")
    OpaInput input;

    /**
     * Input data structure for OPA policy evaluation.
     */
    @Value
    @Builder
    public static class OpaInput {
        /**
         * Principal information (user, service account, etc.)
         */
        @JsonProperty("principal")
        Map<String, Object> principal;

        /**
         * Action being performed (e.g., "read", "write", "delete").
         */
        @JsonProperty("action")
        String action;

        /**
         * Resource being accessed.
         */
        @JsonProperty("resource")
        Map<String, Object> resource;

        /**
         * Additional context for policy evaluation (e.g., time, IP address, attributes).
         */
        @JsonProperty("context")
        Map<String, Object> context;
    }
}
