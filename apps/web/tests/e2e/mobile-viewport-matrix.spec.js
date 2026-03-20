import { expect, test } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'

const TITLES = {
  dashboard: /Dashboard|总览/,
  quotes: /Quotes|报价/,
  orders: /Orders|订单/,
}

const DETAIL_BTN_TEXT = /Detail|Details|详情/
const CLOSE_BTN_TEXT = /Close|关闭/

const MOBILE_VIEWPORTS = [
  { name: 'iphone-se', width: 375, height: 667 },
  { name: 'iphone-14', width: 390, height: 844 },
  { name: 'pixel-7', width: 412, height: 915 },
]

async function openMobileMenu(page) {
  const menu = page.locator('.menu.grouped')
  if (await menu.isVisible()) return
  await page.locator('.mobile-menu-toggle').click()
  await expect(menu).toBeVisible()
}

async function gotoMobileNav(page, navTestId, title) {
  await openMobileMenu(page)
  await page.getByTestId(navTestId).click()
  await expect(page.getByTestId('page-title')).toContainText(title)
}

for (const viewport of MOBILE_VIEWPORTS) {
  test(`mobile matrix ${viewport.name}: dashboard/quotes/orders primary interactions`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await ensureLoggedIn(page)

    await gotoMobileNav(page, 'nav-dashboard', TITLES.dashboard)
    await expect(page.getByTestId('topbar-refresh')).toBeVisible()
    await page.getByTestId('topbar-refresh').click()

    const dashLayout = await page.evaluate(() => ({
      vw: window.innerWidth,
      sw: document.documentElement.scrollWidth,
    }))
    expect(dashLayout.sw).toBeLessThanOrEqual(dashLayout.vw + 1)

    await gotoMobileNav(page, 'nav-quotes', TITLES.quotes)
    await expect(page.getByTestId('quotes-page')).toBeVisible()
    const quoteDetailBtn = page
      .locator('[data-testid="quotes-page"] .table-row .mini-btn')
      .filter({ hasText: DETAIL_BTN_TEXT })
      .first()
    await quoteDetailBtn.click()
    await expect(page.locator('.modal-mask')).toBeVisible()
    await page.locator('.modal-mask .mini-btn').filter({ hasText: CLOSE_BTN_TEXT }).first().click()

    await gotoMobileNav(page, 'nav-orders', TITLES.orders)
    await expect(page.getByTestId('page-orders')).toBeVisible()
    const orderDetailBtn = page.getByTestId('page-orders').getByRole('button', { name: DETAIL_BTN_TEXT }).first()
    await orderDetailBtn.click()
    await expect(page.locator('.modal-mask')).toBeVisible()
  })
}