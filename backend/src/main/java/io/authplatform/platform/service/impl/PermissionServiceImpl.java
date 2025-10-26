package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.PermissionCreateRequest;
import io.authplatform.platform.api.dto.PermissionListResponse;
import io.authplatform.platform.api.dto.PermissionResponse;
import io.authplatform.platform.api.dto.PermissionUpdateRequest;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Permission;
import io.authplatform.platform.domain.repository.OrganizationRepository;
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
 * <p>This service provides business logic for CRUD operations on permissions
 * with organization-based filtering and validation.
 *
 * @since 0.1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public PermissionResponse createPermission(PermissionCreateRequest request) {
        log.info("Creating permission: {} in organization: {}",
                request.getName(), request.getOrganizationId());

        // Validate organization exists
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + request.getOrganizationId()));

        // Check name uniqueness
        if (permissionRepository.existsByOrganizationIdAndName(
                request.getOrganizationId(), request.getName())) {
            throw new IllegalStateException("Permission with name already exists: " + request.getName());
        }

        // Check resource:action uniqueness
        if (permissionRepository.existsByOrganizationIdAndResourceTypeAndAction(
                request.getOrganizationId(), request.getResourceType(), request.getAction())) {
            throw new IllegalStateException(
                    String.format("Permission for resource '%s' and action '%s' already exists",
                            request.getResourceType(), request.getAction()));
        }

        // Create permission
        Permission permission = Permission.builder()
                .organization(organization)
                .name(request.getName())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .resourceType(request.getResourceType())
                .action(request.getAction())
                .effect(Permission.PermissionEffect.valueOf(request.getEffect().toUpperCase()))
                .conditions(request.getConditions() != null ? request.getConditions() : java.util.Map.of())
                .build();

        Permission saved = permissionRepository.save(permission);
        log.info("Permission created successfully: {}", saved.getId());

        return PermissionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(UUID permissionId) {
        log.debug("Getting permission by ID: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));

        return PermissionResponse.fromEntity(permission);
    }

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

    @Override
    public PermissionResponse updatePermission(UUID permissionId, PermissionUpdateRequest request) {
        log.info("Updating permission: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));

        // Update only allowed fields (name, resourceType, and action are immutable)
        if (request.getDisplayName() != null) {
            permission.setDisplayName(request.getDisplayName());
        }

        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }

        if (request.getEffect() != null) {
            permission.setEffect(Permission.PermissionEffect.valueOf(request.getEffect().toUpperCase()));
        }

        if (request.getConditions() != null) {
            permission.setConditions(request.getConditions());
        }

        Permission updated = permissionRepository.save(permission);
        log.info("Permission updated successfully: {}", updated.getId());

        return PermissionResponse.fromEntity(updated);
    }

    @Override
    public void deletePermission(UUID permissionId) {
        log.info("Deleting permission: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));

        permissionRepository.delete(permission);
        log.info("Permission deleted successfully: {}", permissionId);
    }
}
