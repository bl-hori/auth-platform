package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.policy.*;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Policy;
import io.authplatform.platform.domain.entity.PolicyVersion;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.PolicyRepository;
import io.authplatform.platform.domain.repository.PolicyVersionRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.opa.client.OpaClient;
import io.authplatform.platform.opa.dto.OpaRequest;
import io.authplatform.platform.opa.dto.OpaResponse;
import io.authplatform.platform.service.PolicyService;
import io.authplatform.platform.service.PolicyValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PolicyService for managing authorization policies.
 *
 * <p>This service handles the full lifecycle of policies including:
 * <ul>
 *   <li>Creation with validation</li>
 *   <li>Versioning</li>
 *   <li>Publication and activation</li>
 *   <li>Testing with sample input</li>
 *   <li>Distribution to OPA instances</li>
 * </ul>
 *
 * @see PolicyService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PolicyValidationService validationService;
    @Nullable
    private final OpaClient opaClient; // Optional - may not be available in test environments

    @Override
    @Transactional
    public PolicyResponse createPolicy(PolicyCreateRequest request) {
        log.info("Creating policy: name={}, organizationId={}", request.getName(), request.getOrganizationId());

        // Validate organization exists
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + request.getOrganizationId()));

        // Check for duplicate policy name
        if (policyRepository.existsByOrganizationIdAndName(request.getOrganizationId(), request.getName())) {
            throw new DuplicatePolicyNameException(
                    "Policy with name '" + request.getName() + "' already exists in organization");
        }

        // Validate Rego code
        PolicyValidationService.PolicyValidationResult validationResult =
                validationService.validateRegoPolicy(request.getRegoCode());
        if (!validationResult.valid()) {
            log.warn("Policy validation failed: {}", validationResult.errors());
            throw new PolicyValidationException(
                    "Policy validation failed", validationResult.errors());
        }

        // Create policy entity
        Policy policy = Policy.builder()
                .organization(organization)
                .name(request.getName())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .policyType(Policy.PolicyType.REGO)
                .status(Policy.PolicyStatus.DRAFT)
                .currentVersion(1)
                .build();

        policy = policyRepository.save(policy);
        log.info("Policy created: id={}, name={}", policy.getId(), policy.getName());

        // Create initial version
        PolicyVersion version = PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content(request.getRegoCode())
                .validationStatus(PolicyVersion.ValidationStatus.VALID)
                .build();

        version.calculateChecksum();
        policyVersionRepository.save(version);
        log.info("Policy version created: policyId={}, version={}", policy.getId(), version.getVersion());

        return toPolicyResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(UUID policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        if (policy.isDeleted()) {
            throw new PolicyNotFoundException("Policy not found (deleted): " + policyId);
        }

        return toPolicyResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse getPolicyByName(UUID organizationId, String name) {
        Policy policy = policyRepository.findByOrganizationIdAndName(organizationId, name)
                .orElseThrow(() -> new PolicyNotFoundException(
                        "Policy not found: " + name + " in organization " + organizationId));

        if (policy.isDeleted()) {
            throw new PolicyNotFoundException("Policy not found (deleted): " + name);
        }

        return toPolicyResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyResponse> listPolicies(UUID organizationId, Pageable pageable) {
        List<Policy> policies = policyRepository.findAllByOrganizationId(organizationId);

        List<PolicyResponse> responses = policies.stream()
                .map(this::toPolicyResponse)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responses.size());

        return new PageImpl<>(
                responses.subList(start, end),
                pageable,
                responses.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponse> listPoliciesByStatus(UUID organizationId, String status) {
        Policy.PolicyStatus policyStatus;
        try {
            policyStatus = Policy.PolicyStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid policy status: " + status);
        }

        List<Policy> policies = policyRepository.findByOrganizationIdAndStatus(organizationId, policyStatus);

        return policies.stream()
                .map(this::toPolicyResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PolicyResponse updatePolicy(UUID policyId, PolicyUpdateRequest request) {
        log.info("Updating policy: id={}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        if (policy.isDeleted()) {
            throw new PolicyNotFoundException("Cannot update deleted policy: " + policyId);
        }

        // Validate new Rego code
        PolicyValidationService.PolicyValidationResult validationResult =
                validationService.validateRegoPolicy(request.getRegoCode());
        if (!validationResult.valid()) {
            log.warn("Policy update validation failed: {}", validationResult.errors());
            throw new PolicyValidationException(
                    "Policy validation failed", validationResult.errors());
        }

        // Update policy metadata
        if (request.getDisplayName() != null) {
            policy.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            policy.setDescription(request.getDescription());
        }

        // Increment version
        policy.incrementVersion();
        policy = policyRepository.save(policy);

        // Create new version
        PolicyVersion newVersion = PolicyVersion.builder()
                .policy(policy)
                .version(policy.getCurrentVersion())
                .content(request.getRegoCode())
                .validationStatus(PolicyVersion.ValidationStatus.VALID)
                .build();

        newVersion.calculateChecksum();
        policyVersionRepository.save(newVersion);

        log.info("Policy updated: id={}, newVersion={}", policy.getId(), policy.getCurrentVersion());

        return toPolicyResponse(policy);
    }

    @Override
    @Transactional
    public void deletePolicy(UUID policyId) {
        log.info("Deleting policy: id={}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        policy.softDelete();
        policyRepository.save(policy);

        log.info("Policy soft deleted: id={}", policyId);
    }

    @Override
    @Transactional
    public PolicyResponse publishPolicy(UUID policyId) {
        log.info("Publishing policy: id={}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        if (!policy.isDraft()) {
            throw new IllegalStateException(
                    "Only DRAFT policies can be published. Current status: " + policy.getStatus());
        }

        // Get latest version and validate
        PolicyVersion latestVersion = policyVersionRepository
                .findLatestByPolicyId(policyId)
                .orElseThrow(() -> new IllegalStateException("No version found for policy: " + policyId));

        if (!latestVersion.isValid()) {
            throw new PolicyValidationException(
                    "Cannot publish invalid policy", latestVersion.getValidationErrors());
        }

        // Mark version as published
        // Note: publishedBy would be set from SecurityContext in real implementation
        latestVersion.publish(null);
        policyVersionRepository.save(latestVersion);

        // Activate policy
        policy.activate();
        policy = policyRepository.save(policy);

        log.info("Policy published: id={}, version={}", policyId, latestVersion.getVersion());

        // TODO: Task 4.12 - Distribute policy to OPA instances
        // This would trigger policy bundle generation and distribution

        return toPolicyResponse(policy);
    }

    @Override
    @Transactional
    public PolicyResponse archivePolicy(UUID policyId) {
        log.info("Archiving policy: id={}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        policy.archive();
        policy = policyRepository.save(policy);

        log.info("Policy archived: id={}", policyId);

        return toPolicyResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyTestResponse testPolicy(UUID policyId, PolicyTestRequest request) {
        log.info("Testing policy: id={}", policyId);

        if (opaClient == null) {
            throw new IllegalStateException("OPA client not available - policy testing is disabled");
        }

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        PolicyVersion latestVersion = policyVersionRepository
                .findLatestByPolicyId(policyId)
                .orElseThrow(() -> new IllegalStateException("No version found for policy: " + policyId));

        // Build OPA request
        OpaRequest opaRequest = OpaRequest.builder()
                .input(OpaRequest.OpaInput.builder()
                        .principal(request.getInput())
                        .action(String.valueOf(request.getInput().get("action")))
                        .resource(request.getInput())
                        .context(Map.of())
                        .build())
                .build();

        // Evaluate with OPA
        long startTime = System.currentTimeMillis();
        OpaResponse opaResponse = opaClient.evaluatePolicy(opaRequest);
        long executionTime = System.currentTimeMillis() - startTime;

        log.info("Policy test complete: allow={}, executionTime={}ms",
                opaResponse.getResult().isAllow(), executionTime);

        return PolicyTestResponse.builder()
                .allow(opaResponse.getResult().isAllow())
                .reasons(opaResponse.getResult().getReasons())
                .evaluatedRules(opaResponse.getResult().getMatchedPolicies())
                .executionTimeMs(executionTime)
                .metadata(opaResponse.getResult().getMetadata())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyVersion> getPolicyVersions(UUID policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new PolicyNotFoundException("Policy not found: " + policyId);
        }

        return policyVersionRepository.findByPolicyId(policyId);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyVersion getPolicyVersion(UUID policyId, Integer version) {
        if (!policyRepository.existsById(policyId)) {
            throw new PolicyNotFoundException("Policy not found: " + policyId);
        }

        return policyVersionRepository.findByPolicyIdAndVersion(policyId, version)
                .orElseThrow(() -> new PolicyNotFoundException(
                        "Policy version not found: policyId=" + policyId + ", version=" + version));
    }

    /**
     * Convert Policy entity to PolicyResponse DTO.
     */
    private PolicyResponse toPolicyResponse(Policy policy) {
        // Get latest version for publishedAt/publishedBy
        PolicyVersion latestPublished = policyVersionRepository
                .findPublishedByPolicyId(policy.getId())
                .stream()
                .findFirst()
                .orElse(null);

        return PolicyResponse.builder()
                .id(policy.getId())
                .organizationId(policy.getOrganization().getId())
                .name(policy.getName())
                .displayName(policy.getDisplayName())
                .description(policy.getDescription())
                .status(policy.getStatus().getValue())
                .policyType(policy.getPolicyType().getValue())
                .currentVersion(policy.getCurrentVersion())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .publishedAt(latestPublished != null ? latestPublished.getPublishedAt() : null)
                .publishedBy(latestPublished != null && latestPublished.getPublishedBy() != null
                        ? latestPublished.getPublishedBy().getId().toString()
                        : null)
                .createdBy(policy.getCreatedBy() != null ? policy.getCreatedBy().getId().toString() : null)
                .build();
    }
}
