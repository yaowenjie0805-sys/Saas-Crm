import { expect, test } from '@playwright/test'
import { ensureLoggedIn, seedAuthSession } from './helpers/auth.js'

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

function filterUnexpectedPageErrors(pageErrors, { allowNetworkFailure = false } = {}) {
  return pageErrors.filter((message) => {
    if (message === 'signal is aborted without reason') return false
    if (allowNetworkFailure && message === 'Failed to fetch') return false
    return true
  })
}

async function expectHealthy(page, pageErrors, options = {}) {
  await expect(page.getByTestId('error-boundary')).toHaveCount(0)
  const unexpectedErrors = filterUnexpectedPageErrors(pageErrors, options)
  expect(unexpectedErrors, `Unexpected page errors:\n${pageErrors.join('\n')}`).toEqual([])
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

async function gotoDashboard(page) {
  await page.getByTestId('nav-dashboard').click({ timeout: 15_000 })
  await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
}

test('topbar refresh and quotes lightweight interactions remain healthy', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  const loginState = await login(page)
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
  await expectHealthy(page, pageErrors, { allowNetworkFailure: loginState.fallbackMode })

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
  await expectHealthy(page, pageErrors, { allowNetworkFailure: loginState.fallbackMode })
})
