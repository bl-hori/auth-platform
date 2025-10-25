package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.AuditLog;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AuditLogRepository}.
 *
 * <p>Tests audit log operations including:
 * <ul>
 *   <li>Basic CRUD operations</li>
 *   <li>Time-range queries</li>
 *   <li>Event type filtering</li>
 *   <li>Decision filtering</li>
 *   <li>Resource and action filtering</li>
 *   <li>Helper methods</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    private Organization org;
    private User user1;
    private OffsetDateTime now;
    private OffsetDateTime hourAgo;
    private OffsetDateTime hourLater;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now();
        hourAgo = now.minusHours(1);
        hourLater = now.plusHours(1);

        org = organizationRepository.save(Organization.builder()
                .name("test-org")
                .displayName("Test Org")
                .build());

        user1 = userRepository.save(User.builder()
                .organization(org)
                .email("user1@test.com")
                .username("user1")
                .displayName("User One")
                .build());
    }

    @Test
    void shouldSaveAndFindAuditLog() {
        // Given
        AuditLog log = AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .actor(user1)
                .actorEmail(user1.getEmail())
                .resourceType("document")
                .resourceId("doc-123")
                .action("read")
                .decision("allow")
                .decisionReason("User has viewer role")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();

        // When
        AuditLog saved = auditLogRepository.save(log);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTimestamp()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("authorization");
        assertThat(saved.getDecision()).isEqualTo("allow");
    }

    @Test
    void shouldFindByOrganizationIdAndTimestampBetween() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .timestamp(now.minusMinutes(30))
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("admin_action")
                .action("create_user")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> logs = auditLogRepository.findByOrganizationIdAndTimestampBetween(
                org.getId(), hourAgo, hourLater);

        // Then
        assertThat(logs).hasSize(2);
    }

    @Test
    void shouldFindByEventTypeAndTimestampBetween() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("admin_action")
                .action("create")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> authLogs = auditLogRepository.findByEventTypeAndTimestampBetween(
                org.getId(), "authorization", hourAgo, hourLater);

        // Then
        assertThat(authLogs).hasSize(1);
        assertThat(authLogs.get(0).getEventType()).isEqualTo("authorization");
    }

    @Test
    void shouldFindAuthorizationEventsByOrganizationId() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .decision("allow")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .decision("deny")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("admin_action")
                .action("create")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> authEvents = auditLogRepository.findAuthorizationEventsByOrganizationId(
                org.getId(), hourAgo, hourLater);

        // Then
        assertThat(authEvents).hasSize(2);
        assertThat(authEvents).allMatch(AuditLog::isAuthorizationEvent);
    }

    @Test
    void shouldFindByActorIdAndTimestampBetween() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .actor(user1)
                .action("read")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("admin_action")
                .actor(user1)
                .action("create")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> userLogs = auditLogRepository.findByActorIdAndTimestampBetween(
                user1.getId(), hourAgo, hourLater);

        // Then
        assertThat(userLogs).hasSize(2);
        assertThat(userLogs).allMatch(log -> log.getActor().equals(user1));
    }

    @Test
    void shouldFindByResourceAndTimestampBetween() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .resourceType("document")
                .resourceId("doc-123")
                .action("read")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .resourceType("document")
                .resourceId("doc-456")
                .action("read")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> resourceLogs = auditLogRepository.findByResourceAndTimestampBetween(
                org.getId(), "document", "doc-123", hourAgo, hourLater);

        // Then
        assertThat(resourceLogs).hasSize(1);
        assertThat(resourceLogs.get(0).getResourceId()).isEqualTo("doc-123");
    }

    @Test
    void shouldFindByActionAndTimestampBetween() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> readLogs = auditLogRepository.findByActionAndTimestampBetween(
                org.getId(), "read", hourAgo, hourLater);

        // Then
        assertThat(readLogs).hasSize(1);
        assertThat(readLogs.get(0).getAction()).isEqualTo("read");
    }

    @Test
    void shouldFindByDecisionAndTimestampBetween() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .decision("allow")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .decision("deny")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> allowedLogs = auditLogRepository.findByDecisionAndTimestampBetween(
                org.getId(), "allow", hourAgo, hourLater);

        // Then
        assertThat(allowedLogs).hasSize(1);
        assertThat(allowedLogs.get(0).isAllowed()).isTrue();
    }

    @Test
    void shouldFindDeniedAuthorizationsByOrganizationId() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .decision("allow")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .decision("deny")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("delete")
                .decision("deny")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> deniedLogs = auditLogRepository.findDeniedAuthorizationsByOrganizationId(
                org.getId(), hourAgo, hourLater);

        // Then
        assertThat(deniedLogs).hasSize(2);
        assertThat(deniedLogs).allMatch(AuditLog::isDenied);
    }

    @Test
    void shouldFindByIpAddressAndTimestampBetween() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .ipAddress("192.168.1.1")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .ipAddress("192.168.1.2")
                .timestamp(now)
                .build());

        // When
        List<AuditLog> ipLogs = auditLogRepository.findByIpAddressAndTimestampBetween(
                org.getId(), "192.168.1.1", hourAgo, hourLater);

        // Then
        assertThat(ipLogs).hasSize(1);
        assertThat(ipLogs.get(0).getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void shouldCountLogs() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .decision("allow")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .decision("deny")
                .timestamp(now)
                .build());

        // When
        long totalCount = auditLogRepository.countByOrganizationIdAndTimestampBetween(
                org.getId(), hourAgo, hourLater);
        long allowCount = auditLogRepository.countByDecisionAndTimestampBetween(
                org.getId(), "allow", hourAgo, hourLater);
        long denyCount = auditLogRepository.countByDecisionAndTimestampBetween(
                org.getId(), "deny", hourAgo, hourLater);

        // Then
        assertThat(totalCount).isEqualTo(2);
        assertThat(allowCount).isEqualTo(1);
        assertThat(denyCount).isEqualTo(1);
    }

    @Test
    void shouldFindDistinctEventTypes() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("admin_action")
                .action("create")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .timestamp(now)
                .build());

        // When
        List<String> eventTypes = auditLogRepository.findDistinctEventTypes(
                org.getId(), hourAgo, hourLater);

        // Then
        assertThat(eventTypes).containsExactly("admin_action", "authorization");
    }

    @Test
    void shouldFindDistinctActions() {
        // Given
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("write")
                .timestamp(now)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .timestamp(now)
                .build());

        // When
        List<String> actions = auditLogRepository.findDistinctActions(
                org.getId(), hourAgo, hourLater);

        // Then
        assertThat(actions).containsExactly("read", "write");
    }

    @Test
    void shouldVerifyHelperMethods() {
        // Given
        AuditLog authLog = auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .actor(user1)
                .actorEmail(user1.getEmail())
                .action("read")
                .decision("allow")
                .timestamp(now)
                .build());

        // Then
        assertThat(authLog.isAuthorizationEvent()).isTrue();
        assertThat(authLog.isAdminAction()).isFalse();
        assertThat(authLog.isAllowed()).isTrue();
        assertThat(authLog.isDenied()).isFalse();
        assertThat(authLog.getEventDescription()).contains("user1@test.com", "read", "allow");
    }

    @Test
    void shouldHandleRequestAndResponseData() {
        // Given
        Map<String, Object> requestData = Map.of(
                "user", Map.of("id", "user-123", "roles", List.of("editor")),
                "resource", Map.of("type", "document", "id", "doc-123")
        );
        Map<String, Object> responseData = Map.of(
                "decision", "allow",
                "duration_ms", 15
        );

        AuditLog log = auditLogRepository.save(AuditLog.builder()
                .organization(org)
                .eventType("authorization")
                .action("read")
                .decision("allow")
                .requestData(requestData)
                .responseData(responseData)
                .timestamp(now)
                .build());

        // When
        AuditLog found = auditLogRepository.findByOrganizationIdAndTimestampBetween(
                org.getId(), hourAgo, hourLater).get(0);

        // Then
        assertThat(found.getRequestData()).isNotNull();
        assertThat(found.getResponseData()).isNotNull();
        assertThat(found.getRequestData()).containsKey("user");
        assertThat(found.getResponseData()).containsKey("decision");
    }
}
