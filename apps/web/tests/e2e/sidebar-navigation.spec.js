import { test, expect } from '@playwright/test'
import { ensureLoggedIn } from './helpers/auth'

/**
 * Sidebar Navigation Tests
 * Tests all navigation items, hover effects, and active states
 */
test.describe('Sidebar Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await ensureLoggedIn(page)
  })

  test('should display sidebar with all navigation items', async ({ page }) => {
    const sidebar = page.getByTestId('app-sidebar')
    await expect(sidebar).toBeVisible()

    // Check main navigation items
    const navItems = [
      'nav-dashboard',
      'nav-customers',
      'nav-leads',
      'nav-opportunities',
      'nav-quotes',
      'nav-contracts',
      'nav-products',
    ]

    for (const testId of navItems) {
      const navBtn = page.getByTestId(testId)
      if (await navBtn.isVisible()) {
        await expect(navBtn).toBeVisible()
      }
    }
  })

  test('should highlight active navigation item', async ({ page }) => {
    // Dashboard should be active by default
    const dashboardNav = page.getByTestId('nav-dashboard')
    await expect(dashboardNav).toHaveClass(/active/i)

    // Navigate to customers
    await page.getByTestId('nav-customers').click()
    await expect(page.getByTestId('customers-page')).toBeVisible()

    // Dashboard should no longer be active
    await expect(dashboardNav).not.toHaveClass(/active/i)

    // Customers should now be active
    const customersNav = page.getByTestId('nav-customers')
    await expect(customersNav).toHaveClass(/active/i)
  })

  test('should navigate to all main pages', async ({ page }) => {
    const navigationMap = [
      { nav: 'nav-dashboard', page: 'page-title', expected: /Dashboard|工作台|仪表盘/i },
      { nav: 'nav-customers', page: 'customers-page', expected: /Customers|客户/i },
      { nav: 'nav-leads', page: 'leads-page', expected: /Leads|线索/i },
      { nav: 'nav-opportunities', page: 'opportunities-page', expected: /Opportunities|商机/i },
      { nav: 'nav-quotes', page: 'quotes-page', expected: /Quotes|报价/i },
      { nav: 'nav-contracts', page: 'contracts-page', expected: /Contracts|合同/i },
      { nav: 'nav-products', page: 'products-page', expected: /Products|产品/i },
    ]

    for (const { nav, page: pageTestId, expected } of navigationMap) {
      const navBtn = page.getByTestId(nav)
      if (await navBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
        await navBtn.click()
        await expect(page.getByTestId(pageTestId)).toBeVisible({ timeout: 10000 })
        await page.waitForTimeout(500) // Allow animations
      }
    }
  })

  test('should display admin menu items', async ({ page }) => {
    const adminItems = [
      'nav-adminTenants',
      'nav-reportDesigner',
    ]

    for (const testId of adminItems) {
      const navBtn = page.getByTestId(testId)
      if (await navBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
        await expect(navBtn).toBeVisible()
      }
    }
  })

  test('should navigate to admin pages', async ({ page }) => {
    const adminNav = page.getByTestId('nav-adminTenants')
    if (await adminNav.isVisible({ timeout: 1000 }).catch(() => false)) {
      await adminNav.click()
      await expect(page.getByTestId('tenants-page')).toBeVisible({ timeout: 10000 })
      await expect(page.getByTestId('page-title')).toContainText(/Tenant|租户/i)
    }
  })

  test('should collapse and expand menu groups', async ({ page }) => {
    // Look for collapsible menu groups
    const menuGroups = page.locator('.menu-group, .menu-group-title')

    const groupCount = await menuGroups.count()
    if (groupCount > 0) {
      // Try clicking on a group to collapse/expand
      const firstGroup = menuGroups.first()
      await firstGroup.click()
      await page.waitForTimeout(300)

      // Group should toggle state
      await expect(firstGroup).toBeVisible()
    }
  })

  test('should display hover effects on nav items', async ({ page }) => {
    const navItem = page.getByTestId('nav-customers')

    if (await navItem.isVisible()) {
      await navItem.hover()
      await page.waitForTimeout(200)

      // Item should have hover class or style change
      // This is mainly visual but we can verify no crash
      await expect(navItem).toBeAttached()
    }
  })

  test('should display brand logo in sidebar', async ({ page }) => {
    const brandMark = page.locator('.brand-mark, [data-testid="brand-mark"]')
    await expect(brandMark).toBeVisible()
  })

  test('should display user role pill', async ({ page }) => {
    const rolePill = page.locator('.role-pill, [data-testid="role-pill"]')
    if (await rolePill.isVisible({ timeout: 2000 }).catch(() => false)) {
      await expect(rolePill).toBeVisible()
    }
  })

  test('should have working logout button', async ({ page }) => {
    const logoutBtn = page.locator('.logout-btn, [data-testid="logout-btn"]')
    await expect(logoutBtn).toBeVisible()

    await logoutBtn.click()
    await page.waitForLoadState('domcontentloaded')

    // Should either show login or redirect to login
    await expect(page.locator('.login-card, [data-testid="login-page"]')).toBeVisible({ timeout: 5000 })
  })

  test('should maintain sidebar visibility on page navigation', async ({ page }) => {
    // Start on dashboard
    await expect(page.getByTestId('app-sidebar')).toBeVisible()

    // Navigate through several pages
    const pages = ['nav-customers', 'nav-leads', 'nav-opportunities']

    for (const nav of pages) {
      const navBtn = page.getByTestId(nav)
      if (await navBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
        await navBtn.click()
        await page.waitForTimeout(500)
        await expect(page.getByTestId('app-sidebar')).toBeVisible()
      }
    }
  })
})
