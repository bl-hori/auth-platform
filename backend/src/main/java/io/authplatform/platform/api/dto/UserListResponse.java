package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated user list response DTO.
 *
 * <p>This DTO wraps a list of users with pagination metadata, allowing
 * clients to navigate through large result sets efficiently.
 *
 * <p><strong>Pagination Parameters:</strong>
 * <ul>
 *   <li>page - Current page number (0-indexed)</li>
 *   <li>size - Number of items per page</li>
 *   <li>totalElements - Total number of users matching the query</li>
 *   <li>totalPages - Total number of pages available</li>
 * </ul>
 *
 * @see UserResponse
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Paginated user list response",
        example = """
                {
                  "content": [
                    {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "organizationId": "123e4567-e89b-12d3-a456-426614174001",
                      "email": "user1@example.com",
                      "username": "user1",
                      "displayName": "User One",
                      "status": "active",
                      "createdAt": "2024-01-15T10:30:00Z",
                      "updatedAt": "2024-01-15T10:30:00Z"
                    }
                  ],
                  "page": 0,
                  "size": 20,
                  "totalElements": 45,
                  "totalPages": 3
                }
                """
)
public class UserListResponse {

    /**
     * List of users for the current page.
     */
    @Schema(
            description = "List of users for the current page",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<UserResponse> content;

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
     * Total number of users matching the query.
     */
    @Schema(
            description = "Total number of users matching the query",
            example = "45",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long totalElements;

    /**
     * Total number of pages available.
     */
    @Schema(
            description = "Total number of pages available",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalPages;
}
