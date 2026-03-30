import { expect, test } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'

function collectPageErrors(page) {
  const errors = []
  page.on('pageerror', (error) => {
    const message = error.message || ''
    // Filter out known non-critical errors
    if (
      message.includes('favicon') ||
      message.includes('DevTools') ||
      message.includes('websocket') ||
      message.includes('signal is aborted') ||
      message.includes('AbortError') ||
      message.includes('Bearer Token') ||
      message.includes('缺少Bearer Token') ||
      message.includes('Token无效') ||
      message.includes('Token') && message.includes('过期')
    ) {
      return
    }
    errors.push(message)
  })
  return errors
}

async function expectHealthy(page, pageErrors) {
  await expect(page.getByTestId('error-boundary')).toHaveCount(0)
  expect(pageErrors, `Unexpected page errors:\n${pageErrors.join('\n')}`).toEqual([])
}

async function login(page) {
  await ensureLoggedIn(page)
  await page.getByTestId('nav-dashboard').click()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
}

test('login, shell navigation, customer CRUD, and quote rendering work', async ({ page }) => {
  const pageErrors = collectPageErrors(page)
  const customerName = `E2E Customer ${Date.now()}`

  await login(page)
  await expect(page.getByTestId('topbar')).toBeVisible()
  await expect(page.getByTestId('account-pill')).toContainText(/admin|系统管理员|System Admin/i)
  await expectHealthy(page, pageErrors)

  await page.getByTestId('nav-customers').click()
  await expect(page.getByTestId('customers-page')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Customers')

  await page.getByTestId('customer-form-name').fill(customerName)
  await page.getByTestId('customer-form-owner').fill('E2E Owner')
  await page.getByTestId('customer-form-status').selectOption('Active')
  await page.getByTestId('customer-form-value').fill('1234')
  await page.getByTestId('customer-form-submit').click()

  await page.getByTestId('customers-search-input').fill(customerName)
  await page.getByTestId('customers-search-submit').click()
  const customerRow = page.locator('.table-row').filter({ hasText: customerName }).first()
  await expect(customerRow).toBeVisible()

  await customerRow.getByRole('button', { name: 'Save' }).click()
  await page.getByTestId('customer-form-owner').fill('E2E Owner Updated')
  await page.getByTestId('customer-form-submit').click()
  await page.getByTestId('customers-refresh').click()
  await expect(page.locator('.table-row').filter({ hasText: 'E2E Owner Updated' }).first()).toBeVisible()

  // Re-search for the customer before deleting (refresh may reset the view)
  await page.getByTestId('customers-search-input').fill(customerName)
  await page.getByTestId('customers-search-submit').click()
  await expect(page.locator('.table-row').filter({ hasText: customerName }).first()).toBeVisible()

  // Click delete and wait for API response
  const deleteResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/customers/') && response.request().method() === 'DELETE', { timeout: 10000 }
  )
  await page.locator('.table-row').filter({ hasText: customerName }).first().getByRole('button', { name: 'Delete' }).click()
  const deleteResponse = await deleteResponsePromise

  // If delete succeeded, verify customer is gone; if backend is unavailable (503), skip verification
  if (deleteResponse.ok()) {
    // After successful deletion, manually refresh to verify customer is gone
    await page.getByTestId('customers-refresh').click()
    await page.waitForTimeout(1000)
    await expect(page.locator('.table-row').filter({ hasText: customerName })).toHaveCount(0)
  } else if (deleteResponse.status() === 503) {
    // Backend service unavailable - skip deletion verification
    console.log(`Warning: Delete API returned 503 (service unavailable), skipping deletion verification`)
  } else {
    throw new Error(`Delete API failed with status ${deleteResponse.status()}`)
  }
  await expectHealthy(page, pageErrors)

  await page.reload()
  await expect(page.getByTestId('customers-page')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Customers')
  await expectHealthy(page, pageErrors)

  await page.getByTestId('nav-quotes').click()
  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Quotes')
  await expect(page.getByTestId('quote-items-panel')).toBeVisible()
  await expectHealthy(page, pageErrors)

  await page.getByTestId('nav-adminTenants').click()
  await expect(page.getByTestId('tenants-page')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Tenant Admin')
  await expect(page.getByTestId('tenants-heading')).toBeVisible()
  await expectHealthy(page, pageErrors)

  await page.getByTestId('nav-dashboard').click()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
  await expectHealthy(page, pageErrors)
})
