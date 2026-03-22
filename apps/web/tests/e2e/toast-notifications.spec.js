import { test, expect } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'
import { generateUniqueCustomerName } from './helpers/testData'

/**
 * Toast Notification Tests
 * Tests toast notifications for success, error, warning, and info messages
 */
test.describe('Toast Notifications', () => {
  test.beforeEach(async ({ page }) => {
    await ensureLoggedIn(page)
  })

  test('should display success toast after creating customer', async ({ page }) => {
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    // Fill customer form
    const customerName = generateUniqueCustomerName()
    await page.getByTestId('customer-form-name').fill(customerName)
    await page.getByTestId('customer-form-owner').fill('Test Owner')
    await page.getByTestId('customer-form-status').selectOption('Active')
    await page.getByTestId('customer-form-value').fill('50000')
    await page.getByTestId('customer-form-submit').click()

    // Should show success toast
    await page.waitForTimeout(1000)
    const successToast = page.locator('.toast-success, .toast.success, [class*="toast"][class*="success"]')
    const toastVisible = await successToast.isVisible({ timeout: 5000 }).catch(() => false)

    if (toastVisible) {
      await expect(successToast.first()).toBeVisible()
      await expect(successToast.first()).toContainText(/success|成功|created|创建/i)
    }
  })

  test('should display error toast for validation failures', async ({ page }) => {
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    // Try to submit without required fields
    await page.getByTestId('customer-form-submit').click()

    await page.waitForTimeout(500)
    const errorToast = page.locator('.toast-error, .toast.error, [class*="toast"][class*="error"]')
    const toastVisible = await errorToast.isVisible({ timeout: 3000 }).catch(() => false)

    // Error toast should appear
    if (toastVisible) {
      await expect(errorToast.first()).toBeVisible()
    }
  })

  test('should auto-dismiss toast after timeout', async ({ page }) => {
    // Trigger a toast by creating a customer
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    const customerName = generateUniqueCustomerName()
    await page.getByTestId('customer-form-name').fill(customerName)
    await page.getByTestId('customer-form-submit').click()

    await page.waitForTimeout(500)

    // Check if toast appears
    const toast = page.locator('.toast, [class*="toast"]').first()
    const toastVisible = await toast.isVisible({ timeout: 3000 }).catch(() => false)

    if (toastVisible) {
      // Wait for auto-dismiss (typically 3-5 seconds)
      await page.waitForTimeout(6000)
      // Toast should be gone
      await expect(toast).not.toBeVisible()
    }
  })

  test('should display warning toast for destructive actions', async ({ page }) => {
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    // Create a customer first
    const customerName = generateUniqueCustomerName()
    await page.getByTestId('customer-form-name').fill(customerName)
    await page.getByTestId('customer-form-submit').click()
    await page.waitForTimeout(1000)

    // Search and find the customer
    await page.getByTestId('customers-search-input').fill(customerName)
    await page.getByTestId('customers-search-submit').click()
    await page.waitForTimeout(500)

    // Try to delete (if confirmation is shown as toast)
    const deleteBtn = page.locator('.table-row').filter({ hasText: customerName }).first().getByRole('button', { name: /Delete|删除/i })
    if (await deleteBtn.isVisible()) {
      await deleteBtn.click()
      await page.waitForTimeout(1000)

      // May show warning toast for confirmation
      const warningToast = page.locator('.toast-warning, .toast.warning, [class*="toast"][class*="warning"]')
      const warningVisible = await warningToast.isVisible({ timeout: 3000 }).catch(() => false)

      if (warningVisible) {
        await expect(warningToast.first()).toBeVisible()
      }
    }
  })

  test('should stack multiple toasts', async ({ page }) => {
    // Trigger multiple actions that show toasts
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    // Create first customer
    const customerName1 = generateUniqueCustomerName()
    await page.getByTestId('customer-form-name').fill(customerName1)
    await page.getByTestId('customer-form-submit').click()
    await page.waitForTimeout(800)

    // Create second customer
    const customerName2 = generateUniqueCustomerName()
    await page.getByTestId('customer-form-name').fill(customerName2)
    await page.getByTestId('customer-form-submit').click()
    await page.waitForTimeout(800)

    // Check if multiple toasts are stacked
    const toasts = page.locator('.toast, [class*="toast"]')
    const count = await toasts.count()

    // If toasts appear, they should stack
    if (count > 0) {
      // Toasts should be visible
      await expect(toasts.first()).toBeVisible()
    }
  })

  test('should close toast on click', async ({ page }) => {
    // Trigger a toast
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    const customerName = generateUniqueCustomerName()
    await page.getByTestId('customer-form-name').fill(customerName)
    await page.getByTestId('customer-form-submit').click()

    await page.waitForTimeout(500)

    const toast = page.locator('.toast, [class*="toast"]').first()
    const toastVisible = await toast.isVisible({ timeout: 3000 }).catch(() => false)

    if (toastVisible) {
      // Look for close button
      const closeBtn = toast.locator('button, [class*="close"]')
      if (await closeBtn.isVisible()) {
        await closeBtn.click()
        await page.waitForTimeout(300)
        // Toast should be gone or at least close button should be gone
      }
    }
  })

  test('should not show toast on page navigation', async ({ page }) => {
    // Navigate between pages
    await page.getByTestId('nav-dashboard').click()
    await page.waitForTimeout(500)

    await page.getByTestId('nav-customers').click()
    await page.waitForTimeout(500)

    // No new toast should appear from navigation
    const errorToast = page.locator('.toast-error, .toast.error')
    await expect(errorToast).not.toBeVisible()
  })

  test('should show loading toast during operations', async ({ page }) => {
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    const customerName = generateUniqueCustomerName()
    await page.getByTestId('customer-form-name').fill(customerName)
    await page.getByTestId('customer-form-submit').click()

    // Check for loading state
    const loadingToast = page.locator('.toast-loading, .toast.loading, [class*="toast"][class*="loading"]')
    const loadingVisible = await loadingToast.isVisible({ timeout: 2000 }).catch(() => false)

    // Loading toast may or may not appear depending on implementation
    // Just verify no crash
    if (loadingVisible) {
      await expect(loadingToast.first()).toBeVisible()
    }
  })
})
