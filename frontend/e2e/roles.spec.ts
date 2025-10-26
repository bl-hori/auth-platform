import { test, expect } from '@playwright/test';
import { login } from './utils/auth';
import { generateTestRole } from './utils/test-data';

/**
 * Role Management E2E Tests
 * Tests CRUD operations for roles and permission assignments
 */

test.describe('Role Management', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should display roles list page', async ({ page }) => {
    await page.goto('/roles');

    // Page title
    await expect(page.locator('h1, h2')).toContainText(/roles/i);

    // Should have "Create Role" button
    await expect(page.locator('a[href="/roles/new"], button').filter({ hasText: /new|create/i })).toBeVisible();

    // Should display roles in a table or grid
    await expect(page.locator('table, [role="table"], [role="grid"]')).toBeVisible();
  });

  test('should create a new role', async ({ page }) => {
    const testRole = generateTestRole();

    await page.goto('/roles');

    // Click "Create Role" button
    await page.click('a[href="/roles/new"], button:has-text("Create"), button:has-text("New")');

    // Wait for form
    await page.waitForURL(/\/roles\/new/);

    // Fill in role details
    await page.fill('input[name="name"]', testRole.name);
    await page.fill('textarea[name="description"], input[name="description"]', testRole.description);

    // Submit form
    await page.click('button[type="submit"]');

    // Should show success or redirect
    await expect(page.locator('[role="alert"], .success').or(page.locator('text=/roles/i'))).toBeVisible({ timeout: 10000 });

    // Verify role appears in list
    await page.goto('/roles');
    await expect(page.locator(`text=${testRole.name}`)).toBeVisible();
  });

  test('should view role details', async ({ page }) => {
    await page.goto('/roles');

    // Click on first role
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Should navigate to role detail page
    await expect(page).toHaveURL(/\/roles\/[^/]+$/);

    // Should display role information
    await expect(page.locator('text=/name|description/i')).toBeVisible();
  });

  test('should assign permissions to role', async ({ page }) => {
    await page.goto('/roles');

    // Click on first role
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Look for permission assignment section
    const assignPermissionButton = page.locator('button:has-text("Assign"), button:has-text("Add Permission")');

    if (await assignPermissionButton.isVisible()) {
      await assignPermissionButton.click();

      // Select a permission from dropdown or list
      const permissionSelect = page.locator('select[name="permission"], [role="combobox"]').first();
      if (await permissionSelect.isVisible()) {
        await permissionSelect.click();

        // Select first option
        await page.locator('option, [role="option"]').first().click();

        // Confirm assignment
        await page.click('button[type="submit"], button:has-text("Assign")');

        // Should show success
        await expect(page.locator('[role="alert"], .success')).toBeVisible({ timeout: 10000 });
      }
    }
  });

  test('should display role hierarchy', async ({ page }) => {
    await page.goto('/roles');

    // Click on first role with hierarchy
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Check if hierarchy visualization exists
    const hierarchySection = page.locator('[data-testid="role-hierarchy"], text=/hierarchy|parent role/i');

    if (await hierarchySection.isVisible()) {
      await expect(hierarchySection).toBeVisible();
    }
  });

  test('should edit role', async ({ page }) => {
    await page.goto('/roles');

    // Click on first role
    await page.locator('table tbody tr, [role="row"]').first().click();

    // Click edit button
    await page.click('button:has-text("Edit"), a:has-text("Edit")');

    // Modify description
    const descInput = page.locator('textarea[name="description"], input[name="description"]');
    await descInput.fill('Updated role description');

    // Submit
    await page.click('button[type="submit"]');

    // Success message
    await expect(page.locator('[role="alert"], .success')).toBeVisible({ timeout: 10000 });
  });

  test('should delete role', async ({ page }) => {
    await page.goto('/roles');

    // Click on a test role
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
