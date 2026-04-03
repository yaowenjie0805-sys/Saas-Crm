import { expect, test } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth.js'
import { expectHealthyPage } from './helpers/health.js'

const TITLES = {
  dashboard: /Dashboard|总览/,
  contracts: /Contracts|合同/,
  customers: /Customers|客户/,
  quotes: /Quotes|报价/,
  orders: /Orders|订单/,
}

const DETAIL_BTN_TEXT = /Detail|Details|详情/
const CLOSE_BTN_TEXT = /Close|关闭/
const IGNORABLE_PAGE_ERROR_PATTERNS = [/signal is aborted without reason/i]

function collectPageErrors(page) {
  const errors = []
  page.on('pageerror', (error) => {
    errors.push(error.message)
  })
  return errors
}
async function expectHealthy(page, pageErrors) {
  await expectHealthyPage(page, pageErrors, {
    ignoreErrorPatterns: IGNORABLE_PAGE_ERROR_PATTERNS,
  })
}

async function openMobileMenu(page) {
  const menu = page.locator('.menu.grouped')
  const mobileToggle = page.locator('.mobile-menu-toggle')
  if (await menu.isVisible()) return
  await mobileToggle.click()
  await expect(menu).toBeVisible()
}

async function gotoMobileNav(page, navTestId, expectedTitle) {
  await openMobileMenu(page)
  await page.getByTestId(navTestId).click()
  await expect(page.getByTestId('page-title')).toContainText(expectedTitle)
}

test('mobile dashboard remains usable without horizontal overflow', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 390, height: 844 })
  await ensureLoggedIn(page)
  await gotoMobileNav(page, 'nav-dashboard', TITLES.dashboard)

  await expect(page.getByTestId('topbar')).toBeVisible()
  await expect(page.getByTestId('topbar-search-input')).toBeVisible()
  await page.getByTestId('topbar-refresh').click()
  await expect(page.getByTestId('page-title')).toContainText(TITLES.dashboard)

  const mobileLayout = await page.evaluate(() => {
    const doc = document.documentElement
    const topbar = document.querySelector('[data-testid="topbar"]')
    const topbarRect = topbar?.getBoundingClientRect() || null
    return {
      docScrollWidth: doc.scrollWidth,
      viewportWidth: window.innerWidth,
      topbarWidth: topbarRect ? topbarRect.width : null,
    }
  })

  expect(mobileLayout.docScrollWidth).toBeLessThanOrEqual(mobileLayout.viewportWidth + 1)
  expect(mobileLayout.topbarWidth).not.toBeNull()
  expect(mobileLayout.topbarWidth).toBeLessThanOrEqual(mobileLayout.viewportWidth + 10)

  await expectHealthy(page, pageErrors)
})

test('mobile contracts keeps filters reachable and row actions clickable', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 390, height: 844 })
  await ensureLoggedIn(page)
  await gotoMobileNav(page, 'nav-contracts', TITLES.contracts)

  const firstFormInput = page.locator('main .inline-tools .tool-input').first()
  await expect(firstFormInput).toBeVisible()

  const noOverflow = await page.evaluate(() => {
    const doc = document.documentElement
    const contractsPage = document.querySelector('main')
    const panelRect = contractsPage?.getBoundingClientRect() || null
    return {
      docScrollWidth: doc.scrollWidth,
      viewportWidth: window.innerWidth,
      panelRight: panelRect ? panelRect.right : null,
    }
  })
  expect(noOverflow.docScrollWidth).toBeLessThanOrEqual(noOverflow.viewportWidth + 1)
  expect(noOverflow.panelRight).not.toBeNull()
  expect(noOverflow.panelRight).toBeLessThanOrEqual(noOverflow.viewportWidth + 1)

  await expect(page.locator('.table-row').first()).toBeVisible()
  const detailBtn = page.locator('.table-row .mini-btn').filter({ hasText: DETAIL_BTN_TEXT }).first()
  await detailBtn.scrollIntoViewIfNeeded()
  await detailBtn.click()
  await expect(page.locator('.detail-drawer')).toBeVisible()

  await expectHealthy(page, pageErrors)
})

test('mobile customers supports search and opens detail drawer', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 390, height: 844 })
  await ensureLoggedIn(page)
  await gotoMobileNav(page, 'nav-customers', TITLES.customers)

  await expect(page.getByTestId('page-title')).toContainText(TITLES.customers)
  await page.getByTestId('customers-search-input').fill('')
  await page.getByTestId('customers-search-submit').click()

  const firstDetail = page.locator('[data-testid="customers-page"] .table-row .mini-btn').filter({ hasText: DETAIL_BTN_TEXT }).first()
  await firstDetail.scrollIntoViewIfNeeded()
  await firstDetail.click()
  await expect(page.locator('.detail-drawer')).toBeVisible()

  await expectHealthy(page, pageErrors)
})

test('mobile quotes and orders keep action entry points clickable', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 390, height: 844 })
  await ensureLoggedIn(page)

  await gotoMobileNav(page, 'nav-quotes', TITLES.quotes)
  await expect(page.getByTestId('page-title')).toContainText(TITLES.quotes)
  await expect(page.getByTestId('quotes-owner-filter')).toBeVisible()
  const quoteDetail = page.locator('.table-row .mini-btn').filter({ hasText: DETAIL_BTN_TEXT }).first()
  await quoteDetail.scrollIntoViewIfNeeded()
  await quoteDetail.click()
  await expect(page.locator('.modal-mask')).toBeVisible()
  await page.locator('.modal-mask .mini-btn').filter({ hasText: CLOSE_BTN_TEXT }).first().click()

  await gotoMobileNav(page, 'nav-orders', TITLES.orders)
  await expect(page.getByTestId('page-title')).toContainText(TITLES.orders)
  const orderDetail = page.getByRole('button', { name: DETAIL_BTN_TEXT }).first()
  await expect(orderDetail).toBeVisible()
  await orderDetail.click()
  await expect(page.locator('.modal-mask')).toBeVisible()

  await expectHealthy(page, pageErrors)
})

