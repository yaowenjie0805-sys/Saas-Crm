import { expect, test } from '@playwright/test'

const MOCK_AUTH = {
  username: 'admin',
  displayName: 'System Admin',
  role: 'ADMIN',
  ownerScope: '',
  tenantId: 'tenant_default',
  department: '',
  dataScope: '',
  dateFormat: 'yyyy-MM-dd',
  token: 'e2e-mock-token',
  sessionActive: true,
}

const MOCK_TENANTS = [
  {
    id: 'tenant_default',
    name: 'Default Tenant',
    status: 'ACTIVE',
    quotaUsers: 120,
    timezone: 'Asia/Shanghai',
    currency: 'CNY',
    marketProfile: 'CN',
    taxRule: 'VAT_CN',
    approvalMode: 'STRICT',
    channels: '["WECOM","DINGTALK"]',
    dataResidency: 'CN',
    maskLevel: 'STANDARD',
    dateFormat: 'yyyy-MM-dd',
  },
  {
    id: 'tenant_global',
    name: 'Global Tenant',
    status: 'ACTIVE',
    quotaUsers: 320,
    timezone: 'UTC',
    currency: 'USD',
    marketProfile: 'GLOBAL',
    taxRule: 'VAT_GLOBAL',
    approvalMode: 'STAGE_GATE',
    channels: '["EMAIL","SLACK"]',
    dataResidency: 'GLOBAL',
    maskLevel: 'STANDARD',
    dateFormat: 'yyyy-MM-dd',
  },
]

function collectPageErrors(page) {
  const errors = []
  page.on('pageerror', (error) => {
    const message = String(error.message || '')
    if (message.includes('signal is aborted') || message.includes('AbortError')) return
    errors.push(message)
  })
  return errors
}

async function mockTenantApi(page) {
  let tenantRows = MOCK_TENANTS.map((row) => ({ ...row }))

  const json = (route, payload, status = 200) => route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(payload),
  })

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method().toUpperCase()

    if (!path.startsWith('/api/')) {
      await route.continue()
      return
    }

    if (path === '/api/v1/auth/login' && method === 'POST') {
      await json(route, MOCK_AUTH)
      return
    }
    if (path === '/api/v1/tenants' && method === 'GET') {
      await json(route, { items: tenantRows })
      return
    }
    if (path.startsWith('/api/v1/tenants/') && method === 'PATCH') {
      const id = decodeURIComponent(path.split('/').pop() || '')
      const payload = request.postDataJSON() || {}
      const existing = tenantRows.find((row) => row.id === id) || { id }
      const updated = { ...existing, ...payload, id }
      tenantRows = tenantRows.map((row) => (row.id === id ? updated : row))
      await json(route, updated)
      return
    }
    if (path === '/api/v2/tenant-config' && method === 'GET') {
      await json(route, { ...MOCK_TENANTS[0] })
      return
    }
    if (path === '/api/v2/tenant-config' && method === 'PATCH') {
      await json(route, { ok: true })
      return
    }
    if (path === '/api/permissions/matrix' && method === 'GET') {
      await json(route, { matrix: [] })
      return
    }
    if (path === '/api/permissions/conflicts' && method === 'GET') {
      await json(route, { items: [] })
      return
    }
    if (path === '/api/v1/leads/assignment-rules' && method === 'GET') {
      await json(route, { items: [] })
      return
    }
    if (path === '/api/v1/automation/rules' && method === 'GET') {
      await json(route, { items: [] })
      return
    }
    if (path === '/api/v1/admin/users' && method === 'GET') {
      await json(route, { items: [] })
      return
    }

    await json(route, {})
  })
}

async function seedMockAuth(page) {
  await page.context().addInitScript((auth) => {
    localStorage.setItem('crm_last_tenant', auth.tenantId)
    localStorage.setItem('tenantId', auth.tenantId)
    localStorage.setItem('token', auth.token)
    localStorage.setItem('crm_auth', JSON.stringify(auth))
  }, MOCK_AUTH)
}

test.beforeEach(async ({ page }) => {
  await mockTenantApi(page)
  await seedMockAuth(page)
})

async function expectHealthy(page, pageErrors) {
  await expect(page.getByTestId('error-boundary')).toHaveCount(0)
  expect(pageErrors, `Unexpected page errors:\n${pageErrors.join('\n')}`).toEqual([])
}

async function assertTenantLayout(page) {
  await page.goto('/admin/tenants', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('tenants-page')).toBeVisible()
  await expect(page.getByTestId('tenant-row').first()).toBeVisible()

  const layout = await page.evaluate(() => {
    const panel = document.querySelector('[data-testid="tenants-page"]')
    const rows = Array.from(panel?.querySelectorAll('[data-testid="tenant-row"]') || [])
    const rowLayout = rows.map((row) => {
      const rowRect = row.getBoundingClientRect()
      const grid = row.querySelector('[data-testid="tenant-config-grid"]')
      const gridRect = grid ? grid.getBoundingClientRect() : null
      return {
        rowBottom: rowRect.bottom,
        gridBottom: gridRect ? gridRect.bottom : null,
      }
    })

    const pager = panel?.querySelector('[data-testid="server-pager"]')
    const pagerTop = pager ? pager.getBoundingClientRect().top : null
    const lastRowBottom = rowLayout.length ? rowLayout[rowLayout.length - 1].rowBottom : null
    return { rowLayout, pagerTop, lastRowBottom }
  })

  expect(layout.rowLayout.length).toBeGreaterThan(0)
  for (const row of layout.rowLayout) {
    expect(row.gridBottom).not.toBeNull()
    expect(row.gridBottom).toBeLessThanOrEqual(row.rowBottom + 1)
  }

  expect(layout.pagerTop).not.toBeNull()
  expect(layout.lastRowBottom).not.toBeNull()
  expect(layout.pagerTop).toBeGreaterThanOrEqual(layout.lastRowBottom - 8)
}

test('tenant config cell stays inside row and does not cover pager', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await assertTenantLayout(page)
  await expectHealthy(page, pageErrors)
})

test('tenant config layout remains contained on narrow viewport', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 1280, height: 720 })
  await assertTenantLayout(page)
  await expectHealthy(page, pageErrors)
})

test('tenant config grid is scrollable and save button remains clickable', async ({ page }) => {
  const pageErrors = collectPageErrors(page)

  await page.setViewportSize({ width: 1366, height: 768 })
  await page.goto('/admin/tenants', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('tenants-page')).toBeVisible()

  const firstGrid = page.getByTestId('tenant-config-grid').first()
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

  const firstSave = page.getByTestId('tenant-save-btn').first()
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

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/admin/tenants', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('tenants-page')).toBeVisible()

  await expect(page.getByTestId('tenant-mobile-list')).toBeVisible()
  await expect(page.locator('[data-testid="tenants-page"] .table-head-row')).toBeHidden()

  const firstSave = page.getByTestId('tenant-save-btn').first()
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
