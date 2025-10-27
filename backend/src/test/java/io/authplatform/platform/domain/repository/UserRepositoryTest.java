package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Organization.OrganizationStatus;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.entity.User.UserStatus;
import io.authplatform.platform.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserRepository}.
 *
 * <p>These tests verify repository operations using a test database.
 * Tests ensure proper CRUD operations, soft delete functionality, multi-tenancy,
 * and custom queries.
 */
@DataJpaTest
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("User Repository Tests")
class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        // Create test organization for multi-tenancy tests
        testOrg = Organization.builder()
                .name("test-org-" + UUID.randomUUID().toString().substring(0, 8))
                .displayName("Test Organization")
                .description("Organization for user tests")
                .status(OrganizationStatus.ACTIVE)
                .settings(Map.of())
                .build();
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    @DisplayName("Should save and retrieve user")
    void shouldSaveAndRetrieveUser() {
        // Given
        User user = User.builder()
                .organization(testOrg)
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .status(UserStatus.ACTIVE)
                .attributes(Map.of("department", "Engineering"))
                .build();

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getOrganization().getId()).isEqualTo(testOrg.getId());
    }

    @Test
    @DisplayName("Should find user by email in organization excluding deleted")
    void shouldFindByEmailExcludingDeleted() {
        // Given
        User active = createUser("active@example.com", "activeuser", UserStatus.ACTIVE);
        User deleted = createUser("deleted@example.com", "deleteduser", UserStatus.DELETED);
        deleted.softDelete();
        userRepository.save(active);
        userRepository.save(deleted);

        // When
        Optional<User> found = userRepository.findByOrganizationIdAndEmailAndDeletedAtIsNull(
                testOrg.getId(), "active@example.com");
        Optional<User> notFound = userRepository.findByOrganizationIdAndEmailAndDeletedAtIsNull(
                testOrg.getId(), "deleted@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("active@example.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should find user by username in organization")
    void shouldFindByUsername() {
        // Given
        User user = createUser("user@example.com", "testuser", UserStatus.ACTIVE);
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByOrganizationIdAndUsernameAndDeletedAtIsNull(
                testOrg.getId(), "testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should find user by external ID")
    void shouldFindByExternalId() {
        // Given
        User user = createUser("external@example.com", "externaluser", UserStatus.ACTIVE);
        user.setExternalId("okta:12345");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByExternalIdAndDeletedAtIsNull("okta:12345");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("okta:12345");
    }

    @Test
    @DisplayName("Should find all active users in organization")
    void shouldFindAllActiveUsers() {
        // Given
        User active1 = createUser("active1@example.com", "active1", UserStatus.ACTIVE);
        User active2 = createUser("active2@example.com", "active2", UserStatus.ACTIVE);
        User inactive = createUser("inactive@example.com", "inactive", UserStatus.INACTIVE);
        User deleted = createUser("deleted@example.com", "deleted", UserStatus.DELETED);
        deleted.softDelete();

        userRepository.saveAll(List.of(active1, active2, inactive, deleted));

        // When
        List<User> activeUsers = userRepository.findAllActiveByOrganizationId(testOrg.getId());

        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("active1@example.com", "active2@example.com");
    }

    @Test
    @DisplayName("Should handle soft delete")
    void shouldHandleSoftDelete() {
        // Given
        User user = createUser("todelete@example.com", "todelete", UserStatus.ACTIVE);
        User saved = userRepository.save(user);

        // When
        saved.softDelete();
        userRepository.save(saved);

        // Then
        Optional<User> found = userRepository.findByIdAndNotDeleted(saved.getId());
        assertThat(found).isEmpty();

        Optional<User> foundWithDeleted = userRepository.findById(saved.getId());
        assertThat(foundWithDeleted).isPresent();
        assertThat(foundWithDeleted.get().isDeleted()).isTrue();
        assertThat(foundWithDeleted.get().getDeletedAt()).isNotNull();
        assertThat(foundWithDeleted.get().getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @DisplayName("Should restore soft-deleted user")
    void shouldRestoreSoftDeletedUser() {
        // Given
        User user = createUser("restore@example.com", "restore", UserStatus.ACTIVE);
        user.softDelete();
        User saved = userRepository.save(user);

        // When
        saved.restore();
        userRepository.save(saved);

        // Then
        Optional<User> found = userRepository.findByIdAndNotDeleted(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isDeleted()).isFalse();
        assertThat(found.get().getDeletedAt()).isNull();
        assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should check email existence in organization")
    void shouldCheckEmailExistence() {
        // Given
        User user = createUser("existing@example.com", "existing", UserStatus.ACTIVE);
        userRepository.save(user);

        // When/Then
        assertThat(userRepository.existsByOrganizationIdAndEmailAndDeletedAtIsNull(
                testOrg.getId(), "existing@example.com")).isTrue();
        assertThat(userRepository.existsByOrganizationIdAndEmailAndDeletedAtIsNull(
                testOrg.getId(), "nonexisting@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should check username existence in organization")
    void shouldCheckUsernameExistence() {
        // Given
        User user = createUser("user@example.com", "existinguser", UserStatus.ACTIVE);
        userRepository.save(user);

        // When/Then
        assertThat(userRepository.existsByOrganizationIdAndUsernameAndDeletedAtIsNull(
                testOrg.getId(), "existinguser")).isTrue();
        assertThat(userRepository.existsByOrganizationIdAndUsernameAndDeletedAtIsNull(
                testOrg.getId(), "nonexistinguser")).isFalse();
    }

    @Test
    @DisplayName("Should count active users in organization")
    void shouldCountActiveUsers() {
        // Given
        long initialActiveCount = userRepository.countActiveByOrganizationId(testOrg.getId());
        long initialTotalCount = userRepository.countNotDeletedByOrganizationId(testOrg.getId());

        userRepository.saveAll(List.of(
                createUser("active1@example.com", "active1", UserStatus.ACTIVE),
                createUser("active2@example.com", "active2", UserStatus.ACTIVE),
                createUser("inactive@example.com", "inactive", UserStatus.INACTIVE)
        ));

        // When
        long activeCount = userRepository.countActiveByOrganizationId(testOrg.getId());
        long totalCount = userRepository.countNotDeletedByOrganizationId(testOrg.getId());

        // Then
        assertThat(activeCount - initialActiveCount).isEqualTo(2);
        assertThat(totalCount - initialTotalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find users by email pattern")
    void shouldFindByEmailPattern() {
        // Given
        userRepository.saveAll(List.of(
                createUser("alice@acme.com", "alice", UserStatus.ACTIVE),
                createUser("bob@acme.com", "bob", UserStatus.ACTIVE),
                createUser("charlie@example.com", "charlie", UserStatus.ACTIVE)
        ));

        // When
        List<User> acmeUsers = userRepository.findByEmailContainingIgnoreCaseAndNotDeleted(
                testOrg.getId(), "%acme%");

        // Then
        assertThat(acmeUsers).hasSize(2);
        assertThat(acmeUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("alice@acme.com", "bob@acme.com");
    }

    @Test
    @DisplayName("Should find users by display name pattern")
    void shouldFindByDisplayNamePattern() {
        // Given
        User user1 = createUser("john@example.com", "john", UserStatus.ACTIVE);
        user1.setDisplayName("John Doe");
        User user2 = createUser("jane@example.com", "jane", UserStatus.ACTIVE);
        user2.setDisplayName("Jane Doe");
        User user3 = createUser("alice@example.com", "alice", UserStatus.ACTIVE);
        user3.setDisplayName("Alice Smith");

        userRepository.saveAll(List.of(user1, user2, user3));

        // When
        List<User> doeUsers = userRepository.findByDisplayNameContainingIgnoreCaseAndNotDeleted(
                testOrg.getId(), "%doe%");

        // Then
        assertThat(doeUsers).hasSize(2);
        assertThat(doeUsers).extracting(User::getDisplayName)
                .containsExactlyInAnyOrder("John Doe", "Jane Doe");
    }

    @Test
    @DisplayName("Should verify isActive method")
    void shouldVerifyIsActiveMethod() {
        // Given
        User active = createUser("active@example.com", "active", UserStatus.ACTIVE);
        User inactive = createUser("inactive@example.com", "inactive", UserStatus.INACTIVE);
        User deleted = createUser("deleted@example.com", "deleted", UserStatus.ACTIVE);
        deleted.softDelete();

        // When/Then
        assertThat(active.isActive()).isTrue();
        assertThat(inactive.isActive()).isFalse();
        assertThat(deleted.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle JSONB attributes field")
    void shouldHandleJsonbAttributes() {
        // Given
        Map<String, Object> attributes = Map.of(
                "department", "Engineering",
                "title", "Senior Developer",
                "location", "US-West",
                "clearanceLevel", "confidential"
        );

        User user = User.builder()
                .organization(testOrg)
                .email("attr@example.com")
                .username("attruser")
                .displayName("Attribute Test User")
                .status(UserStatus.ACTIVE)
                .attributes(attributes)
                .build();

        // When
        User saved = userRepository.save(user);
        userRepository.flush();

        // Then
        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAttributes()).isNotNull();
        assertThat(found.get().getAttributes()).containsKeys("department", "title", "location", "clearanceLevel");
        assertThat(found.get().getAttributes().get("department")).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Should enforce organization scoping")
    void shouldEnforceOrganizationScoping() {
        // Given
        Organization org2 = Organization.builder()
                .name("test-org2-" + UUID.randomUUID().toString().substring(0, 8))
                .displayName("Test Organization 2")
                .status(OrganizationStatus.ACTIVE)
                .build();
        org2 = organizationRepository.save(org2);

        User userOrg1 = createUser("user@org1.com", "user1", UserStatus.ACTIVE);
        userRepository.save(userOrg1);

        User userOrg2 = User.builder()
                .organization(org2)
                .email("user@org2.com")
                .username("user2")
                .displayName("User in Org 2")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(userOrg2);

        // When
        Optional<User> foundInOrg1 = userRepository.findByOrganizationIdAndEmailAndDeletedAtIsNull(
                testOrg.getId(), "user@org1.com");
        Optional<User> notFoundInOrg1 = userRepository.findByOrganizationIdAndEmailAndDeletedAtIsNull(
                testOrg.getId(), "user@org2.com");

        // Then
        assertThat(foundInOrg1).isPresent();
        assertThat(notFoundInOrg1).isEmpty();

        List<User> org1Users = userRepository.findAllActiveByOrganizationId(testOrg.getId());
        assertThat(org1Users).hasSize(1);
        assertThat(org1Users.get(0).getEmail()).isEqualTo("user@org1.com");
    }

    /**
     * Helper method to create a test user.
     */
    private User createUser(String email, String username, UserStatus status) {
        return User.builder()
                .organization(testOrg)
                .email(email)
                .username(username)
                .displayName(email.split("@")[0].toUpperCase())
                .status(status)
                .attributes(Map.of())
                .build();
    }
}
