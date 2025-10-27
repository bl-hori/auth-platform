# Spec Delta: E2E Testing Infrastructure

## ADDED Requirements

### Requirement: System Dependency Management for Playwright
The E2E testing infrastructure MUST manage system dependencies required for Playwright browser execution.

#### Scenario: Install system dependencies via Playwright CLI
**Given** a development environment without Playwright system dependencies
**When** developer runs `pnpm exec playwright install-deps chromium`
**Then** all required system libraries MUST be installed
**And** libnspr4, libnss3, and libasound2t64 MUST be present
**And** Chromium browser MUST launch successfully

#### Scenario: Install system dependencies manually
**Given** a restricted environment without sudo access to Playwright CLI
**When** developer runs `sudo apt-get install libnspr4 libnss3 libasound2t64`
**Then** essential system libraries MUST be installed
**And** Chromium browser MUST launch successfully
**And** tests MUST execute without browser dependency errors

#### Scenario: Verify dependency installation
**Given** system dependencies have been installed
**When** developer runs `dpkg -l | grep -E 'libnspr4|libnss3|libasound2'`
**Then** all required packages MUST appear in the output
**And** package status MUST be "ii" (installed)

### Requirement: Browser Installation for E2E Tests
The E2E testing infrastructure MUST provide Playwright browsers for test execution.

#### Scenario: Install Chromium browser
**Given** a fresh development environment
**When** developer runs `pnpm exec playwright install chromium`
**Then** Chromium browser MUST be downloaded
**And** browser MUST be installed in ~/.cache/ms-playwright/
**And** chromium_headless_shell executable MUST exist

#### Scenario: Verify browser installation
**Given** Chromium browser has been installed
**When** developer checks ~/.cache/ms-playwright/chromium_headless_shell-*/
**Then** chrome-linux/headless_shell executable MUST exist
**And** executable MUST have proper permissions

### Requirement: Test Execution Reliability
The E2E testing infrastructure MUST execute tests reliably and efficiently.

#### Scenario: Run full E2E test suite
**Given** system dependencies and browsers are installed
**And** Next.js dev server is available
**When** developer runs `pnpm test:e2e`
**Then** dev server MUST start on http://localhost:3000 within 120s
**And** all 15 E2E tests MUST execute
**And** all tests MUST pass (100% success rate)
**And** execution MUST complete in under 2 minutes

#### Scenario: Run specific test file
**Given** system dependencies and browsers are installed
**When** developer runs `pnpm test:e2e 01-basic-auth`
**Then** only authentication tests MUST execute
**And** 6 tests MUST run
**And** results MUST be displayed

#### Scenario: Run tests in UI mode
**Given** system dependencies and browsers are installed
**When** developer runs `pnpm test:e2e:ui`
**Then** Playwright UI MUST launch
**And** tests MUST be visible in the UI
**And** developer can step through tests interactively

### Requirement: Test Artifact Generation
The E2E testing infrastructure MUST capture artifacts for debugging test failures.

#### Scenario: Generate artifacts on test failure
**Given** a test fails during execution
**When** test completes
**Then** screenshot MUST be captured at failure point
**And** video MUST be saved showing full test execution
**And** trace MUST be available for step-by-step analysis
**And** artifacts MUST be stored in playwright-report/

#### Scenario: Access test report
**Given** tests have completed execution
**When** developer opens playwright-report/index.html
**Then** test results MUST be displayed
**And** failed tests MUST show screenshots
**And** videos MUST be playable inline
**And** traces MUST be accessible via links

### Requirement: Comprehensive E2E Testing Documentation
The E2E testing infrastructure MUST provide comprehensive documentation for setup and usage.

#### Scenario: Follow setup instructions
**Given** a new developer joins the project
**When** developer follows README.md E2E testing section
**Then** all prerequisites MUST be clearly listed
**And** system dependency installation MUST have step-by-step commands
**And** both sudo and non-sudo methods MUST be documented
**And** developer can successfully run tests

#### Scenario: Troubleshoot common issues
**Given** developer encounters an error
**When** developer consults troubleshooting section
**Then** common errors MUST be listed with solutions
**And** missing dependencies error MUST have fix instructions
**And** dev server timeout MUST have mitigation steps

## REMOVED Requirements

None - this is a new capability focused on establishing E2E testing infrastructure.

## Cross-References

This capability affects frontend testing:
- Frontend authentication flow testing
- Frontend navigation testing
- Frontend user management UI testing

All E2E tests MUST follow the infrastructure requirements defined here.
