package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Organization.OrganizationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OrganizationRepository}.
 *
 * <p>These tests verify repository operations using a test database.
 * Tests ensure proper CRUD operations, soft delete functionality, and custom queries.
 */
@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Organization Repository Tests")
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    @DisplayName("Should save and retrieve organization")
    void shouldSaveAndRetrieveOrganization() {
        // Given
        Organization organization = Organization.builder()
                .name("test-org")
                .displayName("Test Organization")
                .description("A test organization")
                .status(OrganizationStatus.ACTIVE)
                .settings(Map.of("key", "value"))
                .build();

        // When
        Organization saved = organizationRepository.save(organization);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("test-org");
        assertThat(saved.getDisplayName()).isEqualTo("Test Organization");
        assertThat(saved.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find organization by name excluding deleted")
    void shouldFindByNameExcludingDeleted() {
        // Given
        Organization active = createOrganization("active-org", OrganizationStatus.ACTIVE);
        Organization deleted = createOrganization("deleted-org", OrganizationStatus.DELETED);
        deleted.softDelete();
        organizationRepository.save(active);
        organizationRepository.save(deleted);

        // When
        Optional<Organization> found = organizationRepository.findByNameAndDeletedAtIsNull("active-org");
        Optional<Organization> notFound = organizationRepository.findByNameAndDeletedAtIsNull("deleted-org");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("active-org");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should find all active organizations")
    void shouldFindAllActiveOrganizations() {
        // Given
        Organization active1 = createOrganization("active-1", OrganizationStatus.ACTIVE);
        Organization active2 = createOrganization("active-2", OrganizationStatus.ACTIVE);
        Organization suspended = createOrganization("suspended", OrganizationStatus.SUSPENDED);
        Organization deleted = createOrganization("deleted", OrganizationStatus.DELETED);
        deleted.softDelete();

        organizationRepository.saveAll(List.of(active1, active2, suspended, deleted));

        // When
        List<Organization> activeOrgs = organizationRepository.findAllActive();

        // Then
        // Note: Database is seeded with "system" organization, so we expect 3 active orgs
        assertThat(activeOrgs).hasSizeGreaterThanOrEqualTo(2);
        assertThat(activeOrgs).extracting(Organization::getName)
                .contains("active-1", "active-2");
    }

    @Test
    @DisplayName("Should handle soft delete")
    void shouldHandleSoftDelete() {
        // Given
        Organization organization = createOrganization("to-delete", OrganizationStatus.ACTIVE);
        Organization saved = organizationRepository.save(organization);

        // When
        saved.softDelete();
        organizationRepository.save(saved);

        // Then
        Optional<Organization> found = organizationRepository.findByIdAndNotDeleted(saved.getId());
        assertThat(found).isEmpty();

        Optional<Organization> foundWithDeleted = organizationRepository.findById(saved.getId());
        assertThat(foundWithDeleted).isPresent();
        assertThat(foundWithDeleted.get().isDeleted()).isTrue();
        assertThat(foundWithDeleted.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should restore soft-deleted organization")
    void shouldRestoreSoftDeletedOrganization() {
        // Given
        Organization organization = createOrganization("to-restore", OrganizationStatus.ACTIVE);
        organization.softDelete();
        Organization saved = organizationRepository.save(organization);

        // When
        saved.restore();
        organizationRepository.save(saved);

        // Then
        Optional<Organization> found = organizationRepository.findByIdAndNotDeleted(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isDeleted()).isFalse();
        assertThat(found.get().getDeletedAt()).isNull();
        assertThat(found.get().getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should check organization name existence")
    void shouldCheckNameExistence() {
        // Given
        Organization organization = createOrganization("existing-org", OrganizationStatus.ACTIVE);
        organizationRepository.save(organization);

        // When/Then
        assertThat(organizationRepository.existsByNameAndDeletedAtIsNull("existing-org")).isTrue();
        assertThat(organizationRepository.existsByNameAndDeletedAtIsNull("non-existing-org")).isFalse();
    }

    @Test
    @DisplayName("Should count active organizations")
    void shouldCountActiveOrganizations() {
        // Given
        long initialActiveCount = organizationRepository.countActive();
        long initialTotalCount = organizationRepository.countNotDeleted();

        organizationRepository.saveAll(List.of(
                createOrganization("active-1", OrganizationStatus.ACTIVE),
                createOrganization("active-2", OrganizationStatus.ACTIVE),
                createOrganization("suspended", OrganizationStatus.SUSPENDED)
        ));

        // When
        long activeCount = organizationRepository.countActive();
        long totalCount = organizationRepository.countNotDeleted();

        // Then
        // Account for seeded data by comparing the difference
        assertThat(activeCount - initialActiveCount).isEqualTo(2);
        assertThat(totalCount - initialTotalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find organizations by name pattern")
    void shouldFindByNamePattern() {
        // Given
        organizationRepository.saveAll(List.of(
                createOrganization("acme-corp", OrganizationStatus.ACTIVE),
                createOrganization("acme-inc", OrganizationStatus.ACTIVE),
                createOrganization("example-org", OrganizationStatus.ACTIVE)
        ));

        // When
        List<Organization> acmeOrgs = organizationRepository
                .findByNameContainingIgnoreCaseAndNotDeleted("%acme%");

        // Then
        assertThat(acmeOrgs).hasSize(2);
        assertThat(acmeOrgs).extracting(Organization::getName)
                .containsExactlyInAnyOrder("acme-corp", "acme-inc");
    }

    @Test
    @DisplayName("Should verify is active method")
    void shouldVerifyIsActiveMethod() {
        // Given
        Organization active = createOrganization("active", OrganizationStatus.ACTIVE);
        Organization suspended = createOrganization("suspended", OrganizationStatus.SUSPENDED);
        Organization deleted = createOrganization("deleted", OrganizationStatus.ACTIVE);
        deleted.softDelete();

        // When/Then
        assertThat(active.isActive()).isTrue();
        assertThat(suspended.isActive()).isFalse();
        assertThat(deleted.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle JSONB settings field")
    void shouldHandleJsonbSettings() {
        // Given
        Map<String, Object> settings = Map.of(
                "branding", Map.of(
                        "logoUrl", "https://example.com/logo.png",
                        "primaryColor", "#3B82F6"
                ),
                "features", Map.of(
                        "mfaRequired", true,
                        "sessionTimeout", 3600
                )
        );

        Organization organization = Organization.builder()
                .name("settings-test")
                .displayName("Settings Test")
                .status(OrganizationStatus.ACTIVE)
                .settings(settings)
                .build();

        // When
        Organization saved = organizationRepository.save(organization);
        organizationRepository.flush();

        // Then
        Optional<Organization> found = organizationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSettings()).isNotNull();
        assertThat(found.get().getSettings()).containsKeys("branding", "features");
    }

    /**
     * Helper method to create a test organization.
     * Note: Each test method runs in its own transaction and rolls back,
     * so we don't need UUID suffixes for uniqueness.
     */
    private Organization createOrganization(String name, OrganizationStatus status) {
        return Organization.builder()
                .name(name)
                .displayName(name.replace("-", " ").toUpperCase())
                .description("Test organization: " + name)
                .status(status)
                .settings(Map.of())
                .build();
    }
}
