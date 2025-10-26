package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.RoleCreateRequest;
import io.authplatform.platform.api.dto.RoleListResponse;
import io.authplatform.platform.api.dto.RoleResponse;
import io.authplatform.platform.api.dto.RoleUpdateRequest;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Role;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.RoleRepository;
import io.authplatform.platform.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link RoleService} for managing roles with hierarchy support.
 *
 * @since 0.1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private static final int MAX_HIERARCHY_LEVEL = 10;

    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public RoleResponse createRole(RoleCreateRequest request) {
        log.info("Creating role: {} in organization: {}", request.getName(), request.getOrganizationId());

        // Validate organization exists
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + request.getOrganizationId()));

        // Check name uniqueness
        if (roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(
                request.getOrganizationId(), request.getName())) {
            throw new IllegalStateException("Role with name already exists: " + request.getName());
        }

        // Validate and get parent role if specified
        Role parentRole = null;
        int level = 0;

        if (request.getParentRoleId() != null) {
            parentRole = roleRepository.findByIdAndNotDeleted(request.getParentRoleId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent role not found: " + request.getParentRoleId()));

            // Validate parent belongs to same organization
            if (!parentRole.getOrganization().getId().equals(request.getOrganizationId())) {
                throw new IllegalStateException("Parent role must belong to the same organization");
            }

            level = parentRole.getLevel() + 1;

            // Check max depth
            if (level > MAX_HIERARCHY_LEVEL) {
                throw new IllegalStateException("Maximum hierarchy depth exceeded (max: " + MAX_HIERARCHY_LEVEL + ")");
            }
        }

        // Create role entity
        Role role = Role.builder()
                .organization(organization)
                .name(request.getName())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .parentRole(parentRole)
                .level(level)
                .isSystem(false)
                .metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
                .build();

        role = roleRepository.save(role);
        log.info("Created role with ID: {}", role.getId());

        return RoleResponse.fromEntity(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID roleId) {
        log.debug("Getting role by ID: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        return RoleResponse.fromEntity(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleListResponse getRolesByOrganization(UUID organizationId, Pageable pageable) {
        log.debug("Getting roles for organization: {}", organizationId);

        // For now, use findAll and filter
        // TODO: Add proper pagination to RoleRepository
        var roles = roleRepository.findAllNotDeletedByOrganizationId(organizationId).stream()
                .sorted((r1, r2) -> {
                    int levelCompare = r1.getLevel().compareTo(r2.getLevel());
                    if (levelCompare != 0) return levelCompare;
                    return r1.getName().compareTo(r2.getName());
                })
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .map(RoleResponse::fromEntity)
                .toList();

        long totalElements = roleRepository.countNotDeletedByOrganizationId(organizationId);

        return RoleListResponse.builder()
                .roles(roles)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRoleHierarchy(UUID roleId) {
        log.debug("Getting role hierarchy for: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        List<String> hierarchy = new ArrayList<>();
        Role current = role;

        while (current != null) {
            hierarchy.add(0, current.getName());  // Add to front to maintain order
            current = current.getParentRole();
        }

        return hierarchy;
    }

    @Override
    public RoleResponse updateRole(UUID roleId, RoleUpdateRequest request) {
        log.info("Updating role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // Cannot update system roles
        if (role.getIsSystem()) {
            throw new IllegalStateException("Cannot update system role: " + roleId);
        }

        // Update display name if provided
        if (request.getDisplayName() != null) {
            role.setDisplayName(request.getDisplayName());
        }

        // Update description if provided
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        // Update parent role if provided
        if (request.getParentRoleId() != null) {
            Role newParent = roleRepository.findByIdAndNotDeleted(request.getParentRoleId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent role not found: " + request.getParentRoleId()));

            // Validate no circular dependency
            if (wouldCreateCircularDependency(role, newParent)) {
                throw new IllegalStateException("Update would create circular dependency");
            }

            // Validate max depth
            if (newParent.getLevel() + 1 > MAX_HIERARCHY_LEVEL) {
                throw new IllegalStateException("Maximum hierarchy depth would be exceeded");
            }

            role.setParentRole(newParent);
            role.setLevel(newParent.getLevel() + 1);
        }

        // Update metadata if provided
        if (request.getMetadata() != null) {
            role.setMetadata(request.getMetadata());
        }

        role = roleRepository.save(role);
        log.info("Updated role: {}", roleId);

        return RoleResponse.fromEntity(role);
    }

    @Override
    public void deleteRole(UUID roleId) {
        log.info("Deleting role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // Cannot delete system roles
        if (role.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system role: " + roleId);
        }

        // Cannot delete if role has children
        List<Role> children = roleRepository.findByParentRoleIdAndDeletedAtIsNull(roleId);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete role with child roles: " + roleId);
        }

        role.softDelete();
        roleRepository.save(role);

        log.info("Deleted role: {}", roleId);
    }

    /**
     * Check if setting newParent as parent of role would create a circular dependency.
     *
     * @param role the role to update
     * @param newParent the proposed new parent
     * @return true if circular dependency would be created
     */
    private boolean wouldCreateCircularDependency(Role role, Role newParent) {
        // Check if newParent is a descendant of role
        List<Role> descendants = roleRepository.findAllDescendants(role.getId());
        return descendants.stream().anyMatch(d -> d.getId().equals(newParent.getId()));
    }
}
