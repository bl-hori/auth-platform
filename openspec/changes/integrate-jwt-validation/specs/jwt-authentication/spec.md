# Capability: jwt-authentication

## Overview

JWT-based authentication capability that validates JSON Web Tokens issued by Keycloak and provides seamless integration with the existing API Key authentication system.

## ADDED Requirements

### Requirement: JWT-AUTH-001 - JWT Token Extraction and Validation

The system MUST extract JWT tokens from HTTP Authorization headers and validate their signatures using Keycloak's public keys.

#### Scenario: Valid JWT token authentication succeeds
**Given** a client has obtained a valid JWT token from Keycloak
**When** the client sends a request with `Authorization: Bearer <valid-jwt>` header
**Then** the system validates the JWT signature using Keycloak's JWK Set
**And** extracts claims (sub, email, organization_id, roles) from the token
**And** sets the SecurityContext with authenticated user information
**And** allows the request to proceed to the API endpoint

#### Scenario: Invalid JWT signature is rejected
**Given** a client has a JWT token with invalid signature
**When** the client sends a request with `Authorization: Bearer <invalid-jwt>` header
**Then** the system rejects the token with 401 Unauthorized
**And** returns error message "Invalid JWT token"
**And** does not set the SecurityContext
**And** logs the authentication failure

#### Scenario: Expired JWT token is rejected
**Given** a client has a JWT token that has expired
**When** the client sends a request with the expired JWT token
**Then** the system rejects the token with 401 Unauthorized
**And** returns error message "JWT token has expired"
**And** logs the expiration event

#### Scenario: JWT with invalid issuer is rejected
**Given** a JWT token with issuer other than configured Keycloak realm
**When** the client sends a request with this token
**Then** the system rejects the token with 401 Unauthorized
**And** returns error message "Invalid JWT issuer"

#### Scenario: JWT with invalid audience is rejected
**Given** a JWT token without "auth-platform-backend" in audience claim
**When** the client sends a request with this token
**Then** the system rejects the token with 401 Unauthorized
**And** returns error message "Invalid JWT audience"

---

### Requirement: JWT-AUTH-002 - Hybrid Authentication Support

The system MUST support both JWT and API Key authentication methods simultaneously, allowing gradual migration from API Key to JWT.

#### Scenario: JWT authentication takes precedence when both headers present
**Given** a client sends both `Authorization: Bearer <jwt>` and `X-API-Key` headers
**When** the JWT is valid
**Then** the system authenticates using JWT
**And** ignores the API Key
**And** sets SecurityContext from JWT claims

#### Scenario: Fallback to API Key when JWT is invalid
**Given** a client sends both `Authorization: Bearer <invalid-jwt>` and `X-API-Key` headers
**When** JWT validation fails
**Then** the system falls back to API Key authentication
**And** validates the API Key
**And** sets SecurityContext from API Key if valid

#### Scenario: API Key authentication works without JWT
**Given** a client sends only `X-API-Key` header (no JWT)
**When** the API Key is valid
**Then** the system authenticates using API Key
**And** sets SecurityContext with organization ID from API Key
**And** maintains backward compatibility with existing clients

#### Scenario: Both authentication methods fail
**Given** a client sends invalid JWT and invalid API Key
**When** both authentication methods fail
**Then** the system returns 401 Unauthorized
**And** logs both authentication failures

---

### Requirement: JWT-AUTH-003 - Just-In-Time User Provisioning

The system MUST automatically create or update user records in the Users table when a valid JWT is presented for the first time.

#### Scenario: New user is automatically created on first JWT authentication
**Given** a JWT with sub="new-user-123", email="newuser@example.com", organization_id="org-uuid"
**When** the user authenticates for the first time
**Then** the system creates a new User record with:
  - keycloak_sub = "new-user-123"
  - email = "newuser@example.com"
  - organization_id = "org-uuid"
  - status = ACTIVE
  - created_at = current timestamp
**And** the user can access the API immediately

#### Scenario: Existing user by keycloak_sub is found and reused
**Given** a User record exists with keycloak_sub="existing-user-456"
**When** a JWT with sub="existing-user-456" is presented
**Then** the system finds the existing user by keycloak_sub
**And** does not create a new user record
**And** uses the existing user information for authorization

#### Scenario: Existing user by email is linked to Keycloak
**Given** a User record exists with email="user@example.com" but keycloak_sub is NULL
**When** a JWT with sub="keycloak-789", email="user@example.com" is presented
**Then** the system finds the user by email
**And** updates the user record with keycloak_sub="keycloak-789"
**And** links the existing user to the Keycloak identity

