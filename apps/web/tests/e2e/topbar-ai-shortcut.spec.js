import { expect, test } from '@playwright/test'
import { ensureLoggedIn, seedAuthSession } from './helpers/auth.js'
import { expectHealthyPage } from './helpers/health.js'

const FALLBACK_SESSION = {
  username: 'admin',
  displayName: 'admin',
  role: 'ADMIN',
  ownerScope: '',
  tenantId: 'tenant_default',
  department: '',
  dataScope: '',
  dateFormat: 'yyyy-MM-dd',
  token: 'COOKIE_SESSION',
  sessionActive: true,
}

function collectPageErrors(page) {
  const errors = []
  page.on('pageerror', (error) => {
    errors.push(error.message)
  })
  return errors
}

function isApiConnectFailure(error) {
  const message = String(error?.message || '')
  return ['ECONNREFUSED', 'ECONNRESET', 'ETIMEDOUT'].some((token) => message.includes(token))
}

async function seedFallbackSession(page) {
  await seedAuthSession(page, FALLBACK_SESSION)
  await page.goto('/', { waitUntil: 'networkidle' })
}

async function login(page) {
  try {
    await ensureLoggedIn(page)
    return { fallbackMode: false }
  } catch (error) {
    if (!isApiConnectFailure(error)) throw error
    await seedFallbackSession(page)
    return { fallbackMode: true }
  }
}

test('topbar AI shortcut jumps to dashboard AI panel', async ({ page }) => {
  const pageErrors = collectPageErrors(page)
  const loginState = await login(page)

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
