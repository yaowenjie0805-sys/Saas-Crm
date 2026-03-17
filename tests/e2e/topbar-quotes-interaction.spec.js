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
  await page.getByTestId('login-page').waitFor()
  await page.getByTestId('login-tenant-id').fill(tenantId)
  await page.getByTestId('login-username').fill(username)
  await page.getByTestId('login-password').fill(password)
  await page.getByTestId('login-submit').click()
  await expect(page.getByTestId('app-sidebar')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
}

test('topbar refresh and quotes lightweight interactions remain healthy', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await login(page)
  await expect(page.getByTestId('topbar')).toBeVisible()

  await page.getByTestId('topbar-refresh').click()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
  await expectHealthy(page, pageErrors)

  await page.getByTestId('nav-quotes').click()
  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Quotes')
  await expect(page.getByTestId('quote-items-panel')).toBeVisible()

  await page.getByTestId('quotes-owner-filter').fill('admin')
  await page.getByTestId('quotes-query').click()
  await page.getByTestId('quotes-select-all').click()
  await page.getByTestId('quotes-select-all').click()
  await page.getByTestId('quotes-refresh').click()

  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('quote-items-panel')).toBeVisible()
  await expectHealthy(page, pageErrors)
})