#### Scenario: User provisioning fails for non-existent organization
**Given** a JWT with organization_id="non-existent-org"
**When** the organization does not exist in the database
**Then** the system rejects the authentication with 401 Unauthorized
**And** returns error message "Invalid organization"
**And** does not create the user record

---

### Requirement: JWT-AUTH-004 - Organization Isolation

The system MUST enforce organization-level isolation using the organization_id claim from JWT tokens.

#### Scenario: User can only access resources in their organization
**Given** a user authenticated with JWT containing organization_id="org-A"
**When** the user requests resources belonging to organization_id="org-A"
**Then** the request is authorized
**And** the user can access the resources

#### Scenario: User cannot access resources from other organizations
**Given** a user authenticated with JWT containing organization_id="org-A"
**When** the user attempts to access resources belonging to organization_id="org-B"
**Then** the system denies access with 403 Forbidden
**And** logs the cross-organization access attempt

#### Scenario: JWT without organization_id claim is rejected
**Given** a JWT that does not contain organization_id claim
**When** the client sends this JWT
**Then** the system rejects the token with 401 Unauthorized
**And** returns error message "Missing organization_id claim"

---

### Requirement: JWT-AUTH-005 - Role-Based Authorization from JWT

The system MUST extract and use role information from JWT tokens for authorization decisions.

#### Scenario: User roles are extracted from JWT claims
**Given** a JWT with roles=["user", "admin"] in claims
**When** the system validates the JWT
**Then** the system extracts the roles
**And** sets GrantedAuthorities in SecurityContext
**And** makes roles available for @PreAuthorize annotations

#### Scenario: Admin role allows access to admin endpoints
**Given** a user authenticated with JWT containing roles=["admin"]
**When** the user accesses an endpoint with @PreAuthorize("hasRole('admin')")
**Then** the authorization succeeds
**And** the user can access the admin endpoint

#### Scenario: User without required role is denied access
**Given** a user authenticated with JWT containing roles=["user"]
**When** the user attempts to access an endpoint requiring "admin" role
**Then** the system denies access with 403 Forbidden
**And** logs the authorization failure

---

### Requirement: JWT-AUTH-006 - Public Key Caching and Rotation

The system MUST cache Keycloak's public keys efficiently and handle key rotation automatically.

#### Scenario: Public keys are cached to minimize Keycloak requests
**Given** the system has fetched Keycloak's JWK Set
**When** subsequent JWT validations occur within the cache TTL (1 hour)
**Then** the system uses cached public keys
**And** does not make additional requests to Keycloak JWK Set URI
**And** JWT validation completes in <5ms (p95)

#### Scenario: Public keys are refreshed when cache expires
**Given** the public key cache has expired (>1 hour)
**When** a new JWT validation is requested
**Then** the system fetches the latest JWK Set from Keycloak
**And** updates the cache
**And** uses the new public keys for validation

#### Scenario: Key rotation is handled transparently
**Given** Keycloak rotates its signing keys and issues JWTs with new kid (key ID)
**When** a JWT signed with the new key is presented
**Then** the system detects the unknown kid
**And** fetches the updated JWK Set from Keycloak
**And** validates the JWT with the new key
**And** caches the new key for future use

---

### Requirement: JWT-AUTH-007 - Performance Requirements

JWT authentication MUST meet strict performance requirements to avoid degrading API response times.

#### Scenario: JWT validation completes within 5ms (p95)
**Given** a valid JWT token
**When** the system performs JWT validation (extraction, signature verification, claims parsing)
**Then** the total validation time is <5ms at p95 percentile
**And** does not significantly impact API response times

#### Scenario: System handles 10,000+ req/s with JWT authentication
**Given** a load test with 10,000 requests per second
**When** all requests use JWT authentication
**Then** the system maintains <5ms JWT validation latency (p95)
**And** achieves >10,000 req/s throughput
**And** maintains error rate <0.1%

#### Scenario: User lookup is optimized with keycloak_sub index
**Given** a JWT with sub="user-12345"
**When** the system looks up the user in the database
**Then** the query uses the keycloak_sub index
**And** the database lookup completes in <2ms

---

### Requirement: JWT-AUTH-008 - Security and Compliance

JWT authentication MUST implement security best practices and comply with OIDC/OAuth2 standards.

