import { expect, test } from '@playwright/test'

const tenantId = 'tenant_default'
const username = 'admin'
const password = 'admin123'

function collectPageErrors(page) {
  const errors = []
  page.on('pageerror', (error) => {
    errors.push(error.message)
  })
  return errors
}

async function expectHealthy(page, pageErrors) {
  await expect(page.getByTestId('error-boundary')).toHaveCount(0)
  expect(pageErrors, `Unexpected page errors:\n${pageErrors.join('\n')}`).toEqual([])
}

async function login(page) {
  await page.goto('/')
  await expect(page).toHaveURL(/\/login$/)
  await page.getByTestId('login-page').waitFor()
  await page.getByTestId('login-tenant-id').fill(tenantId)
  await page.getByTestId('login-username').fill(username)
  await page.getByTestId('login-password').fill(password)
  await page.getByTestId('login-submit').click()
  await expect(page.getByTestId('app-sidebar')).toBeVisible()
  await expect(page).not.toHaveURL(/\/login$/)
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
}

test('login, shell navigation, customer CRUD, and quote rendering work', async ({ page }) => {
  const pageErrors = collectPageErrors(page)
  const customerName = `E2E Customer ${Date.now()}`

  await login(page)
  await expect(page.getByTestId('topbar')).toBeVisible()
  await expect(page.getByTestId('account-pill')).toContainText('Admin')
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

  await page.locator('.table-row').filter({ hasText: customerName }).first().getByRole('button', { name: 'Delete' }).click()
  await page.getByTestId('customers-refresh').click()
  await expect(page.locator('.table-row').filter({ hasText: customerName })).toHaveCount(0)
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
