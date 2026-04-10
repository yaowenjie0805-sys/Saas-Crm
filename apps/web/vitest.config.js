import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/*.{test,spec}.{js,jsx,ts,tsx}'],
    exclude: ['node_modules', 'dist', 'e2e'],
    reporters: ['default', 'junit'],
    outputFile: {
      junit: 'test-results/vitest/results.xml',
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      thresholds: {
        statements: 20,
        branches: 12,
        functions: 15,
        lines: 20,
      },
      exclude: [
        'node_modules',
        'dist',
        'e2e',
        '**/*.config.js',
        '**/index.{js,jsx,ts,tsx}',
      ],
    },
  },
})
