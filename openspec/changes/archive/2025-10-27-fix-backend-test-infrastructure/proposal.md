# Proposal: Fix Backend Test Infrastructure

## Overview
Fix failing backend tests by properly configuring Testcontainers integration and ensuring all tests use isolated test databases instead of requiring manual PostgreSQL setup.

## Problem Statement
Backend tests are currently failing with the error:
```
org.postgresql.util.PSQLException: FATAL: database "authplatform_test" does not exist
```

### Root Causes Identified:
1. **Inconsistent Test Base Classes**: Some `@SpringBootTest` tests (e.g., `AuthorizationControllerTest`) do not extend `BaseIntegrationTest`, causing them to attempt connections to a non-existent local PostgreSQL database instead of using Testcontainers.
2. **Static Configuration Conflicts**: `application-test.yml` hardcodes database connection strings that conflict with Testcontainers' dynamic configuration.
3. **Missing Test Documentation**: No clear documentation on how to run tests locally or in CI.

## Proposed Solution

### Phase 1: Fix Testcontainers Integration
1. Update all `@SpringBootTest` integration tests to extend `BaseIntegrationTest`
2. Ensure `BaseIntegrationTest` properly configures Testcontainers for all test types
3. Remove hardcoded database URLs from `application-test.yml` (will be provided dynamically by Testcontainers)

### Phase 2: Add Proper Test Categorization
1. Create separate base classes for different test types:
   - `BaseIntegrationTest` - For tests requiring full Spring context + database
   - Repository tests already use `@DataJpaTest` - no changes needed
   - Controller tests should use Testcontainers for consistency

### Phase 3: Documentation & Validation
1. Add README for running tests locally
2. Validate all tests pass with `./gradlew test`
3. Document Testcontainers requirements (Docker)

## Benefits
- Tests can run anywhere without manual database setup
- Consistent test infrastructure across all test types
- Faster CI/CD pipeline with containerized dependencies
- Better developer experience (no manual setup required)

## Impact Assessment
- **Breaking Changes**: None (internal testing infrastructure only)
- **Migration Required**: No
- **Performance Impact**: Minimal (Testcontainers reuses containers)
- **Security Impact**: None

## Success Criteria
- [ ] All backend tests pass with `./gradlew test`
- [ ] No manual PostgreSQL setup required
- [ ] Tests run consistently in CI and local environments
- [ ] Test execution time remains under 2 minutes

## Timeline
- **Implementation**: 1-2 hours
- **Testing**: 30 minutes
- **Documentation**: 30 minutes

## Related Specs
- `authorization-core` - Tests affected
- `policy-management` - Tests affected
- `user-identity` - Tests affected
- `audit-logging` - Tests affected
