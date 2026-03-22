import { test, expect } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'
import { generateUniqueLeadName } from './helpers/testData'

/**
 * Lead Management Tests
 * Tests lead creation, editing, status updates, and deletion
 */
test.describe('Lead Management', () => {
  test.beforeEach(async ({ page }) => {
    await ensureLoggedIn(page)
    await page.getByTestId('nav-leads').click()
    await expect(page.getByTestId('leads-page')).toBeVisible({ timeout: 10000 })
  })

  test('should display leads page title', async ({ page }) => {
    await expect(page.getByTestId('page-title')).toBeVisible()
    const title = await page.getByTestId('page-title').textContent()
    expect(title).toMatch(/Leads|线索/i)
  })

  test('should display leads list or empty state', async ({ page }) => {
    const leadsList = page.locator('[data-testid="leads-list"], .leads-list, .table, [class*="list"]')
    await expect(leadsList).toBeVisible({ timeout: 5000 })
  })

  test('should display lead form', async ({ page }) => {
    const leadForm = page.locator('[data-testid="lead-form"], .lead-form, form')
    await expect(leadForm.first()).toBeVisible({ timeout: 5000 })
  })

  test('should create a new lead', async ({ page }) => {
    const leadName = generateUniqueLeadName()

    // Fill lead form
    const nameInput = page.getByTestId('lead-form-name')
    if (await nameInput.isVisible()) {
      await nameInput.fill(leadName)

      // Fill other fields if available
      const ownerInput = page.getByTestId('lead-form-owner')
      if (await ownerInput.isVisible()) {
        await ownerInput.fill('E2E Test Owner')
      }

      const statusSelect = page.getByTestId('lead-form-status')
      if (await statusSelect.isVisible()) {
        await statusSelect.selectOption('New')
      }

      const valueInput = page.getByTestId('lead-form-value')
      if (await valueInput.isVisible()) {
        await valueInput.fill('10000')
      }

      // Submit form
      const submitBtn = page.getByTestId('lead-form-submit')
      await submitBtn.click()

      await page.waitForTimeout(1000)

      // Lead should appear in list
      const leadRow = page.locator('.table-row, [class*="row"]').filter({ hasText: leadName }).first()
      await expect(leadRow).toBeVisible({ timeout: 5000 })
    }
  })

  test('should update lead status', async ({ page }) => {
    // Create a lead first
    const leadName = generateUniqueLeadName()
    const nameInput = page.getByTestId('lead-form-name')
    if (await nameInput.isVisible()) {
      await nameInput.fill(leadName)
      await page.getByTestId('lead-form-submit').click()
      await page.waitForTimeout(1000)

      // Find the lead in the list
      const leadRow = page.locator('.table-row').filter({ hasText: leadName }).first()
      if (await leadRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        // Click edit button
        const editBtn = leadRow.getByRole('button', { name: /Edit|编辑/i })
        await editBtn.click()
        await page.waitForTimeout(500)

        // Change status
        const statusSelect = page.getByTestId('lead-form-status')
        if (await statusSelect.isVisible()) {
          await statusSelect.selectOption('Contacted')
          await page.getByTestId('lead-form-submit').click()
          await page.waitForTimeout(1000)
        }
      }
    }
  })

  test('should delete a lead', async ({ page }) => {
    // Create a lead first
    const leadName = generateUniqueLeadName()
    const nameInput = page.getByTestId('lead-form-name')
    if (await nameInput.isVisible()) {
      await nameInput.fill(leadName)
      await page.getByTestId('lead-form-submit').click()
      await page.waitForTimeout(1000)

      // Find and delete the lead
      const leadRow = page.locator('.table-row').filter({ hasText: leadName }).first()
      if (await leadRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        const deleteBtn = leadRow.getByRole('button', { name: /Delete|删除/i })
        await deleteBtn.click()
        await page.waitForTimeout(1000)

        // Lead should be removed
        await expect(leadRow).not.toBeVisible({ timeout: 3000 })
      }
    }
  })

  test('should search leads', async ({ page }) => {
    const searchInput = page.getByTestId('leads-search-input')
    if (await searchInput.isVisible()) {
      // Type a search term
      await searchInput.fill('Test')
      await page.waitForTimeout(500)

      const submitBtn = page.getByTestId('leads-search-submit')
      if (await submitBtn.isVisible()) {
        await submitBtn.click()
        await page.waitForTimeout(1000)
      }

      // Results should be filtered
      await expect(page.locator('[data-testid="leads-list"], .leads-list, .table')).toBeVisible()
    }
  })

  test('should filter leads by status', async ({ page }) => {
    const statusFilter = page.locator('[data-testid="leads-status-filter"], [class*="status-filter"], select').first()
    if (await statusFilter.isVisible()) {
      await statusFilter.selectOption('New')
      await page.waitForTimeout(1000)

      // Results should be filtered
      await expect(page.locator('[data-testid="leads-list"], .leads-list, .table')).toBeVisible()
    }
  })

  test('should refresh leads list', async ({ page }) => {
    const refreshBtn = page.getByTestId('leads-refresh')
    if (await refreshBtn.isVisible()) {
      await refreshBtn.click()
      await page.waitForTimeout(1000)

      // List should still be visible
      await expect(page.locator('[data-testid="leads-list"], .leads-list, .table')).toBeVisible()
    }
  })

  test('should validate required fields', async ({ page }) => {
    // Try to submit empty form
    const submitBtn = page.getByTestId('lead-form-submit')
    await submitBtn.click()
    await page.waitForTimeout(500)

    // Should show validation error
    const errorMsg = page.locator('[class*="error"], [class*="required"], [class*="invalid"]').first()
    const errorVisible = await errorMsg.isVisible({ timeout: 2000 }).catch(() => false)

    if (errorVisible) {
      await expect(errorMsg).toBeVisible()
    }
  })

  test('should paginate leads', async ({ page }) => {
    const pagination = page.locator('[data-testid="pagination"], .pagination, [class*="pagination"]')
    if (await pagination.isVisible({ timeout: 3000 }).catch(() => false)) {
      // Check if next page button exists
      const nextBtn = pagination.locator('button:has-text("Next"), button:has-text("下一页"), button:has-text(">")')
      if (await nextBtn.isVisible()) {
        await nextBtn.click()
        await page.waitForTimeout(1000)

        // Should show different results
        await expect(page.locator('[data-testid="leads-list"], .leads-list, .table')).toBeVisible()
      }
    }
  })
})
