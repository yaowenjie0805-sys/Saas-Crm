import { spawn } from 'node:child_process'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const DB_NAME = process.env.DB_NAME || 'crm_local_e2e'
const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const API_PORT = process.env.API_PORT || '18080'
const BASE_URL = `http://127.0.0.1:${API_PORT}`
const DB_URL = process.env.DB_URL || `jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`

async function waitHealth(isExited) {
  for (let i = 0; i < 150; i += 1) {
    if (isExited()) return false
    try {
      const res = await fetch(`${BASE_URL}/api/health`)
      if (res.ok) return true
    } catch (_) {}
    await sleep(400)
  }
  return false
}

async function req(path, options = {}) {
  return fetch(`${BASE_URL}${path}`, options)
}

async function ensureUserAuth(adminToken, username, password, role) {
  const loginTry = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (loginTry.status === 200) {
    const loginBody = await loginTry.json()
    if (loginBody?.token) return loginBody
  }

  const registerRes = await req('/api/auth/register', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username, password, displayName: username }),
  })
  if (registerRes.status !== 201 && registerRes.status !== 409) {
    const body = await registerRes.text()
    throw new Error(`register_failed_${username}_${registerRes.status}_${body}`)
  }

  if (role && role !== 'SALES') {
    const patchRole = await req(`/api/admin/users/${encodeURIComponent(username)}`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${adminToken}`, 'content-type': 'application/json' },
      body: JSON.stringify({ role }),
    })
    if (patchRole.status !== 200) {
      const body = await patchRole.text()
      throw new Error(`patch_role_failed_${username}_${patchRole.status}_${body}`)
    }
  }

  const loginRes = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  const loginBody = await loginRes.json()
  if (loginRes.status !== 200 || !loginBody?.token) {
    throw new Error(`relogin_failed_${username}_${loginRes.status}_${JSON.stringify(loginBody)}`)
  }
  return loginBody
}

async function registerAndLogin(username, password) {
  const registerRes = await req('/api/auth/register', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username, password, displayName: username }),
  })
  if (registerRes.status !== 201 && registerRes.status !== 409) {
    const body = await registerRes.text()
    throw new Error(`bootstrap_register_failed_${registerRes.status}_${body}`)
  }

  const loginRes = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  const loginBody = await loginRes.json()
  if (loginRes.status !== 200 || !loginBody?.token) {
    throw new Error(`bootstrap_login_failed_${loginRes.status}_${JSON.stringify(loginBody)}`)
  }
  return loginBody
}

const app = spawn('java', [
  '-Dspring.profiles.active=dev',
  '-Dlead.import.listener.enabled=false',
  '-Dlead.import.mq.declare.enabled=false',
  '-Dlead.import.mq.publish.enabled=false',
  '-Dspring.rabbitmq.listener.simple.auto-startup=false',
  '-Dspring.rabbitmq.listener.direct.auto-startup=false',
  '-Dspring.rabbitmq.dynamic=false',
  `-Dserver.port=${API_PORT}`,
  `-DDB_URL=${DB_URL}`,
  `-DDB_USER=${DB_USER}`,
  `-DDB_PASSWORD=${DB_PASSWORD}`,
  '-jar',
  'backend/target/crm-backend-1.0.0.jar',
], {
  cwd: process.cwd(),
  stdio: ['ignore', 'pipe', 'pipe'],
})

let startupLog = ''
let exited = false
let exitCode = null
app.on('exit', (code) => {
  exited = true
  exitCode = code
})
app.stdout.on('data', (chunk) => {
  startupLog += String(chunk)
  if (startupLog.length > 200000) startupLog = startupLog.slice(-200000)
})
app.stderr.on('data', (chunk) => {
  startupLog += String(chunk)
  if (startupLog.length > 200000) startupLog = startupLog.slice(-200000)
})

try {
  if (exited) {
    throw new Error(`backend_exited_early_${exitCode}\n${startupLog}`)
  }
  const ready = await waitHealth(() => exited)
  if (!ready) {
    const exitHint = exited ? `\nbackend_exited_${exitCode}` : ''
    throw new Error(`backend_not_ready${exitHint}\n${startupLog}`)
  }

  const login = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' }),
  })
  let auth = await login.json()
  let hasAdmin = login.status === 200 && !!auth?.token
  if (!hasAdmin) {
    const fallbackUsername = `smoke_owner_${Date.now()}`
    auth = await registerAndLogin(fallbackUsername, 'smoke123')
  }
  if (!auth?.token) {
    throw new Error(`login_failed_${login.status}_${JSON.stringify(auth)}\n${startupLog}`)
  }
  let managerAuth = auth
  let salesAuth = auth
  let analystAuth = auth
  if (hasAdmin) {
    managerAuth = await ensureUserAuth(auth.token, 'manager', 'manager123', 'MANAGER')
    salesAuth = await ensureUserAuth(auth.token, 'sales', 'sales123', 'SALES')
    analystAuth = await ensureUserAuth(auth.token, 'analyst', 'analyst123', 'ANALYST')
  }

  const headers = { Authorization: `Bearer ${auth.token}`, 'content-type': 'application/json' }

  const createCustomer = await req('/api/customers', {
    method: 'POST',
    headers,
    body: JSON.stringify({ name: 'SmokeEditCo', owner: 'Bot', tag: 'A', value: 100, status: 'Active' }),
  })
  const customer = await createCustomer.json()
  if (createCustomer.status !== 201) throw new Error(`create_customer_failed_${createCustomer.status}_${JSON.stringify(customer)}`)

  const patchCustomer = await req(`/api/customers/${customer.id}`, {
    method: 'PATCH',
    headers,
    body: JSON.stringify({ status: 'Pending', value: 200 }),
  })
  if (patchCustomer.status !== 200) throw new Error('patch_customer_failed')

  const createTask = await req('/api/tasks', {
    method: 'POST',
    headers,
    body: JSON.stringify({ title: 'SmokeTask', time: 'today', level: 'Medium' }),
  })
  const task = await createTask.json()
  if (createTask.status !== 201) throw new Error('create_task_failed')

  const patchTask = await req(`/api/tasks/${task.id}`, {
    method: 'PATCH',
    headers,
    body: JSON.stringify({ title: 'SmokeTaskUpdated', level: 'High' }),
  })
  if (patchTask.status !== 200) throw new Error('patch_task_failed')

  const customerSearch = await req('/api/customers/search?q=Smoke&page=1&size=5', { headers })
  if (customerSearch.status !== 200) throw new Error('customer_search_failed')
  const customerSorted = await req('/api/customers/search?q=Smoke&page=1&size=5&sortBy=value&sortDir=asc', { headers })
  if (customerSorted.status !== 200) throw new Error('customer_sort_failed')

  if (hasAdmin) {
    const auditSearch = await req('/api/audit-logs/search?action=UPDATE&role=ADMIN&page=1&size=5', { headers })
    if (auditSearch.status !== 200) throw new Error('audit_search_failed')
    const auditDateSearch = await req('/api/audit-logs/search?action=UPDATE&page=1&size=5&from=2000-01-01&to=2999-01-01', { headers })
    if (auditDateSearch.status !== 200) throw new Error('audit_date_search_failed')
  }

  const oppSearch = await req('/api/opportunities/search?stage=&page=1&size=5', { headers })
  if (oppSearch.status !== 200) throw new Error('opportunity_search_failed')
  const oppSorted = await req('/api/opportunities/search?stage=&page=1&size=5&sortBy=amount&sortDir=desc', { headers })
  if (oppSorted.status !== 200) throw new Error('opportunity_sort_failed')
  const oppData = await oppSorted.json()
  let opp = oppData.items?.[0]
  if (!opp) {
    const createOpp = await req('/api/opportunities', {
      method: 'POST',
      headers,
      body: JSON.stringify({ stage: 'Lead', count: 1, amount: 0, progress: 10 }),
    })
    if (createOpp.status !== 201) {
      const body = await createOpp.text()
      throw new Error(`create_opportunity_failed_${createOpp.status}_${body}`)
    }
    opp = await createOpp.json()
  }
  if (!opp) throw new Error('opportunity_empty')

  const taskSearch = await req('/api/tasks/search?q=Smoke&page=1&size=5&sortBy=updatedAt&sortDir=desc', { headers })
  if (taskSearch.status !== 200) throw new Error('task_search_failed')

  const createFollowUp = await req('/api/follow-ups', {
    method: 'POST',
    headers,
    body: JSON.stringify({
      customerId: customer.id,
      summary: 'Smoke follow-up',
      channel: 'Phone',
      result: 'Pending',
      nextActionDate: '2026-03-10',
    }),
  })
  if (createFollowUp.status !== 201) {
    const errBody = await createFollowUp.text()
    throw new Error(`create_follow_up_failed_${createFollowUp.status}_${errBody}`)
  }
  const followUp = await createFollowUp.json()

  const followSearch = await req(`/api/follow-ups/search?customerId=${encodeURIComponent(customer.id)}&q=Smoke&page=1&size=5`, { headers })
  if (followSearch.status !== 200) throw new Error('follow_search_failed')

  if (hasAdmin) {
    const reportAdmin = await req('/api/reports/overview', { headers: { Authorization: `Bearer ${auth.token}` } })
    if (reportAdmin.status !== 200) throw new Error('report_admin_failed')
    const reportAnalyst = await req('/api/reports/overview', { headers: { Authorization: `Bearer ${analystAuth.token}` } })
    if (reportAnalyst.status !== 200) throw new Error('report_analyst_failed')
  } else {
    const reportSelf = await req('/api/reports/overview', { headers: { Authorization: `Bearer ${auth.token}` } })
    if (reportSelf.status !== 200 && reportSelf.status !== 403) throw new Error('report_self_unexpected_status')
  }

  if (hasAdmin) {
    const salesAmountPatch = await req(`/api/opportunities/${opp.id}`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${salesAuth.token}`, 'content-type': 'application/json' },
      body: JSON.stringify({ amount: Number(opp.amount || 0) + 1 }),
    })
    if (salesAmountPatch.status !== 403) throw new Error('sales_amount_should_be_forbidden')

    const auditExport = await req('/api/audit-logs/export?action=UPDATE', { headers: { Authorization: `Bearer ${managerAuth.token}` } })
    if (auditExport.status !== 200) throw new Error('audit_export_failed')
  }

  await req(`/api/follow-ups/${followUp.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
  await req(`/api/customers/${customer.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })

  console.log('API_SMOKE_TEST_OK')
} catch (err) {
  console.error('API_SMOKE_TEST_FAIL', err.message)
  process.exitCode = 1
} finally {
  app.kill()
}
