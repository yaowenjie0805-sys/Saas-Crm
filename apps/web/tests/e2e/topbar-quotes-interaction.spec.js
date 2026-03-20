import { expect, test } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'

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
  await ensureLoggedIn(page)
  await page.getByTestId('nav-dashboard').click()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
}

test('topbar refresh and quotes lightweight interactions remain healthy', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await login(page)
  await expect(page.getByTestId('topbar')).toBeVisible()
  await page.keyboard.press('ControlOrMeta+K')
  await expect(page.getByTestId('topbar-search-input')).toBeFocused()
  await page.getByTestId('topbar-search-input').fill('q4')
  await page.keyboard.press('Enter')
  await page.getByTestId('topbar-search-clear').click()
  await expect(page.getByTestId('topbar-search-input')).toHaveValue('')

  await page.getByTestId('topbar-refresh').click()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
  await expectHealthy(page, pageErrors)

  await page.getByTestId('nav-quotes').click()
  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Quotes')
  await expect(page.getByTestId('quote-items-panel')).toBeVisible()
  await page.getByTestId('topbar-search-input').fill('bridge-owner')
  await page.keyboard.press('Enter')
  await expect(page.getByTestId('quotes-owner-filter')).toHaveValue('bridge-owner')

  await page.getByTestId('quotes-owner-filter').fill('admin')
  await page.getByTestId('quotes-query').click()
  await page.getByTestId('quotes-select-all').click()
  await page.getByTestId('quotes-select-all').click()
  await page.getByTestId('quotes-refresh').click()

  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('quote-items-panel')).toBeVisible()
  await expectHealthy(page, pageErrors)
})
