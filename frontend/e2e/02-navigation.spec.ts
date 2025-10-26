import { test, expect } from '@playwright/test';

/**
 * Navigation Tests - Step 2
 *
 * Tests navigation between pages after authentication
 */

test.describe('Navigation', () => {
  // Login before each test
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.context().clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });

    // Login
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    // Use data-testid for stable selection
    const apiKeyInput = page.getByTestId('api-key-input');
    await apiKeyInput.fill('test-api-key-12345');

    // Click login button
    const loginButton = page.getByTestId('login-button');
    await loginButton.click();

    // Wait for navigation to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    await page.waitForLoadState('networkidle');
  });

  test('should navigate to users page', async ({ page }) => {
    // Click on users link in sidebar
    await page.click('text=ユーザー管理');

    // Wait for navigation
    await page.waitForURL('**/users', { timeout: 10000 });

    // Verify we're on users page
    expect(page.url()).toContain('/users');

    // Should have users page content
    await expect(page.locator('h1, h2').filter({ hasText: /ユーザー|Users/i })).toBeVisible({ timeout: 5000 });
  });

  test('should navigate to roles page', async ({ page }) => {
    await page.click('text=ロール管理');

    await page.waitForURL('**/roles', { timeout: 10000 });
    expect(page.url()).toContain('/roles');

    await expect(page.locator('h1, h2').filter({ hasText: /ロール|Role/i })).toBeVisible({ timeout: 5000 });
  });

  test('should navigate to permissions page', async ({ page }) => {
    await page.click('text=権限管理');

    await page.waitForURL('**/permissions', { timeout: 10000 });
    expect(page.url()).toContain('/permissions');

    await expect(page.locator('h1, h2').filter({ hasText: /権限|Permission/i })).toBeVisible({ timeout: 5000 });
  });

  test('should navigate to policies page', async ({ page }) => {
    await page.click('text=ポリシー管理');

    await page.waitForURL('**/policies', { timeout: 10000 });
    expect(page.url()).toContain('/policies');

    await expect(page.locator('h1, h2').filter({ hasText: /ポリシー|Polic/i })).toBeVisible({ timeout: 5000 });
  });

  test('should navigate to audit logs page', async ({ page }) => {
    await page.click('text=監査ログ');

    await page.waitForURL('**/audit-logs', { timeout: 10000 });
    expect(page.url()).toContain('/audit-logs');

    await expect(page.locator('h1, h2').filter({ hasText: /監査|Audit/i })).toBeVisible({ timeout: 5000 });
  });

  test('should navigate back to dashboard', async ({ page }) => {
    // Go to users page first
    await page.click('text=ユーザー管理');
    await page.waitForURL('**/users', { timeout: 10000 });

    // Click dashboard link
    await page.click('text=ダッシュボード');

    await page.waitForURL('**/dashboard', { timeout: 10000 });
    expect(page.url()).toContain('/dashboard');
  });
});
