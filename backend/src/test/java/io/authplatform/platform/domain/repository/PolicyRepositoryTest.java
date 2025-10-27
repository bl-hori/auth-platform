package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Policy;
import io.authplatform.platform.domain.entity.Policy.PolicyStatus;
import io.authplatform.platform.domain.entity.Policy.PolicyType;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PolicyRepository}.
 *
 * <p>Tests policy management operations including:
 * <ul>
 *   <li>Basic CRUD operations</li>
 *   <li>Status-based queries (draft, active, archived)</li>
 *   <li>Type-based queries (Rego, Cedar)</li>
 *   <li>Soft delete support</li>
 *   <li>Organization scoping</li>
 *   <li>Lifecycle management</li>
 * </ul>
 */
@DataJpaTest
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class PolicyRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    private Organization org1;
    private Organization org2;
    private User user1;

    @BeforeEach
    void setUp() {
        // Create test organizations
        org1 = organizationRepository.save(Organization.builder()
                .name("test-org-1")
                .displayName("Test Org 1")
                .build());

        org2 = organizationRepository.save(Organization.builder()
                .name("test-org-2")
                .displayName("Test Org 2")
                .build());

        // Create test user
        user1 = userRepository.save(User.builder()
                .organization(org1)
                .email("user1@test.com")
                .username("user1")
                .displayName("User One")
                .build());
    }

    @Test
    void shouldSaveAndFindPolicy() {
        // Given
        Policy policy = Policy.builder()
                .organization(org1)
                .name("document-access-policy")
                .displayName("Document Access Policy")
                .description("Controls access to documents")
                .policyType(PolicyType.REGO)
                .status(PolicyStatus.DRAFT)
                .createdBy(user1)
                .build();

        // When
        Policy saved = policyRepository.save(policy);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("document-access-policy");
        assertThat(saved.getPolicyType()).isEqualTo(PolicyType.REGO);
        assertThat(saved.getStatus()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(saved.getCurrentVersion()).isEqualTo(1);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByOrganizationIdAndName() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("test-policy")
                .policyType(PolicyType.REGO)
                .build());

        // When
        Optional<Policy> found = policyRepository.findByOrganizationIdAndName(org1.getId(), "test-policy");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-policy");
    }

    @Test
    void shouldFindAllByOrganizationId() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("policy-1")
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("policy-2")
                .policyType(PolicyType.CEDAR)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org2)
                .name("policy-3")
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> org1Policies = policyRepository.findAllByOrganizationId(org1.getId());

        // Then
        assertThat(org1Policies).hasSize(2);
        assertThat(org1Policies).extracting(Policy::getName)
                .containsExactlyInAnyOrder("policy-1", "policy-2");
    }

    @Test
    void shouldFindByOrganizationIdAndStatus() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("draft-policy")
                .status(PolicyStatus.DRAFT)
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("active-policy")
                .status(PolicyStatus.ACTIVE)
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> draftPolicies = policyRepository.findByOrganizationIdAndStatus(
                org1.getId(), PolicyStatus.DRAFT);

        // Then
        assertThat(draftPolicies).hasSize(1);
        assertThat(draftPolicies.get(0).getName()).isEqualTo("draft-policy");
    }

    @Test
    void shouldFindActiveByOrganizationId() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("draft-policy")
                .status(PolicyStatus.DRAFT)
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("active-policy")
                .status(PolicyStatus.ACTIVE)
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> activePolicies = policyRepository.findActiveByOrganizationId(org1.getId());

        // Then
        assertThat(activePolicies).hasSize(1);
        assertThat(activePolicies.get(0).getName()).isEqualTo("active-policy");
    }

    @Test
    void shouldFindDraftByOrganizationId() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("draft-1")
                .status(PolicyStatus.DRAFT)
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("draft-2")
                .status(PolicyStatus.DRAFT)
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("active-1")
                .status(PolicyStatus.ACTIVE)
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> drafts = policyRepository.findDraftByOrganizationId(org1.getId());

        // Then
        assertThat(drafts).hasSize(2);
        assertThat(drafts).extracting(Policy::getName)
                .containsExactlyInAnyOrder("draft-1", "draft-2");
    }

    @Test
    void shouldFindArchivedByOrganizationId() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("archived-1")
                .status(PolicyStatus.ARCHIVED)
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("active-1")
                .status(PolicyStatus.ACTIVE)
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> archived = policyRepository.findArchivedByOrganizationId(org1.getId());

        // Then
        assertThat(archived).hasSize(1);
        assertThat(archived.get(0).getName()).isEqualTo("archived-1");
    }

    @Test
    void shouldFindByOrganizationIdAndType() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("rego-policy")
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("cedar-policy")
                .policyType(PolicyType.CEDAR)
                .build());

        // When
        List<Policy> regoPolicies = policyRepository.findByOrganizationIdAndType(
                org1.getId(), PolicyType.REGO);

        // Then
        assertThat(regoPolicies).hasSize(1);
        assertThat(regoPolicies.get(0).getPolicyType()).isEqualTo(PolicyType.REGO);
    }

    @Test
    void shouldFindRegoByOrganizationId() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("rego-1")
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("rego-2")
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("cedar-1")
                .policyType(PolicyType.CEDAR)
                .build());

        // When
        List<Policy> regoPolicies = policyRepository.findRegoByOrganizationId(org1.getId());

        // Then
        assertThat(regoPolicies).hasSize(2);
        assertThat(regoPolicies).allMatch(Policy::isRego);
    }

    @Test
    void shouldFindCedarByOrganizationId() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("cedar-1")
                .policyType(PolicyType.CEDAR)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("rego-1")
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> cedarPolicies = policyRepository.findCedarByOrganizationId(org1.getId());

        // Then
        assertThat(cedarPolicies).hasSize(1);
        assertThat(cedarPolicies).allMatch(Policy::isCedar);
    }

    @Test
    void shouldHandleSoftDelete() {
        // Given
        Policy policy = policyRepository.save(Policy.builder()
                .organization(org1)
                .name("test-policy")
                .policyType(PolicyType.REGO)
                .build());

        // When
        policy.softDelete();
        policyRepository.save(policy);

        // Then
        List<Policy> activePolicies = policyRepository.findAllByOrganizationId(org1.getId());
        List<Policy> deletedPolicies = policyRepository.findDeletedByOrganizationId(org1.getId());

        assertThat(activePolicies).isEmpty();
        assertThat(deletedPolicies).hasSize(1);
        assertThat(deletedPolicies.get(0).isDeleted()).isTrue();
        assertThat(deletedPolicies.get(0).isArchived()).isTrue();
    }

    @Test
    void shouldFindByCreatedById() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("user-policy-1")
                .createdBy(user1)
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("user-policy-2")
                .createdBy(user1)
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> userPolicies = policyRepository.findByCreatedById(user1.getId());

        // Then
        assertThat(userPolicies).hasSize(2);
        assertThat(userPolicies).allMatch(p -> p.getCreatedBy().equals(user1));
    }

    @Test
    void shouldFindByNamePattern() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("document-access-policy")
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("document-write-policy")
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("user-access-policy")
                .policyType(PolicyType.REGO)
                .build());

        // When
        List<Policy> documentPolicies = policyRepository.findByNameContainingIgnoreCase(
                org1.getId(), "document%");

        // Then
        assertThat(documentPolicies).hasSize(2);
        assertThat(documentPolicies).allMatch(p -> p.getName().startsWith("document"));
    }

    @Test
    void shouldCheckExistence() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("test-policy")
                .policyType(PolicyType.REGO)
                .build());

        // When
        boolean exists = policyRepository.existsByOrganizationIdAndName(org1.getId(), "test-policy");
        boolean notExists = policyRepository.existsByOrganizationIdAndName(org1.getId(), "other-policy");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCountPolicies() {
        // Given
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("policy-1")
                .status(PolicyStatus.ACTIVE)
                .policyType(PolicyType.REGO)
                .build());
        policyRepository.save(Policy.builder()
                .organization(org1)
                .name("policy-2")
                .status(PolicyStatus.DRAFT)
                .policyType(PolicyType.REGO)
                .build());

        // When
        long totalCount = policyRepository.countByOrganizationId(org1.getId());
        long activeCount = policyRepository.countActiveByOrganizationId(org1.getId());
        long draftCount = policyRepository.countDraftByOrganizationId(org1.getId());

        // Then
        assertThat(totalCount).isEqualTo(2);
        assertThat(activeCount).isEqualTo(1);
        assertThat(draftCount).isEqualTo(1);
    }

    @Test
    void shouldVerifyHelperMethods() {
        // Given
        Policy policy = policyRepository.save(Policy.builder()
                .organization(org1)
                .name("test-policy")
                .status(PolicyStatus.DRAFT)
                .policyType(PolicyType.REGO)
                .build());

        // Then
        assertThat(policy.isDraft()).isTrue();
        assertThat(policy.isActive()).isFalse();
        assertThat(policy.isArchived()).isFalse();
        assertThat(policy.isRego()).isTrue();
        assertThat(policy.isCedar()).isFalse();

        // When
        policy.activate();
        policyRepository.save(policy);

        // Then
        assertThat(policy.isActive()).isTrue();
        assertThat(policy.isDraft()).isFalse();
    }
}
