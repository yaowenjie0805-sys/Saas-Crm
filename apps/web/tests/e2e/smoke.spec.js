import { expect, test } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth.js'
import { expectHealthyPage } from './helpers/health.js'

function collectPageErrors(page) {
  const errors = []
  page.on('pageerror', (error) => {
    const message = error.message || ''
    if (
      message.includes('favicon') ||
      message.includes('DevTools') ||
      message.includes('websocket') ||
      message.includes('signal is aborted') ||
      message.includes('AbortError') ||
      message.includes('X-Tenant-Id') ||
      message.includes('Bearer Token') ||
      message.includes('Token') && message.includes('expired')
    ) {
      return
    }
    errors.push(message)
  })
  return errors
}

test('login and shell navigation smoke', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await ensureLoggedIn(page)
  await page.getByTestId('nav-dashboard').click()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
  await expect(page.getByTestId('topbar')).toBeVisible()
  await expect(page.getByTestId('account-pill')).toContainText(/admin|system admin/i)
  await expectHealthyPage(page, pageErrors)

  await page.getByTestId('nav-customers').click()
  await expect(page.getByTestId('page-title')).toHaveText('Customers')
  await expectHealthyPage(page, pageErrors)
})
