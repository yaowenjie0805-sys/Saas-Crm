import { spawn } from 'node:child_process'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function waitHealth() {
  for (let i = 0; i < 80; i += 1) {
    try {
      const res = await fetch('http://localhost:8080/api/health')
      if (res.ok) return true
    } catch (_) {}
    await sleep(400)
  }
  return false
}

async function req(path, options = {}) {
  return fetch(`http://localhost:8080${path}`, options)
}

const app = spawn('java', ['-jar', 'backend/target/crm-backend-1.0.0.jar'], {
  cwd: process.cwd(),
  stdio: ['ignore', 'ignore', 'ignore'],
})

try {
  const ready = await waitHealth()
  if (!ready) throw new Error('backend_not_ready')

  const login = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' }),
  })
  const auth = await login.json()
  if (login.status !== 200 || !auth.token) throw new Error('login_failed')
  const salesLogin = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: 'sales', password: 'sales123' }),
  })
  const salesAuth = await salesLogin.json()
  if (salesLogin.status !== 200 || !salesAuth.token) throw new Error('sales_login_failed')
  const managerLogin = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: 'manager', password: 'manager123' }),
  })
  const managerAuth = await managerLogin.json()
  if (managerLogin.status !== 200 || !managerAuth.token) throw new Error('manager_login_failed')
  const analystLogin = await req('/api/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: 'analyst', password: 'analyst123' }),
  })
  const analystAuth = await analystLogin.json()
  if (analystLogin.status !== 200 || !analystAuth.token) throw new Error('analyst_login_failed')

  const headers = { Authorization: `Bearer ${auth.token}`, 'content-type': 'application/json' }

  const createCustomer = await req('/api/customers', {
    method: 'POST',
    headers,
    body: JSON.stringify({ name: 'SmokeEditCo', owner: 'Bot', tag: 'test', value: 100, status: 'new' }),
  })
  const customer = await createCustomer.json()
  if (createCustomer.status !== 201) throw new Error('create_customer_failed')

  const patchCustomer = await req(`/api/customers/${customer.id}`, {
    method: 'PATCH',
    headers,
    body: JSON.stringify({ status: 'updated', value: 200 }),
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

  const auditSearch = await req('/api/audit-logs/search?action=UPDATE&role=ADMIN&page=1&size=5', { headers })
  if (auditSearch.status !== 200) throw new Error('audit_search_failed')
  const auditDateSearch = await req('/api/audit-logs/search?action=UPDATE&page=1&size=5&from=2000-01-01&to=2999-01-01', { headers })
  if (auditDateSearch.status !== 200) throw new Error('audit_date_search_failed')

  const oppSearch = await req('/api/opportunities/search?stage=&page=1&size=5', { headers })
  if (oppSearch.status !== 200) throw new Error('opportunity_search_failed')
  const oppSorted = await req('/api/opportunities/search?stage=&page=1&size=5&sortBy=amount&sortDir=desc', { headers })
  if (oppSorted.status !== 200) throw new Error('opportunity_sort_failed')
  const oppData = await oppSorted.json()
  const opp = oppData.items?.[0]
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

  const reportAdmin = await req('/api/reports/overview', { headers: { Authorization: `Bearer ${auth.token}` } })
  if (reportAdmin.status !== 200) throw new Error('report_admin_failed')
  const reportAnalyst = await req('/api/reports/overview', { headers: { Authorization: `Bearer ${analystAuth.token}` } })
  if (reportAnalyst.status !== 200) throw new Error('report_analyst_failed')

  const salesAmountPatch = await req(`/api/opportunities/${opp.id}`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${salesAuth.token}`, 'content-type': 'application/json' },
    body: JSON.stringify({ amount: Number(opp.amount || 0) + 1 }),
  })
  if (salesAmountPatch.status !== 403) throw new Error('sales_amount_should_be_forbidden')

  const auditExport = await req('/api/audit-logs/export?action=UPDATE', { headers: { Authorization: `Bearer ${managerAuth.token}` } })
  if (auditExport.status !== 200) throw new Error('audit_export_failed')

  await req(`/api/follow-ups/${followUp.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
  await req(`/api/customers/${customer.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })

  console.log('API_SMOKE_TEST_OK')
} catch (err) {
  console.error('API_SMOKE_TEST_FAIL', err.message)
  process.exitCode = 1
} finally {
  app.kill()
}
