package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Policy;
import io.authplatform.platform.domain.entity.Policy.PolicyType;
import io.authplatform.platform.domain.entity.PolicyVersion;
import io.authplatform.platform.domain.entity.PolicyVersion.ValidationStatus;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PolicyVersionRepository}.
 */
@DataJpaTest
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class PolicyVersionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private PolicyVersionRepository policyVersionRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    private Organization org;
    private User user;
    private Policy policy;

    @BeforeEach
    void setUp() {
        org = organizationRepository.save(Organization.builder()
                .name("test-org")
                .displayName("Test Org")
                .build());

        user = userRepository.save(User.builder()
                .organization(org)
                .email("user@test.com")
                .username("user")
                .displayName("User")
                .build());

        policy = policyRepository.save(Policy.builder()
                .organization(org)
                .name("test-policy")
                .policyType(PolicyType.REGO)
                .build());
    }

    @Test
    void shouldSaveAndFindPolicyVersion() {
        // Given
        String content = "package authz\nallow { true }";
        PolicyVersion version = PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content(content)
                .createdBy(user)
                .build();

        // When
        PolicyVersion saved = policyVersionRepository.save(version);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getChecksum()).isNotNull();
        assertThat(saved.getValidationStatus()).isEqualTo(ValidationStatus.PENDING);
        assertThat(saved.verifyChecksum()).isTrue();
    }

    @Test
    void shouldFindByPolicyId() {
        // Given
        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("v1 content")
                .build());
        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(2)
                .content("v2 content")
                .build());

        // When
        List<PolicyVersion> versions = policyVersionRepository.findByPolicyId(policy.getId());

        // Then
        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).getVersion()).isEqualTo(2); // DESC order
    }

    @Test
    void shouldFindLatestVersion() {
        // Given
        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("v1")
                .build());
        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(2)
                .content("v2")
                .build());

        // When
        Optional<PolicyVersion> latest = policyVersionRepository.findLatestByPolicyId(policy.getId());

        // Then
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(2);
    }

    @Test
    void shouldFindPublishedVersions() {
        // Given
        PolicyVersion v1 = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("v1")
                .build());
        v1.publish(user);
        policyVersionRepository.save(v1);

        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(2)
                .content("v2")
                .build());

        // When
        List<PolicyVersion> published = policyVersionRepository.findPublishedByPolicyId(policy.getId());
        List<PolicyVersion> unpublished = policyVersionRepository.findUnpublishedByPolicyId(policy.getId());

        // Then
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getVersion()).isEqualTo(1);
        assertThat(unpublished).hasSize(1);
        assertThat(unpublished.get(0).getVersion()).isEqualTo(2);
    }

    @Test
    void shouldFindByValidationStatus() {
        // Given
        PolicyVersion valid = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("valid content")
                .build());
        valid.markAsValid();
        policyVersionRepository.save(valid);

        PolicyVersion invalid = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(2)
                .content("invalid content")
                .build());
        invalid.markAsInvalid(Map.of("error", "syntax error"));
        policyVersionRepository.save(invalid);

        // When
        List<PolicyVersion> validVersions = policyVersionRepository.findValidByPolicyId(policy.getId());
        List<PolicyVersion> invalidVersions = policyVersionRepository.findInvalidByPolicyId(policy.getId());

        // Then
        assertThat(validVersions).hasSize(1);
        assertThat(validVersions.get(0).isValid()).isTrue();
        assertThat(invalidVersions).hasSize(1);
        assertThat(invalidVersions.get(0).isInvalid()).isTrue();
        assertThat(invalidVersions.get(0).hasValidationErrors()).isTrue();
    }

    @Test
    void shouldFindByChecksum() {
        // Given
        String content = "package authz\nallow { true }";
        PolicyVersion v1 = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content(content)
                .build());

        PolicyVersion v2 = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(2)
                .content(content) // Same content
                .build());

        // When
        List<PolicyVersion> sameContent = policyVersionRepository.findByPolicyIdAndChecksum(
                policy.getId(), v1.getChecksum());

        // Then
        assertThat(sameContent).hasSize(2);
        assertThat(sameContent.get(0).getChecksum()).isEqualTo(sameContent.get(1).getChecksum());
    }

    @Test
    void shouldCountVersions() {
        // Given
        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("v1")
                .build());
        PolicyVersion v2 = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(2)
                .content("v2")
                .build());
        v2.publish(user);
        v2.markAsValid();
        policyVersionRepository.save(v2);

        // When
        long totalCount = policyVersionRepository.countByPolicyId(policy.getId());
        long publishedCount = policyVersionRepository.countPublishedByPolicyId(policy.getId());
        long validCount = policyVersionRepository.countValidByPolicyId(policy.getId());

        // Then
        assertThat(totalCount).isEqualTo(2);
        assertThat(publishedCount).isEqualTo(1);
        assertThat(validCount).isEqualTo(1);
    }

    @Test
    void shouldFindMaxVersion() {
        // Given
        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("v1")
                .build());
        policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(3)
                .content("v3")
                .build());

        // When
        Optional<Integer> maxVersion = policyVersionRepository.findMaxVersionByPolicyId(policy.getId());

        // Then
        assertThat(maxVersion).isPresent();
        assertThat(maxVersion.get()).isEqualTo(3);
    }

    @Test
    void shouldVerifyChecksumCalculation() {
        // Given
        String content = "package authz\nallow { input.user.role == \"admin\" }";
        PolicyVersion version = PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content(content)
                .build();

        // When
        version.calculateChecksum();
        PolicyVersion saved = policyVersionRepository.save(version);

        // Then
        assertThat(saved.getChecksum()).isNotNull();
        assertThat(saved.getChecksum()).hasSize(64); // SHA-256 = 64 hex chars
        assertThat(saved.verifyChecksum()).isTrue();
    }

    @Test
    void shouldVerifyHelperMethods() {
        // Given
        PolicyVersion version = policyVersionRepository.save(PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("test content")
                .build());

        // Then
        assertThat(version.isPending()).isTrue();
        assertThat(version.isValid()).isFalse();
        assertThat(version.isPublished()).isFalse();
        assertThat(version.getContentSize()).isGreaterThan(0);

        // When
        version.markAsValid();
        version.publish(user);
        policyVersionRepository.save(version);

        // Then
        assertThat(version.isValid()).isTrue();
        assertThat(version.isPublished()).isTrue();
        assertThat(version.isPending()).isFalse();
    }
}
