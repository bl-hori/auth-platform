package io.authplatform.platform.domain.listener;

import io.authplatform.platform.domain.entity.*;
import io.authplatform.platform.service.AuthorizationCacheService;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Entity Listener for automatic cache invalidation.
 *
 * <p>This listener automatically invalidates authorization caches when:
 * <ul>
 *   <li>User roles are added/removed/updated (UserRole entity)</li>
 *   <li>Role permissions are added/removed/updated (RolePermission entity)</li>
 *   <li>Policies are updated/deleted (Policy entity)</li>
 *   <li>Users are updated/deleted (User entity)</li>
 * </ul>
 *
 * <p><strong>Cache Invalidation Strategy:</strong>
 * <ul>
 *   <li><strong>UserRole changes:</strong> Invalidate cache for the affected user</li>
 *   <li><strong>RolePermission changes:</strong> Invalidate cache for all users with that role</li>
 *   <li><strong>Policy changes:</strong> Invalidate cache for the entire organization</li>
 *   <li><strong>User changes:</strong> Invalidate cache for that user</li>
 * </ul>
 */
@Slf4j
public class CacheInvalidationListener {

    private static AuthorizationCacheService cacheService;

    /**
     * Set the cache service (called by Spring during initialization).
     *
     * <p>Note: We use static setter injection here because JPA entity listeners
     * are instantiated by JPA, not Spring, so we need to set the dependency
     * statically through a Spring component.
     *
     * @param authorizationCacheService the cache service
     */
    public static void setCacheService(AuthorizationCacheService authorizationCacheService) {
        CacheInvalidationListener.cacheService = authorizationCacheService;
    }

    /**
     * Invalidate cache when a user role is updated.
     *
     * @param userRole the updated user role
     */
    @PostUpdate
    public void onUserRoleUpdate(UserRole userRole) {
        if (cacheService != null && userRole.getUser() != null) {
            log.info("UserRole updated: userId={}, roleId={}",
                    userRole.getUser().getId(), userRole.getRole().getId());

            cacheService.invalidate(
                    userRole.getUser().getOrganization().getId(),
                    userRole.getUser().getExternalId()
            );
        }
    }

    /**
     * Invalidate cache when a user role is deleted.
     *
     * @param userRole the deleted user role
     */
    @PostRemove
    public void onUserRoleRemove(UserRole userRole) {
        if (cacheService != null && userRole.getUser() != null) {
            log.info("UserRole removed: userId={}, roleId={}",
                    userRole.getUser().getId(), userRole.getRole().getId());

            cacheService.invalidate(
                    userRole.getUser().getOrganization().getId(),
                    userRole.getUser().getExternalId()
            );
        }
    }

    /**
     * Invalidate cache when a role permission is updated.
     *
     * <p>This invalidates cache for the entire organization because we don't know
     * which users have this role without additional queries.
     *
     * @param rolePermission the updated role permission
     */
    @PostUpdate
    public void onRolePermissionUpdate(RolePermission rolePermission) {
        if (cacheService != null && rolePermission.getRole() != null) {
            log.info("RolePermission updated: roleId={}, permissionId={}",
                    rolePermission.getRole().getId(), rolePermission.getPermission().getId());

            // Invalidate entire organization cache
            cacheService.invalidateOrganization(
                    rolePermission.getRole().getOrganization().getId()
            );
        }
    }

    /**
     * Invalidate cache when a role permission is deleted.
     *
     * @param rolePermission the deleted role permission
     */
    @PostRemove
    public void onRolePermissionRemove(RolePermission rolePermission) {
        if (cacheService != null && rolePermission.getRole() != null) {
            log.info("RolePermission removed: roleId={}, permissionId={}",
                    rolePermission.getRole().getId(), rolePermission.getPermission().getId());

            // Invalidate entire organization cache
            cacheService.invalidateOrganization(
                    rolePermission.getRole().getOrganization().getId()
            );
        }
    }

    /**
     * Invalidate cache when a policy is updated.
     *
     * @param policy the updated policy
     */
    @PostUpdate
    public void onPolicyUpdate(Policy policy) {
        if (cacheService != null && policy.getOrganization() != null) {
            log.info("Policy updated: policyId={}, name={}", policy.getId(), policy.getName());

            // Invalidate entire organization cache
            cacheService.invalidateOrganization(policy.getOrganization().getId());
        }
    }

    /**
     * Invalidate cache when a policy is deleted.
     *
     * @param policy the deleted policy
     */
    @PostRemove
    public void onPolicyRemove(Policy policy) {
        if (cacheService != null && policy.getOrganization() != null) {
            log.info("Policy removed: policyId={}, name={}", policy.getId(), policy.getName());

            // Invalidate entire organization cache
            cacheService.invalidateOrganization(policy.getOrganization().getId());
        }
    }

    /**
     * Invalidate cache when a user is updated.
     *
     * @param user the updated user
     */
    @PostUpdate
    public void onUserUpdate(User user) {
        if (cacheService != null && user.getOrganization() != null) {
            log.info("User updated: userId={}, externalId={}", user.getId(), user.getExternalId());

            cacheService.invalidate(
                    user.getOrganization().getId(),
                    user.getExternalId()
            );
        }
    }

    /**
     * Invalidate cache when a user is deleted.
     *
     * @param user the deleted user
     */
    @PostRemove
    public void onUserRemove(User user) {
        if (cacheService != null && user.getOrganization() != null) {
            log.info("User removed: userId={}, externalId={}", user.getId(), user.getExternalId());

            cacheService.invalidate(
                    user.getOrganization().getId(),
                    user.getExternalId()
            );
        }
    }
}
