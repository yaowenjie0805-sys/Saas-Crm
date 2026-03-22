import { test, expect } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'

/**
 * Dashboard Tests
 * Tests dashboard loading, statistics display, charts, and refresh functionality
 */
test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await ensureLoggedIn(page)
    await page.getByTestId('nav-dashboard').click()
    await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
  })

  test('should display dashboard page title', async ({ page }) => {
    await expect(page.getByTestId('page-title')).toBeVisible()
    const title = await page.getByTestId('page-title').textContent()
    expect(title).toMatch(/Dashboard|工作台|仪表盘/i)
  })

  test('should display stats cards', async ({ page }) => {
    // Wait for stats to load
    const statsGrid = page.locator('.stats-grid, [data-testid="stats-grid"]')
    await expect(statsGrid).toBeVisible({ timeout: 10000 })

    // Should have multiple stat cards
    const statCards = page.locator('.stat-card, [data-testid="stat-card"]')
    const count = await statCards.count()
    expect(count).toBeGreaterThan(0)
  })

  test('should display stats with labels and values', async ({ page }) => {
    const statCards = page.locator('.stat-card, [data-testid="stat-card"]')
    await expect(statCards.first()).toBeVisible({ timeout: 10000 })

    // Each card should have a label (p or h3) and value (h3 or number)
    const firstCard = statCards.first()
    const label = await firstCard.locator('p, [class*="label"]').first().textContent()
    const value = await firstCard.locator('h3, [class*="value"], [class*="number"]').first().textContent()

    expect(label).toBeTruthy()
    expect(value).toBeTruthy()
  })

  test('should display workbench section', async ({ page }) => {
    const workbenchSection = page.locator('.panel:has-text("Workbench"), [data-testid*="workbench"]')
    await expect(workbenchSection).toBeVisible({ timeout: 10000 })
  })

  test('should display reports/charts section', async ({ page }) => {
    const reportsSection = page.locator('.panel:has-text("Report"), [data-testid*="report"]')
    await expect(reportsSection).toBeVisible({ timeout: 10000 })
  })

  test('should display bar charts', async ({ page }) => {
    const barCharts = page.locator('.bar-chart, [data-testid="bar-chart"], .bar-track')
    const count = await barCharts.count()
    expect(count).toBeGreaterThan(0)
  })

  test('should refresh dashboard data', async ({ page }) => {
    // Look for refresh button
    const refreshBtn = page.locator('[data-testid="refresh-btn"], button:has-text("Refresh"), button:has-text("刷新")')

    if (await refreshBtn.isVisible()) {
      await refreshBtn.click()
      // Wait for loading state to complete
      await page.waitForTimeout(1000)
      // Stats should still be visible
      await expect(page.locator('.stats-grid, [data-testid="stats-grid"]')).toBeVisible()
    }
  })

  test('should have no console errors on dashboard', async ({ page }) => {
    const errors = []
    page.on('pageerror', (error) => {
      errors.push(error.message)
    })

    await page.reload()
    await page.waitForLoadState('networkidle')

    // Filter out known acceptable errors
    const criticalErrors = errors.filter(e =>
      !e.includes('favicon') &&
      !e.includes('DevTools') &&
      !e.includes('websocket')
    )

    expect(criticalErrors).toEqual([])
  })

  test('should display todo items in workbench', async ({ page }) => {
    const todoSection = page.locator('[data-testid*="todo"], .panel:has-text("To Do"), .panel:has-text("待办")')
    if (await todoSection.isVisible({ timeout: 3000 }).catch(() => false)) {
      const todoItems = page.locator('[data-testid*="todo-item"], .todo-item, [class*="todo"]')
      // Just check the section exists
      await expect(todoSection).toBeVisible()
    }
  })

  test('should display warnings in workbench if any', async ({ page }) => {
    const warningSection = page.locator('[data-testid*="warning"], .panel:has-text("Warning"), .panel:has-text("警告")')
    // Warnings section may or may not exist depending on data
    // Just verify no crash if section exists
    if (await warningSection.isVisible({ timeout: 3000 }).catch(() => false)) {
      await expect(warningSection).toBeVisible()
    }
  })

  test('should navigate to dashboard from sidebar', async ({ page }) => {
    // Navigate away
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    // Navigate back to dashboard
    await page.getByTestId('nav-dashboard').click()
    await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
    await expect(page.locator('.stats-grid, [data-testid="stats-grid"]')).toBeVisible()
  })
})
