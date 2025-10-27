# Design: Fix Backend Test Infrastructure

## Architecture Decision

### Current State
```
@SpringBootTest Tests
├── AuthorizationControllerTest (❌ No Testcontainers)
│   └── Tries to connect to localhost:5432/authplatform_test
├── SecurityConfigIntegrationTest (❌ No Testcontainers)
│   └── Tries to connect to localhost:5432/authplatform_test
└── RateLimitIntegrationTest (❌ No Testcontainers)
    └── Tries to connect to localhost:5432/authplatform_test

@DataJpaTest Tests
├── Repository Tests (✅ Extend BaseIntegrationTest)
│   └── Use Testcontainers properly
└── DatabaseIntegrationTest (✅ Extends BaseIntegrationTest)
    └── Use Testcontainers properly
```

### Target State
```
All Integration Tests
├── @SpringBootTest Tests
│   ├── AuthorizationControllerTest (✅ Extends BaseIntegrationTest)
│   ├── SecurityConfigIntegrationTest (✅ Extends BaseIntegrationTest)
│   └── RateLimitIntegrationTest (✅ Extends BaseIntegrationTest)
└── @DataJpaTest Tests
    ├── Repository Tests (✅ Already extend BaseIntegrationTest)
    └── DatabaseIntegrationTest (✅ Already extends BaseIntegrationTest)
```

## Technical Approach

### 1. Testcontainers Configuration Strategy

**BaseIntegrationTest** provides:
- Static PostgreSQL 15 container (reused across tests)
- Dynamic property configuration via `@DynamicPropertySource`
- Automatic Flyway migrations
- Test profile activation

**Key Design Decisions**:
- Use single static container with `.withReuse(true)` for performance
- Configure Spring datasource dynamically (no hardcoded URLs)
- Enable Flyway to ensure schema consistency
- Disable Redis/Cache for unit tests (use in-memory alternatives)

### 2. Configuration Hierarchy

```yaml
application.yml (main)
  ├── Production settings
  └── Default configurations

application-test.yml (test)
  ├── Disable external dependencies (Redis, OPA)
  ├── Remove hardcoded database URLs ← FIX NEEDED
  ├── Enable test security settings
  └── Disable rate limiting

BaseIntegrationTest (Java)
  └── Override datasource with Testcontainers ← PROVIDES DYNAMIC CONFIG
```

### 3. Test Execution Flow

```
1. JUnit discovers test class
2. @ActiveProfiles("test") loads application-test.yml
3. @Testcontainers starts PostgreSQL container (if not already running)
4. @DynamicPropertySource injects container connection details
5. Spring Boot context starts with test configuration
6. Flyway runs migrations on container database
7. Tests execute
8. Container remains running for subsequent tests (reuse)
9. Container stops when all tests complete
```

## Implementation Plan

### Step 1: Update BaseIntegrationTest (if needed)
- Verify container configuration is correct
- Ensure `@DynamicPropertySource` properly overrides application-test.yml

### Step 2: Fix application-test.yml
Remove hardcoded database connection (will be provided by Testcontainers):
```yaml
spring:
  datasource:
    # REMOVE these lines - provided dynamically by Testcontainers
    # url: jdbc:postgresql://localhost:5432/authplatform_test
    # username: authplatform
    # password: authplatform
```

### Step 3: Update @SpringBootTest Tests
For each failing test class:
```java
@SpringBootTest
@AutoConfigureMockMvc
-@ActiveProfiles("test")  // Remove if extending BaseIntegrationTest
 @DisplayName("...")
-class SomeTest {
+class SomeTest extends BaseIntegrationTest {
```

### Step 4: Validate Test Isolation
- Ensure each test runs in isolation (no shared state)
- Verify Flyway migrations create clean schema
- Check that container reuse doesn't cause test pollution

## Trade-offs Analysis

### Option 1: Use Testcontainers (SELECTED)
**Pros**:
- No manual setup required
- Consistent across environments
- Isolated test databases
- Easy to run in CI/CD

**Cons**:
- Requires Docker installation
- Slightly slower first test run (container startup)
- Increased complexity for newcomers

### Option 2: Use H2 In-Memory Database
**Pros**:
- No Docker required
- Very fast startup
- Simple setup

**Cons**:
- PostgreSQL-specific features won't work
- SQL dialect differences cause bugs
- Not production-like

### Option 3: Require Manual PostgreSQL Setup
**Pros**:
- Simple configuration
- No additional dependencies

**Cons**:
- Manual setup required for each developer
- Inconsistent environments
- Difficult to automate CI/CD
- Database state pollution between test runs

**Decision**: Use Testcontainers (Option 1) for production-like testing with minimal setup.

## Risk Mitigation

### Risk 1: Docker Not Available
**Mitigation**: Document Docker as a prerequisite in README.md

### Risk 2: Container Startup Time
**Mitigation**: Use `.withReuse(true)` to reuse container across test runs

### Risk 3: Test Pollution
**Mitigation**: Flyway migrations run fresh on each container start; Spring transactions roll back after each test

### Risk 4: CI/CD Environment
**Mitigation**: GitHub Actions provides Docker by default; document for other CI systems

## Validation Strategy

### Unit Test Validation
- Run `./gradlew test --info` to see detailed test execution
- Verify all tests pass
- Check test execution time (should be < 2 minutes)

### Integration Test Validation
- Verify Testcontainers starts PostgreSQL
- Check Flyway migrations apply successfully
- Ensure tests can insert/query data

### CI/CD Validation
- Run tests in GitHub Actions
- Verify no manual setup required
- Check for consistent results across runs

## Metrics & Monitoring

### Success Metrics
- ✅ 100% of tests pass
- ✅ 0 manual setup steps required
- ✅ Test execution time < 2 minutes
- ✅ 0 test failures due to environment issues

### Performance Benchmarks
- Container startup: ~5-10 seconds (first run)
- Container reuse: ~0 seconds (subsequent runs)
- Total test suite: ~60-120 seconds
