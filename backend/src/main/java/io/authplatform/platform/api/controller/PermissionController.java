package io.authplatform.platform.api.controller;

import io.authplatform.platform.api.dto.PermissionListResponse;
import io.authplatform.platform.api.security.CurrentOrganizationId;
import io.authplatform.platform.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API controller for permission management.
 *
 * <p>This controller provides endpoints for retrieving permissions within organizations.
 *
 * <p><strong>Authentication:</strong> All endpoints require API key authentication.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Permission Management", description = "Permission retrieval API")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * List permissions in an organization.
     *
     * @param organizationId the organization ID (injected from authentication context)
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sort field and direction
     * @return paginated list of permissions
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "List permissions",
            description = "Lists permissions in an organization with pagination, sorted by resource type and action"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PermissionListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing API key"
            )
    })
    public PermissionListResponse listPermissions(
            @Parameter(
                    description = "Organization ID (automatically injected from authentication context)",
                    hidden = true
            )
            @CurrentOrganizationId UUID organizationId,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction (e.g., 'resourceType,asc')")
            @RequestParam(defaultValue = "resourceType,asc") String sort
    ) {
        log.debug("Listing permissions for organization: {}", organizationId);

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        return permissionService.getPermissions(organizationId, pageable);
    }
}
