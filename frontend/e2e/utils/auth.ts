import { Page } from '@playwright/test';

/**
 * Authentication utilities for E2E tests
 */

export const TEST_API_KEY = process.env.TEST_API_KEY || 'test-api-key-12345';

/**
 * Login to the application with API key
 */
export async function login(page: Page, apiKey: string = TEST_API_KEY) {
  await page.goto('/login');

  // Fill in the API key
  await page.fill('input[name="apiKey"]', apiKey);

  // Click login button
  await page.click('button[type="submit"]');

  // Wait for navigation to dashboard
  await page.waitForURL('/dashboard', { timeout: 10000 });
}

/**
 * Logout from the application
 */
export async function logout(page: Page) {
  // Click on user menu or logout button
  await page.click('[data-testid="user-menu"]');
  await page.click('[data-testid="logout-button"]');

  // Wait for redirect to login page
  await page.waitForURL('/login', { timeout: 10000 });
}

/**
 * Check if user is authenticated
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  try {
    // Check for presence of authenticated elements
    const dashboardLink = await page.$('[data-testid="dashboard-link"]');
    return dashboardLink !== null;
  } catch {
    return false;
  }
}

/**
 * Set authentication token in localStorage
 * Useful for bypassing login UI in tests
 */
export async function setAuthToken(page: Page, token: string) {
  await page.addInitScript((authToken) => {
    localStorage.setItem('apiKey', authToken);
  }, token);
}
