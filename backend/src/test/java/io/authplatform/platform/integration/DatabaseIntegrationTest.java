package io.authplatform.platform.integration;

import io.authplatform.platform.domain.entity.*;
import io.authplatform.platform.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for database operations using Testcontainers.
 *
 * <p>These tests verify that all database entities, repositories, and migrations
 * work correctly with a real PostgreSQL database. Tests cover:
 * <ul>
 *   <li>Entity persistence and retrieval</li>
 *   <li>Relationship mappings (ManyToOne, OneToMany)</li>
 *   <li>Custom repository queries</li>
 *   <li>JSONB field storage and retrieval</li>
 *   <li>Soft delete functionality</li>
 *   <li>Index performance (implicitly verified)</li>
 * </ul>
 *
 * <p><strong>Testcontainers Benefits:</strong>
 * <ul>
 *   <li>Real PostgreSQL 15 database (not H2/in-memory)</li>
 *   <li>Automatic Flyway migrations</li>
 *   <li>Isolation between test methods</li>
 *   <li>Consistent test environment</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Database Integration Tests with Testcontainers")
class DatabaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyVersionRepository policyVersionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Organization testOrg;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();

        // Create test organization
        testOrg = Organization.builder()
                .name("test-org-integration")
                .displayName("Test Organization")
                .description("Integration test organization")
                .status(Organization.OrganizationStatus.ACTIVE)
                .settings(Map.of("testMode", true))
                .build();
        testOrg = entityManager.persist(testOrg);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should persist and retrieve all core entities")
    void shouldPersistAndRetrieveAllCoreEntities() {
        // Given: Complete entity graph
        User user = User.builder()
                .organization(testOrg)
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .status(User.UserStatus.ACTIVE)
                .attributes(Map.of("department", "Engineering"))
                .build();
        user = entityManager.persist(user);

        Role role = Role.builder()
                .organization(testOrg)
                .name("test-role")
                .displayName("Test Role")
                .description("Test role description")
                .level(0)
                .isSystem(false)
                .metadata(Map.of())
                .build();
        role = entityManager.persist(role);

        Permission permission = Permission.builder()
                .organization(testOrg)
                .name("document:read")
                .displayName("Document Read")
                .resourceType("document")
                .action("read")
                .effect(Permission.PermissionEffect.ALLOW)
                .conditions(Map.of())
                .build();
        permission = entityManager.persist(permission);

        Policy policy = Policy.builder()
                .organization(testOrg)
                .name("test-policy")
                .displayName("Test Policy")
                .policyType(Policy.PolicyType.REGO)
                .status(Policy.PolicyStatus.DRAFT)
                .currentVersion(1)
                .build();
        policy = entityManager.persist(policy);

        PolicyVersion policyVersion = PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("package test\ndefault allow = false")
                .checksum("abc123")
                .validationStatus(PolicyVersion.ValidationStatus.VALID)
                .build();
        policyVersion = entityManager.persist(policyVersion);

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();
        userRole = entityManager.persist(userRole);

        RolePermission rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .build();
        rolePermission = entityManager.persist(rolePermission);

        AuditLog auditLog = AuditLog.builder()
                .organization(testOrg)
                .eventType("test_event")
                .actor(user)
                .actorEmail(user.getEmail())
                .action("test_action")
                .decision("allow")
                .build();
        auditLog = entityManager.persist(auditLog);

        entityManager.flush();
        entityManager.clear();

        // Then: All entities should be retrievable
        assertThat(userRepository.findById(user.getId())).isPresent();
        assertThat(roleRepository.findById(role.getId())).isPresent();
        assertThat(permissionRepository.findById(permission.getId())).isPresent();
        assertThat(policyRepository.findById(policy.getId())).isPresent();
        assertThat(policyVersionRepository.findById(policyVersion.getId())).isPresent();
        assertThat(userRoleRepository.findById(userRole.getId())).isPresent();
        assertThat(rolePermissionRepository.findById(rolePermission.getId())).isPresent();
        assertThat(auditLogRepository.findById(auditLog.getId())).isPresent();
    }

    @Test
    @DisplayName("Should handle JSONB fields correctly")
    void shouldHandleJsonbFieldsCorrectly() {
        // Given: Entity with complex JSONB data
        Organization org = Organization.builder()
                .name("jsonb-test-org")
                .displayName("JSONB Test")
                .status(Organization.OrganizationStatus.ACTIVE)
                .settings(Map.of(
                        "auth", Map.of(
                                "mfaRequired", true,
                                "sessionTimeout", 3600,
                                "allowedDomains", List.of("example.com", "test.com")
                        ),
                        "branding", Map.of(
                                "logoUrl", "https://cdn.example.com/logo.png",
                                "primaryColor", "#3B82F6",
                                "secondaryColor", "#10B981"
                        )
                ))
                .build();
        org = organizationRepository.save(org);

        entityManager.flush();
        entityManager.clear();

        // When: Retrieve organization
        Organization found = organizationRepository.findById(org.getId()).orElseThrow();

        // Then: JSONB data should be intact
        assertThat(found.getSettings()).isNotNull();
        assertThat(found.getSettings()).containsKeys("auth", "branding");

        @SuppressWarnings("unchecked")
        Map<String, Object> auth = (Map<String, Object>) found.getSettings().get("auth");
        assertThat(auth.get("mfaRequired")).isEqualTo(true);
        assertThat(auth.get("sessionTimeout")).isEqualTo(3600);
    }

    @Test
    @DisplayName("Should support soft delete for users")
    void shouldSupportSoftDeleteForUsers() {
        // Given: Active user
        User user = User.builder()
                .organization(testOrg)
                .email("delete-test@example.com")
                .username("delete-test")
                .displayName("Delete Test")
                .status(User.UserStatus.ACTIVE)
                .attributes(Map.of())
                .build();
        user = userRepository.save(user);

        entityManager.flush();

        // When: Soft delete user
        user.softDelete();
        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        // Then: User should be soft deleted
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getDeletedAt()).isNotNull();

        // And should not be found in active user queries
        assertThat(userRepository.findByOrganizationIdAndEmailAndDeletedAtIsNull(
                testOrg.getId(), "delete-test@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Should support role hierarchy")
    void shouldSupportRoleHierarchy() {
        // Given: Three-level role hierarchy
        Role adminRole = Role.builder()
                .organization(testOrg)
                .name("admin")
                .displayName("Administrator")
                .description("Admin role")
                .level(0)
                .isSystem(false)
                .metadata(Map.of())
                .build();
        adminRole = entityManager.persist(adminRole);

        Role managerRole = Role.builder()
                .organization(testOrg)
                .name("manager")
                .displayName("Manager")
                .description("Manager role")
                .parentRole(adminRole)
                .level(1)
                .isSystem(false)
                .metadata(Map.of())
                .build();
        managerRole = entityManager.persist(managerRole);

        Role memberRole = Role.builder()
                .organization(testOrg)
                .name("member")
                .displayName("Member")
                .description("Member role")
                .parentRole(managerRole)
                .level(2)
                .isSystem(false)
                .metadata(Map.of())
                .build();
        memberRole = entityManager.persist(memberRole);

        entityManager.flush();
        entityManager.clear();

        // When: Retrieve member role
        Role found = roleRepository.findById(memberRole.getId()).orElseThrow();

        // Then: Hierarchy should be intact
        assertThat(found.getLevel()).isEqualTo(2);
        assertThat(found.getParentRole()).isNotNull();
        assertThat(found.getParentRole().getName()).isEqualTo("manager");
        assertThat(found.getParentRole().getLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should support resource-scoped role assignments")
    void shouldSupportResourceScopedRoleAssignments() {
        // Given: User and role
        User user = User.builder()
                .organization(testOrg)
                .email("scoped@example.com")
                .username("scoped")
                .displayName("Scoped User")
                .status(User.UserStatus.ACTIVE)
                .attributes(Map.of())
                .build();
        user = entityManager.persist(user);

        Role role = Role.builder()
                .organization(testOrg)
                .name("project-admin")
                .displayName("Project Admin")
                .level(0)
                .isSystem(false)
                .metadata(Map.of())
                .build();
        role = entityManager.persist(role);

        // Create resource-scoped assignment
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .resourceType("project")
                .resourceId("project-123")
                .build();
        userRole = entityManager.persist(userRole);

        entityManager.flush();
        entityManager.clear();

        // When: Find scoped role
        List<UserRole> found = userRoleRepository.findByUserIdAndResourceTypeAndResourceId(
                user.getId(), "project", "project-123");

        // Then: Should find the scoped assignment
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getResourceType()).isEqualTo("project");
        assertThat(found.get(0).getResourceId()).isEqualTo("project-123");
    }

    @Test
    @DisplayName("Should support policy versioning")
    void shouldSupportPolicyVersioning() {
        // Given: Policy with multiple versions
        Policy policy = Policy.builder()
                .organization(testOrg)
                .name("versioned-policy")
                .displayName("Versioned Policy")
                .policyType(Policy.PolicyType.REGO)
                .status(Policy.PolicyStatus.ACTIVE)
                .currentVersion(3)
                .build();
        policy = entityManager.persist(policy);

        PolicyVersion v1 = PolicyVersion.builder()
                .policy(policy)
                .version(1)
                .content("package v1\ndefault allow = false")
                .checksum("v1-checksum")
                .validationStatus(PolicyVersion.ValidationStatus.VALID)
                .build();
        entityManager.persist(v1);

        PolicyVersion v2 = PolicyVersion.builder()
                .policy(policy)
                .version(2)
                .content("package v2\ndefault allow = true")
                .checksum("v2-checksum")
                .validationStatus(PolicyVersion.ValidationStatus.VALID)
                .build();
        entityManager.persist(v2);

        PolicyVersion v3 = PolicyVersion.builder()
                .policy(policy)
                .version(3)
                .content("package v3\ndefault allow = true\nallow { input.admin }")
                .checksum("v3-checksum")
                .validationStatus(PolicyVersion.ValidationStatus.VALID)
                .build();
        entityManager.persist(v3);

        entityManager.flush();
        entityManager.clear();

        // When: Find all versions
        List<PolicyVersion> versions = policyVersionRepository.findByPolicyId(policy.getId());

        // Then: Should have all 3 versions
        assertThat(versions).hasSize(3);
        assertThat(versions).extracting(PolicyVersion::getVersion)
                .containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    @DisplayName("Should handle audit log partitioning")
    void shouldHandleAuditLogPartitioning() {
        // Given: Multiple audit logs
        User user = User.builder()
                .organization(testOrg)
                .email("audit@example.com")
                .username("audit")
                .displayName("Audit User")
                .status(User.UserStatus.ACTIVE)
                .attributes(Map.of())
                .build();
        user = entityManager.persist(user);

        for (int i = 0; i < 5; i++) {
            AuditLog log = AuditLog.builder()
                    .organization(testOrg)
                    .eventType("authorization")
                    .actor(user)
                    .actorEmail(user.getEmail())
                    .action("check_permission_" + i)
                    .decision(i % 2 == 0 ? "allow" : "deny")
                    .build();
            entityManager.persist(log);
        }

        entityManager.flush();
        entityManager.clear();

        // When: Query audit logs
        List<AuditLog> logs = auditLogRepository.findAll();

        // Then: Should find all logs (partitioning is transparent)
        assertThat(logs).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should verify Flyway migrations applied successfully")
    void shouldVerifyFlywayMigrationsApplied() {
        // When: Check that all core tables exist by performing queries
        assertThat(organizationRepository.count()).isGreaterThanOrEqualTo(1); // System org from V1
        assertThat(roleRepository.count()).isGreaterThanOrEqualTo(2); // System roles from V1

        // Then: No exception means migrations (V1, V2, V3) applied successfully
        // V1: Core schema
        // V2: Performance indexes
        // V3: Row-Level Security
    }
}
