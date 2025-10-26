package io.authplatform.platform.audit;

import io.authplatform.platform.api.security.CurrentOrganizationId;
import io.authplatform.platform.security.ApiKeyAuthenticationToken;
import io.authplatform.platform.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

/**
 * Aspect for automatic audit logging of administrative actions.
 *
 * <p>This aspect intercepts methods annotated with {@link Audited} and automatically
 * logs the operation to the audit log with:
 * <ul>
 *   <li>Organization ID from security context</li>
 *   <li>User ID (if available)</li>
 *   <li>Action performed</li>
 *   <li>Resource type and ID</li>
 *   <li>Success/failure status</li>
 *   <li>IP address and user agent</li>
 * </ul>
 *
 * @see Audited
 * @see AuditLogService
 * @since 0.1.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * Intercepts methods annotated with @Audited and logs the operation.
     *
     * @param joinPoint the join point
     * @param audited the @Audited annotation
     * @return the method return value
     * @throws Throwable if the method throws an exception
     */
    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Extract context information
        UUID organizationId = extractOrganizationId(joinPoint);
        UUID userId = extractUserId();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = extractIpAddress(request);
        String userAgent = extractUserAgent(request);

        Object result = null;
        String decision = "SUCCESS";
        String decisionReason = null;

        try {
            // Execute the method
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            decision = "FAILURE";
            decisionReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw e;
        } finally {
            try {
                // Extract resource ID
                String resourceId = extractResourceId(joinPoint, audited, result);

                // Log execution time
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Audited action: {}:{} by user {} - {} ({}ms)",
                        audited.resourceType(), audited.action(), userId, decision, executionTime);

                // Async audit logging
                auditLogService.logAdministrativeAction(
                        organizationId,
                        userId,
                        audited.resourceType(),
                        resourceId,
                        audited.action(),
                        decision,
                        decisionReason,
                        ipAddress,
                        userAgent
                );
            } catch (Exception e) {
                log.error("Failed to log audit event", e);
                // Don't fail the operation if audit logging fails
            }
        }
    }

    /**
     * Extracts organization ID from method parameters or security context.
     */
    private UUID extractOrganizationId(ProceedingJoinPoint joinPoint) {
        // First, try to find @CurrentOrganizationId parameter
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CurrentOrganizationId.class)) {
                return (UUID) args[i];
            }
        }

        // Fallback: extract from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthenticationToken) {
            String orgId = ((ApiKeyAuthenticationToken) auth).getOrganizationId();
            return UUID.fromString(orgId);
        }

        return null;
    }

    /**
     * Extracts user ID from security context (if available).
     */
    private UUID extractUserId() {
        // Currently, API key authentication doesn't include user ID
        // This will be populated when JWT authentication is implemented
        return null;
    }

    /**
     * Extracts resource ID using SpEL expression.
     */
    private String extractResourceId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        if (audited.resourceIdExpression().isEmpty()) {
            return null;
        }

        try {
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Add method parameters to context
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            // Add result to context
            context.setVariable("result", result);

            // Evaluate expression
            Expression expression = expressionParser.parseExpression(audited.resourceIdExpression());
            Object value = expression.getValue(context);

            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to evaluate resourceId expression: {}", audited.resourceIdExpression(), e);
            return null;
        }
    }

    /**
     * Gets the current HTTP request.
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Extracts IP address from request.
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // Check for proxy headers
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Take first IP if multiple (proxy chain)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Extracts user agent from request.
     */
    private String extractUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : null;
    }
}
