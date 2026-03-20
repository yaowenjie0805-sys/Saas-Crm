import { expect, test } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'

const TITLES = {
  dashboard: /Dashboard|总览/,
  contracts: /Contracts|合同/,
  customers: /Customers|客户/,
  quotes: /Quotes|报价/,
  orders: /Orders|订单/,
}

const DETAIL_BTN_TEXT = /Detail|Details|详情/
const CLOSE_BTN_TEXT = /Close|关闭/

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
  await expect(page.getByTestId('page-dashboard')).toBeVisible()

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
  expect(mobileLayout.topbarWidth).toBeLessThanOrEqual(mobileLayout.viewportWidth + 4)

  await expectHealthy(page, pageErrors)
})

test('mobile contracts keeps filters reachable and row actions clickable', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 390, height: 844 })
  await ensureLoggedIn(page)
  await gotoMobileNav(page, 'nav-contracts', TITLES.contracts)

  await expect(page.getByTestId('page-contracts')).toBeVisible()

  const firstFormInput = page.locator('[data-testid="page-contracts"] .inline-tools .tool-input').first()
  await expect(firstFormInput).toBeVisible()

  const noOverflow = await page.evaluate(() => {
    const doc = document.documentElement
    const contractsPage = document.querySelector('[data-testid="page-contracts"]')
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

  await expect(page.locator('[data-testid="page-contracts"] .table-row').first()).toBeVisible()
  const detailBtn = page.locator('[data-testid="page-contracts"] .table-row .mini-btn').filter({ hasText: DETAIL_BTN_TEXT }).first()
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

  await expect(page.getByTestId('customers-page')).toBeVisible()
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
  await expect(page.getByTestId('quotes-page')).toBeVisible()
  await expect(page.getByTestId('quotes-owner-filter')).toBeVisible()
  const quoteDetail = page.locator('[data-testid="quotes-page"] .table-row .mini-btn').filter({ hasText: DETAIL_BTN_TEXT }).first()
  await quoteDetail.scrollIntoViewIfNeeded()
  await quoteDetail.click()
  await expect(page.locator('.modal-mask')).toBeVisible()
  await page.locator('.modal-mask .mini-btn').filter({ hasText: CLOSE_BTN_TEXT }).first().click()

  await gotoMobileNav(page, 'nav-orders', TITLES.orders)
  await expect(page.getByTestId('page-orders')).toBeVisible()
  const orderDetail = page.getByTestId('page-orders').getByRole('button', { name: DETAIL_BTN_TEXT }).first()
  await expect(orderDetail).toBeVisible()
  await orderDetail.click()
  await expect(page.locator('.modal-mask')).toBeVisible()

  await expectHealthy(page, pageErrors)
})
