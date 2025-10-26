import { test, expect } from '@playwright/test';

/**
 * Basic Authentication Tests - Step 1
 *
 * Tests the most fundamental authentication flow
 */

test.describe('Basic Authentication', () => {
  // Clear storage before each test
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.context().clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  });

  test('should display login page', async ({ page }) => {
    await page.goto('/login');

    // Wait for page to load
    await page.waitForLoadState('networkidle');

    // Should have "Auth Platform" text
    await expect(page.locator('text=Auth Platform')).toBeVisible();

    // Should have API key input
    const apiKeyInput = page.locator('input[type="password"]');
    await expect(apiKeyInput).toBeVisible();

    // Should have login button
    const loginButton = page.locator('button[type="submit"]');
    await expect(loginButton).toBeVisible();
  });

  test('should show error with empty API key', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    // Try to submit without entering API key
    await page.click('button[type="submit"]');

    // Should show error message
    await expect(page.locator('text=/APIキー/i')).toBeVisible({ timeout: 5000 });
  });

  test('should login successfully with any API key', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    // Fill in API key (any key works in development)
    const apiKeyInput = page.locator('input[type="password"]').first();
    await apiKeyInput.fill('test-api-key-12345');

    // Click login button
    await page.click('button[type="submit"]');

    // Should navigate to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Verify we're on dashboard
    expect(page.url()).toContain('/dashboard');
  });

  test('should persist authentication on reload', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    const apiKeyInput = page.locator('input[type="password"]').first();
    await apiKeyInput.fill('test-api-key-12345');
    await page.click('button[type="submit"]');

    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Reload the page
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Should still be on dashboard (not redirected to login)
    expect(page.url()).toContain('/dashboard');
  });

  test('should logout successfully', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    const apiKeyInput = page.locator('input[type="password"]').first();
    await apiKeyInput.fill('test-api-key-12345');
    await page.click('button[type="submit"]');

    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Find and click logout button
    const logoutButton = page.locator('button', { hasText: 'ログアウト' });
    await logoutButton.click();

    // Should navigate back to login
    await page.waitForURL('**/login', { timeout: 10000 });
    expect(page.url()).toContain('/login');
  });

  test('should redirect to login when accessing protected route', async ({ page }) => {
    // Try to access dashboard directly without logging in
    await page.goto('/dashboard');

    // Should redirect to login
    await page.waitForURL('**/login', { timeout: 10000 });
    expect(page.url()).toContain('/login');
  });
});
