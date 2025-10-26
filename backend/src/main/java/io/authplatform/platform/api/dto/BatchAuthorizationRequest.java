package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch authorization request DTO for processing multiple authorization decisions in a single API call.
 *
 * <p>This allows clients to reduce network overhead by bundling multiple authorization
 * checks into a single HTTP request. Each request in the batch is processed independently,
 * and responses are returned in the same order.
 *
 * <p><strong>Performance Characteristics:</strong>
 * <ul>
 *   <li>Requests are processed in parallel using virtual threads (Project Loom)</li>
 *   <li>Cache hits are still honored for each individual request</li>
 *   <li>Batch size is limited to prevent resource exhaustion</li>
 *   <li>Recommended batch size: 10-100 requests per batch</li>
 * </ul>
 *
 * <p><strong>Error Handling:</strong>
 * Individual request failures do not fail the entire batch. Each response will
 * contain either a decision (ALLOW/DENY) or an ERROR status with details.
 *
 * @see AuthorizationRequest
 * @see BatchAuthorizationResponse
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Batch authorization request containing multiple authorization checks",
        example = """
                {
                  "requests": [
                    {
                      "organizationId": "550e8400-e29b-41d4-a716-446655440000",
                      "principal": {"id": "user-123", "type": "user"},
                      "action": "read",
                      "resource": {"type": "document", "id": "doc-456"}
                    },
                    {
                      "organizationId": "550e8400-e29b-41d4-a716-446655440000",
                      "principal": {"id": "user-123", "type": "user"},
                      "action": "write",
                      "resource": {"type": "document", "id": "doc-789"}
                    }
                  ]
                }
                """
)
public class BatchAuthorizationRequest {

    /**
     * List of individual authorization requests to process.
     *
     * <p><strong>Constraints:</strong>
     * <ul>
     *   <li>Must contain at least 1 request</li>
     *   <li>Must not exceed 100 requests per batch</li>
     *   <li>Each request must be valid according to {@link AuthorizationRequest} validation rules</li>
     * </ul>
     */
    @NotEmpty(message = "Requests list must not be empty")
    @Size(min = 1, max = 100, message = "Batch size must be between 1 and 100 requests")
    @Valid
    @Schema(
            description = "List of authorization requests to evaluate",
            minLength = 1,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<AuthorizationRequest> requests;
}
