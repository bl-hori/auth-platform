import { test, expect } from '@playwright/test';
import { login, logout, TEST_API_KEY } from './utils/auth';

/**
 * Authentication E2E Tests
 * Tests the login and logout flows
 */

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    // Clear any existing auth state
    await page.goto('/');
    await page.context().clearCookies();
    await page.evaluate(() => localStorage.clear());
  });

  test('should display login page for unauthenticated users', async ({ page }) => {
    await page.goto('/');

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/);

    // Should display login form
    await expect(page.locator('h1, h2')).toContainText(/login|sign in/i);
    await expect(page.locator('input[name="apiKey"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('should login successfully with valid API key', async ({ page }) => {
    await page.goto('/login');

    // Fill in API key
    await page.fill('input[name="apiKey"]', TEST_API_KEY);

    // Submit form
    await page.click('button[type="submit"]');

    // Should navigate to dashboard
    await page.waitForURL('/dashboard', { timeout: 10000 });

    // Verify we're on the dashboard
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('should show error with invalid API key', async ({ page }) => {
    await page.goto('/login');

    // Fill in invalid API key
    await page.fill('input[name="apiKey"]', 'invalid-key-12345');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show error message
    await expect(page.locator('[role="alert"], .error, [data-testid="error-message"]')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    // Login first
    await login(page);

    // Verify we're authenticated
    await expect(page).toHaveURL(/\/dashboard/);

    // Logout
    await logout(page);

    // Should redirect to login page
    await expect(page).toHaveURL(/\/login/);

    // Should not be able to access protected routes
    await page.goto('/users');
    await expect(page).toHaveURL(/\/login/);
  });

  test('should persist authentication on page reload', async ({ page }) => {
    await login(page);

    // Reload the page
    await page.reload();

    // Should still be authenticated
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('should redirect to login when accessing protected routes', async ({ page }) => {
    const protectedRoutes = [
      '/dashboard',
      '/users',
      '/roles',
      '/policies',
      '/audit-logs',
    ];

    for (const route of protectedRoutes) {
      await page.goto(route);
      await expect(page).toHaveURL(/\/login/);
    }
  });
});
