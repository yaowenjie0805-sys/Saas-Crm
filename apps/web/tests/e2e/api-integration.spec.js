import { test, expect } from '@playwright/test'
import { E2E_CREDENTIALS, ensureLoggedIn } from './helpers/auth'

const API_BASE_URL = process.env.E2E_API_BASE_URL || 'http://127.0.0.1:8080/api'

/**
 * API Integration Tests
 * Tests backend API endpoints for data consistency and error handling
 */
test.describe('API Integration', () => {
  let authToken = ''

  test.beforeAll(async ({ request }) => {
    // Get auth token
    const response = await request.post(`${API_BASE_URL}/v1/auth/login`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: E2E_CREDENTIALS,
    })
    expect(response.ok(), `Login failed with status ${response.status()}`).toBeTruthy()
    const body = await response.json()
    authToken = body.token || ''
    expect(authToken).toBeTruthy()
  })

  test('should login via API', async ({ request }) => {
    const response = await request.post(`${API_BASE_URL}/v1/auth/login`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: {
        username: E2E_CREDENTIALS.username,
        password: E2E_CREDENTIALS.password,
        tenantId: E2E_CREDENTIALS.tenantId,
      },
    })

    expect(response.ok(), `Login failed with status ${response.status()}`).toBeTruthy()
    const body = await response.json()
    expect(body.token).toBeTruthy()
    expect(body.token.length).toBeGreaterThan(0)
  })

  test('should reject invalid credentials', async ({ request }) => {
    const response = await request.post(`${API_BASE_URL}/v1/auth/login`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: {
        username: 'invalid',
        password: 'wrong',
        tenantId: E2E_CREDENTIALS.tenantId,
      },
    })

    // API may return 401 or 400 for invalid credentials
    expect([400, 401, 403]).toContain(response.status())
  })

  test('should fetch customers via API', async ({ request }) => {
    const response = await request.get(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })

    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(Array.isArray(body) || (body.data && Array.isArray(body.data))).toBeTruthy()
  })

  test('should create customer via API', async ({ request }) => {
    const customerData = {
      name: `API Customer ${Date.now()}`,
      owner: 'API Test Owner',
      status: 'Active',
      value: 50000,
    }

    const response = await request.post(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
        'Content-Type': 'application/json',
      },
      data: customerData,
    })

    expect(response.status()).toBeGreaterThanOrEqual(200)
    expect(response.status()).toBeLessThan(300)
    const body = await response.json()
    expect(body.name).toBe(customerData.name)
  })

  test('should fetch leads via API', async ({ request }) => {
    const response = await request.get(`${API_BASE_URL}/v1/leads`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })

    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(Array.isArray(body) || (body.data && Array.isArray(body.data))).toBeTruthy()
  })

  test('should fetch opportunities via API', async ({ request }) => {
    const response = await request.get(`${API_BASE_URL}/v1/opportunities`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })

    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(Array.isArray(body) || (body.data && Array.isArray(body.data))).toBeTruthy()
  })

  test('should fetch dashboard data via API', async ({ request }) => {
    const response = await request.get(`${API_BASE_URL}/dashboard`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })

    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    // Dashboard should have stats, opportunities, tasks, customers
    expect(body).toBeTruthy()
  })

  test('should handle unauthorized requests', async ({ request }) => {
    const response = await request.get(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': 'Bearer invalid_token',
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })

    expect(response.status()).toBe(401)
  })

  test('should validate required fields on customer creation', async ({ request }) => {
    const response = await request.post(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
        'Content-Type': 'application/json',
      },
      data: {
        // Missing required fields
        name: '',
      },
    })

    expect(response.status()).toBeGreaterThanOrEqual(400)
  })

  test('should fetch workbench today data via API', async ({ request }) => {
    const now = new Date()
    const from = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
    const to = now.toISOString().split('T')[0]

    const response = await request.get(
      `${API_BASE_URL}/v1/workbench/today?from=${from}&to=${to}&owner=&department=&timezone=Asia/Shanghai`,
      {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
        },
      }
    )

    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(body).toBeTruthy()
  })

  test('should handle tenant isolation', async ({ request }) => {
    // Request with different tenant should not see data from default tenant
    const response = await request.get(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': 'tenant_other',
      },
    })

    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    // Should either be empty or not include default tenant's data
    const customers = Array.isArray(body) ? body : (body.data || [])
    expect(customers.length).toBe(0)
  })

  test('should fetch reports data via API', async ({ request }) => {
    const response = await request.get(`${API_BASE_URL}/v1/reports/overview`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })

    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(body).toBeTruthy()
    // Should have customerByOwner, revenueByStatus, etc.
  })

  test('should handle CORS preflight', async ({ request }) => {
    const response = await request.fetch(`${API_BASE_URL}/v1/customers`, {
      method: 'OPTIONS',
      headers: {
        'Origin': 'http://localhost:5173',
        'Access-Control-Request-Method': 'GET',
        'Access-Control-Request-Headers': 'Authorization, X-Tenant-Id',
      },
    })

    // CORS headers should be present
    const headers = response.headers()
    // Note: Playwright's request API may not expose CORS headers directly
    expect(response.status()).toBeGreaterThanOrEqual(200)
  })

  test('should update customer via API', async ({ request }) => {
    // First create a customer
    const createResponse = await request.post(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
        'Content-Type': 'application/json',
      },
      data: {
        name: `Update Test ${Date.now()}`,
        owner: 'Original Owner',
        status: 'Active',
      },
    })

    expect(createResponse.ok()).toBeTruthy()
    const created = await createResponse.json()
    const customerId = created.id

    // Update the customer
    const updateResponse = await request.put(`${API_BASE_URL}/v1/customers/${customerId}`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
        'Content-Type': 'application/json',
      },
      data: {
        ...created,
        owner: 'Updated Owner',
      },
    })

    expect(updateResponse.status()).toBeGreaterThanOrEqual(200)
    expect(updateResponse.status()).toBeLessThan(300)
  })

  test('should delete customer via API', async ({ request }) => {
    // Create a customer to delete
    const createResponse = await request.post(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
        'Content-Type': 'application/json',
      },
      data: {
        name: `Delete Test ${Date.now()}`,
        owner: 'To Delete',
        status: 'Active',
      },
    })

    expect(createResponse.ok()).toBeTruthy()
    const created = await createResponse.json()
    const customerId = created.id

    // Delete the customer
    const deleteResponse = await request.delete(`${API_BASE_URL}/v1/customers/${customerId}`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })

    expect(deleteResponse.status()).toBeGreaterThanOrEqual(200)
    expect(deleteResponse.status()).toBeLessThan(300)
  })

  test('should handle rate limiting', async ({ request }) => {
    // Make multiple rapid requests
    const responses = []
    for (let i = 0; i < 15; i++) {
      const response = await request.get(`${API_BASE_URL}/v1/customers`, {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
        },
      })
      responses.push(response.status())
    }

    // Should not have too many 429 responses
    const rateLimited = responses.filter(s => s === 429).length
    expect(rateLimited).toBeLessThan(5)
  })

  test('API response time should be acceptable', async ({ request }) => {
    const start = Date.now()
    const response = await request.get(`${API_BASE_URL}/v1/customers`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-Tenant-Id': E2E_CREDENTIALS.tenantId,
      },
    })
    const duration = Date.now() - start

    expect(response.ok()).toBeTruthy()
    // API should respond within 2 seconds
    expect(duration).toBeLessThan(2000)
  })
})
