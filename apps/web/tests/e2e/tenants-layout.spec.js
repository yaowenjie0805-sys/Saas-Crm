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

async function assertTenantLayout(page) {
  await page.goto('/admin/tenants', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('tenants-page')).toBeVisible()
  await expect(page.locator('.table-row.tenant-row').first()).toBeVisible()

  const layout = await page.evaluate(() => {
    const panel = document.querySelector('[data-testid="tenants-page"]')
    const rows = Array.from(panel?.querySelectorAll('.table-row.tenant-row') || [])
    const rowLayout = rows.map((row) => {
      const rowRect = row.getBoundingClientRect()
      const grid = row.querySelector('.tenant-config-grid')
      const gridRect = grid ? grid.getBoundingClientRect() : null
      return {
        rowTop: rowRect.top,
        rowBottom: rowRect.bottom,
        gridBottom: gridRect ? gridRect.bottom : null,
      }
    })

    const jumpBtn = Array.from(panel?.querySelectorAll('button') || []).find((btn) => {
      const text = String(btn.textContent || '').trim()
      return text === '跳转' || /^go$/i.test(text)
    })
    const jumpTop = jumpBtn ? jumpBtn.getBoundingClientRect().top : null
    const lastRowBottom = rowLayout.length ? rowLayout[rowLayout.length - 1].rowBottom : null
    return { rowLayout, jumpTop, lastRowBottom }
  })

  expect(layout.rowLayout.length).toBeGreaterThan(0)
  for (const row of layout.rowLayout) {
    expect(row.gridBottom).not.toBeNull()
    expect(row.gridBottom).toBeLessThanOrEqual(row.rowBottom + 1)
  }

  expect(layout.jumpTop).not.toBeNull()
  expect(layout.lastRowBottom).not.toBeNull()
  expect(layout.jumpTop).toBeGreaterThanOrEqual(layout.lastRowBottom - 8)
}

test('tenant config cell stays inside row and does not cover pager', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await ensureLoggedIn(page)
  await assertTenantLayout(page)
  await expectHealthy(page, pageErrors)
})

test('tenant config layout remains contained on narrow viewport', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 1280, height: 720 })
  await ensureLoggedIn(page)
  await assertTenantLayout(page)
  await expectHealthy(page, pageErrors)
})

test('tenant config grid is scrollable and save button remains clickable', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await ensureLoggedIn(page)
  await page.setViewportSize({ width: 1366, height: 768 })
  await page.goto('/admin/tenants', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('tenants-page')).toBeVisible()

  const firstGrid = page.locator('.tenant-row .tenant-config-grid').first()
  await expect(firstGrid).toBeVisible()
  const scrollState = await firstGrid.evaluate((el) => {
    const before = el.scrollTop
    const scrollHeight = el.scrollHeight
    const clientHeight = el.clientHeight
    el.scrollTop = scrollHeight
    const after = el.scrollTop
    return { before, after, scrollHeight, clientHeight }
  })
  expect(scrollState.scrollHeight).toBeGreaterThan(scrollState.clientHeight)
  expect(scrollState.after).toBeGreaterThanOrEqual(scrollState.before)

  const firstSave = page.locator('.tenant-row .tenant-save-btn').first()
  await firstSave.scrollIntoViewIfNeeded()
  await expect(firstSave).toBeVisible()
  const saveResponse = page.waitForResponse(
    (resp) => resp.url().includes('/api/v1/tenants/') && resp.request().method() === 'PATCH',
    { timeout: 15_000 },
  )
  await firstSave.click()
  const resp = await saveResponse
  expect(resp.ok()).toBeTruthy()

  await expectHealthy(page, pageErrors)
})

test('tenant config switches to mobile list and save remains clickable on phone', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await ensureLoggedIn(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/admin/tenants', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('tenants-page')).toBeVisible()

  await expect(page.locator('.tenant-mobile-list')).toBeVisible()
  await expect(page.locator('.tenant-table .table-head')).toHaveCount(0)

  const firstSave = page.locator('.tenant-row .tenant-save-btn').first()
  await firstSave.scrollIntoViewIfNeeded()
  await expect(firstSave).toBeVisible()
  const saveResponse = page.waitForResponse(
    (resp) => resp.url().includes('/api/v1/tenants/') && resp.request().method() === 'PATCH',
    { timeout: 15_000 },
  )
  await firstSave.click()
  const resp = await saveResponse
  expect(resp.ok()).toBeTruthy()

  await expectHealthy(page, pageErrors)
})
