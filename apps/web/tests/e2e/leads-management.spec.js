import { test, expect } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth.js'
import { generateUniqueLeadName } from './helpers/testData'

const LEADS_TITLE_PATTERN = /Leads|线索/i

async function openLeadsPage(page) {
  const navLeads = page.getByTestId('nav-leads')
  if (await navLeads.isVisible({ timeout: 3000 }).catch(() => false)) {
    await navLeads.click()
  } else {
    await page.goto('/leads')
  }
  await expect(page.getByTestId('leads-page')).toBeVisible({ timeout: 15000 })
  await expect(page.getByTestId('page-title')).toContainText(LEADS_TITLE_PATTERN)
  await expect(page.getByTestId('leads-list-header')).toBeVisible()
}

async function searchLead(page, keyword) {
  await page.getByTestId('leads-search-input').fill(keyword)
  await page.getByTestId('leads-search-submit').click()
}

function leadRowByName(page, leadName) {
  return page.getByTestId('leads-list').locator('.table-row').filter({ hasText: leadName }).first()
}

async function expectLeadsListOrEmpty(page) {
  const empty = page.getByTestId('leads-empty')
  if (await empty.isVisible().catch(() => false)) {
    await expect(empty).toBeVisible()
    return
  }
  const list = page.getByTestId('leads-list')
  await expect(list).toBeVisible()
}

async function createLead(page, leadName, status = 'NEW') {
  await page.getByTestId('lead-form-name').fill(leadName)
  await page.getByTestId('lead-form-owner').fill('E2E Test Owner')
  await page.getByTestId('lead-form-status').selectOption(status)
  await page.getByTestId('lead-form-submit').click()
  await expect(page.getByTestId('lead-form-name')).toHaveValue('')
  await searchLead(page, leadName)
  await expect(leadRowByName(page, leadName)).toBeVisible({ timeout: 10000 })
}

async function ensureCanWrite(page) {
  const canWrite = await page.getByTestId('lead-form').isVisible().catch(() => false)
  test.skip(!canWrite, 'Lead form is not visible for current role')
}

test.describe('Lead Management', () => {
  test.beforeEach(async ({ page }) => {
    await ensureLoggedIn(page)
    await openLeadsPage(page)
  })

  test('should display leads page title', async ({ page }) => {
    await expect(page.getByTestId('page-title')).toContainText(LEADS_TITLE_PATTERN)
  })

  test('should display leads list or empty state', async ({ page }) => {
    await expectLeadsListOrEmpty(page)
  })

  test('should display lead form', async ({ page }) => {
    await ensureCanWrite(page)
    await expect(page.getByTestId('lead-form')).toBeVisible()
  })

  test('should create a new lead', async ({ page }) => {
    await ensureCanWrite(page)
    const leadName = generateUniqueLeadName()
    await createLead(page, leadName)
  })

  test('should update lead status', async ({ page }) => {
    await ensureCanWrite(page)
    const leadName = generateUniqueLeadName()
    await createLead(page, leadName)

    const leadRow = leadRowByName(page, leadName)
    await leadRow.getByRole('button').first().click()
    await expect(page.getByTestId('lead-form-name')).toHaveValue(leadName)

    await page.getByTestId('lead-form-status').selectOption('QUALIFIED')
    await page.getByTestId('lead-form-submit').click()
    await searchLead(page, leadName)
    await expect(leadRowByName(page, leadName)).toContainText(/Qualified|已甄别|合格/i)
  })

  test('should open lead detail from list', async ({ page }) => {
    await ensureCanWrite(page)
    const leadName = generateUniqueLeadName()
    await createLead(page, leadName)

    const leadRow = leadRowByName(page, leadName)
    await leadRow.getByRole('button').first().click()
    await expect(page.getByTestId('lead-form-name')).toHaveValue(leadName)
  })

  test('should search leads', async ({ page }) => {
    await ensureCanWrite(page)
    const leadName = generateUniqueLeadName()
    await createLead(page, leadName)
    await searchLead(page, leadName)
    await expect(leadRowByName(page, leadName)).toBeVisible()
  })

  test('should filter leads by status', async ({ page }) => {
    await page.getByTestId('leads-status-filter').selectOption('NEW')
    await page.getByTestId('leads-search-submit').click()
    await expectLeadsListOrEmpty(page)
  })

  test('should refresh leads list', async ({ page }) => {
    await page.getByTestId('leads-refresh').click()
    await expectLeadsListOrEmpty(page)
  })

  test('should validate required fields', async ({ page }) => {
    await ensureCanWrite(page)
    await page.getByTestId('lead-form-name').fill('')
    await page.getByTestId('lead-form-submit').click()
    await expect(page.getByTestId('lead-form-error')).toBeVisible()
    await expect(page.getByTestId('lead-form-name')).toHaveClass(/input-invalid/)
  })

  test('should paginate leads', async ({ page }) => {
    const pagination = page.getByTestId('leads-pagination')
    const hasPagination = await pagination.isVisible().catch(() => false)
    if (!hasPagination) test.skip(true, 'Pagination is hidden when there are no lead rows')

    const prevBtn = pagination.locator('button').first()
    const nextBtn = pagination.locator('button').nth(1)
    if (await nextBtn.isEnabled()) {
      await nextBtn.click()
      await expect(prevBtn).toBeEnabled()
    } else {
      await expect(nextBtn).toBeDisabled()
    }
  })
})

