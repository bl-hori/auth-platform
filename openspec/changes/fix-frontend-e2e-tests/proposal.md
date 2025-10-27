# Proposal: Fix Frontend E2E Tests

## Overview
Fix failing Playwright E2E tests by installing missing system dependencies and documenting the setup process. All 15 E2E tests are currently failing due to missing browser dependencies.

## Problem Statement
Frontend E2E tests fail with the error:
```
Error: browserType.launch:
Host system is missing dependencies to run browsers.
```

### Root Causes Identified:
1. **Missing System Dependencies**: Playwright requires `libnspr4`, `libnss3`, and `libasound2t64` to run Chromium browser
2. **Browsers Not Installed**: Playwright browsers (Chromium, FFMPEG, Chromium Headless Shell) not installed
3. **Missing Documentation**: No clear instructions on how to run E2E tests locally
4. **CI/CD Setup Missing**: No automated E2E test execution in GitHub Actions

## Current State
- **Test Files**: 3 E2E test suites with 15 tests total
  - `e2e/01-basic-auth.spec.ts` - 6 authentication tests
  - `e2e/02-navigation.spec.ts` - 6 navigation tests
  - `e2e/03-users-list.spec.ts` - 3 users list tests
- **Test Results**: 15/15 tests failing (100% failure rate)
- **Failure Reason**: Browser dependencies not installed

## Proposed Solution

### Phase 1: Install Dependencies
1. Install Playwright browsers: `pnpm exec playwright install chromium`
2. Install system dependencies: `sudo pnpm exec playwright install-deps chromium`
3. Alternative for non-sudo environments: Manual apt-get install

### Phase 2: Documentation
1. Add E2E testing section to `frontend/README.md`
2. Document prerequisite system dependencies
3. Document how to run tests locally
4. Add troubleshooting guide for common issues

### Phase 3: CI/CD Integration (Optional)
1. Add E2E test workflow to GitHub Actions
2. Configure Playwright to run in CI environment
3. Upload test artifacts (videos, screenshots, traces) on failure

## Benefits
- E2E tests can run successfully
- Better documentation for developers
- Catch UI regressions before deployment
- Improved developer experience with clear setup instructions

## Impact Assessment
- **Breaking Changes**: None (fixing existing tests)
- **Migration Required**: No (one-time setup of dependencies)
- **Performance Impact**: None (tests run on-demand)
- **Security Impact**: None (development/testing only)

## Success Criteria
- [x] All 15 E2E tests pass locally ✅
- [x] Documentation includes clear setup instructions ✅
- [x] System dependencies documented ✅
- [x] Troubleshooting guide available ✅
- [ ] (Optional) E2E tests run in CI/CD

## Timeline
- **Dependency Installation**: 10-15 minutes
- **Documentation**: 15-20 minutes
- **CI/CD Setup (Optional)**: 30 minutes

## Related Specs
- Frontend testing infrastructure
- CI/CD pipeline configuration
