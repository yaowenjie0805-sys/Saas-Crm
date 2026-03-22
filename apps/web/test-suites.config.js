// Test configuration for running different test suites
// Usage: npx playwright test --config=test-suites.config.js --project=smoke

import { defineConfig } from '@playwright/test'

const baseConfig = {
  testDir: './tests/e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : 1,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'test-results/results.json' }],
  ],
  outputDir: 'test-results/playwright',
  use: {
    headless: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
}

export default defineConfig({
  projects: [
    {
      name: 'smoke',
      testMatch: ['**/smoke.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
    {
      name: 'auth',
      testMatch: ['**/auth-flow.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
    {
      name: 'dashboard',
      testMatch: ['**/dashboard.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
    {
      name: 'navigation',
      testMatch: ['**/sidebar-navigation.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
    {
      name: 'leads',
      testMatch: ['**/leads-management.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
    {
      name: 'toast',
      testMatch: ['**/toast-notifications.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
    {
      name: 'api',
      testMatch: ['**/api-integration.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
    {
      name: 'full',
      testMatch: ['**/*.spec.js'],
      ...baseConfig,
      use: {
        ...baseConfig.use,
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:14173',
      },
    },
  ],
})
