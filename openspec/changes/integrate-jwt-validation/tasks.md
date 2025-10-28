# Implementation Tasks: integrate-jwt-validation

## Overview

This document outlines the implementation tasks for Phase 2: JWT Validation Integration. Tasks are organized into logical sections and designed to be completed incrementally with validation at each step.

**Estimated Timeline**: 2-3 weeks
**Total Tasks**: 98

---

## Section 1: Dependencies and Configuration (8 tasks)

### 1.1 Add Spring Security OAuth2 Dependencies

- [ ] 1.1.1 Add `spring-boot-starter-oauth2-resource-server` to `backend/build.gradle`
- [ ] 1.1.2 Add `nimbus-jose-jwt` version 9.37.3 to `backend/build.gradle`
- [ ] 1.1.3 Verify dependencies resolve without conflicts
- [ ] 1.1.4 Update `backend/build.gradle` comments to remove Phase 2 TODO notes

### 1.2 Configuration Properties

- [ ] 1.2.1 Uncomment Keycloak configuration in `backend/src/main/resources/application.yml`
- [ ] 1.2.2 Set `authplatform.keycloak.enabled=true`
- [ ] 1.2.3 Create `KeycloakProperties.java` configuration class with `@ConfigurationProperties`
- [ ] 1.2.4 Add validation annotations to KeycloakProperties (@NotNull, @NotEmpty)

---

## Section 2: Database Schema Migration (6 tasks)

### 2.1 Users Table Extension

- [ ] 2.1.1 Create Flyway migration `V1.X__add_keycloak_fields_to_users.sql`
- [ ] 2.1.2 Add `keycloak_sub VARCHAR(255) UNIQUE` column
- [ ] 2.1.3 Add `keycloak_synced_at TIMESTAMP` column
- [ ] 2.1.4 Create index `idx_users_keycloak_sub` on `keycloak_sub`
- [ ] 2.1.5 Add column comments for documentation
- [ ] 2.1.6 Test migration on local PostgreSQL

---

## Section 3: JWT Decoder Configuration (7 tasks)

### 3.1 JwtDecoder Bean

- [ ] 3.1.1 Create `JwtDecoderConfig.java` configuration class
- [ ] 3.1.2 Implement `JwtDecoder` bean using `NimbusJwtDecoder.withJwkSetUri()`
- [ ] 3.1.3 Configure JWK Set URI from KeycloakProperties
- [ ] 3.1.4 Add JWT timestamp validator
- [ ] 3.1.5 Add JWT issuer validator (verify issuer matches Keycloak realm)
- [ ] 3.1.6 Add JWT audience validator (verify "auth-platform-backend" in aud claim)
- [ ] 3.1.7 Add comprehensive Javadoc to JwtDecoderConfig

---

## Section 4: JWT Authentication Filter (12 tasks)

### 4.1 JwtAuthenticationFilter Implementation

- [ ] 4.1.1 Create `JwtAuthenticationFilter.java` extending `OncePerRequestFilter`
- [ ] 4.1.2 Implement `extractJwtFromHeader()` method (extract Bearer token)
- [ ] 4.1.3 Implement JWT validation using `JwtDecoder.decode()`
- [ ] 4.1.4 Extract claims: sub, email, organization_id, roles
- [ ] 4.1.5 Handle JWT validation exceptions (JwtException, BadJwtException)
- [ ] 4.1.6 Return 401 Unauthorized with clear error messages on validation failure
- [ ] 4.1.7 Skip JWT authentication if no Authorization header present (allow API Key fallback)
- [ ] 4.1.8 Add SLF4J logging for authentication events (success, failure, errors)

### 4.2 JwtAuthenticationToken

- [ ] 4.2.1 Create `JwtAuthenticationToken.java` extending `AbstractAuthenticationToken`
- [ ] 4.2.2 Store userId, organizationId, and Jwt object
- [ ] 4.2.3 Implement `getCredentials()` returning the Jwt
- [ ] 4.2.4 Implement `getPrincipal()` returning the userId

---

## Section 5: User Service Extensions (10 tasks)

### 5.1 UserRepository Updates

- [ ] 5.1.1 Add `findByKeycloakSub(String keycloakSub)` query method
- [ ] 5.1.2 Add `findByEmail(String email)` query method (if not exists)
- [ ] 5.1.3 Add custom query with proper organization isolation

### 5.2 UserService JWT Methods

- [ ] 5.2.1 Implement `findOrCreateFromJwt(String keycloakSub, String email, String organizationId)`
- [ ] 5.2.2 Logic: Search by keycloak_sub first
- [ ] 5.2.3 Logic: If not found, search by email and link to Keycloak (update keycloak_sub)
- [ ] 5.2.4 Logic: If not found, create new user with JIT provisioning
- [ ] 5.2.5 Validate organization exists before creating user
- [ ] 5.2.6 Set keycloak_synced_at timestamp on create/update
- [ ] 5.2.7 Add comprehensive Javadoc with examples

