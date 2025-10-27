# Spec Delta: Test Infrastructure

## ADDED Requirements

### Requirement: Testcontainers Integration for All Integration Tests
All integration tests requiring database access MUST use Testcontainers to provide isolated PostgreSQL instances.

#### Scenario: @SpringBootTest tests extend BaseIntegrationTest
**Given** a developer writes a new `@SpringBootTest` integration test that requires database access
**When** the test class is created
**Then** the test class MUST extend `BaseIntegrationTest`
**And** the test MUST NOT hardcode database connection strings
**And** Testcontainers MUST provide the PostgreSQL instance dynamically

#### Scenario: Tests run without manual database setup
**Given** a developer clones the repository for the first time
**When** they run `./gradlew test`
**Then** all tests MUST pass without requiring manual PostgreSQL installation or configuration
**And** Testcontainers MUST automatically start a PostgreSQL container
**And** the container MUST be reused across test executions for performance

#### Scenario: Test database schema is managed by Flyway
**Given** a Testcontainers PostgreSQL instance is running
**When** a Spring Boot test context starts
**Then** Flyway MUST automatically apply all migrations from `db/migration`
**And** the database schema MUST match production schema
**And** migrations MUST run successfully without errors

### Requirement: Test Configuration Isolation
Test configuration MUST be isolated from production configuration and MUST NOT require environment-specific setup.

#### Scenario: Dynamic database configuration overrides static config
**Given** `application-test.yml` contains test configurations
**When** `BaseIntegrationTest` uses `@DynamicPropertySource`
**Then** dynamic Testcontainers properties MUST override any static database URLs
**And** tests MUST connect to the Testcontainers PostgreSQL instance
**And** tests MUST NOT attempt to connect to `localhost:5432`

#### Scenario: External dependencies are disabled in tests
**Given** tests are running with the `test` profile
**When** Spring context initializes
**Then** Redis cache MUST be disabled (use `cache.type: none`)
**And** OPA integration MUST be mocked or disabled
**And** rate limiting MUST be disabled by default
**And** tests MUST NOT require external services to be running

### Requirement: Test Execution Performance
Test execution MUST be performant and MUST complete within reasonable time limits.

#### Scenario: Container reuse for performance
**Given** multiple test classes require PostgreSQL
**When** tests execute sequentially or in parallel
**Then** Testcontainers MUST reuse the same PostgreSQL container instance
**And** container startup MUST occur only once per test execution
**And** total test suite execution MUST complete in under 2 minutes

#### Scenario: Test isolation without performance penalty
**Given** multiple tests run against the same container
**When** each test executes
**Then** tests MUST be isolated from each other (no shared state)
**And** Flyway migrations MUST provide clean schema
**And** Spring transaction rollback MUST prevent data pollution
**And** test execution order MUST NOT affect test results

## MODIFIED Requirements

### Requirement: Test Profile Configuration
Test profile MUST use dynamic configuration from Testcontainers via `@DynamicPropertySource` instead of hardcoded database connections.

**Previously**: Test profile required hardcoded database connection to `localhost:5432/authplatform_test`
**Now**: Dynamic configuration from Testcontainers

#### Scenario: Test profile does not hardcode database URLs
**Given** `application-test.yml` configures the test profile
**When** the file is loaded
**Then** it MUST NOT contain hardcoded `spring.datasource.url` values
**And** it MUST NOT contain hardcoded `spring.datasource.username` values
**And** it MUST NOT contain hardcoded `spring.datasource.password` values
**And** these properties MUST be provided dynamically by `BaseIntegrationTest`

## REMOVED Requirements

None - this is a new capability focused on improving existing test infrastructure.

## Cross-References

This capability affects the following existing specs:
- `authorization-core` - Authorization API tests require database
- `policy-management` - Policy CRUD tests require database
- `user-identity` - User/Role/Permission tests require database
- `audit-logging` - Audit log tests require database

All tests in these capabilities MUST follow the test infrastructure requirements defined here.
