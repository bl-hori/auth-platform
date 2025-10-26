package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.auditlog.AuditLogQueryParams;
import io.authplatform.platform.api.dto.auditlog.AuditLogResponse;
import io.authplatform.platform.domain.entity.AuditLog;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.repository.AuditLogRepository;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AuditLogService with async logging support.
 *
 * <p>Uses Spring's @Async annotation for non-blocking audit log writes.
 * Provides comprehensive querying and CSV export capabilities.
 *
 * @see AuditLogService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Override
    @Async
    @Transactional
    public void logAuthorizationDecision(
            UUID organizationId,
            UUID userId,
            String resourceType,
            String resourceId,
            String action,
            String decision,
            String decisionReason) {
        log.debug("Logging authorization decision: user={}, resource={}:{}, action={}, decision={}",
                userId, resourceType, resourceId, action, decision);

        Organization organization = organizationRepository.findById(organizationId)
                .orElse(null);
        if (organization == null) {
            log.warn("Organization not found for audit log: {}", organizationId);
            return;
        }

        User user = userRepository.findById(userId).orElse(null);

        AuditLog auditLog = AuditLog.builder()
                .organization(organization)
                .eventType("authorization")
                .actor(user)
                .actorEmail(user != null ? user.getEmail() : null)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .action(action)
                .decision(decision)
                .decisionReason(decisionReason)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Authorization decision logged: id={}", auditLog.getId());
    }

    @Override
    @Async
    @Transactional
    public void logAdministrativeAction(
            UUID organizationId,
            UUID actorId,
            String action,
            String resourceType,
            String resourceId,
            String details) {
        log.debug("Logging administrative action: actor={}, action={}, resource={}:{}",
                actorId, action, resourceType, resourceId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElse(null);
        if (organization == null) {
            log.warn("Organization not found for audit log: {}", organizationId);
            return;
        }

        User actor = userRepository.findById(actorId).orElse(null);

        AuditLog auditLog = AuditLog.builder()
                .organization(organization)
                .eventType("admin_action")
                .actor(actor)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .action(action)
                .decisionReason(details)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Administrative action logged: id={}", auditLog.getId());
    }

    @Override
    @Async
    @Transactional
    public void logPolicyChange(
            UUID organizationId,
            UUID actorId,
            UUID policyId,
            String action,
            String details) {
        log.debug("Logging policy change: actor={}, policy={}, action={}",
                actorId, policyId, action);

        Organization organization = organizationRepository.findById(organizationId)
                .orElse(null);
        if (organization == null) {
            log.warn("Organization not found for audit log: {}", organizationId);
            return;
        }

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        AuditLog auditLog = AuditLog.builder()
                .organization(organization)
                .eventType("policy_change")
                .actor(actor)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .resourceType("policy")
                .resourceId(policyId.toString())
                .action(action)
                .decisionReason(details)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Policy change logged: id={}", auditLog.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> queryAuditLogs(AuditLogQueryParams params, Pageable pageable) {
        log.info("Querying audit logs: organizationId={}, eventType={}, timeRange={} to {}",
                params.getOrganizationId(), params.getEventType(),
                params.getStartDate(), params.getEndDate());

        // Build query based on parameters
        List<AuditLog> allLogs = findAuditLogsWithFilters(params);

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allLogs.size());

        List<AuditLogResponse> responses = allLogs.subList(start, end).stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, allLogs.size());
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(UUID logId) {
        AuditLog auditLog = auditLogRepository.findById(logId)
                .orElseThrow(() -> new AuditLogNotFoundException("Audit log not found: " + logId));

        return toAuditLogResponse(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAuditLogsToCsv(AuditLogQueryParams params) {
        log.info("Exporting audit logs to CSV: organizationId={}", params.getOrganizationId());

        List<AuditLog> logs = findAuditLogsWithFilters(params);

        StringBuilder csv = new StringBuilder();

        // CSV header
        csv.append("Timestamp,Event Type,Actor Email,Resource Type,Resource ID,Action,Decision,Decision Reason,IP Address\n");

        // CSV rows
        for (AuditLog log : logs) {
            csv.append(escapeCsv(log.getTimestamp().toString())).append(",");
            csv.append(escapeCsv(log.getEventType())).append(",");
            csv.append(escapeCsv(log.getActorEmail())).append(",");
            csv.append(escapeCsv(log.getResourceType())).append(",");
            csv.append(escapeCsv(log.getResourceId())).append(",");
            csv.append(escapeCsv(log.getAction())).append(",");
            csv.append(escapeCsv(log.getDecision())).append(",");
            csv.append(escapeCsv(log.getDecisionReason())).append(",");
            csv.append(escapeCsv(log.getIpAddress())).append("\n");
        }

        log.info("Exported {} audit logs to CSV", logs.size());
        return csv.toString();
    }

    @Override
    @Transactional
    public long deleteLogsOlderThan(int retentionDays) {
        log.info("Deleting audit logs older than {} days", retentionDays);

        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(retentionDays);

        // Note: This would need a custom query in the repository
        // For now, we'll return 0 as this is a placeholder
        // TODO: Implement bulk delete query in repository

        log.warn("Audit log retention deletion not yet implemented");
        return 0;
    }

    /**
     * Find audit logs with filters applied.
     */
    private List<AuditLog> findAuditLogsWithFilters(AuditLogQueryParams params) {
        // Determine time range (default to last 90 days if not specified)
        OffsetDateTime startDate = params.getStartDate() != null
                ? params.getStartDate()
                : OffsetDateTime.now().minusDays(90);
        OffsetDateTime endDate = params.getEndDate() != null
                ? params.getEndDate()
                : OffsetDateTime.now();

        // Start with time-range query (most efficient due to partitioning)
        List<AuditLog> logs = auditLogRepository.findByOrganizationIdAndTimestampBetween(
                params.getOrganizationId(), startDate, endDate);

        // Apply additional filters in memory (for MVP simplicity)
        // In production, these should be database queries

        if (params.getEventType() != null) {
            logs = logs.stream()
                    .filter(log -> params.getEventType().equals(log.getEventType()))
                    .collect(Collectors.toList());
        }

        if (params.getActorId() != null) {
            logs = logs.stream()
                    .filter(log -> log.getActor() != null &&
                            params.getActorId().equals(log.getActor().getId()))
                    .collect(Collectors.toList());
        }

        if (params.getResourceType() != null) {
            logs = logs.stream()
                    .filter(log -> params.getResourceType().equals(log.getResourceType()))
                    .collect(Collectors.toList());
        }

        if (params.getResourceId() != null) {
            logs = logs.stream()
                    .filter(log -> params.getResourceId().equals(log.getResourceId()))
                    .collect(Collectors.toList());
        }

        if (params.getAction() != null) {
            logs = logs.stream()
                    .filter(log -> params.getAction().equals(log.getAction()))
                    .collect(Collectors.toList());
        }

        if (params.getDecision() != null) {
            logs = logs.stream()
                    .filter(log -> params.getDecision().equals(log.getDecision()))
                    .collect(Collectors.toList());
        }

        return logs;
    }

    /**
     * Convert AuditLog entity to AuditLogResponse DTO.
     */
    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .organizationId(log.getOrganization().getId())
                .timestamp(log.getTimestamp())
                .eventType(log.getEventType())
                .actorId(log.getActor() != null ? log.getActor().getId() : null)
                .actorEmail(log.getActorEmail())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .action(log.getAction())
                .decision(log.getDecision())
                .decisionReason(log.getDecisionReason())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .metadata(log.getRequestData())
                .build();
    }

    /**
     * Escape CSV values to prevent injection and handle special characters.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
