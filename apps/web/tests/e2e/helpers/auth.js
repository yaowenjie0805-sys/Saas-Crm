import { expect } from '@playwright/test'

const DEFAULT_TIMEOUT = 15_000
const API_BASE_URL = globalThis.process?.env?.E2E_API_BASE_URL || 'http://127.0.0.1:8080/api'
const API_ORIGIN = new URL(API_BASE_URL).origin

export const E2E_CREDENTIALS = {
  tenantId: 'tenant_default',
  username: 'admin',
  password: 'admin123',
}

const buildPersistedAuth = (auth, tenantId) => ({
  username: auth?.username || '',
  displayName: auth?.displayName || '',
  role: auth?.role || '',
  ownerScope: auth?.ownerScope || '',
  tenantId: auth?.tenantId || tenantId,
  department: auth?.department || '',
  dataScope: auth?.dataScope || '',
  dateFormat: auth?.dateFormat || 'yyyy-MM-dd',
  sessionActive: true,
})

async function bootstrapAuth(page, credentials) {
  const response = await page.request.post(`${API_BASE_URL}/v1/auth/login`, {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Id': credentials.tenantId,
    },
    data: credentials,
  })
  expect(response.ok(), `Expected API login to succeed, got ${response.status()}`).toBeTruthy()
  const authBody = await response.json()
  const token = String(authBody?.token || '').trim()
  expect(token, 'Expected API login to return token').not.toBe('')
  const persistedAuth = buildPersistedAuth(authBody, credentials.tenantId)
  await page.context().addCookies([
    {
      name: 'CRM_SESSION',
      value: token,
      url: API_ORIGIN,
      httpOnly: true,
      sameSite: 'Lax',
    },
  ])

  await page.addInitScript(
    ({ auth, tenantId }) => {
      localStorage.setItem('crm_auth', JSON.stringify(auth))
      localStorage.setItem('crm_last_tenant', tenantId)
    },
    { auth: persistedAuth, tenantId: credentials.tenantId },
  )
}

export async function ensureLoggedIn(page, credentials = E2E_CREDENTIALS) {
  await bootstrapAuth(page, credentials)
  await page.goto('/', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('app-sidebar')).toBeVisible({ timeout: DEFAULT_TIMEOUT })
}