---

## Section 6: Security Configuration Updates (8 tasks)

### 6.1 SecurityConfig Integration

- [ ] 6.1.1 Inject JwtDecoder, UserService, KeycloakProperties into SecurityConfig
- [ ] 6.1.2 Create `jwtAuthenticationFilter()` bean
- [ ] 6.1.3 Add JwtAuthenticationFilter to filter chain AFTER RateLimitFilter
- [ ] 6.1.4 Add JwtAuthenticationFilter BEFORE ApiKeyAuthenticationFilter
- [ ] 6.1.5 Ensure filter order: RateLimit → JWT → API Key
- [ ] 6.1.6 Update SecurityConfig Javadoc to document JWT authentication
- [ ] 6.1.7 Add conditional JWT filter registration (if keycloak.enabled=true)
- [ ] 6.1.8 Verify API Key authentication still works (backward compatibility)

---

## Section 7: Unit Tests (15 tasks)

### 7.1 JwtAuthenticationFilter Tests

- [ ] 7.1.1 Test: Valid JWT authentication succeeds
- [ ] 7.1.2 Test: Invalid JWT signature returns 401
- [ ] 7.1.3 Test: Expired JWT returns 401
- [ ] 7.1.4 Test: JWT without Authorization header skips filter
- [ ] 7.1.5 Test: JWT with malformed Authorization header returns 401
- [ ] 7.1.6 Test: SecurityContext is set correctly with userId and organizationId

### 7.2 UserService Tests

- [ ] 7.2.1 Test: findOrCreateFromJwt creates new user when not exists
- [ ] 7.2.2 Test: findOrCreateFromJwt finds existing user by keycloak_sub
- [ ] 7.2.3 Test: findOrCreateFromJwt links existing user by email
- [ ] 7.2.4 Test: findOrCreateFromJwt updates keycloak_synced_at timestamp
- [ ] 7.2.5 Test: findOrCreateFromJwt fails for non-existent organization

### 7.3 JwtDecoder Tests

- [ ] 7.3.1 Test: JwtDecoder validates RS256 signature
- [ ] 7.3.2 Test: JwtDecoder rejects HS256 algorithm
- [ ] 7.3.3 Test: JwtDecoder validates issuer claim
- [ ] 7.3.4 Test: JwtDecoder validates audience claim

---

## Section 8: Integration Tests with Testcontainers (12 tasks)

### 8.1 Testcontainers Setup

- [ ] 8.1.1 Add Testcontainers dependencies to `backend/build.gradle`
- [ ] 8.1.2 Create `KeycloakContainer` configuration for tests
- [ ] 8.1.3 Create test realm JSON file with test users and clients
- [ ] 8.1.4 Configure Testcontainers to import test realm on startup

### 8.2 Integration Test Scenarios

- [ ] 8.2.1 Test: Obtain JWT from Keycloak and authenticate successfully
- [ ] 8.2.2 Test: API request with valid JWT returns 200 OK
- [ ] 8.2.3 Test: API request with invalid JWT returns 401
- [ ] 8.2.4 Test: API request with expired JWT returns 401
- [ ] 8.2.5 Test: JWT authentication → API Key fallback when JWT invalid
- [ ] 8.2.6 Test: User is auto-created on first JWT authentication (JIT provisioning)
- [ ] 8.2.7 Test: Organization isolation works with JWT authentication
- [ ] 8.2.8 Test: Role-based authorization works with JWT roles

---

## Section 9: E2E Tests (10 tasks)

### 9.1 Playwright E2E Tests

- [ ] 9.1.1 Create helper function to obtain JWT from Keycloak
- [ ] 9.1.2 Test: Complete authentication flow (Keycloak → Backend)
- [ ] 9.1.3 Test: JWT-authenticated user can access protected resources
- [ ] 9.1.4 Test: JWT-authenticated user cannot access other organization's resources
- [ ] 9.1.5 Test: Admin role from JWT allows access to admin endpoints
- [ ] 9.1.6 Test: User role from JWT denies access to admin endpoints
- [ ] 9.1.7 Test: Expired JWT is rejected with proper error message
- [ ] 9.1.8 Test: Invalid JWT signature is rejected
- [ ] 9.1.9 Test: API Key authentication still works (backward compatibility)
- [ ] 9.1.10 Test: Hybrid scenario (some requests with JWT, some with API Key)

---

## Section 10: Performance Tests (8 tasks)

### 10.1 Gatling Performance Tests