#### Scenario: Only RS256 algorithm is accepted
**Given** a JWT signed with HS256 (symmetric) algorithm
**When** the client presents this JWT
**Then** the system rejects the token with 401 Unauthorized
**And** only accepts RS256 (asymmetric) algorithm

#### Scenario: Clock skew tolerance is applied
**Given** a JWT with exp timestamp within 30 seconds of current time
**When** the system validates the JWT
**Then** the system applies 30-second clock skew tolerance
**And** accepts the token as valid

#### Scenario: JWT validation failures are logged for security auditing
**Given** any JWT validation failure occurs
**When** the authentication attempt fails
**Then** the system logs:
  - Timestamp
  - Client IP address
  - Failure reason (invalid signature, expired, wrong issuer, etc.)
  - JWT subject (if available)
**And** increments authentication failure metrics

---

### Requirement: JWT-AUTH-009 - Configuration Management

JWT authentication behavior MUST be configurable through application properties without code changes.

#### Scenario: JWT authentication can be enabled/disabled via configuration
**Given** `authplatform.keycloak.enabled=false` in application.yml
**When** the application starts
**Then** JWT authentication filter is not initialized
**And** only API Key authentication is available

#### Scenario: Keycloak connection details are configurable
**Given** custom Keycloak configuration in application.yml:
  - issuer-uri: https://keycloak.example.com/realms/custom
  - jwk-set-uri: https://keycloak.example.com/realms/custom/protocol/openid-connect/certs
**When** the application starts
**Then** the system uses the configured URIs for JWT validation

#### Scenario: Cache TTL and clock skew are configurable
**Given** custom JWT validation settings:
  - public-key-cache-ttl: 7200 (2 hours)
  - clock-skew-seconds: 60
**When** JWT validation occurs
**Then** the system uses the configured cache TTL and clock skew values

---

### Requirement: JWT-AUTH-010 - Error Handling and Observability

The system MUST provide clear error messages and comprehensive observability for JWT authentication.

#### Scenario: Detailed error responses for JWT validation failures
**Given** a JWT validation fails for any reason
**When** the system returns 401 Unauthorized
**Then** the response includes:
  - HTTP status: 401
  - Error message: specific reason (e.g., "JWT signature invalid", "Token expired")
  - Timestamp
  - Request ID (for tracing)
**And** does not expose sensitive information (private keys, internal errors)

#### Scenario: Metrics are collected for JWT authentication
**Given** JWT authentication is enabled
**When** authentication requests are processed
**Then** the system collects metrics for:
  - Total JWT authentication attempts
  - Successful authentications
  - Failed authentications (by reason)
  - JWT validation latency (p50, p95, p99)
  - Public key cache hit/miss rate

#### Scenario: Health check includes Keycloak connectivity
**Given** JWT authentication is enabled
**When** the /actuator/health endpoint is queried
**Then** the response includes Keycloak connectivity status
**And** indicates if JWK Set URI is reachable

---

### Requirement: JWT-AUTH-011 - Database Schema Migration

The Users table MUST be extended to support Keycloak integration with proper indexing.

#### Scenario: Users table includes keycloak_sub column
**Given** the database schema migration is applied
**When** querying the users table structure
**Then** the table includes:
  - keycloak_sub VARCHAR(255) UNIQUE
  - keycloak_synced_at TIMESTAMP
  - Index on keycloak_sub for fast lookups

#### Scenario: Existing users can be linked to Keycloak
**Given** an existing user with email="user@example.com" and keycloak_sub=NULL
**When** the user authenticates with Keycloak for the first time
**Then** the system updates keycloak_sub with the JWT sub claim
**And** sets keycloak_synced_at to current timestamp
**And** future authentications use keycloak_sub for lookup

---

### Requirement: JWT-AUTH-012 - Integration Testing

Comprehensive integration tests MUST validate JWT authentication with real Keycloak instances.

#### Scenario: Integration test with Testcontainers Keycloak
**Given** a test environment with Testcontainers Keycloak
**When** the integration test runs
**Then** the test:
  - Starts a Keycloak container with test realm
  - Obtains a valid JWT token
  - Calls the API with the JWT
  - Verifies successful authentication
  - Cleans up the container

#### Scenario: End-to-end authentication flow is tested
**Given** a complete E2E test scenario
**When** the test executes
**Then** the test covers:
  - JWT token acquisition from Keycloak
  - API request with JWT
  - User provisioning (JIT)
  - Organization isolation
  - Role-based authorization
  - Token expiration handling
