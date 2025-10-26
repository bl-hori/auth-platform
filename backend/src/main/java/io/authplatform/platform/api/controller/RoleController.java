package io.authplatform.platform.api.controller;

import io.authplatform.platform.api.dto.PermissionAssignRequest;
import io.authplatform.platform.api.dto.RoleCreateRequest;
import io.authplatform.platform.api.dto.RoleListResponse;
import io.authplatform.platform.api.dto.RoleResponse;
import io.authplatform.platform.api.dto.RoleUpdateRequest;
import io.authplatform.platform.api.security.CurrentOrganizationId;
import io.authplatform.platform.service.RoleService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for role management.
 *
 * <p>This controller provides endpoints for managing roles within organizations,
 * including CRUD operations and hierarchy management.
 *
 * <p><strong>Authentication:</strong> All endpoints require API key authentication.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Role Management", description = "Role CRUD and hierarchy management API")
public class RoleController {

    private final RoleService roleService;

    /**
     * Create a new role.
     *
     * @param request the role creation request
     * @return the created role response
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new role",
            description = "Creates a new role with optional parent role for hierarchy"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Role created successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation error"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Role with name already exists or hierarchy violation"
            )
    })
    public RoleResponse createRole(@Valid @RequestBody RoleCreateRequest request) {
        log.info("Creating role: {}", request.getName());
        return roleService.createRole(request);
    }

    /**
     * Get a role by ID.
     *
     * @param roleId the role ID
     * @return the role response
     */
    @GetMapping(
            value = "/{roleId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get role by ID",
            description = "Retrieves a role by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Role retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Role not found"
            )
    })
    public RoleResponse getRoleById(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId
    ) {
        log.debug("Getting role by ID: {}", roleId);
        return roleService.getRoleById(roleId);
    }

    /**
     * List roles in an organization.
     *
     * @param organizationId the organization ID
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sort field and direction
     * @return paginated list of roles
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "List roles",
            description = "Lists roles in an organization with pagination, sorted by hierarchy level"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Roles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleListResponse.class))
            )
    })
    public RoleListResponse listRoles(
            @Parameter(description = "Organization ID (automatically injected from authentication context)", hidden = true)
            @CurrentOrganizationId UUID organizationId,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction (e.g., 'level,asc')")
            @RequestParam(defaultValue = "level,asc") String sort
    ) {
        log.debug("Listing roles for organization: {}", organizationId);

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        return roleService.getRolesByOrganization(organizationId, pageable);
    }

    /**
     * Get role hierarchy path.
     *
     * @param roleId the role ID
     * @return list of role names from root to current role
     */
    @GetMapping(
            value = "/{roleId}/hierarchy",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get role hierarchy",
            description = "Returns the full hierarchy path from root to the specified role"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Hierarchy retrieved successfully",
                    content = @Content(schema = @Schema(implementation = List.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Role not found"
            )
    })
    public List<String> getRoleHierarchy(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId
    ) {
        log.debug("Getting hierarchy for role: {}", roleId);
        return roleService.getRoleHierarchy(roleId);
    }

    /**
     * Update a role.
     *
     * @param roleId the role ID
     * @param request the update request
     * @return the updated role response
     */
    @PutMapping(
            value = "/{roleId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Update role",
            description = "Updates a role's information (system roles cannot be updated)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Role updated successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Role not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "System role or hierarchy violation"
            )
    })
    public RoleResponse updateRole(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId,

            @Valid @RequestBody RoleUpdateRequest request
    ) {
        log.info("Updating role: {}", roleId);
        return roleService.updateRole(roleId, request);
    }

    /**
     * Delete a role (soft delete).
     *
     * @param roleId the role ID
     */
    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete role",
            description = "Deletes a role (soft delete - system roles and roles with children cannot be deleted)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Role deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Role not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "System role or role has child roles"
            )
    })
    public void deleteRole(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId
    ) {
        log.info("Deleting role: {}", roleId);
        roleService.deleteRole(roleId);
    }

    /**
     * Assign a permission to a role.
     *
     * @param roleId the role ID
     * @param request the permission assignment request
     */
    @PostMapping(
            value = "/{roleId}/permissions",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Assign permission to role",
            description = "Assigns a permission to a role, granting the permission to users with this role"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Permission assigned successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Role or permission not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Permission already assigned or organization mismatch"
            )
    })
    public void assignPermission(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId,

            @Valid @RequestBody PermissionAssignRequest request
    ) {
        log.info("Assigning permission {} to role {}", request.getPermissionId(), roleId);
        roleService.assignPermissionToRole(roleId, request.getPermissionId());
    }

    /**
     * Remove a permission from a role.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     */
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove permission from role",
            description = "Removes a permission from a role, revoking the permission from users with this role"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Permission removed successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Role, permission, or assignment not found"
            )
    })
    public void removePermission(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId,

            @Parameter(description = "Permission ID", required = true)
            @PathVariable UUID permissionId
    ) {
        log.info("Removing permission {} from role {}", permissionId, roleId);
        roleService.removePermissionFromRole(roleId, permissionId);
    }
}
