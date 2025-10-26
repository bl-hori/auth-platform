package io.authplatform.platform.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically inject the current organization ID from the security context.
 *
 * <p>This annotation should be used on UUID parameters in controller methods to automatically
 * extract the organization ID from the authenticated {@link io.authplatform.platform.security.ApiKeyAuthenticationToken}.
 *
 * <p>The organization ID is obtained from the security context, ensuring that:
 * <ul>
 *   <li>Multi-tenant isolation is enforced (users can only access their organization's data)</li>
 *   <li>Frontend doesn't need to explicitly send organizationId in every request</li>
 *   <li>Reduced risk of authorization bypass (user cannot manipulate organizationId)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @GetMapping("/v1/users")
 * public UserListResponse listUsers(
 *     @CurrentOrganizationId UUID organizationId,
 *     @RequestParam(required = false) String search) {
 *     // organizationId is automatically populated from the security context
 *     return userService.listUsers(organizationId, search);
 * }
 * }</pre>
 *
 * @see io.authplatform.platform.security.ApiKeyAuthenticationToken
 * @since 0.1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentOrganizationId {
}
