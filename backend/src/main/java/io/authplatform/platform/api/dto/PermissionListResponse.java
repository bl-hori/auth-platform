package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Permission list response DTO with pagination support.
 *
 * <p>This DTO wraps a list of permissions with pagination metadata,
 * allowing clients to navigate through large permission sets.
 *
 * <p><strong>Usage:</strong>
 * <ul>
 *   <li>GET /v1/permissions - List all permissions with pagination</li>
 * </ul>
 *
 * @see PermissionResponse
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Paginated permission list response",
        example = """
                {
                  "content": [
                    {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "organizationId": "123e4567-e89b-12d3-a456-426614174001",
                      "name": "document:read",
                      "displayName": "Read Documents",
                      "resourceType": "document",
                      "action": "read",
                      "effect": "allow"
                    }
                  ],
                  "page": 0,
                  "size": 20,
                  "totalElements": 50,
                  "totalPages": 3
                }
                """
)
public class PermissionListResponse {

    /**
     * List of permissions for the current page.
     */
    @Schema(
            description = "List of permissions for the current page",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<PermissionResponse> content;

    /**
     * Current page number (0-indexed).
     */
    @Schema(
            description = "Current page number (0-indexed)",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer page;

    /**
     * Number of items per page.
     */
    @Schema(
            description = "Number of items per page",
            example = "20",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer size;

    /**
     * Total number of permissions across all pages.
     */
    @Schema(
            description = "Total number of permissions across all pages",
            example = "50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long totalElements;

    /**
     * Total number of pages.
     */
    @Schema(
            description = "Total number of pages",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalPages;
}
