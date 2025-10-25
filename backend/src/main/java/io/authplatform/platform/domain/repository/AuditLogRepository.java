package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for {@link AuditLog} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for audit log management.
 * Optimized for time-range queries on partitioned table.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private AuditLogRepository auditLogRepository;
 *
 * // Log an authorization decision
 * AuditLog log = AuditLog.builder()
 *     .organization(org)
 *     .eventType("authorization")
 *     .actor(user)
 *     .action("read")
 *     .decision("allow")
 *     .build();
 * auditLogRepository.save(log);
 *
 * // Query logs by time range
 * List<AuditLog> recentLogs = auditLogRepository.findByOrganizationIdAndTimestampBetween(
 *     orgId, startTime, endTime);
 *
 * // Query authorization decisions
 * List<AuditLog> authLogs = auditLogRepository.findAuthorizationEventsByOrganizationId(
 *     orgId, startTime, endTime);
 * }</pre>
 *
 * @see AuditLog
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by organization within a time range.
     * This query benefits from table partitioning.
     *
     * @param organizationId the organization ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByOrganizationIdAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find audit logs by event type within a time range.
     *
     * @param organizationId the organization ID
     * @param eventType the event type
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.eventType = :eventType " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByEventTypeAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("eventType") String eventType,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find authorization events within a time range.
     *
     * @param organizationId the organization ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of authorization audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.eventType = 'authorization' " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findAuthorizationEventsByOrganizationId(
            @Param("organizationId") UUID organizationId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find audit logs by actor (user) within a time range.
     *
     * @param actorId the user ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.actor.id = :actorId " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByActorIdAndTimestampBetween(
            @Param("actorId") UUID actorId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find audit logs by resource within a time range.
     *
     * @param organizationId the organization ID
     * @param resourceType the resource type
     * @param resourceId the resource ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.resourceType = :resourceType AND al.resourceId = :resourceId " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByResourceAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find audit logs by action within a time range.
     *
     * @param organizationId the organization ID
     * @param action the action
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.action = :action " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByActionAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("action") String action,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find audit logs by decision within a time range.
     *
     * @param organizationId the organization ID
     * @param decision the decision (allow, deny, error)
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.decision = :decision " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByDecisionAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("decision") String decision,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find denied authorization attempts within a time range.
     *
     * @param organizationId the organization ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of denied authorization logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.decision = 'deny' " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findDeniedAuthorizationsByOrganizationId(
            @Param("organizationId") UUID organizationId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find audit logs by IP address within a time range.
     *
     * @param organizationId the organization ID
     * @param ipAddress the IP address
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.ipAddress = :ipAddress " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByIpAddressAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("ipAddress") String ipAddress,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Count audit logs by organization within a time range.
     *
     * @param organizationId the organization ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return count of audit logs
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.timestamp BETWEEN :startTime AND :endTime")
    long countByOrganizationIdAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Count authorization events by decision within a time range.
     *
     * @param organizationId the organization ID
     * @param decision the decision
     * @param startTime start of time range
     * @param endTime end of time range
     * @return count of authorization events
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.decision = :decision " +
           "AND al.timestamp BETWEEN :startTime AND :endTime")
    long countByDecisionAndTimestampBetween(
            @Param("organizationId") UUID organizationId,
            @Param("decision") String decision,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find distinct event types within a time range.
     *
     * @param organizationId the organization ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of distinct event types
     */
    @Query("SELECT DISTINCT al.eventType FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.eventType")
    List<String> findDistinctEventTypes(
            @Param("organizationId") UUID organizationId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find distinct actions within a time range.
     *
     * @param organizationId the organization ID
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of distinct actions
     */
    @Query("SELECT DISTINCT al.action FROM AuditLog al WHERE al.organization.id = :organizationId " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.action")
    List<String> findDistinctActions(
            @Param("organizationId") UUID organizationId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);
}
