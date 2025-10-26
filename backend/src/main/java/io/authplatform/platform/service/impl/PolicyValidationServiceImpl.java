package io.authplatform.platform.service.impl;

import io.authplatform.platform.config.OpaProperties;
import io.authplatform.platform.service.PolicyValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of PolicyValidationService for validating Rego and Cedar policies.
 *
 * <p>This service uses OPA's compile API to validate Rego policy syntax and semantics.
 * It also performs security checks to detect forbidden imports that could pose security risks.
 *
 * <p><strong>Validation Process:</strong>
 * <ol>
 *   <li>Basic syntax validation using regex patterns</li>
 *   <li>Forbidden import detection</li>
 *   <li>OPA compilation via REST API (if available)</li>
 *   <li>Semantic validation (undefined variables, type errors)</li>
 * </ol>
 *
 * @see PolicyValidationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyValidationServiceImpl implements PolicyValidationService {

    private final WebClient.Builder webClientBuilder;
    private final OpaProperties opaProperties;

    /**
     * Forbidden Rego imports that pose security risks.
     */
    private static final Set<String> FORBIDDEN_IMPORTS = Set.of(
            "http.send",          // External HTTP calls
            "net.lookup_ip_addr", // DNS lookups
            "net.cidr_contains",  // May be ok, but can be misused
            "time.now_ns"         // Non-deterministic time (use input.context.timestamp)
    );

    /**
     * Regex pattern to extract package name from Rego code.
     * Example: "package authz.rbac" -> "authz.rbac"
     */
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "^\\s*package\\s+([a-zA-Z][a-zA-Z0-9._]*)",
            Pattern.MULTILINE
    );

    /**
     * Regex pattern to detect import statements in Rego code.
     * Example: "import data.roles" or "import future.keywords.if"
     */
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^\\s*import\\s+([a-zA-Z][a-zA-Z0-9._]*)",
            Pattern.MULTILINE
    );

    @Override
    public PolicyValidationResult validateRegoPolicy(String regoCode) {
        if (regoCode == null || regoCode.isBlank()) {
            return PolicyValidationResult.failure(Map.of(
                    "error", "Policy code cannot be null or empty"
            ));
        }

        log.debug("Validating Rego policy, length: {} bytes", regoCode.length());

        // Step 1: Check for forbidden imports
        if (containsForbiddenImports(regoCode)) {
            List<String> detectedImports = detectForbiddenImports(regoCode);
            log.warn("Forbidden imports detected in Rego policy: {}", detectedImports);
            return PolicyValidationResult.failure(Map.of(
                    "error", "Forbidden imports detected",
                    "forbidden_imports", detectedImports,
                    "message", "The following imports are not allowed for security reasons: " + String.join(", ", detectedImports)
            ));
        }

        // Step 2: Check for package declaration
        String packageName = extractPackageName(regoCode);
        if (packageName == null || packageName.isBlank()) {
            log.warn("Rego policy missing package declaration");
            return PolicyValidationResult.failure(Map.of(
                    "error", "Missing package declaration",
                    "message", "Rego policies must start with a package declaration (e.g., 'package authz')"
            ));
        }

        // Step 3: Validate with OPA compile API
        try {
            Map<String, Object> opaValidation = validateWithOpa(regoCode);
            if (opaValidation.containsKey("errors")) {
                log.warn("OPA validation failed: {}", opaValidation.get("errors"));
                return PolicyValidationResult.failure(Map.of(
                        "error", "OPA compilation failed",
                        "opa_errors", opaValidation.get("errors"),
                        "message", "Policy contains syntax or semantic errors"
                ));
            }
            log.debug("OPA validation passed for package: {}", packageName);
            return PolicyValidationResult.success();
        } catch (Exception e) {
            // Fallback: If OPA is not available, do basic validation
            log.warn("OPA validation failed, using fallback validation: {}", e.getMessage());
            return performBasicRegoValidation(regoCode, packageName);
        }
    }

    @Override
    public PolicyValidationResult validateCedarPolicy(String cedarCode) {
        // Cedar support is deferred to Phase 2
        throw new UnsupportedOperationException(
                "Cedar policy validation is not supported in Phase 1 MVP. " +
                "Only Rego (OPA) policies are supported."
        );
    }

    @Override
    public boolean containsForbiddenImports(String regoCode) {
        return !detectForbiddenImports(regoCode).isEmpty();
    }

    @Override
    public String extractPackageName(String regoCode) {
        Matcher matcher = PACKAGE_PATTERN.matcher(regoCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Detect forbidden imports in Rego code.
     *
     * @param regoCode the Rego code to check
     * @return list of detected forbidden imports
     */
    private List<String> detectForbiddenImports(String regoCode) {
        List<String> detected = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(regoCode);

        while (matcher.find()) {
            String importPath = matcher.group(1);
            for (String forbidden : FORBIDDEN_IMPORTS) {
                if (importPath.equals(forbidden) || importPath.startsWith(forbidden + ".")) {
                    detected.add(importPath);
                }
            }
        }

        // Also check for direct usage without import (e.g., http.send(...))
        for (String forbidden : FORBIDDEN_IMPORTS) {
            if (regoCode.contains(forbidden + "(")) {
                if (!detected.contains(forbidden)) {
                    detected.add(forbidden);
                }
            }
        }

        return detected;
    }

    /**
     * Validate Rego code using OPA's compile API.
     *
     * <p>Sends the policy to OPA's /v1/compile endpoint for validation.
     *
     * @param regoCode the Rego code to validate
     * @return validation result from OPA
     * @throws RuntimeException if OPA communication fails
     */
    private Map<String, Object> validateWithOpa(String regoCode) {
        WebClient webClient = webClientBuilder
                .baseUrl(opaProperties.getBaseUrl())
                .build();

        // OPA compile API request format
        Map<String, Object> compileRequest = Map.of(
                "query", "data",
                "input", Map.of(),
                "unknowns", List.of(),
                "modules", Map.of(
                        "policy.rego", regoCode
                )
        );

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/v1/compile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(compileRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return response != null ? response : Map.of();

        } catch (WebClientResponseException e) {
            log.error("OPA compile API error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            // Parse OPA error response
            String errorBody = e.getResponseBodyAsString();
            if (errorBody != null && errorBody.contains("error")) {
                return Map.of("errors", List.of(Map.of(
                        "message", "OPA compilation failed: " + errorBody
                )));
            }
            throw new RuntimeException("OPA validation failed", e);

        } catch (Exception e) {
            log.error("Failed to communicate with OPA for validation", e);
            throw new RuntimeException("OPA communication error", e);
        }
    }

    /**
     * Perform basic Rego validation without OPA (fallback).
     *
     * <p>This is a simple regex-based validation used when OPA is not available.
     *
     * @param regoCode the Rego code
     * @param packageName the extracted package name
     * @return validation result
     */
    private PolicyValidationResult performBasicRegoValidation(String regoCode, String packageName) {
        List<String> errors = new ArrayList<>();

        // Check for basic Rego syntax elements
        if (!regoCode.contains("package ")) {
            errors.add("Missing package declaration");
        }

        // Check for unmatched braces
        long openBraces = regoCode.chars().filter(ch -> ch == '{').count();
        long closeBraces = regoCode.chars().filter(ch -> ch == '}').count();
        if (openBraces != closeBraces) {
            errors.add("Unmatched braces: " + openBraces + " open, " + closeBraces + " close");
        }

        // Check for unmatched brackets
        long openBrackets = regoCode.chars().filter(ch -> ch == '[').count();
        long closeBrackets = regoCode.chars().filter(ch -> ch == ']').count();
        if (openBrackets != closeBrackets) {
            errors.add("Unmatched brackets: " + openBrackets + " open, " + closeBrackets + " close");
        }

        // Check for unmatched parentheses
        long openParens = regoCode.chars().filter(ch -> ch == '(').count();
        long closeParens = regoCode.chars().filter(ch -> ch == ')').count();
        if (openParens != closeParens) {
            errors.add("Unmatched parentheses: " + openParens + " open, " + closeParens + " close");
        }

        if (!errors.isEmpty()) {
            return PolicyValidationResult.failure(Map.of(
                    "error", "Basic syntax validation failed",
                    "errors", errors,
                    "message", "Policy contains syntax errors: " + String.join(", ", errors),
                    "note", "OPA server validation was not available, using basic validation"
            ));
        }

        log.debug("Basic Rego validation passed (OPA not available)");
        return PolicyValidationResult.success();
    }
}
