package io.authplatform.platform.api.dto.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for testing a policy.
 *
 * <p>Allows testing a policy with sample input before publication.
 *
 * <p><strong>Example JSON:</strong>
 * <pre>{@code
 * {
 *   "input": {
 *     "user": {
 *       "id": "user-123",
 *       "role": "admin"
 *     },
 *     "resource": {
 *       "type": "document",
 *       "id": "doc-456"
 *     },
 *     "action": "read"
 *   }
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to test a policy with sample input")
public class PolicyTestRequest {

    /**
     * Test input data for policy evaluation.
     */
    @NotNull(message = "Input is required")
    @Schema(
            description = "Test input data for policy evaluation",
            example = "{\"user\": {\"id\": \"user-123\", \"role\": \"admin\"}, \"action\": \"read\", \"resource\": {\"type\": \"document\"}}",
            required = true
    )
    private Map<String, Object> input;
}
