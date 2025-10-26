package io.authplatform.platform.api.controller;

import io.authplatform.platform.api.dto.UserCreateRequest;
import io.authplatform.platform.api.dto.UserListResponse;
import io.authplatform.platform.api.dto.UserResponse;
import io.authplatform.platform.api.dto.UserUpdateRequest;
import io.authplatform.platform.service.UserService;
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

import java.util.UUID;

/**
 * REST API controller for user management.
 *
 * <p>This controller provides endpoints for managing users within organizations,
 * including CRUD operations, search, and activation/deactivation.
 *
 * <p><strong>Authentication:</strong> All endpoints require API key authentication.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "User Management", description = "User CRUD and management API")
public class UserController {

    private final UserService userService;

    /**
     * Create a new user.
     *
     * <p>Creates a new user in the specified organization with the provided details.
     *
     * @param request the user creation request
     * @return the created user response
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user in the specified organization"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation error"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User with email or username already exists"
            )
    })
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        return userService.createUser(request);
    }

    /**
     * Get a user by ID.
     *
     * @param userId the user ID
     * @return the user response
     */
    @GetMapping(
            value = "/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user by their unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    public UserResponse getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId
    ) {
        log.debug("Getting user by ID: {}", userId);
        return userService.getUserById(userId);
    }

    /**
     * List users in an organization with search and filtering.
     *
     * @param organizationId the organization ID
     * @param search optional search query
     * @param status optional status filter
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sort field and direction
     * @return paginated list of users
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "List users",
            description = "Lists users in an organization with pagination, search, and filtering"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserListResponse.class))
            )
    })
    public UserListResponse listUsers(
            @Parameter(description = "Organization ID", required = true)
            @RequestParam UUID organizationId,

            @Parameter(description = "Search query (searches email, username, displayName)")
            @RequestParam(required = false) String search,

            @Parameter(description = "Status filter", schema = @Schema(allowableValues = {"active", "inactive", "suspended"}))
            @RequestParam(required = false) String status,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction (e.g., 'createdAt,desc')")
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        log.debug("Listing users for organization: {}", organizationId);

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        return userService.getUsersByOrganization(organizationId, search, status, pageable);
    }

    /**
     * Update a user.
     *
     * @param userId the user ID
     * @param request the update request
     * @return the updated user response
     */
    @PutMapping(
            value = "/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Update user",
            description = "Updates a user's information (partial updates supported)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email or username conflict"
            )
    })
    public UserResponse updateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,

            @Valid @RequestBody UserUpdateRequest request
    ) {
        log.info("Updating user: {}", userId);
        return userService.updateUser(userId, request);
    }

    /**
     * Deactivate a user (soft delete).
     *
     * @param userId the user ID
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Deactivate user",
            description = "Deactivates a user (soft delete - can be reactivated)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User deactivated successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    public void deactivateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId
    ) {
        log.info("Deactivating user: {}", userId);
        userService.deactivateUser(userId);
    }

    /**
     * Activate a user.
     *
     * @param userId the user ID
     * @return the activated user response
     */
    @PostMapping(
            value = "/{userId}/activate",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Activate user",
            description = "Activates a previously deactivated user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User activated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    public UserResponse activateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId
    ) {
        log.info("Activating user: {}", userId);
        userService.activateUser(userId);
        return userService.getUserById(userId);
    }
}
