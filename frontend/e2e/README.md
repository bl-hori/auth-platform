# E2E Tests with Playwright

This directory contains end-to-end tests for the Auth Platform frontend using Playwright.

## Overview

The E2E test suite covers:

- **Authentication** - Login/logout flows
- **User Management** - CRUD operations for users
- **Role Management** - Role creation, permission assignment, hierarchy
- **Policy Management** - Policy CRUD, validation, testing
- **Audit Logs** - Viewing, filtering, exporting logs
- **Accessibility** - WCAG 2.1 compliance testing

## Prerequisites

- Node.js 18+ and pnpm
- Backend API running on `http://localhost:8080`
- Frontend dev server running on `http://localhost:3000`

## Installation

Playwright and dependencies are already installed if you ran `pnpm install`.

To install Playwright browsers:

```bash
npx playwright install
```

## Running Tests

### Run all E2E tests

```bash
pnpm test:e2e
```

### Run tests in UI mode (interactive)

```bash
pnpm test:e2e:ui
```

### Run tests in headed mode (see browser)

```bash
pnpm test:e2e:headed
```

### Debug tests

```bash
pnpm test:e2e:debug
```

### View test report

```bash
pnpm test:e2e:report
```

## Test Structure

```
e2e/
├── auth.spec.ts          # Authentication tests
├── users.spec.ts         # User management tests
├── roles.spec.ts         # Role management tests
├── policies.spec.ts      # Policy management tests
├── audit-logs.spec.ts    # Audit log tests
├── accessibility.spec.ts # Accessibility tests
├── utils/
│   ├── auth.ts           # Authentication utilities
│   └── test-data.ts      # Test data generators
└── README.md             # This file
```

## Configuration

Test configuration is in `playwright.config.ts`:

- **Base URL**: `http://localhost:3000` (configurable via `PLAYWRIGHT_BASE_URL`)
- **Test timeout**: 30 seconds
- **Retries**: 2 on CI, 0 locally
- **Reporters**: HTML, JSON, JUnit, List

## Environment Variables

- `PLAYWRIGHT_BASE_URL` - Frontend base URL (default: `http://localhost:3000`)
- `TEST_API_KEY` - API key for test authentication (default: `test-api-key-12345`)
- `CI` - Set to enable CI mode (stricter settings)

## Writing Tests

### Example test structure

```typescript
import { test, expect } from '@playwright/test';
import { login } from './utils/auth';

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should perform action', async ({ page }) => {
    await page.goto('/feature');

    // Test assertions
    await expect(page.locator('h1')).toContainText('Feature');
  });
});
```

### Using test utilities

```typescript
import { login, logout } from './utils/auth';
import { generateTestUser, generateTestRole } from './utils/test-data';

// Login helper
await login(page);

// Generate test data
const testUser = generateTestUser({ email: 'custom@example.com' });
```

## CI/CD Integration

E2E tests run automatically in GitHub Actions CI pipeline:

- Tests run on every PR
- Chromium browser only in CI
- Artifacts (screenshots, videos, reports) uploaded on failure

## Best Practices

1. **Use data-testid attributes** for stable selectors
2. **Wait for explicit conditions** instead of arbitrary timeouts
3. **Clean up test data** created during tests
4. **Use page object pattern** for complex pages
5. **Run tests in parallel** when possible
6. **Mock external dependencies** when appropriate

## Troubleshooting

### Tests failing locally

1. Ensure backend API is running on port 8080
2. Ensure frontend dev server is running on port 3000
3. Check test API key is valid
4. Run with `--headed` to see what's happening

### Tests timing out

1. Increase timeout in `playwright.config.ts`
2. Check network requests are completing
3. Verify selectors are correct

### Screenshots/videos not captured

1. Check `use.screenshot` and `use.video` settings in config
2. Verify test is actually failing
3. Check `playwright-report` directory

## Resources

- [Playwright Documentation](https://playwright.dev)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Axe Accessibility Testing](https://github.com/dequelabs/axe-core-npm/tree/develop/packages/playwright)
