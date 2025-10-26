package io.authplatform.platform.api.controller;

import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;
import io.authplatform.platform.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for authorization decisions.
 *
 * <p>This controller provides endpoints for making authorization decisions
 * based on policies, roles, and permissions.
 *
 * <p><strong>Authentication:</strong> All endpoints require API key authentication.
 *
 * <p><strong>Rate Limiting:</strong> Will be implemented in Task 3.9.
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Authorization", description = "Authorization decision API")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    /**
     * Make an authorization decision.
     *
     * <p>This endpoint evaluates whether a principal (user/service) is authorized
     * to perform a specific action on a resource within an organization.
     *
     * <p><strong>Evaluation Flow:</strong>
     * <ol>
     *   <li>Check L1 cache (Caffeine) - if hit, return immediately</li>
     *   <li>Check L2 cache (Redis) - if hit, return and promote to L1</li>
     *   <li>Evaluate with OPA (if enabled) - if configured, use policy engine</li>
     *   <li>Evaluate with RBAC - use role-based access control</li>
     *   <li>Cache the result in both L1 and L2</li>
     * </ol>
     *
     * <p><strong>Performance:</strong>
     * <ul>
     *   <li>L1 cache hit: ~1-2ms</li>
     *   <li>L2 cache hit: ~3-5ms</li>
     *   <li>Full evaluation: ~10-50ms</li>
     * </ul>
     *
     * @param request the authorization request containing principal, action, and resource
     * @return authorization response with decision (ALLOW/DENY/ERROR) and reasoning
     */
    @PostMapping(
            value = "/authorize",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Make an authorization decision",
            description = "Evaluates whether a principal is authorized to perform an action on a resource"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authorization decision made successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthorizationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - missing required fields or validation errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - missing or invalid API key",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during authorization evaluation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public AuthorizationResponse authorize(
            @Valid @RequestBody AuthorizationRequest request
    ) {
        log.debug("Authorization request received: organizationId={}, principal={}, action={}, resource={}",
                request.getOrganizationId(),
                request.getPrincipal().getId(),
                request.getAction(),
                request.getResource().getType());

        AuthorizationResponse response = authorizationService.authorize(request);

        log.info("Authorization decision: organizationId={}, principal={}, action={}, resource={}, decision={}, evaluationTime={}ms",
                request.getOrganizationId(),
                request.getPrincipal().getId(),
                request.getAction(),
                request.getResource().getType(),
                response.getDecision(),
                response.getEvaluationTimeMs());

        return response;
    }
}
