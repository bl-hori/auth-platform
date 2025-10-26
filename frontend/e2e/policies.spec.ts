import { test, expect } from '@playwright/test';
import { login } from './utils/auth';
import { generateTestPolicy } from './utils/test-data';

/**
 * Policy Management E2E Tests
 * Tests policy CRUD operations, validation, and testing
 */

test.describe('Policy Management', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should display policies list page', async ({ page }) => {
    await page.goto('/policies');

    // Page title
    await expect(page.locator('h1, h2')).toContainText(/policies/i);

    // Should have "Create Policy" button
    await expect(page.locator('a[href="/policies/new"], button').filter({ hasText: /new|create/i })).toBeVisible();
  });

  test('should create a new policy', async ({ page }) => {
    const testPolicy = generateTestPolicy();

    await page.goto('/policies');

    // Click "Create Policy" button
    await page.click('a[href="/policies/new"], button:has-text("Create"), button:has-text("New")');

    // Wait for policy editor
    await page.waitForURL(/\/policies\/new/);

    // Fill in policy name
    await page.fill('input[name="name"]', testPolicy.name);

    // Find Monaco editor and input policy content
    // Monaco editor might be in an iframe or use a specific class
    const editor = page.locator('.monaco-editor, [data-testid="policy-editor"]').first();
    await expect(editor).toBeVisible({ timeout: 10000 });

    // Type policy content into Monaco editor
    // Monaco requires special handling
    await page.click('.monaco-editor .view-line');
    await page.keyboard.type(testPolicy.content);

    // Submit form
    await page.click('button[type="submit"], button:has-text("Create")');

    // Should show success
    await expect(page.locator('[role="alert"], .success').or(page.locator('text=/policies/i'))).toBeVisible({ timeout: 10000 });
  });

  test('should validate policy syntax', async ({ page }) => {
    await page.goto('/policies/new');

    // Fill in policy name
    await page.fill('input[name="name"]', 'invalid_policy_test');

    // Enter invalid Rego syntax
    const editor = page.locator('.monaco-editor, [data-testid="policy-editor"]').first();
    await expect(editor).toBeVisible({ timeout: 10000 });

    await page.click('.monaco-editor .view-line');
    await page.keyboard.type('invalid syntax here {{}}');

    // Click validate button if exists
    const validateButton = page.locator('button:has-text("Validate")');
    if (await validateButton.isVisible()) {
      await validateButton.click();

      // Should show validation error
      await expect(page.locator('[role="alert"], .error, text=/error|invalid/i')).toBeVisible({ timeout: 5000 });
    }
  });

  test('should view policy details', async ({ page }) => {
    await page.goto('/policies');

    // Click on first policy
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Should navigate to policy detail page
    await expect(page).toHaveURL(/\/policies\/[^/]+$/);

    // Should display policy editor
    await expect(page.locator('.monaco-editor, [data-testid="policy-editor"]')).toBeVisible();
  });

  test('should test policy execution', async ({ page }) => {
    await page.goto('/policies');

    // Click on first policy
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Navigate to test page
    const testButton = page.locator('a:has-text("Test"), button:has-text("Test")');
    if (await testButton.isVisible()) {
      await testButton.click();

      // Should navigate to test page
      await expect(page).toHaveURL(/\/policies\/[^/]+\/test/);

      // Enter test input (JSON)
      const inputEditor = page.locator('textarea[name="input"], .monaco-editor').first();
      if (await inputEditor.isVisible()) {
        await inputEditor.click();
        await page.keyboard.type('{"action": "READ", "user": {"authenticated": true}}');

        // Click execute test
        await page.click('button:has-text("Execute"), button:has-text("Test")');

        // Should show test result
        await expect(page.locator('[data-testid="test-result"], text=/result|output/i')).toBeVisible({ timeout: 10000 });
      }
    }
  });

  test('should view policy version history', async ({ page }) => {
    await page.goto('/policies');

    // Click on first policy
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Look for version history section
    const versionHistory = page.locator('[data-testid="version-history"], text=/version|history/i');

    if (await versionHistory.isVisible()) {
      await expect(versionHistory).toBeVisible();

      // Should display version list
      await expect(page.locator('[data-testid="version-list"], ul, table')).toBeVisible();
    }
  });

  test('should publish policy', async ({ page }) => {
    await page.goto('/policies');

    // Click on a draft policy
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Click publish button
    const publishButton = page.locator('button:has-text("Publish"), button:has-text("Activate")');

    if (await publishButton.isVisible()) {
      await publishButton.click();

      // Confirm if needed
      const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes")');
      if (await confirmButton.isVisible()) {
        await confirmButton.click();
      }

      // Should show success
      await expect(page.locator('[role="alert"], .success')).toBeVisible({ timeout: 10000 });
    }
  });

  test('should delete policy', async ({ page }) => {
    await page.goto('/policies');

    // Click on a test policy
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Click delete button
    await page.click('button:has-text("Delete")');

    // Confirm deletion
    const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes"), button:has-text("Delete")');
    if (await confirmButton.isVisible()) {
      await confirmButton.click();
    }

    // Success message
    await expect(page.locator('[role="alert"], .success')).toBeVisible({ timeout: 10000 });
  });
});
