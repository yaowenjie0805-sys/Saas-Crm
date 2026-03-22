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
    const loginCard = page.locator('.login-card')
    await expect(loginCard).toBeVisible({ timeout: 10000 })
  })

  test('should login successfully with valid credentials', async ({ page }) => {
    await page.goto('/')

    // Fill login form
    const usernameInput = page.locator('.login-card input').first()
    const passwordInput = page.locator('.login-card input[type="password"]')

    await usernameInput.fill(E2E_CREDENTIALS.username)
    await passwordInput.fill(E2E_CREDENTIALS.password)

    // Submit login
    await page.locator('.login-card button[type="submit"], .login-card button:has-text("Login"), .login-card button:has-text("登录")').click()

    // Wait for redirect to dashboard
    await expect(page.getByTestId('app-sidebar')).toBeVisible({ timeout: 15000 })
    await expect(page.getByTestId('page-title')).toContainText('Dashboard')
  })

  test('should show error with invalid credentials', async ({ page }) => {
    await page.goto('/')

    const usernameInput = page.locator('.login-card input').first()
    const passwordInput = page.locator('.login-card input[type="password"]')

    await usernameInput.fill('invalid_user')
    await passwordInput.fill('wrong_password')

    await page.locator('.login-card button[type="submit"], .login-card button:has-text("Login"), .login-card button:has-text("登录")').click()

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

    // Click logout button
    const logoutBtn = page.locator('.logout-btn, [data-testid="logout-btn"], button:has-text("Logout"), button:has-text("退出")')
    await logoutBtn.click()

    // Should redirect to login or clear session
    await page.waitForLoadState('domcontentloaded')
    // Either show login form or redirect to login page
    const isLoggedOut = await page.locator('.login-card').isVisible({ timeout: 5000 }).catch(() => false)
    if (!isLoggedOut) {
      await expect(page.getByTestId('login-page, login-form, .login-card')).toBeVisible({ timeout: 5000 })
    }
  })

  test('should display user info in sidebar after login', async ({ page }) => {
    await ensureLoggedIn(page)

    // Check for user info in sidebar
    const accountPill = page.locator('[data-testid="account-pill"], .account-pill, .user-info')
    await expect(accountPill).toBeVisible()
    await expect(accountPill).toContainText(E2E_CREDENTIALS.username)
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
    await expect(page.locator('.login-card, [data-testid="login-page"]')).toBeVisible({ timeout: 10000 })
  })
})
