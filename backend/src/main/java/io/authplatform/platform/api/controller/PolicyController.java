package io.authplatform.platform.api.controller;

import io.authplatform.platform.api.dto.policy.*;
import io.authplatform.platform.api.security.CurrentOrganizationId;
import io.authplatform.platform.audit.Audited;
import io.authplatform.platform.domain.entity.PolicyVersion;
import io.authplatform.platform.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for policy management.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Creating and managing policies</li>
 *   <li>Policy versioning</li>
 *   <li>Policy publication and testing</li>
 *   <li>Policy lifecycle management (draft → active → archived)</li>
 * </ul>
 *
 * <p>All endpoints require API key authentication via X-API-Key header.
 *
 * @see PolicyService
 */
@RestController
@RequestMapping("/v1/policies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Policy Management", description = "APIs for managing authorization policies")
public class PolicyController {

    private final PolicyService policyService;

    /**
     * Create a new policy.
     *
     * <p>POST /v1/policies
     *
     * @param request the policy creation request
     * @return the created policy
     */
    @PostMapping
    @Audited(
            action = "CREATE_POLICY",
            resourceType = "Policy",
            resourceIdExpression = "#result.body.id"
    )
    @Operation(
            summary = "Create a new policy",
            description = "Creates a new policy in DRAFT status. The Rego code is validated before creation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Policy created successfully",
                    content = @Content(schema = @Schema(implementation = PolicyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failed"),
            @ApiResponse(responseCode = "409", description = "Policy with same name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<PolicyResponse> createPolicy(
            @Valid @RequestBody PolicyCreateRequest request) {
        log.info("POST /v1/policies - Creating policy: {}", request.getName());

        PolicyResponse response = policyService.createPolicy(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all policies with pagination.
     *
     * <p>GET /v1/policies?organizationId={orgId}&page=0&size=20
     *
     * @param organizationId the organization ID
     * @param pageable pagination parameters
     * @return page of policies
     */
    @GetMapping
    @Operation(
            summary = "List policies",
            description = "Retrieves a paginated list of policies for an organization"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policies retrieved successfully"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<Page<PolicyResponse>> listPolicies(
            @Parameter(description = "Organization ID (automatically injected from authentication context)", hidden = true)
            @CurrentOrganizationId UUID organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("GET /v1/policies - organizationId={}, page={}, size={}",
                organizationId, pageable.getPageNumber(), pageable.getPageSize());

        Page<PolicyResponse> policies = policyService.listPolicies(organizationId, pageable);

        return ResponseEntity.ok(policies);
    }

    /**
     * Get a specific policy by ID.
     *
     * <p>GET /v1/policies/{id}
     *
     * @param id the policy ID
     * @return the policy
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get policy by ID",
            description = "Retrieves detailed information about a specific policy"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policy retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PolicyResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<PolicyResponse> getPolicyById(
            @Parameter(description = "Policy ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /v1/policies/{} - Fetching policy", id);

        PolicyResponse policy = policyService.getPolicyById(id);

        return ResponseEntity.ok(policy);
    }

    /**
     * Update a policy (creates new version).
     *
     * <p>PUT /v1/policies/{id}
     *
     * @param id the policy ID
     * @param request the update request
     * @return the updated policy
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update policy",
            description = "Updates a policy by creating a new version. The version number is automatically incremented."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policy updated successfully",
                    content = @Content(schema = @Schema(implementation = PolicyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failed"),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<PolicyResponse> updatePolicy(
            @Parameter(description = "Policy ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody PolicyUpdateRequest request) {
        log.info("PUT /v1/policies/{} - Updating policy", id);

        PolicyResponse response = policyService.updatePolicy(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete a policy.
     *
     * <p>DELETE /v1/policies/{id}
     *
     * @param id the policy ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete policy",
            description = "Soft deletes a policy (marks as deleted and archives it)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Policy deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<Void> deletePolicy(
            @Parameter(description = "Policy ID", required = true)
            @PathVariable UUID id) {
        log.info("DELETE /v1/policies/{} - Deleting policy", id);

        policyService.deletePolicy(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Publish a policy (make it active).
     *
     * <p>POST /v1/policies/{id}/publish
     *
     * @param id the policy ID
     * @return the published policy
     */
    @PostMapping("/{id}/publish")
    @Operation(
            summary = "Publish policy",
            description = "Publishes a policy, transitioning it from DRAFT to ACTIVE status"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policy published successfully",
                    content = @Content(schema = @Schema(implementation = PolicyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Policy is not in DRAFT status or validation failed"),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<PolicyResponse> publishPolicy(
            @Parameter(description = "Policy ID", required = true)
            @PathVariable UUID id) {
        log.info("POST /v1/policies/{}/publish - Publishing policy", id);

        PolicyResponse response = policyService.publishPolicy(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Test a policy with sample input.
     *
     * <p>POST /v1/policies/{id}/test
     *
     * @param id the policy ID
     * @param request the test request with sample input
     * @return the test result
     */
    @PostMapping("/{id}/test")
    @Operation(
            summary = "Test policy",
            description = "Tests a policy with sample input to see the authorization decision"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policy tested successfully",
                    content = @Content(schema = @Schema(implementation = PolicyTestResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid test input"),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<PolicyTestResponse> testPolicy(
            @Parameter(description = "Policy ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody PolicyTestRequest request) {
        log.info("POST /v1/policies/{}/test - Testing policy", id);

        PolicyTestResponse response = policyService.testPolicy(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all versions of a policy.
     *
     * <p>GET /v1/policies/{id}/versions
     *
     * @param id the policy ID
     * @return list of policy versions
     */
    @GetMapping("/{id}/versions")
    @Operation(
            summary = "Get policy versions",
            description = "Retrieves all versions of a policy, ordered by version number descending"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policy versions retrieved successfully"
            ),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<List<PolicyVersion>> getPolicyVersions(
            @Parameter(description = "Policy ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /v1/policies/{}/versions - Fetching policy versions", id);

        List<PolicyVersion> versions = policyService.getPolicyVersions(id);

        return ResponseEntity.ok(versions);
    }

    /**
     * Get a specific version of a policy.
     *
     * <p>GET /v1/policies/{id}/versions/{version}
     *
     * @param id the policy ID
     * @param version the version number
     * @return the policy version
     */
    @GetMapping("/{id}/versions/{version}")
    @Operation(
            summary = "Get specific policy version",
            description = "Retrieves a specific version of a policy"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policy version retrieved successfully"
            ),
            @ApiResponse(responseCode = "404", description = "Policy or version not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<PolicyVersion> getPolicyVersion(
            @Parameter(description = "Policy ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Version number", required = true)
            @PathVariable Integer version) {
        log.info("GET /v1/policies/{}/versions/{} - Fetching policy version", id, version);

        PolicyVersion policyVersion = policyService.getPolicyVersion(id, version);

        return ResponseEntity.ok(policyVersion);
    }

    /**
     * Exception handler for PolicyNotFoundException.
     */
    @ExceptionHandler(PolicyService.PolicyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePolicyNotFound(
            PolicyService.PolicyNotFoundException ex) {
        log.warn("Policy not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "Policy not found",
                        "message", ex.getMessage()
                ));
    }

    /**
     * Exception handler for PolicyValidationException.
     */
    @ExceptionHandler(PolicyService.PolicyValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePolicyValidation(
            PolicyService.PolicyValidationException ex) {
        log.warn("Policy validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Policy validation failed",
                        "message", ex.getMessage(),
                        "validation_errors", ex.getValidationErrors()
                ));
    }

    /**
     * Exception handler for DuplicatePolicyNameException.
     */
    @ExceptionHandler(PolicyService.DuplicatePolicyNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePolicyName(
            PolicyService.DuplicatePolicyNameException ex) {
        log.warn("Duplicate policy name: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "Duplicate policy name",
                        "message", ex.getMessage()
                ));
    }

    /**
     * Exception handler for IllegalStateException.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Invalid operation",
                        "message", ex.getMessage()
                ));
    }

    /**
     * Exception handler for IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Invalid request",
                        "message", ex.getMessage()
                ));
    }
}
