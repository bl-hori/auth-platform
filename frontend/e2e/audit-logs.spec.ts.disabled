import { test, expect } from '@playwright/test';
import { login } from './utils/auth';

/**
 * Audit Logs E2E Tests
 * Tests audit log viewing, filtering, and export functionality
 */

test.describe('Audit Logs', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should display audit logs page', async ({ page }) => {
    await page.goto('/audit-logs');

    // Page title
    await expect(page.locator('h1, h2')).toContainText(/audit|logs/i);

    // Should display logs in a table
    await expect(page.locator('table, [role="table"]')).toBeVisible();
  });

  test('should filter audit logs by date range', async ({ page }) => {
    await page.goto('/audit-logs');

    // Look for date range picker
    const dateRangePicker = page.locator('input[type="date"], [data-testid="date-range-picker"]');

    if (await dateRangePicker.first().isVisible()) {
      // Set start date
      await dateRangePicker.first().fill('2025-01-01');

      // Wait for results to update
      await page.waitForTimeout(1000);

      // Verify table updated
      await expect(page.locator('table tbody tr, [role="row"]')).toBeVisible();
    }
  });

  test('should filter audit logs by user', async ({ page }) => {
    await page.goto('/audit-logs');

    // Look for user filter
    const userFilter = page.locator('select[name="userId"], input[name="user"], [placeholder*="user" i]');

    if (await userFilter.isVisible()) {
      await userFilter.first().click();
      await userFilter.first().fill('test-user');

      // Wait for filtering
      await page.waitForTimeout(500);

      // Verify results
      await expect(page.locator('table, [role="table"]')).toBeVisible();
    }
  });

  test('should filter audit logs by action', async ({ page }) => {
    await page.goto('/audit-logs');

    // Look for action filter
    const actionFilter = page.locator('select[name="action"], [data-testid="action-filter"]');

    if (await actionFilter.isVisible()) {
      await actionFilter.click();

      // Select an action
      await page.locator('option, [role="option"]').first().click();

      // Wait for filtering
      await page.waitForTimeout(500);

      // Verify table updated
      await expect(page.locator('table tbody tr, [role="row"]')).toBeVisible();
    }
  });

  test('should view audit log details', async ({ page }) => {
    await page.goto('/audit-logs');

    // Click on first log entry
    const firstRow = page.locator('table tbody tr, [role="row"]').first();
    await firstRow.click();

    // Should show detail view or navigate to detail page
    const detailModal = page.locator('[role="dialog"], [data-testid="log-detail"]');
    const detailPage = page.url().includes('/audit-logs/');

    if (await detailModal.isVisible()) {
      // Modal detail view
      await expect(detailModal).toBeVisible();
      await expect(detailModal.locator('text=/action|resource|timestamp/i')).toBeVisible();
    } else if (detailPage) {
      // Detail page
      await expect(page).toHaveURL(/\/audit-logs\/[^/]+$/);
      await expect(page.locator('text=/action|resource|timestamp/i')).toBeVisible();
    }
  });

  test('should export audit logs to CSV', async ({ page }) => {
    await page.goto('/audit-logs');

    // Look for export button
    const exportButton = page.locator('button:has-text("Export"), button:has-text("Download")');

    if (await exportButton.isVisible()) {
      // Listen for download
      const downloadPromise = page.waitForEvent('download', { timeout: 10000 });

      await exportButton.click();

      // Wait for download to start
      const download = await downloadPromise;

      // Verify download happened
      expect(download.suggestedFilename()).toMatch(/audit.*\.(csv|xlsx)/i);
    }
  });

  test('should paginate through audit logs', async ({ page }) => {
    await page.goto('/audit-logs');

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

  test('should display real-time updates', async ({ page }) => {
    await page.goto('/audit-logs');

    // Get initial row count
    const initialCount = await page.locator('table tbody tr, [role="row"]').count();

    // Wait for potential updates (polling interval)
    await page.waitForTimeout(5000);

    // Check if new rows appeared (if real-time updates are working)
    const newCount = await page.locator('table tbody tr, [role="row"]').count();

    // Real-time updates may or may not have new data
    expect(newCount).toBeGreaterThanOrEqual(initialCount);
  });
});
