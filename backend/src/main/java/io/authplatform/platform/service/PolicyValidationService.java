package io.authplatform.platform.service;

import java.util.Map;

/**
 * Service interface for policy validation.
 *
 * <p>Validates policy code (Rego or Cedar) for syntax errors, semantic errors,
 * and security issues such as forbidden imports.
 *
 * <p><strong>Validation Levels:</strong>
 * <ul>
 *   <li><strong>Syntax Validation:</strong> Checks if the policy code is syntactically correct</li>
 *   <li><strong>Semantic Validation:</strong> Checks for logical errors and undefined references</li>
 *   <li><strong>Security Validation:</strong> Detects forbidden imports and dangerous operations</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * @Autowired
 * private PolicyValidationService validationService;
 *
 * String regoCode = "package authz\ndefault allow = false\nallow { ... }";
 *
 * PolicyValidationResult result = validationService.validateRegoPolicy(regoCode);
 * if (!result.isValid()) {
 *     System.out.println("Validation errors: " + result.getErrors());
 * }
 * }</pre>
 *
 * @see io.authplatform.platform.service.impl.PolicyValidationServiceImpl
 */
public interface PolicyValidationService {

    /**
     * Validation result containing status and errors.
     *
     * @param valid true if validation passed, false otherwise
     * @param errors Map containing error details (null if valid)
     */
    record PolicyValidationResult(boolean valid, Map<String, Object> errors) {
        /**
         * Create a successful validation result.
         *
         * @return successful validation result
         */
        public static PolicyValidationResult success() {
            return new PolicyValidationResult(true, null);
        }

        /**
         * Create a failed validation result with errors.
         *
         * @param errors the validation errors
         * @return failed validation result
         */
        public static PolicyValidationResult failure(Map<String, Object> errors) {
            return new PolicyValidationResult(false, errors);
        }
    }

    /**
     * Validate a Rego policy for syntax, semantics, and security issues.
     *
     * <p>Validation includes:
     * <ul>
     *   <li>Syntax checking (valid Rego code)</li>
     *   <li>Forbidden import detection (http.send, net.*)</li>
     *   <li>Basic semantic validation</li>
     * </ul>
     *
     * @param regoCode the Rego policy code to validate
     * @return validation result containing status and any errors
     * @throws IllegalArgumentException if regoCode is null or empty
     */
    PolicyValidationResult validateRegoPolicy(String regoCode);

    /**
     * Validate a Cedar policy for syntax and semantic errors.
     *
     * <p>Note: Cedar support is planned for Phase 2.
     *
     * @param cedarCode the Cedar policy code to validate
     * @return validation result containing status and any errors
     * @throws UnsupportedOperationException in current MVP (Phase 1)
     */
    PolicyValidationResult validateCedarPolicy(String cedarCode);

    /**
     * Detect forbidden imports in Rego code.
     *
     * <p><strong>Forbidden imports include:</strong>
     * <ul>
     *   <li>http.send - External HTTP calls (security risk)</li>
     *   <li>net.* - Network operations (security risk)</li>
     *   <li>time.now_ns - Non-deterministic time functions (use input.context.timestamp instead)</li>
     * </ul>
     *
     * @param regoCode the Rego code to check
     * @return true if forbidden imports are detected, false otherwise
     */
    boolean containsForbiddenImports(String regoCode);

    /**
     * Extract package name from Rego code.
     *
     * <p>Example: For "package authz.rbac", returns "authz.rbac"
     *
     * @param regoCode the Rego code
     * @return the package name, or null if not found
     */
    String extractPackageName(String regoCode);
}
