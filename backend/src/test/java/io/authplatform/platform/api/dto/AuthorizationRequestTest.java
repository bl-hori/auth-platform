package io.authplatform.platform.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuthorizationRequest} DTO validation.
 *
 * <p>Verifies Bean Validation constraints and ensures proper error messages.
 */
@DisplayName("AuthorizationRequest DTO Tests")
class AuthorizationRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should validate a valid authorization request")
    void shouldValidateValidRequest() {
        // Given: Valid authorization request
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(UUID.randomUUID())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .attributes(Map.of("email", "user@example.com"))
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .attributes(Map.of("owner", "user-789"))
                        .build())
                .context(Map.of("ipAddress", "192.168.1.1"))
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest>> violations = validator.validate(request);

        // Then: No violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should reject request with null organization ID")
    void shouldRejectNullOrganizationId() {
        // Given: Request with null organization ID
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(null)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest>> violations = validator.validate(request);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("organizationId");
        assertThat(violation.getMessage()).isEqualTo("Organization ID is required");
    }

    @Test
    @DisplayName("Should reject request with null principal")
    void shouldRejectNullPrincipal() {
        // Given: Request with null principal
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(UUID.randomUUID())
                .principal(null)
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest>> violations = validator.validate(request);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("principal");
        assertThat(violation.getMessage()).isEqualTo("Principal is required");
    }

    @Test
    @DisplayName("Should reject request with blank action")
    void shouldRejectBlankAction() {
        // Given: Request with blank action
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(UUID.randomUUID())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest>> violations = validator.validate(request);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("action");
        assertThat(violation.getMessage()).isEqualTo("Action is required");
    }

    @Test
    @DisplayName("Should reject request with null resource")
    void shouldRejectNullResource() {
        // Given: Request with null resource
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(UUID.randomUUID())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(null)
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest>> violations = validator.validate(request);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("resource");
        assertThat(violation.getMessage()).isEqualTo("Resource is required");
    }

    @Test
    @DisplayName("Should validate principal with blank ID")
    void shouldRejectPrincipalWithBlankId() {
        // Given: Principal with blank ID
        AuthorizationRequest.Principal principal = AuthorizationRequest.Principal.builder()
                .id("")
                .type("user")
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest.Principal>> violations = validator.validate(principal);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest.Principal> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("id");
        assertThat(violation.getMessage()).isEqualTo("Principal ID is required");
    }

    @Test
    @DisplayName("Should reject principal with blank type")
    void shouldRejectPrincipalWithBlankType() {
        // Given: Principal with blank type
        AuthorizationRequest.Principal principal = AuthorizationRequest.Principal.builder()
                .id("user-123")
                .type("")
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest.Principal>> violations = validator.validate(principal);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest.Principal> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("type");
        assertThat(violation.getMessage()).isEqualTo("Principal type is required");
    }

    @Test
    @DisplayName("Should reject resource with blank type")
    void shouldRejectResourceWithBlankType() {
        // Given: Resource with blank type
        AuthorizationRequest.Resource resource = AuthorizationRequest.Resource.builder()
                .type("")
                .id("doc-456")
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest.Resource>> violations = validator.validate(resource);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest.Resource> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("type");
        assertThat(violation.getMessage()).isEqualTo("Resource type is required");
    }

    @Test
    @DisplayName("Should reject resource with blank ID")
    void shouldRejectResourceWithBlankId() {
        // Given: Resource with blank ID
        AuthorizationRequest.Resource resource = AuthorizationRequest.Resource.builder()
                .type("document")
                .id("")
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest.Resource>> violations = validator.validate(resource);

        // Then: Should have violation
        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthorizationRequest.Resource> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("id");
        assertThat(violation.getMessage()).isEqualTo("Resource ID is required");
    }

    @Test
    @DisplayName("Should allow request with null context")
    void shouldAllowNullContext() {
        // Given: Request with null context
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(UUID.randomUUID())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .context(null)
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest>> violations = validator.validate(request);

        // Then: No violations (context is optional)
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should allow principal with null attributes")
    void shouldAllowPrincipalWithNullAttributes() {
        // Given: Principal with null attributes
        AuthorizationRequest.Principal principal = AuthorizationRequest.Principal.builder()
                .id("user-123")
                .type("user")
                .attributes(null)
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest.Principal>> violations = validator.validate(principal);

        // Then: No violations (attributes are optional)
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should allow resource with null attributes")
    void shouldAllowResourceWithNullAttributes() {
        // Given: Resource with null attributes
        AuthorizationRequest.Resource resource = AuthorizationRequest.Resource.builder()
                .type("document")
                .id("doc-456")
                .attributes(null)
                .build();

        // When: Validate
        Set<ConstraintViolation<AuthorizationRequest.Resource>> violations = validator.validate(resource);

        // Then: No violations (attributes are optional)
        assertThat(violations).isEmpty();
    }
}
