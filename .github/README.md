# CI/CD Pipeline Documentation

## Overview

This directory contains GitHub Actions workflows for automated CI/CD pipeline of the Authorization Platform.

## Workflows

### CI Pipeline (`ci.yml`)

Automated continuous integration pipeline that runs on every push and pull request.

#### Triggers
- **Push**: `main`, `develop`, `feature/**` branches
- **Pull Request**: to `main` or `develop` branches

#### Jobs

##### 1. Build and Test
Builds the application and runs all tests with PostgreSQL and Redis services.

**Steps:**
- Checkout code
- Set up JDK 21 (Temurin)
- Cache Gradle dependencies
- Build application
- Run unit and integration tests
- Generate code coverage reports (JaCoCo)
- Upload test results and coverage artifacts
- Comment test results on PRs

**Services:**
- PostgreSQL 15 (port 5432)
- Redis 7 (port 6379)

**Artifacts:**
- Test results (retention: 30 days)
- Coverage reports (retention: 30 days)

##### 2. Code Quality Checks
Runs static code analysis tools.

**Steps:**
- Run Checkstyle (Google Java Style Guide)
- Upload Checkstyle reports

**Artifacts:**
- Checkstyle reports (retention: 30 days)

##### 3. Security Scan
Performs security vulnerability scanning on dependencies.

**Steps:**
- Run OWASP Dependency Check
- Upload dependency check reports

**Configuration:**
- Fails on CVSS score ≥ 7
- Suppressions: `backend/config/dependency-check-suppressions.xml`

**Artifacts:**
- Dependency check reports (retention: 30 days)

##### 4. Build Summary
Aggregates results from all jobs and provides build status.

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JAVA_VERSION` | JDK version | `21` |
| `GRADLE_VERSION` | Gradle version | `8.5` |

### Test Database Configuration

The CI pipeline uses dedicated test database credentials:

```yaml
services:
  postgres:
    env:
      POSTGRES_DB: authplatform_test
      POSTGRES_USER: authplatform
      POSTGRES_PASSWORD: authplatform_test_password
```

### Gradle Caching

Gradle dependencies and wrapper are cached to speed up builds:

- Cache key: `${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}`
- Restore keys: Previous OS-specific Gradle caches

## Code Quality Standards

### Test Coverage
- **Target**: 80% code coverage
- **Tool**: JaCoCo
- **Verification**: `jacocoTestCoverageVerification` task
- **Reports**: HTML and XML formats

### Code Style
- **Standard**: Google Java Style Guide
- **Tool**: Checkstyle 10.12.7
- **Configuration**: `backend/config/checkstyle/checkstyle.xml`

### Security
- **Tool**: OWASP Dependency Check 9.0.9
- **Threshold**: CVSS ≥ 7 fails the build
- **Suppressions**: `backend/config/dependency-check-suppressions.xml`

## Artifacts

All artifacts are retained for 30 days and include:

1. **Test Results**
   - JUnit XML reports
   - HTML test reports
   - Published as PR comments

2. **Coverage Reports**
   - JaCoCo HTML reports
   - JaCoCo XML reports (for external tools)

3. **Checkstyle Reports**
   - Checkstyle XML and HTML reports

4. **Security Reports**
   - OWASP Dependency Check HTML reports

## Local Development

To run the same checks locally before pushing:

### Build and Test
```bash
cd backend
./gradlew clean build test
```

### Generate Coverage Report
```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Run Checkstyle
```bash
./gradlew checkstyleMain checkstyleTest
```

### Run Security Scan
```bash
./gradlew dependencyCheckAnalyze
open build/reports/dependency-check-report.html
```

### Run All Checks
```bash
./gradlew clean build test \
  jacocoTestReport \
  jacocoTestCoverageVerification \
  checkstyleMain checkstyleTest \
  dependencyCheckAnalyze
```

## Troubleshooting

### Build Failures

**Symptom**: Tests fail in CI but pass locally

**Possible Causes:**
1. Database connection issues
   - Check PostgreSQL service health
   - Verify environment variables
2. Redis connection issues
   - Check Redis service health
   - Verify port configuration
3. Timezone differences
   - Use UTC in tests
4. File path issues
   - Use platform-independent paths

**Solution:**
```bash
# Run with CI environment variables locally
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=authplatform_test
export DB_USER=authplatform
export DB_PASSWORD=authplatform_test_password
export REDIS_HOST=localhost
export REDIS_PORT=6379

./gradlew test
```

### Coverage Verification Failures

**Symptom**: `jacocoTestCoverageVerification` fails

**Solution:**
1. Check current coverage: `./gradlew jacocoTestReport`
2. Open report: `build/reports/jacoco/test/html/index.html`
3. Add tests for uncovered code
4. Verify coverage: `./gradlew jacocoTestCoverageVerification`

### Checkstyle Violations

**Symptom**: Checkstyle task fails

**Solution:**
1. Review violations: `build/reports/checkstyle/main.html`
2. Fix code style issues
3. Re-run: `./gradlew checkstyleMain checkstyleTest`

### Security Vulnerabilities

**Symptom**: OWASP Dependency Check fails

**Solution:**
1. Review report: `build/reports/dependency-check-report.html`
2. Update vulnerable dependencies
3. If false positive, add suppression to `config/dependency-check-suppressions.xml`
4. Document reason and review date in suppression

## Performance Optimization

### Build Time
- **Gradle caching**: Reduces dependency download time
- **Test parallelization**: Enabled by default with JUnit 5
- **Incremental compilation**: Enabled in Gradle

### Expected Build Times
- **Fresh build**: ~3-5 minutes
- **Cached build**: ~1-2 minutes
- **Test execution**: ~30-60 seconds

## Future Enhancements

Planned improvements (deferred to later phases):

- [ ] SonarQube integration for code quality metrics
- [ ] Automated deployment to staging environment
- [ ] Docker image building and publishing
- [ ] Performance testing with Gatling
- [ ] E2E testing with Playwright
- [ ] Automated changelog generation
- [ ] Release automation
- [ ] Multi-region deployment pipeline
- [ ] Canary deployment strategy

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Build Scans](https://scans.gradle.com/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Checkstyle Documentation](https://checkstyle.org/)
- [OWASP Dependency Check](https://jeremylong.github.io/DependencyCheck/)

---

**Last Updated**: 2025-10-25
**Maintained by**: Authorization Platform Team