- [ ] 10.1.1 Create `JwtAuthenticationSimulation.scala` Gatling scenario
- [ ] 10.1.2 Implement JWT token acquisition in Gatling setup
- [ ] 10.1.3 Test: 10,000 req/s with JWT authentication
- [ ] 10.1.4 Test: Measure JWT validation latency (p50, p95, p99)
- [ ] 10.1.5 Test: Compare JWT vs API Key authentication performance
- [ ] 10.1.6 Test: Hybrid scenario (50% JWT, 50% API Key)
- [ ] 10.1.7 Verify JWT validation <5ms (p95)
- [ ] 10.1.8 Generate performance report and document results

---

## Section 11: Documentation (10 tasks)

### 11.1 API Documentation

- [ ] 11.1.1 Update `docs/KEYCLOAK_INTEGRATION.md` with Phase 2 implementation details
- [ ] 11.1.2 Add JWT authentication usage examples (curl, Postman)
- [ ] 11.1.3 Document JWT Claims structure and requirements
- [ ] 11.1.4 Document error responses for JWT authentication failures

### 11.2 Migration Guide

- [ ] 11.2.1 Create `docs/JWT_AUTHENTICATION_GUIDE.md`
- [ ] 11.2.2 Document how to obtain JWT from Keycloak
- [ ] 11.2.3 Document how to use JWT with Backend API
- [ ] 11.2.4 Document API Key to JWT migration strategy

### 11.3 Troubleshooting

- [ ] 11.3.1 Add JWT troubleshooting section to `docs/TROUBLESHOOTING.md`
- [ ] 11.3.2 Document common JWT authentication errors and solutions

---

## Section 12: Monitoring and Observability (7 tasks)

### 12.1 Metrics

- [ ] 12.1.1 Add Micrometer counter for JWT authentication attempts
- [ ] 12.1.2 Add Micrometer counter for JWT authentication successes
- [ ] 12.1.3 Add Micrometer counter for JWT authentication failures (by type)
- [ ] 12.1.4 Add Micrometer timer for JWT validation latency

### 12.2 Logging

- [ ] 12.2.1 Add structured logging for JWT authentication events
- [ ] 12.2.2 Log JWT validation failures with reason codes
- [ ] 12.2.3 Add correlation IDs for request tracing

---

## Section 13: Security Hardening (5 tasks)

### 13.1 Security Best Practices

- [ ] 13.1.1 Verify only RS256 algorithm is accepted (reject HS256)
- [ ] 13.1.2 Implement 30-second clock skew tolerance
- [ ] 13.1.3 Add rate limiting for JWT authentication failures
- [ ] 13.1.4 Ensure JWT validation failures are logged for audit
- [ ] 13.1.5 Review and test OWASP Top 10 security considerations

---

## Section 14: Deployment and Validation (8 tasks)

### 14.1 Deployment Preparation

- [ ] 14.1.1 Update `backend/README.md` with JWT configuration instructions
- [ ] 14.1.2 Add JWT authentication to application startup logs
- [ ] 14.1.3 Create deployment checklist for Phase 2

### 14.2 Validation

- [ ] 14.2.1 Smoke test: Verify JWT authentication works in development environment
- [ ] 14.2.2 Smoke test: Verify API Key authentication still works (backward compatibility)
- [ ] 14.2.3 Smoke test: Verify user auto-creation (JIT provisioning)
- [ ] 14.2.4 Smoke test: Verify organization isolation with JWT
- [ ] 14.2.5 Performance validation: Confirm <5ms JWT validation latency (p95)

---

## Task Dependencies

### Critical Path
1. Section 1 (Dependencies) → Section 2 (Database) → Section 3 (JWT Decoder)
2. Section 3 → Section 4 (JWT Filter) → Section 5 (User Service)
3. Section 5 → Section 6 (Security Config)
4. Section 6 → Section 7 (Unit Tests) → Section 8 (Integration Tests) → Section 9 (E2E Tests)
5. Section 10 (Performance Tests) can run in parallel with Section 9
6. Section 11 (Documentation) can start after Section 6
7. Section 12 (Monitoring) can run in parallel with testing
8. Section 14 (Deployment) requires all previous sections

### Parallelizable Work
- Section 7 (Unit Tests) + Section 11 (Documentation) + Section 12 (Monitoring)
- Section 9 (E2E Tests) + Section 10 (Performance Tests)

---

## Progress Tracking

**Total Tasks**: 98
**Completed**: 0
**In Progress**: 0
**Pending**: 98

**Progress**: 0% (0/98)

---

## Notes

- All tests must pass before moving to next section
- Performance tests must demonstrate <5ms JWT validation (p95) before deployment
- Documentation should be updated incrementally as features are implemented
- Security review required before merging to main branch
