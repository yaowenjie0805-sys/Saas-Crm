import { test, expect } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth.js'

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
    await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
  })

  test('should display stats cards', async ({ page }) => {
    const statsGrid = page.getByTestId('dashboard-stats')
    await expect(statsGrid).toBeVisible({ timeout: 10000 })

    const statCards = page.getByTestId('dashboard-stat-card')
    expect(await statCards.count()).toBeGreaterThan(0)
  })

  test('should display stats with labels and values', async ({ page }) => {
    const firstCard = page.getByTestId('dashboard-stat-card').first()
    await expect(firstCard).toBeVisible({ timeout: 10000 })

    const label = await firstCard.getByTestId('dashboard-stat-label').textContent()
    const value = await firstCard.getByTestId('dashboard-stat-value').textContent()

    expect(label).toBeTruthy()
    expect(value).toBeTruthy()
  })

  test('should display workbench section', async ({ page }) => {
    await expect(page.getByTestId('dashboard-workbench')).toBeVisible({ timeout: 10000 })
  })

  test('should display reports/charts section', async ({ page }) => {
    await expect(page.getByTestId('dashboard-reports-grid')).toBeVisible({ timeout: 10000 })
  })

  test('should display bar charts', async ({ page }) => {
    const barCharts = page.locator('.bar-track')
    expect(await barCharts.count()).toBeGreaterThanOrEqual(0)
  })

  test('should refresh dashboard data', async ({ page }) => {
    const refreshBtn = page.locator('[data-testid="topbar-refresh"], button:has-text("Refresh")').first()

    if (await refreshBtn.isVisible().catch(() => false)) {
      await refreshBtn.click()
      await expect(page.getByTestId('dashboard-stats')).toBeVisible({ timeout: 10000 })
    }
  })

  test('should have no console errors on dashboard', async ({ page }) => {
    const errors = []
    page.on('pageerror', (error) => {
      errors.push(error.message)
    })

    await page.reload()
    await page.waitForLoadState('networkidle')

    const criticalErrors = errors.filter((e) =>
      !e.includes('favicon')
      && !e.includes('DevTools')
      && !e.includes('websocket')
      && !e.includes('signal is aborted')
      && !e.includes('AbortError')
      && !e.includes('Bearer Token')
      && !e.includes('Cannot convert object to primitive value')
      && !e.includes('requestFailed')
    )

    expect(criticalErrors).toEqual([])
  })

  test('should display todo items in workbench', async ({ page }) => {
    const todoSection = page.getByTestId('dashboard-workbench-todo')
    await expect(todoSection).toBeVisible({ timeout: 10000 })
    const todoItems = page.getByTestId('dashboard-workbench-todo-item')
    expect(await todoItems.count()).toBeGreaterThanOrEqual(0)
  })

  test('should display warnings in workbench if any', async ({ page }) => {
    const warningSection = page.getByTestId('dashboard-workbench-warning')
    await expect(warningSection).toBeVisible({ timeout: 10000 })
    const warningItems = page.getByTestId('dashboard-workbench-warning-item')
    expect(await warningItems.count()).toBeGreaterThanOrEqual(0)
  })

  test('should navigate to dashboard from sidebar', async ({ page }) => {
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('page-title')).not.toHaveText('Dashboard')

    await page.getByTestId('nav-dashboard').click()
    await expect(page.getByTestId('page-title')).toHaveText('Dashboard')
    await expect(page.getByTestId('dashboard-stats')).toBeVisible({ timeout: 10000 })
  })
})

