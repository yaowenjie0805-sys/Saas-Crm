import { expect } from '@playwright/test'

const DEFAULT_TIMEOUT = 15_000
const API_BASE_URL = globalThis.process?.env?.E2E_API_BASE_URL || 'http://127.0.0.1:8080/api'
const API_ORIGIN = new URL(API_BASE_URL).origin

export const E2E_CREDENTIALS = {
  tenantId: 'tenant_default',
  username: 'admin',
  password: 'admin123',
}

export async function ensureLoggedIn(page, credentials = E2E_CREDENTIALS) {
  // Navigate to login page
  await page.goto('/', { waitUntil: 'domcontentloaded' })
  
  // Check if already logged in
  const sidebar = page.getByTestId('app-sidebar')
  if (await sidebar.isVisible({ timeout: 3000 }).catch(() => false)) {
    return
  }
  
  // Perform login via UI
  await page.locator('[data-testid="login-tenant-id"]').fill(credentials.tenantId)
  await page.locator('[data-testid="login-username"]').fill(credentials.username)
  await page.locator('[data-testid="login-password"]').fill(credentials.password)
  await page.locator('[data-testid="login-submit"]').click()
  
  // Wait for navigation to dashboard
  await expect(page.getByTestId('app-sidebar')).toBeVisible({ timeout: DEFAULT_TIMEOUT })
}
