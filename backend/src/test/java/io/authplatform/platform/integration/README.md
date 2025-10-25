# Integration Tests with Testcontainers

This package contains integration tests that use [Testcontainers](https://www.testcontainers.org/) to provide real PostgreSQL database instances for testing.

## Overview

Integration tests in this package verify database operations, entity mappings, and repository queries against a real PostgreSQL 15 database, ensuring:
- Flyway migrations apply correctly (V1, V2, V3)
- Entity relationships work as expected
- JSONB fields store and retrieve data correctly
- Soft delete functionality operates properly
- Custom repository queries function correctly
- Performance indexes are created successfully

## Test Structure

### BaseIntegrationTest
Base class providing:
- Shared PostgreSQL 15 container (postgres:15-alpine)
- Automatic Flyway migration execution
- Container reuse across test classes for performance
- Dynamic property injection for database connection

### DatabaseIntegrationTest
Comprehensive integration tests covering:
- All core entities (Organization, User, Role, Permission, Policy, etc.)
- JSONB field handling
- Soft delete operations
- Role hierarchy
- Resource-scoped role assignments
- Policy versioning
- Audit log partitioning
- Migration verification

## Running Tests

### Run integration tests only:
```bash
./gradlew test --tests "io.authplatform.platform.integration.*"
```

### Run specific test class:
```bash
./gradlew test --tests "io.authplatform.platform.integration.DatabaseIntegrationTest"
```

### Run all tests:
```bash
./gradlew test
```

## Requirements

- Docker must be running (Testcontainers will automatically pull and start PostgreSQL containers)
- No manual database setup required
- Tests are isolated and idempotent

## Benefits of Testcontainers

1. **Real Database**: Tests run against PostgreSQL 15, not H2 or in-memory databases
2. **Migration Testing**: Flyway migrations are applied automatically
3. **Isolation**: Each test class gets a fresh database state
4. **CI/CD Ready**: Works in GitHub Actions with Docker support
5. **No Manual Setup**: Containers are managed automatically

## Test Coverage

Integration tests complement unit tests by verifying:
- Database schema correctness
- Migration scripts (V1, V2, V3)
- Entity mappings (@ManyToOne, @OneToMany)
- Custom queries (@Query annotations)
- JSONB PostgreSQL-specific features
- Index creation and performance
- Row-Level Security (RLS) policies

## Performance

- PostgreSQL container starts once per test run
- Container is reused across test classes (`@Container` with `static`)
- Each test method runs in its own transaction (automatic rollback)
- Typical test execution: ~20-30 seconds for full integration test suite

## Future Enhancements

- Add tests for RLS policy enforcement with session variables
- Add tests for multi-tenant data isolation
- Add performance benchmarks for indexed queries
- Add tests for audit log time-series partitioning
