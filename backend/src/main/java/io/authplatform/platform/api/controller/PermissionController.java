package io.authplatform.platform.api.controller;

import io.authplatform.platform.api.dto.PermissionCreateRequest;
import io.authplatform.platform.api.dto.PermissionListResponse;
import io.authplatform.platform.api.dto.PermissionResponse;
import io.authplatform.platform.api.dto.PermissionUpdateRequest;
import io.authplatform.platform.api.security.CurrentOrganizationId;
import io.authplatform.platform.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API controller for permission management.
 *
 * <p>This controller provides endpoints for CRUD operations on permissions within organizations.
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
@Tag(name = "Permission Management", description = "Permission CRUD API")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * Create a new permission.
     *
     * @param request the permission creation request
     * @return the created permission response
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new permission",
            description = "Creates a new permission with resource type and action"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Permission created successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation error"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Permission with name or resource:action already exists"
            )
    })
    public PermissionResponse createPermission(@Valid @RequestBody PermissionCreateRequest request) {
        log.info("Creating permission: {}", request.getName());
        return permissionService.createPermission(request);
    }

    /**
     * Get a permission by ID.
     *
     * @param permissionId the permission ID
     * @return the permission response
     */
    @GetMapping(
            value = "/{permissionId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get permission by ID",
            description = "Retrieves a single permission by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permission retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Permission not found"
            )
    })
    public PermissionResponse getPermissionById(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable UUID permissionId
    ) {
        log.debug("Getting permission by ID: {}", permissionId);
        return permissionService.getPermissionById(permissionId);
    }

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

    /**
     * Update a permission.
     *
     * @param permissionId the permission ID
     * @param request the permission update request
     * @return the updated permission response
     */
    @PutMapping(
            value = "/{permissionId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Update a permission",
            description = "Updates an existing permission. Note: name, resourceType, and action are immutable."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permission updated successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Permission not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation error"
            )
    })
    public PermissionResponse updatePermission(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable UUID permissionId,
            @Valid @RequestBody PermissionUpdateRequest request
    ) {
        log.info("Updating permission: {}", permissionId);
        return permissionService.updatePermission(permissionId, request);
    }

    /**
     * Delete a permission.
     *
     * @param permissionId the permission ID
     */
    @DeleteMapping("/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a permission",
            description = "Deletes a permission permanently. This will also remove all role-permission associations."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Permission deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Permission not found"
            )
    })
    public void deletePermission(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable UUID permissionId
    ) {
        log.info("Deleting permission: {}", permissionId);
        permissionService.deletePermission(permissionId);
    }
}
