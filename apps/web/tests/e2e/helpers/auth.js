import { expect } from '@playwright/test'

const DEFAULT_TIMEOUT = 15_000
const API_BASE_URL = globalThis.process?.env?.E2E_API_BASE_URL || 'http://127.0.0.1:8080/api'

export const E2E_CREDENTIALS = {
  tenantId: 'tenant_default',
  username: 'admin',
  password: 'admin123',
}

export async function seedAuthSession(page, session) {
  await page.context().addInitScript((payload) => {
    localStorage.setItem('crm_last_tenant', payload.tenantId)
    localStorage.setItem('tenantId', payload.tenantId)
    localStorage.setItem('token', payload.token)
    localStorage.setItem('crm_auth', JSON.stringify(payload))
  }, session)
}

export async function ensureLoggedIn(page, credentials = E2E_CREDENTIALS) {
  const loginResponse = await page.request.post(`${API_BASE_URL}/v1/auth/login`, {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Id': credentials.tenantId,
    },
    data: {
      tenantId: credentials.tenantId,
      username: credentials.username,
      password: credentials.password,
      mfaCode: '',
    },
  })
  expect(loginResponse.ok(), `login response status ${loginResponse.status()}`).toBe(true)

  const auth = await loginResponse.json()
  const tenantId = auth.tenantId || credentials.tenantId || 'tenant_default'
  const token = auth.token || 'COOKIE_SESSION'
  const session = {
    username: auth.username || credentials.username || 'admin',
    displayName: auth.displayName || auth.username || credentials.username || 'admin',
    role: auth.role || 'ADMIN',
    ownerScope: auth.ownerScope || '',
    tenantId,
    department: auth.department || '',
    dataScope: auth.dataScope || '',
    dateFormat: auth.dateFormat || 'yyyy-MM-dd',
    token,
    sessionActive: true,
  }

  await seedAuthSession(page, session)

  await page.goto('/', { waitUntil: 'networkidle' })
  const sidebar = page.locator('.sidebar, [data-testid="app-sidebar"]').first()
  if (!(await sidebar.isVisible({ timeout: 3000 }).catch(() => false))) {
    await page.getByTestId('login-tenant-id').fill(credentials.tenantId)
    await page.getByTestId('login-username').fill(credentials.username)
    await page.getByTestId('login-password').fill(credentials.password)
    await page.getByTestId('login-submit').click()
  }
  await expect(sidebar).toBeVisible({ timeout: DEFAULT_TIMEOUT })
  await expect(page.getByTestId('page-title')).toBeVisible({ timeout: DEFAULT_TIMEOUT })
}
