package io.authplatform.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated role list response DTO.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated role list response")
public class RoleListResponse {

    @Schema(description = "List of roles for the current page", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoleResponse> roles;

    @Schema(description = "Current page number (0-indexed)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer page;

    @Schema(description = "Number of items per page", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer size;

    @Schema(description = "Total number of roles", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long totalElements;

    @Schema(description = "Total number of pages", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalPages;
}
