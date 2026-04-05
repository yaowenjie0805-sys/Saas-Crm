import { ensureLoggedIn, seedAuthSession } from './auth.js'

export const DEFAULT_FALLBACK_SESSION = {
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

export function isApiConnectFailure(error) {
  const message = String(error?.message || '')
  return ['ECONNREFUSED', 'ECONNRESET', 'ETIMEDOUT'].some((token) => message.includes(token))
}

export async function seedFallbackSession(page, session = DEFAULT_FALLBACK_SESSION) {
  await seedAuthSession(page, session)
  await page.goto('/', { waitUntil: 'networkidle' })
}

export async function loginWithFallback(page, session = DEFAULT_FALLBACK_SESSION) {
  try {
    await ensureLoggedIn(page)
    return { fallbackMode: false }
  } catch (error) {
    if (!isApiConnectFailure(error)) throw error
    await seedFallbackSession(page, session)
    return { fallbackMode: true }
  }
}
