package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.PermissionListResponse;
import io.authplatform.platform.api.dto.PermissionResponse;
import io.authplatform.platform.domain.repository.PermissionRepository;
import io.authplatform.platform.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link PermissionService} for managing permissions.
 *
 * <p>This service provides business logic for retrieving permissions
 * with organization-based filtering and pagination support.
 *
 * @since 0.1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public PermissionListResponse getPermissions(UUID organizationId, Pageable pageable) {
        log.debug("Getting permissions for organization: {}", organizationId);

        // Get all permissions for the organization and apply pagination manually
        // TODO: Add proper pagination to PermissionRepository
        var permissions = permissionRepository.findAllByOrganizationId(organizationId).stream()
                .sorted((p1, p2) -> {
                    int resourceCompare = p1.getResourceType().compareTo(p2.getResourceType());
                    if (resourceCompare != 0) {
                        return resourceCompare;
                    }
                    return p1.getAction().compareTo(p2.getAction());
                })
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .map(PermissionResponse::fromEntity)
                .toList();

        long totalElements = permissionRepository.countByOrganizationId(organizationId);

        return PermissionListResponse.builder()
                .content(permissions)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .build();
    }
}
