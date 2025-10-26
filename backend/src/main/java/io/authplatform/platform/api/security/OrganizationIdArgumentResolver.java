package io.authplatform.platform.api.security;

import io.authplatform.platform.security.ApiKeyAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * Resolves method parameters annotated with {@link CurrentOrganizationId} by extracting
 * the organization ID from the current security context.
 *
 * <p>This resolver:
 * <ul>
 *   <li>Retrieves the {@link ApiKeyAuthenticationToken} from SecurityContext</li>
 *   <li>Extracts the organizationId from the authentication token</li>
 *   <li>Converts the organizationId string to UUID</li>
 *   <li>Injects it into the controller method parameter</li>
 * </ul>
 *
 * <p>Security Benefits:
 * <ul>
 *   <li>Prevents tampering - organizationId comes from authenticated context, not user input</li>
 *   <li>Enforces multi-tenant isolation automatically</li>
 *   <li>Reduces frontend complexity - no need to track/send organizationId</li>
 * </ul>
 *
 * @see CurrentOrganizationId
 * @see ApiKeyAuthenticationToken
 * @since 0.1.0
 */
@Component
@Slf4j
public class OrganizationIdArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Determines if this resolver supports the given method parameter.
     *
     * @param parameter the method parameter to check
     * @return true if the parameter is annotated with @CurrentOrganizationId and is of type UUID
     */
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOrganizationId.class)
                && UUID.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * Resolves the organization ID from the security context.
     *
     * @param parameter the method parameter to resolve
     * @param mavContainer the ModelAndViewContainer for the current request
     * @param webRequest the current request
     * @param binderFactory a factory for creating WebDataBinder instances
     * @return the organization ID as a UUID
     * @throws IllegalStateException if no authentication is present or authentication is not ApiKeyAuthenticationToken
     */
    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in security context");
            throw new IllegalStateException("Authentication required for @CurrentOrganizationId");
        }

        if (!(authentication instanceof ApiKeyAuthenticationToken)) {
            log.error("Authentication is not ApiKeyAuthenticationToken: {}", authentication.getClass().getName());
            throw new IllegalStateException(
                    "Invalid authentication type. Expected ApiKeyAuthenticationToken but got: "
                            + authentication.getClass().getSimpleName());
        }

        ApiKeyAuthenticationToken apiKeyAuth = (ApiKeyAuthenticationToken) authentication;
        String organizationId = apiKeyAuth.getOrganizationId();

        if (organizationId == null || organizationId.isBlank()) {
            log.error("Organization ID is null or empty in authentication token");
            throw new IllegalStateException("Organization ID not found in authentication context");
        }

        try {
            UUID orgUuid = UUID.fromString(organizationId);
            log.debug("Resolved organization ID from security context: {}", orgUuid);
            return orgUuid;
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for organization ID: {}", organizationId, e);
            throw new IllegalStateException("Invalid organization ID format: " + organizationId, e);
        }
    }
}
