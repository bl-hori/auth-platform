package io.authplatform.platform.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited.
 *
 * <p>When applied to a method, the {@link AuditAspect} will automatically log:
 * <ul>
 *   <li>Method execution (success/failure)</li>
 *   <li>User who performed the action</li>
 *   <li>Resource type and ID</li>
 *   <li>Action performed</li>
 *   <li>Execution time</li>
 *   <li>Any errors that occurred</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Audited(
 *     action = "CREATE_POLICY",
 *     resourceType = "Policy"
 * )
 * public PolicyResponse createPolicy(PolicyCreateRequest request) {
 *     // Implementation
 * }
 * }</pre>
 *
 * @see AuditAspect
 * @since 0.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The action being performed (e.g., "CREATE_USER", "UPDATE_POLICY").
     *
     * @return the action name
     */
    String action();

    /**
     * The type of resource being acted upon (e.g., "User", "Policy", "Role").
     *
     * @return the resource type
     */
    String resourceType();

    /**
     * SpEL expression to extract the resource ID from method parameters or return value.
     * <p>Examples:
     * <ul>
     *   <li>{@code "#result.id"} - Extract ID from returned object</li>
     *   <li>{@code "#id"} - Use the 'id' parameter</li>
     *   <li>{@code "#request.userId"} - Extract from nested property</li>
     * </ul>
     *
     * @return SpEL expression for resource ID, or empty string if not applicable
     */
    String resourceIdExpression() default "";

    /**
     * Whether to log the request details (parameters).
     *
     * @return true to log request details, false otherwise
     */
    boolean logRequest() default true;

    /**
     * Whether to log the response details (return value).
     *
     * @return true to log response details, false otherwise
     */
    boolean logResponse() default false;
}
