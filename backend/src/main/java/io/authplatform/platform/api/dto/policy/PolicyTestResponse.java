package io.authplatform.platform.api.dto.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for policy test results.
 *
 * <p><strong>Example JSON:</strong>
 * <pre>{@code
 * {
 *   "allow": true,
 *   "reasons": ["User has admin role", "Resource is owned by user's department"],
 *   "evaluatedRules": ["admin_access", "department_ownership"],
 *   "executionTimeMs": 5
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Policy test result")
public class PolicyTestResponse {

    @Schema(description = "Whether the policy allows the action", example = "true")
    private boolean allow;

    @Schema(description = "Reasons for the decision", example = "[\"User has admin role\"]")
    private List<String> reasons;

    @Schema(description = "Rules that were evaluated", example = "[\"admin_access\", \"role_check\"]")
    private List<String> evaluatedRules;

    @Schema(description = "Policy evaluation execution time in milliseconds", example = "5")
    private Long executionTimeMs;

    @Schema(description = "Additional metadata from policy evaluation")
    private Map<String, Object> metadata;
}
