package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.UserCreateRequest;
import io.authplatform.platform.api.dto.UserListResponse;
import io.authplatform.platform.api.dto.UserResponse;
import io.authplatform.platform.api.dto.UserUpdateRequest;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.api.dto.UserRoleAssignRequest;
import io.authplatform.platform.api.dto.UserRoleResponse;
import io.authplatform.platform.api.dto.UserUpdateRequest;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Role;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.entity.UserRole;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.RoleRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.domain.repository.UserRoleRepository;
import io.authplatform.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserService} for managing users.
 *
 * <p>This service handles all user-related business logic including validation,
 * uniqueness checks, and cache invalidation.
 *
 * @since 0.1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with email: {} in organization: {}", request.getEmail(), request.getOrganizationId());

        // Validate organization exists
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + request.getOrganizationId()));

        // Check email uniqueness
        if (userRepository.existsByOrganizationIdAndEmailAndDeletedAtIsNull(
                request.getOrganizationId(), request.getEmail())) {
            throw new IllegalStateException("User with email already exists: " + request.getEmail());
        }

        // Check username uniqueness if provided
        if (request.getUsername() != null &&
                userRepository.existsByOrganizationIdAndUsernameAndDeletedAtIsNull(
                        request.getOrganizationId(), request.getUsername())) {
            throw new IllegalStateException("User with username already exists: " + request.getUsername());
        }

        // Create user entity
        User user = User.builder()
                .organization(organization)
                .email(request.getEmail())
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .externalId(request.getExternalId())
                .status(User.UserStatus.ACTIVE)
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();

        user = userRepository.save(user);
        log.info("Created user with ID: {}", user.getId());

        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("Getting user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResponse getUsersByOrganization(
            UUID organizationId,
            String search,
            String status,
            Pageable pageable) {
        log.debug("Getting users for organization: {}, search: {}, status: {}",
                organizationId, search, status);

        // For now, use findAll and filter in memory
        // TODO: Add proper pagination with search and status filtering to UserRepository
        Page<User> userPage = userRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
                )
        );

        // Filter by organization and non-deleted
        var users = userPage.getContent().stream()
                .filter(u -> u.getOrganization().getId().equals(organizationId))
                .filter(u -> !u.isDeleted())
                .filter(u -> search == null || matchesSearch(u, search))
                .filter(u -> status == null || u.getStatus().getValue().equals(status))
                .map(UserResponse::fromEntity)
                .toList();

        long totalElements = userRepository.countNotDeletedByOrganizationId(organizationId);

        return UserListResponse.builder()
                .content(users)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .build();
    }

    @Override
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByOrganizationIdAndEmailAndDeletedAtIsNull(
                    user.getOrganization().getId(), request.getEmail())) {
                throw new IllegalStateException("User with email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // Update username if provided and different
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByOrganizationIdAndUsernameAndDeletedAtIsNull(
                    user.getOrganization().getId(), request.getUsername())) {
                throw new IllegalStateException("User with username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        // Update other fields if provided
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request.getExternalId() != null) {
            user.setExternalId(request.getExternalId());
        }

        if (request.getStatus() != null) {
            user.setStatus(User.UserStatus.valueOf(request.getStatus().toUpperCase()));
        }

        if (request.getAttributes() != null) {
            user.setAttributes(request.getAttributes());
        }

        user = userRepository.save(user);
        log.info("Updated user: {}", userId);

        return UserResponse.fromEntity(user);
    }

    @Override
    public void deactivateUser(UUID userId) {
        log.info("Deactivating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.isDeleted()) {
            throw new IllegalStateException("User is already deleted: " + userId);
        }

        user.softDelete();
        userRepository.save(user);

        log.info("Deactivated user: {}", userId);
    }

    @Override
    public void activateUser(UUID userId) {
        log.info("Activating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.isDeleted()) {
            throw new IllegalStateException("User is not deleted: " + userId);
        }

        user.restore();
        userRepository.save(user);

        log.info("Activated user: {}", userId);
    }

    @Override
    public UserRoleResponse assignRole(UUID userId, UserRoleAssignRequest request) {
        log.info("Assigning role {} to user {} with resource: {}",
                request.getRoleId(), userId, request.getResourceId());

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate role exists
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRoleId()));

        // Validate user and role are in the same organization
        if (!user.getOrganization().getId().equals(role.getOrganization().getId())) {
            throw new IllegalStateException(
                    "User and role must be in the same organization. "
                            + "User org: " + user.getOrganization().getId()
                            + ", Role org: " + role.getOrganization().getId());
        }

        // Check for duplicate assignment
        boolean alreadyAssigned = userRoleRepository
                .findByUserId(userId)
                .stream()
                .anyMatch(ur -> ur.getRole().getId().equals(request.getRoleId())
                        && java.util.Objects.equals(ur.getResourceId(), request.getResourceId()));

        if (alreadyAssigned) {
            String resourceInfo = request.getResourceId() != null
                    ? " for resource: " + request.getResourceId()
                    : " (global)";
            throw new IllegalStateException(
                    "Role " + role.getName() + " is already assigned to user" + resourceInfo);
        }

        // Create user-role assignment
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .resourceId(request.getResourceId())
                .expiresAt(request.getExpiresAt())
                .grantedBy(null) // TODO: Get from security context
                .build();

        UserRole savedUserRole = userRoleRepository.save(userRole);

        log.info("Successfully assigned role {} to user {}", request.getRoleId(), userId);

        return UserRoleResponse.fromEntity(savedUserRole);
    }

    @Override
    public void removeRole(UUID userId, UUID roleId) {
        log.info("Removing role {} from user {}", roleId, userId);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate role exists
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // Find all user-role assignments for this user and role (may be multiple with different resource scopes)
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId)
                .stream()
                .filter(ur -> ur.getRole().getId().equals(roleId))
                .collect(Collectors.toList());

        if (userRoles.isEmpty()) {
            throw new IllegalStateException(
                    "Role " + role.getName() + " is not assigned to user");
        }

        // Delete all matching user-role assignments (handles multiple resource scopes)
        userRoleRepository.deleteAll(userRoles);

        log.info("Successfully removed {} role assignment(s) for role {} from user {}",
                userRoles.size(), roleId, userId);
    }

    @Override
    public List<UserRoleResponse> getUserRoles(UUID userId) {
        log.debug("Getting roles for user: {}", userId);

        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        // Get all active user roles (non-expired)
        OffsetDateTime now = OffsetDateTime.now();
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId)
                .stream()
                .filter(ur -> ur.getExpiresAt() == null || ur.getExpiresAt().isAfter(now))
                .collect(Collectors.toList());

        return userRoles.stream()
                .map(UserRoleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Check if user matches search query (case-insensitive).
     *
     * @param user the user to check
     * @param search the search query
     * @return true if user matches search
     */
    private boolean matchesSearch(User user, String search) {
        String searchLower = search.toLowerCase();
        return (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower)) ||
                (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchLower)) ||
                (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(searchLower));
    }

    // ===== Keycloak Integration (Phase 2) =====

    @Override
    @Transactional
    public User findOrCreateFromJwt(String keycloakSub, String email, String organizationId) {
        log.info("Finding or creating user from JWT: keycloakSub={}, email={}, organizationId={}",
                keycloakSub, email, organizationId);

        // 1. Try to find user by keycloak_sub (fastest, indexed)
        java.util.Optional<User> userByKeycloakSub = userRepository.findByKeycloakSubAndDeletedAtIsNull(keycloakSub);
        if (userByKeycloakSub.isPresent()) {
            User user = userByKeycloakSub.get();
            log.debug("User found by keycloak_sub: userId={}", user.getId());

            // Update sync timestamp
            user.setKeycloakSyncedAt(java.time.OffsetDateTime.now());
            userRepository.save(user);

            return user;
        }

        // 2. Try to find user by email (for linking existing users)
        java.util.Optional<User> userByEmail = userRepository.findByEmailAndDeletedAtIsNull(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            log.info("Linking existing user to Keycloak: userId={}, email={}", user.getId(), email);

            // Link user to Keycloak
            user.setKeycloakSub(keycloakSub);
            user.setKeycloakSyncedAt(java.time.OffsetDateTime.now());
            userRepository.save(user);

            return user;
        }

        // 3. Create new user (JIT Provisioning)
        log.info("Creating new user from JWT: email={}, organizationId={}", email, organizationId);

        // Validate organization exists
        UUID orgUuid;
        try {
            orgUuid = UUID.fromString(organizationId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid organization ID format: " + organizationId, e);
        }

        Organization organization = organizationRepository.findById(orgUuid)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + organizationId));

        // Create new user
        User newUser = User.builder()
                .organization(organization)
                .email(email)
                .displayName(email) // Use email as default display name
                .keycloakSub(keycloakSub)
                .keycloakSyncedAt(java.time.OffsetDateTime.now())
                .status(User.UserStatus.ACTIVE)
                .attributes(Map.of())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New user created via JIT provisioning: userId={}, email={}",
                savedUser.getId(), savedUser.getEmail());

        return savedUser;
    }
}
