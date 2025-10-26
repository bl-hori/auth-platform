package io.authplatform.platform.api.controller;

import io.authplatform.platform.api.dto.auditlog.AuditLogQueryParams;
import io.authplatform.platform.api.dto.auditlog.AuditLogResponse;
import io.authplatform.platform.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for audit log management.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Querying audit logs with filters</li>
 *   <li>Retrieving specific audit log details</li>
 *   <li>Exporting audit logs to CSV format</li>
 * </ul>
 *
 * <p>All endpoints require API key authentication via X-API-Key header.
 *
 * @see AuditLogService
 */
@RestController
@RequestMapping("/v1/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Logs", description = "APIs for querying and exporting audit logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Query audit logs with filtering and pagination.
     *
     * <p>GET /v1/audit-logs?organizationId={orgId}&eventType=authorization&startDate=...&endDate=...
     *
     * @param organizationId organization ID (required)
     * @param eventType event type filter (optional)
     * @param actorId actor ID filter (optional)
     * @param resourceType resource type filter (optional)
     * @param resourceId resource ID filter (optional)
     * @param action action filter (optional)
     * @param decision decision filter (optional)
     * @param startDate start date filter (optional)
     * @param endDate end date filter (optional)
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    @GetMapping
    @Operation(
            summary = "Query audit logs",
            description = "Retrieves audit logs with optional filtering and pagination"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audit logs retrieved successfully"
            ),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<Page<AuditLogResponse>> queryAuditLogs(
            @Parameter(description = "Organization ID", required = true)
            @RequestParam UUID organizationId,

            @Parameter(description = "Event type filter")
            @RequestParam(required = false) String eventType,

            @Parameter(description = "Actor (user) ID filter")
            @RequestParam(required = false) UUID actorId,

            @Parameter(description = "Resource type filter")
            @RequestParam(required = false) String resourceType,

            @Parameter(description = "Resource ID filter")
            @RequestParam(required = false) String resourceId,

            @Parameter(description = "Action filter")
            @RequestParam(required = false) String action,

            @Parameter(description = "Decision filter (allow/deny)")
            @RequestParam(required = false) String decision,

            @Parameter(description = "Start date (ISO 8601 format)")
            @RequestParam(required = false) OffsetDateTime startDate,

            @Parameter(description = "End date (ISO 8601 format)")
            @RequestParam(required = false) OffsetDateTime endDate,

            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        log.info("GET /v1/audit-logs - organizationId={}, eventType={}, page={}, size={}",
                organizationId, eventType, pageable.getPageNumber(), pageable.getPageSize());

        AuditLogQueryParams params = AuditLogQueryParams.builder()
                .organizationId(organizationId)
                .eventType(eventType)
                .actorId(actorId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .action(action)
                .decision(decision)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Page<AuditLogResponse> auditLogs = auditLogService.queryAuditLogs(params, pageable);

        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get a specific audit log by ID.
     *
     * <p>GET /v1/audit-logs/{id}
     *
     * @param id the audit log ID
     * @return the audit log
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get audit log by ID",
            description = "Retrieves detailed information about a specific audit log"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audit log retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AuditLogResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Audit log not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<AuditLogResponse> getAuditLogById(
            @Parameter(description = "Audit log ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /v1/audit-logs/{} - Fetching audit log", id);

        AuditLogResponse auditLog = auditLogService.getAuditLogById(id);

        return ResponseEntity.ok(auditLog);
    }

    /**
     * Export audit logs to CSV format.
     *
     * <p>GET /v1/audit-logs/export?organizationId={orgId}&eventType=...
     *
     * @param organizationId organization ID (required)
     * @param eventType event type filter (optional)
     * @param actorId actor ID filter (optional)
     * @param resourceType resource type filter (optional)
     * @param resourceId resource ID filter (optional)
     * @param action action filter (optional)
     * @param decision decision filter (optional)
     * @param startDate start date filter (optional)
     * @param endDate end date filter (optional)
     * @return CSV content
     */
    @GetMapping("/export")
    @Operation(
            summary = "Export audit logs to CSV",
            description = "Exports audit logs matching the query parameters to CSV format"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV export successful",
                    content = @Content(mediaType = "text/csv")
            ),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid API key")
    })
    public ResponseEntity<String> exportAuditLogsToCsv(
            @Parameter(description = "Organization ID", required = true)
            @RequestParam UUID organizationId,

            @Parameter(description = "Event type filter")
            @RequestParam(required = false) String eventType,

            @Parameter(description = "Actor (user) ID filter")
            @RequestParam(required = false) UUID actorId,

            @Parameter(description = "Resource type filter")
            @RequestParam(required = false) String resourceType,

            @Parameter(description = "Resource ID filter")
            @RequestParam(required = false) String resourceId,

            @Parameter(description = "Action filter")
            @RequestParam(required = false) String action,

            @Parameter(description = "Decision filter (allow/deny)")
            @RequestParam(required = false) String decision,

            @Parameter(description = "Start date (ISO 8601 format)")
            @RequestParam(required = false) OffsetDateTime startDate,

            @Parameter(description = "End date (ISO 8601 format)")
            @RequestParam(required = false) OffsetDateTime endDate) {

        log.info("GET /v1/audit-logs/export - organizationId={}, eventType={}",
                organizationId, eventType);

        AuditLogQueryParams params = AuditLogQueryParams.builder()
                .organizationId(organizationId)
                .eventType(eventType)
                .actorId(actorId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .action(action)
                .decision(decision)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        String csvContent = auditLogService.exportAuditLogsToCsv(params);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"audit-logs.csv\"")
                .body(csvContent);
    }

    /**
     * Exception handler for AuditLogNotFoundException.
     */
    @ExceptionHandler(AuditLogService.AuditLogNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAuditLogNotFound(
            AuditLogService.AuditLogNotFoundException ex) {
        log.warn("Audit log not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "Audit log not found",
                        "message", ex.getMessage()
                ));
    }
}
