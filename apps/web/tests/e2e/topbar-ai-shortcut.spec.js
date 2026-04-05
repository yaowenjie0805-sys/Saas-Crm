import { expect, test } from '@playwright/test'
import { collectPageErrors, expectHealthyPage } from './helpers/health.js'
import { loginWithFallback } from './helpers/sessionFallback.js'

test('topbar AI shortcut jumps to dashboard AI panel', async ({ page }) => {
  const pageErrors = collectPageErrors(page)
  const loginState = await loginWithFallback(page)

  await page.getByTestId('nav-products').click()
  await expect(page.getByTestId('products-page')).toBeVisible()
  await expect(page.getByTestId('topbar-ai-shortcut')).toBeVisible()

  await page.getByTestId('topbar-ai-shortcut').click()

  await expect(page.getByTestId('page-dashboard')).toBeVisible()
  await expect(page.getByTestId('ai-followup-summary-panel')).toBeVisible()
  await expect(page.getByTestId('ai-followup-summary-input')).toBeVisible()

  await expectHealthyPage(page, pageErrors, {
    ignoreErrorMessages: [
      'signal is aborted without reason',
      ...(loginState.fallbackMode ? ['Failed to fetch'] : []),
    ],
  })
})
