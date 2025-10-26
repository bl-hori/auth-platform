package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch authorization response DTO containing results for multiple authorization decisions.
 *
 * <p>This response contains a list of individual authorization responses corresponding
 * to each request in the batch. The responses are returned in the same order as the
 * requests, allowing clients to correlate results with their original requests.
 *
 * <p><strong>Response Order Guarantee:</strong>
 * The order of responses always matches the order of requests. If request[0] asked
 * about "read document-123", then response[0] will contain the decision for that request.
 *
 * <p><strong>Error Handling:</strong>
 * Individual request failures are included in the response list with decision=ERROR.
 * The HTTP status code will still be 200 OK for the batch endpoint, as the batch
 * itself was processed successfully.
 *
 * <p><strong>Performance Metrics:</strong>
 * The {@code totalEvaluationTimeMs} represents the wall-clock time for processing
 * the entire batch, including parallelization overhead. This is typically less than
 * the sum of individual evaluation times due to parallel processing.
 *
 * @see AuthorizationResponse
 * @see BatchAuthorizationRequest
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Batch authorization response containing results for multiple requests",
        example = """
                {
                  "responses": [
                    {
                      "decision": "ALLOW",
                      "reason": "User has required permissions",
                      "evaluationTimeMs": 3
                    },
                    {
                      "decision": "DENY",
                      "reason": "User lacks write permission",
                      "evaluationTimeMs": 2
                    }
                  ],
                  "totalEvaluationTimeMs": 12
                }
                """
)
public class BatchAuthorizationResponse {

    /**
     * List of individual authorization responses.
     *
     * <p>Each response corresponds to a request in the original batch,
     * in the same order. The list will always have the same size as the
     * request list.
     */
    @Schema(
            description = "List of authorization decisions corresponding to each request",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<AuthorizationResponse> responses;

    /**
     * Total wall-clock time taken to evaluate the entire batch (in milliseconds).
     *
     * <p>This includes:
     * <ul>
     *   <li>Parallel processing coordination overhead</li>
     *   <li>All cache lookups (L1 and L2)</li>
     *   <li>All policy evaluations</li>
     * </ul>
     *
     * <p>Due to parallel processing, this value is typically less than the sum
     * of individual {@code evaluationTimeMs} values in the responses.
     *
     * <p><strong>Example:</strong>
     * If 10 requests each take 5ms but are processed in parallel on 10 threads,
     * the total evaluation time might be ~7ms (5ms for processing + 2ms overhead).
     */
    @Schema(
            description = "Total time to process the entire batch in milliseconds",
            example = "12",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long totalEvaluationTimeMs;
}
