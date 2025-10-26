package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.policy.PolicyCreateRequest;
import io.authplatform.platform.api.dto.policy.PolicyResponse;
import io.authplatform.platform.api.dto.policy.PolicyTestRequest;
import io.authplatform.platform.api.dto.policy.PolicyTestResponse;
import io.authplatform.platform.api.dto.policy.PolicyUpdateRequest;
import io.authplatform.platform.domain.entity.Policy;
import io.authplatform.platform.domain.entity.PolicyVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for policy management operations.
 *
 * <p>Provides CRUD operations, versioning, validation, and publication
 * of authorization policies.
 *
 * <p><strong>Policy Lifecycle:</strong>
 * <ol>
 *   <li>Create policy in DRAFT status</li>
 *   <li>Validate Rego code</li>
 *   <li>Test policy with sample inputs</li>
 *   <li>Publish policy (transition to ACTIVE)</li>
 *   <li>Archive policy when no longer needed</li>
 * </ol>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * @Autowired
 * private PolicyService policyService;
 *
 * // Create a new policy
 * PolicyCreateRequest request = PolicyCreateRequest.builder()
 *     .name("document-access")
 *     .displayName("Document Access Policy")
 *     .regoCode("package authz\nallow { ... }")
 *     .organizationId(orgId)
 *     .build();
 *
 * PolicyResponse policy = policyService.createPolicy(request);
 *
 * // Test the policy
 * PolicyTestRequest testRequest = new PolicyTestRequest(...);
 * PolicyTestResponse result = policyService.testPolicy(policy.getId(), testRequest);
 *
 * // Publish if tests pass
 * if (result.isAllow()) {
 *     policyService.publishPolicy(policy.getId());
 * }
 * }</pre>
 *
 * @see Policy
 * @see PolicyVersion
 */
public interface PolicyService {

    /**
     * Create a new policy in DRAFT status.
     *
     * <p>The policy code is validated before creation. If validation fails,
     * a {@link PolicyValidationException} is thrown.
     *
     * @param request the policy creation request
     * @return the created policy
     * @throws PolicyValidationException if policy validation fails
     * @throws DuplicatePolicyNameException if a policy with the same name exists
     * @throws IllegalArgumentException if request is invalid
     */
    PolicyResponse createPolicy(PolicyCreateRequest request);

    /**
     * Get a policy by ID.
     *
     * @param policyId the policy ID
     * @return the policy
     * @throws PolicyNotFoundException if policy not found
     */
    PolicyResponse getPolicyById(UUID policyId);

    /**
     * Get a policy by name within an organization.
     *
     * @param organizationId the organization ID
     * @param name the policy name
     * @return the policy
     * @throws PolicyNotFoundException if policy not found
     */
    PolicyResponse getPolicyByName(UUID organizationId, String name);

    /**
     * List all policies in an organization with pagination.
     *
     * @param organizationId the organization ID
     * @param pageable pagination parameters
     * @return page of policies
     */
    Page<PolicyResponse> listPolicies(UUID organizationId, Pageable pageable);

    /**
     * List policies by status in an organization.
     *
     * @param organizationId the organization ID
     * @param status the policy status (DRAFT, ACTIVE, ARCHIVED)
     * @return list of policies with the specified status
     */
    List<PolicyResponse> listPoliciesByStatus(UUID organizationId, String status);

    /**
     * Update a policy, creating a new version.
     *
     * <p>The policy code is validated before creating the new version.
     * The policy's currentVersion is incremented.
     *
     * @param policyId the policy ID
     * @param request the update request
     * @return the updated policy
     * @throws PolicyNotFoundException if policy not found
     * @throws PolicyValidationException if validation fails
     */
    PolicyResponse updatePolicy(UUID policyId, PolicyUpdateRequest request);

    /**
     * Soft delete a policy.
     *
     * <p>The policy is not physically deleted but marked as deleted
     * and transitioned to ARCHIVED status.
     *
     * @param policyId the policy ID
     * @throws PolicyNotFoundException if policy not found
     */
    void deletePolicy(UUID policyId);

    /**
     * Publish a policy, making it active.
     *
     * <p>Transitions the policy from DRAFT to ACTIVE status.
     * The policy must be validated before publication.
     *
     * @param policyId the policy ID
     * @return the published policy
     * @throws PolicyNotFoundException if policy not found
     * @throws IllegalStateException if policy is not in DRAFT status
     * @throws PolicyValidationException if policy validation fails
     */
    PolicyResponse publishPolicy(UUID policyId);

    /**
     * Archive a policy.
     *
     * <p>Transitions the policy to ARCHIVED status.
     * Archived policies are not evaluated but kept for audit purposes.
     *
     * @param policyId the policy ID
     * @return the archived policy
     * @throws PolicyNotFoundException if policy not found
     */
    PolicyResponse archivePolicy(UUID policyId);

    /**
     * Test a policy with sample input.
     *
     * <p>Evaluates the policy against test input without affecting
     * the policy's status or creating a version.
     *
     * @param policyId the policy ID
     * @param request the test request with sample input
     * @return the test result
     * @throws PolicyNotFoundException if policy not found
     */
    PolicyTestResponse testPolicy(UUID policyId, PolicyTestRequest request);

    /**
     * Get all versions of a policy.
     *
     * @param policyId the policy ID
     * @return list of policy versions, ordered by version number descending
     * @throws PolicyNotFoundException if policy not found
     */
    List<PolicyVersion> getPolicyVersions(UUID policyId);

    /**
     * Get a specific version of a policy.
     *
     * @param policyId the policy ID
     * @param version the version number
     * @return the policy version
     * @throws PolicyNotFoundException if policy or version not found
     */
    PolicyVersion getPolicyVersion(UUID policyId, Integer version);

    /**
     * Exception thrown when a policy is not found.
     */
    class PolicyNotFoundException extends RuntimeException {
        public PolicyNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when policy validation fails.
     */
    class PolicyValidationException extends RuntimeException {
        private final Object validationErrors;

        public PolicyValidationException(String message, Object validationErrors) {
            super(message);
            this.validationErrors = validationErrors;
        }

        public Object getValidationErrors() {
            return validationErrors;
        }
    }

    /**
     * Exception thrown when a policy with the same name already exists.
     */
    class DuplicatePolicyNameException extends RuntimeException {
        public DuplicatePolicyNameException(String message) {
            super(message);
        }
    }
}
