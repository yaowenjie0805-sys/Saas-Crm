import { expect, test } from '@playwright/test'
import { collectPageErrors, expectHealthyPage } from './helpers/health.js'
import { loginWithFallback } from './helpers/sessionFallback.js'

async function gotoDashboard(page) {
  await page.getByTestId('nav-dashboard').click({ timeout: 15_000 })
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
}

test('topbar refresh and quotes lightweight interactions remain healthy', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  const loginState = await loginWithFallback(page)
  await gotoDashboard(page)
  await expect(page.getByTestId('topbar')).toBeVisible()
  await page.keyboard.press('ControlOrMeta+K')
  await expect(page.getByTestId('topbar-search-input')).toBeFocused()
  await page.getByTestId('topbar-search-input').fill('q4')
  await page.keyboard.press('Enter')
  await page.getByTestId('topbar-search-clear').click()
  await expect(page.getByTestId('topbar-search-input')).toHaveValue('')

  await page.getByTestId('topbar-refresh').click()
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
  await expectHealthyPage(page, pageErrors, {
    ignoreErrorMessages: [
      'signal is aborted without reason',
      ...(loginState.fallbackMode ? ['Failed to fetch'] : []),
    ],
  })

  await page.getByTestId('nav-quotes').click()
  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('page-title')).toHaveText('Quotes')
  await expect(page.getByTestId('quote-items-panel')).toBeVisible()
  await page.getByTestId('topbar-search-input').fill('bridge-owner')
  await page.keyboard.press('Enter')
  await expect(page.getByTestId('quotes-owner-filter')).toHaveValue('bridge-owner')

  await page.getByTestId('quotes-owner-filter').fill('admin')
  await page.getByTestId('quotes-query').click()
  const selectAll = page.getByTestId('quotes-select-all')
  await expect(selectAll).toBeVisible()
  await selectAll.click()
  await selectAll.click()
  await page.getByTestId('quotes-refresh').click()

  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('quote-items-panel')).toBeVisible()
  await expectHealthyPage(page, pageErrors, {
    ignoreErrorMessages: [
      'signal is aborted without reason',
      ...(loginState.fallbackMode ? ['Failed to fetch'] : []),
    ],
  })
})
