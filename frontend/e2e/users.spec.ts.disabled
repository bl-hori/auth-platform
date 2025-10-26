import { test, expect } from '@playwright/test';
import { login } from './utils/auth';
import { generateTestUser } from './utils/test-data';

/**
 * User Management E2E Tests
 * Tests CRUD operations for users
 */

test.describe('User Management', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should display users list page', async ({ page }) => {
    await page.goto('/users');

    // Page title
    await expect(page.locator('h1, h2')).toContainText(/users/i);

    // Should have "Create User" button
    await expect(page.locator('a[href="/users/new"], button').filter({ hasText: /new|create/i })).toBeVisible();

    // Should have search or filter functionality
    await expect(page.locator('input[type="search"], input[placeholder*="search" i]')).toBeVisible();
  });

  test('should create a new user', async ({ page }) => {
    const testUser = generateTestUser();

    await page.goto('/users');

    // Click "Create User" button
    await page.click('a[href="/users/new"], button:has-text("Create"), button:has-text("New")');

    // Wait for form to load
    await page.waitForURL(/\/users\/new/);

    // Fill in user details
    await page.fill('input[name="email"]', testUser.email);
    await page.fill('input[name="name"]', testUser.name);

    // Submit form
    await page.click('button[type="submit"]');

    // Should show success message or redirect to users list
    await expect(page.locator('[role="alert"], .success, [data-testid="success-message"]').or(page.locator('text=/users/i'))).toBeVisible({ timeout: 10000 });

    // Verify user appears in list
    await page.goto('/users');
    await expect(page.locator(`text=${testUser.email}`)).toBeVisible();
  });

  test('should search for users', async ({ page }) => {
    await page.goto('/users');

    // Find search input
    const searchInput = page.locator('input[type="search"], input[placeholder*="search" i]').first();
    await searchInput.fill('test-user');

    // Wait for results to update
    await page.waitForTimeout(500);

    // Verify search is working (table should update)
    await expect(page.locator('table, [role="table"]')).toBeVisible();
  });

  test('should view user details', async ({ page }) => {
    await page.goto('/users');

    // Click on first user row or view button
    const firstUserRow = page.locator('table tbody tr, [role="row"]').first();
    await firstUserRow.click();

    // Should navigate to user detail page
    await expect(page).toHaveURL(/\/users\/[^/]+$/);

    // Should display user information
    await expect(page.locator('text=/email|name/i')).toBeVisible();
  });

  test('should edit user information', async ({ page }) => {
    await page.goto('/users');

    // Click on first user
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Click edit button
    await page.click('button:has-text("Edit"), a:has-text("Edit")');

    // Modify user name
    const nameInput = page.locator('input[name="name"]');
    await nameInput.fill('Updated Test User');

    // Submit changes
    await page.click('button[type="submit"]');

    // Should show success message
    await expect(page.locator('[role="alert"], .success, [data-testid="success-message"]')).toBeVisible({ timeout: 10000 });
  });

  test('should deactivate user', async ({ page }) => {
    await page.goto('/users');

    // Click on first user
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Click deactivate or delete button
    await page.click('button:has-text("Deactivate"), button:has-text("Delete")');

    // Confirm action in dialog
    const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes"), button:has-text("Deactivate")');
    if (await confirmButton.isVisible()) {
      await confirmButton.click();
    }

    // Should show success message
    await expect(page.locator('[role="alert"], .success, [data-testid="success-message"]')).toBeVisible({ timeout: 10000 });
  });

  test('should paginate through users', async ({ page }) => {
    await page.goto('/users');

    // Look for pagination controls
    const paginationNext = page.locator('button:has-text("Next"), [aria-label="Next page"]');

    if (await paginationNext.isVisible()) {
      await paginationNext.click();

      // Wait for page to update
      await page.waitForTimeout(500);

      // Verify URL or content changed
      await expect(page).toHaveURL(/page=|offset=/);
    }
  });
});
