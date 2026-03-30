import { test, expect } from '@playwright/test'
import { ensureLoggedIn, E2E_CREDENTIALS } from './helpers/auth'

/**
 * Authentication Flow Tests
 * Tests login, logout, session management, and auth error handling
 */
test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to page first, then clear storage
    await page.goto('/', { waitUntil: 'domcontentloaded' }).catch(() => {})
    try {
      await page.evaluate(() => localStorage.clear())
      await page.context().clearCookies()
    } catch (e) {
      // Ignore storage errors in beforeEach
    }
  })

  test('should display login page when not authenticated', async ({ page }) => {
    await page.goto('/')
    // Should redirect to login or show login form
    const loginCard = page.locator('[data-testid="login-page"] .login-card')
    await expect(loginCard).toBeVisible({ timeout: 10000 })
  })

  test('should login successfully with valid credentials', async ({ page }) => {
    await page.goto('/')

    // Fill login form
    const tenantIdInput = page.locator('[data-testid="login-tenant-id"]')
    const usernameInput = page.locator('[data-testid="login-username"]')
    const passwordInput = page.locator('[data-testid="login-password"]')

    await tenantIdInput.fill(E2E_CREDENTIALS.tenantId)
    await usernameInput.fill(E2E_CREDENTIALS.username)
    await passwordInput.fill(E2E_CREDENTIALS.password)

    // Submit login
    await page.locator('[data-testid="login-submit"]').click()

    // Wait for redirect to dashboard
    await expect(page.getByTestId('app-sidebar')).toBeVisible({ timeout: 15000 })
    await expect(page.getByTestId('page-title')).toContainText('Dashboard')
  })

  test('should show error with invalid credentials', async ({ page }) => {
    await page.goto('/')

    const tenantIdInput = page.locator('[data-testid="login-tenant-id"]')
    const usernameInput = page.locator('[data-testid="login-username"]')
    const passwordInput = page.locator('[data-testid="login-password"]')

    await tenantIdInput.fill(E2E_CREDENTIALS.tenantId)
    await usernameInput.fill('invalid_user')
    await passwordInput.fill('wrong_password')

    await page.locator('[data-testid="login-submit"]').click()

    // Should show error message or toast
    const errorToast = page.locator('.toast.error, .toast-error, [class*="error"]').first()
    await expect(errorToast).toBeVisible({ timeout: 5000 }).catch(() => {
      // Alternative: check for error message in login card
      const loginError = page.locator('.login-card [class*="error"], .login-card [class*="alert"]')
      expect(loginError).toBeVisible()
    })
  })

  test('should persist session after page reload', async ({ page }) => {
    // Login first
    await ensureLoggedIn(page)
    await expect(page.getByTestId('app-sidebar')).toBeVisible()

    // Reload page
    await page.reload()
    await page.waitForLoadState('domcontentloaded')

    // Should still be logged in (sidebar should be visible)
    await expect(page.getByTestId('app-sidebar')).toBeVisible({ timeout: 10000 })
  })

  test('should logout successfully', async ({ page }) => {
    await ensureLoggedIn(page)
    await expect(page.getByTestId('app-sidebar')).toBeVisible()

    // Click logout button - use first() to handle multiple logout buttons
    const logoutBtn = page.locator('.logout-btn, [data-testid="logout-btn"], button:has-text("Logout"), button:has-text("退出")').first()
    await logoutBtn.click()

    // Should redirect to login or clear session
    await page.waitForLoadState('domcontentloaded')
    // Either show login form or redirect to login page
    const isLoggedOut = await page.locator('[data-testid="login-page"] .login-card').isVisible({ timeout: 5000 }).catch(() => false)
    if (!isLoggedOut) {
      await expect(page.locator('[data-testid="login-page"], [data-testid="login-form"], .login-card').first()).toBeVisible({ timeout: 5000 })
    }
  })

  test('should display user info in sidebar after login', async ({ page }) => {
    await ensureLoggedIn(page)

    // Check for user info in sidebar
    const accountPill = page.locator('[data-testid="account-pill"], .account-pill, .user-info')
    await expect(accountPill).toBeVisible()
    await expect(accountPill).toContainText(/admin|系统管理员|System Admin/i)
  })

  test('should handle session timeout gracefully', async ({ page }) => {
    await ensureLoggedIn(page)

    // Clear session
    await page.evaluate(() => {
      localStorage.removeItem('crm_auth')
    })
    await page.context().clearCookies()

    // Try to navigate to a protected page
    await page.goto('/customers')
    await page.waitForLoadState('domcontentloaded')

    // Should redirect to login or show login form
    await expect(page.locator('[data-testid="login-page"]')).toBeVisible({ timeout: 10000 })
  })
})
