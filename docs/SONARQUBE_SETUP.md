# SonarQube Setup Guide

This guide explains how to configure and use SonarQube for code quality analysis in the Auth Platform project.

## Overview

SonarQube is integrated into the CI/CD pipeline to provide:
- Code quality metrics
- Code coverage analysis
- Security vulnerability detection
- Code smell identification
- Technical debt tracking

## Prerequisites

- SonarQube server (local, cloud, or SonarCloud)
- Gradle 8.5+
- Java 21
- Project already built with test coverage reports

## Local Development

### Option 1: Using Docker (Recommended)

Run a local SonarQube instance:

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  sonarqube:latest
```

Access SonarQube at [http://localhost:9000](http://localhost:9000)
- Default credentials: `admin` / `admin`
- Change password on first login

### Option 2: Manual Installation

Download and install from [https://www.sonarqube.org/downloads/](https://www.sonarqube.org/downloads/)

## Configuration

### 1. Create a Project in SonarQube

1. Log in to your SonarQube instance
2. Click "Create Project" → "Manually"
3. Project key: `auth-platform`
4. Display name: `Auth Platform`
5. Generate a token for authentication

### 2. Local Analysis

Run SonarQube analysis locally:

```bash
cd backend

# Build and run tests first
./gradlew clean build test jacocoTestReport

# Run SonarQube analysis
./gradlew sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE
```

### 3. CI/CD Integration

The project is configured to run SonarQube analysis in GitHub Actions automatically.

#### Required GitHub Secrets

Configure these secrets in your repository settings:

1. **SONAR_TOKEN** (Required)
   - Your SonarQube authentication token
   - Generate in: User → My Account → Security → Generate Token

2. **SONAR_HOST_URL** (Required)
   - Your SonarQube server URL
   - Examples:
     - SonarCloud: `https://sonarcloud.io`
     - Local: `http://your-server:9000`
     - Self-hosted: `https://sonar.yourcompany.com`

3. **SONAR_ORGANIZATION** (Required for SonarCloud only)
   - Your SonarCloud organization key
   - Find in: SonarCloud → Your Organization → Information → Organization Key
   - **Not needed** for self-hosted SonarQube servers

#### How to Set Secrets

```bash
# Using GitHub CLI

# For SonarCloud:
gh secret set SONAR_TOKEN -b"your_token_here"
gh secret set SONAR_HOST_URL -b"https://sonarcloud.io"
gh secret set SONAR_ORGANIZATION -b"your-org-key"

# For self-hosted SonarQube:
gh secret set SONAR_TOKEN -b"your_token_here"
gh secret set SONAR_HOST_URL -b"https://sonar.yourcompany.com"
# SONAR_ORGANIZATION is not needed

# Or via GitHub Web UI:
# Settings → Secrets and variables → Actions → New repository secret
```

**Note**: The CI/CD pipeline will automatically detect if you're using SonarCloud (by checking if the URL contains `sonarcloud.io`) and will skip the scan with a warning if `SONAR_ORGANIZATION` is not set. For self-hosted SonarQube, the organization setting is optional.

## Configuration Files

### build.gradle

SonarQube plugin and configuration:

```gradle
plugins {
    id 'org.sonarqube' version '4.4.1.3373'
}

sonar {
    properties {
        property "sonar.projectKey", "auth-platform"
        property "sonar.projectName", "Auth Platform"
        property "sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "http://localhost:9000"
        // ... other properties
    }
}
```

### sonar-project.properties

Standalone configuration file for SonarQube Scanner:

```properties
sonar.projectKey=auth-platform
sonar.projectName=Auth Platform
sonar.sources=backend/src/main/java
sonar.tests=backend/src/test/java
# ... other properties
```

## Quality Gates

### Default Quality Gate Conditions

- Code Coverage: ≥ 80%
- Duplicated Lines: < 3%
- Maintainability Rating: A
- Reliability Rating: A
- Security Rating: A

### Custom Quality Gates

You can configure custom quality gates in SonarQube:

1. Quality Gates → Create
2. Add conditions:
   - Coverage on New Code ≥ 80%
   - Duplicated Lines on New Code < 3%
   - Maintainability Rating on New Code = A
   - Security Hotspots Reviewed = 100%

## Analysis Exclusions

### Code Coverage Exclusions

The following files are excluded from coverage analysis:
- `**/*Config.java` - Spring configuration classes
- `**/*Application.java` - Application entry points
- `**/*Entity.java` - JPA entities
- `**/*DTO.java` - Data transfer objects
- `**/*Request.java` - Request models
- `**/*Response.java` - Response models

### Duplicate Detection Exclusions

Entities and DTOs are excluded from duplicate detection as they often have similar structures.

## Gradle Tasks

```bash
# Run SonarQube analysis
./gradlew sonar

# Run with custom parameters
./gradlew sonar \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=YOUR_TOKEN

# Run analysis with verbose logging
./gradlew sonar --info

# Run all quality checks (Checkstyle + JaCoCo + SonarQube)
./gradlew check jacocoTestReport sonar
```

## Viewing Results

### SonarQube Dashboard

After analysis, view results in SonarQube:

1. Navigate to your SonarQube instance
2. Select "auth-platform" project
3. View:
   - Overview: Key metrics summary
   - Issues: Code smells, bugs, vulnerabilities
   - Measures: Detailed metrics
   - Code: Annotated source code
   - Activity: Historical trends

### GitHub Actions

View analysis results in GitHub Actions:

1. Go to Actions tab in GitHub
2. Select the workflow run
3. Check "Code Quality Checks" job
4. SonarQube results link in the logs

## Troubleshooting

### Analysis Fails with "No coverage information"

**Solution**: Ensure tests run before SonarQube analysis:

```bash
./gradlew clean test jacocoTestReport sonar
```

### Authentication Error

**Solution**: Verify your token is valid:

```bash
curl -u YOUR_TOKEN: https://sonarcloud.io/api/authentication/validate
```

### Build Takes Too Long

**Solution**: Use incremental analysis:

```bash
./gradlew sonar -Dsonar.scm.disabled=true
```

### Quality Gate Fails

**Solution**: Check specific failures in SonarQube dashboard and fix:

1. Review failed conditions
2. Address critical/blocker issues first
3. Improve test coverage for new code
4. Refactor code smells

## SonarCloud Setup (Alternative)

For open-source projects or cloud-hosted analysis:

1. Go to [SonarCloud.io](https://sonarcloud.io)
2. Sign in with GitHub
3. Import your repository
4. Configure organization and project
5. Update `SONAR_HOST_URL` secret to `https://sonarcloud.io`
6. Add organization key to `sonar-project.properties`:
   ```properties
   sonar.organization=your-org-key
   ```

## Best Practices

1. **Run Locally Before Push**
   ```bash
   ./gradlew clean build test jacocoTestReport sonar
   ```

2. **Fix Issues Incrementally**
   - Focus on new code first
   - Address blocker/critical issues immediately
   - Plan technical debt reduction

3. **Monitor Trends**
   - Track coverage over time
   - Monitor code duplication
   - Review technical debt ratio

4. **Integrate with PR Reviews**
   - Check SonarQube report before merging
   - Require quality gate to pass
   - Use SonarQube comments in PR discussions

## Resources

- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [SonarQube Gradle Plugin](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/)
- [SonarCloud Documentation](https://docs.sonarcloud.io/)
- [Java Code Quality Rules](https://rules.sonarsource.com/java)

## Support

For issues or questions:
- Check [SonarQube Community](https://community.sonarsource.com/)
- Review project [GitHub Issues](../issues)
- Contact the development team
