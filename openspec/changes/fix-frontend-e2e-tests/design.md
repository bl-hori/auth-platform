# Design: Fix Frontend E2E Tests

## Architecture Decision

### Current State
```
Frontend E2E Tests (Playwright)
├── Browsers: ❌ Not installed initially, now installed
├── System Dependencies: ❌ Missing (libnspr4, libnss3, libasound2t64)
├── Documentation: ❌ No E2E setup instructions
└── CI/CD: ❌ No automated E2E tests

Test Results: 15/15 failing (100% failure rate)
```

### Target State
```
Frontend E2E Tests (Playwright)
├── Browsers: ✅ Chromium installed via `playwright install`
├── System Dependencies: ✅ Installed via `playwright install-deps` or apt-get
├── Documentation: ✅ Clear setup instructions in README
└── CI/CD: ✅ (Optional) Automated E2E tests in GitHub Actions

Test Results: 15/15 passing (targeting 100% success rate)
```

## Technical Approach

### 1. Dependency Installation Strategy

**Playwright Dependencies Hierarchy:**
```
Playwright Test Framework
└── Playwright Browsers (chromium, firefox, webkit)
    └── System Libraries
        ├── libnspr4      - Netscape Portable Runtime
        ├── libnss3       - Network Security Services
        ├── libasound2t64 - ALSA sound library
        └── Other libs    - fontconfig, libatk, libcairo, etc.
```

**Installation Options:**

**Option 1: Playwright CLI (Recommended)**
```bash
# Install browsers
pnpm exec playwright install chromium

# Install system dependencies (requires sudo)
sudo pnpm exec playwright install-deps chromium
```

**Option 2: Manual apt-get**
```bash
# For environments without sudo access to Playwright
sudo apt-get install libnspr4 libnss3 libasound2t64
```

**Option 3: Docker (CI/CD)**
```bash
# Use official Playwright Docker image
docker run -it mcr.microsoft.com/playwright:v1.56.1-jammy
```

### 2. Test Execution Flow

```
Developer runs: pnpm test:e2e
    ↓
playwright.config.ts loaded
    ↓
webServer starts: pnpm dev (Next.js on :3000)
    ↓
Wait for http://localhost:3000 (timeout: 120s)
    ↓
Browser launches: Chromium headless
    ├── Requires system libraries
    └── Fails if dependencies missing ← CURRENT ISSUE
    ↓
Tests run in parallel (6 workers)
    ↓
Results + artifacts generated
```

### 3. Test Structure

**E2E Test Organization:**
```
frontend/e2e/
├── 01-basic-auth.spec.ts        # Step 1: Authentication flow
│   ├── Login page display
│   ├── Empty API key validation
│   ├── Successful login
│   ├── Auth persistence on reload
│   ├── Logout functionality
│   └── Protected route redirect
│
├── 02-navigation.spec.ts         # Step 2: Navigation
│   ├── Users page navigation
│   ├── Roles page navigation
│   ├── Permissions page navigation
│   ├── Policies page navigation
│   ├── Audit logs page navigation
│   └── Dashboard return navigation
│
└── 03-users-list.spec.ts         # Step 3: Users list functionality
    ├── Page display
    ├── Search functionality
    └── Create user button
```

**Test Dependencies:**
- All tests use `data-testid` selectors for stability
- Tests clean storage before each run
- Tests use `waitForLoadState('networkidle')` for reliability
- Retry strategy: 1 retry locally, 2 retries in CI

### 4. Configuration Analysis

**playwright.config.ts Key Settings:**
```typescript
{
  testDir: './e2e',
  timeout: 30000,           // 30s per test
  fullyParallel: true,      // Run tests in parallel
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 1 : undefined,  // Parallel workers

  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    navigationTimeout: 30000
  },

  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:3000',
    timeout: 120000,        // 2 min to start dev server
    reuseExistingServer: !process.env.CI
  }
}
```

## Implementation Plan

### Step 1: Document Current State
- Identify all missing dependencies
- Document exact error messages
- List all 15 failing tests

### Step 2: Install Dependencies
- Install Playwright browsers (chromium, ffmpeg, headless shell)
- Install system dependencies (libnspr4, libnss3, libasound2t64)
- Verify installation success

### Step 3: Run Tests
- Execute `pnpm test:e2e`
- Verify all 15 tests pass
- Check test execution time
- Review generated artifacts

### Step 4: Update Documentation
- Add E2E testing section to `frontend/README.md`
- Document prerequisite requirements
- Document setup commands
- Add troubleshooting section

### Step 5: (Optional) CI/CD Integration
- Create `.github/workflows/e2e-tests.yml`
- Use Playwright Docker image or install dependencies
- Configure artifact upload for failures
- Set up test result reporting

## Trade-offs Analysis

### Option 1: Playwright install-deps (SELECTED)
**Pros:**
- Official Playwright solution
- Installs all required dependencies automatically
- Works across different Linux distributions
- Includes browser-specific optimizations

**Cons:**
- Requires sudo access
- Installs more packages than strictly necessary
- Larger disk footprint (~200-300MB)

### Option 2: Manual apt-get install
**Pros:**
- Minimal installation (only essential libs)
- No sudo access to Playwright needed
- Faster installation
- Smaller disk footprint

**Cons:**
- Need to track dependency updates manually
- May miss some optional dependencies
- Distribution-specific package names

### Option 3: Docker for E2E tests
**Pros:**
- Consistent environment across dev/CI
- No local dependency installation
- Pre-configured Playwright image available
- Easy to upgrade Playwright versions

**Cons:**
- Requires Docker installed
- Slower startup time
- More complex local development workflow
- Additional Docker layer to maintain

**Decision**: Use **Option 1** (Playwright install-deps) for local development, **Option 3** (Docker) for CI/CD.

## Risk Mitigation

### Risk 1: Sudo Access Not Available
**Mitigation**: Provide manual apt-get commands in documentation

### Risk 2: Tests Flaky Due to Timing
**Mitigation**: Already configured with `waitForLoadState`, `networkidle`, and retries

### Risk 3: Dev Server Takes Too Long to Start
**Mitigation**: 120s timeout configured, `reuseExistingServer` for local dev

### Risk 4: CI Environment Different from Local
**Mitigation**: Use official Playwright Docker image in CI

## Validation Strategy

### Local Testing
- Run `pnpm test:e2e` after dependency installation
- Verify all 15 tests pass
- Check artifacts generated (screenshots, videos, traces)
- Test retry behavior by introducing temporary failures

### CI/CD Testing (If Implemented)
- Run E2E tests on pull requests
- Upload artifacts on failure
- Generate test report
- Fail build if tests fail

## Success Metrics
- ✅ 15/15 tests passing (100% success rate)
- ✅ Test execution time < 2 minutes
- ✅ Clear documentation available
- ✅ Artifacts generated on failure (screenshots, videos, traces)
- ✅ (Optional) CI/CD integration working
