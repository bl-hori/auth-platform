# Authorization Platform - Backend

Spring Boot 3 monolithic application providing RBAC authorization services.

## Features

- **Authorization API**: REST API for authorization decisions (<10ms p95 latency)
- **Policy Management**: Create, validate, and manage Rego-based policies
- **User & Role Management**: CRUD operations for users, roles, and permissions
- **Audit Logging**: Comprehensive logging of all authorization decisions
- **Multi-Tenancy**: Organization-level isolation

## Tech Stack

- **Java 21** (LTS)
- **Spring Boot 3.2.1**
- **PostgreSQL 15** (primary database)
- **Redis 7** (L2 cache)
- **OPA 0.60** (policy evaluation engine)
- **Caffeine** (L1 in-memory cache)
- **Flyway** (database migrations)

## Prerequisites

- JDK 21
- Docker & Docker Compose (for local development)

## Quick Start

### 1. Start Infrastructure

```bash
cd ../infrastructure
docker-compose up -d
```

### 2. Run Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 3. Verify Health

```bash
curl http://localhost:8080/actuator/health
```

## Development

### Build

```bash
# Build without tests
./gradlew build -x test

# Build with tests
./gradlew build

# Clean build
./gradlew clean build
```

### Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Code Quality

```bash
# Run Checkstyle
./gradlew checkstyleMain checkstyleTest

# View Checkstyle reports
open build/reports/checkstyle/main.html
```

### Database Migrations

```bash
# Create new migration
# Add file to: src/main/resources/db/migration/V<version>__<description>.sql

# Flyway will automatically run migrations on application startup
```

## Configuration

Key configuration in `src/main/resources/application.yml`:

```yaml
authplatform:
  security:
    api-key:
      header-name: X-API-Key
  opa:
    url: http://localhost:8181
    timeout-ms: 100
  cache:
    l1:
      ttl-seconds: 10
      max-entries: 10000
    l2:
      ttl-seconds: 300
```

## API Documentation

Once running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI spec available at:
```
http://localhost:8080/v3/api-docs
```

## Project Structure

```
src/main/java/io/authplatform/platform/
├── api/              # REST controllers
├── service/          # Business logic
│   ├── authorization/
│   ├── policy/
│   ├── identity/
│   └── audit/
├── domain/           # Domain models and repositories
├── infrastructure/   # OPA, Redis, PostgreSQL clients
└── config/           # Spring configuration
```

## Testing Strategy

- **Unit Tests**: 50% of test suite (JUnit 5 + Mockito)
- **Integration Tests**: 20% (Spring Boot Test + Testcontainers)
- **API Tests**: REST Assured for endpoint testing

Target: 80%+ code coverage

## Monitoring

Prometheus metrics available at:
```
http://localhost:8080/actuator/prometheus
```

Key metrics:
- `authorization_requests_total`
- `authorization_latency_seconds`
- `cache_hit_rate`
- `opa_evaluation_duration_seconds`

## License

Proprietary - Internal Use Only
