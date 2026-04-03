import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist', 'node_modules', 'coverage', 'test-suites.config.js']),
  {
    files: ['server/**/*.js'],
    languageOptions: {
      globals: globals.node,
    },
  },
  {
    files: ['test-suites.config.js'],
    languageOptions: {
      globals: globals.node,
    },
  },
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]', argsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' }],
      'no-console': ['error', { allow: ['warn', 'error'] }],
      'no-debugger': 'error',
      'eqeqeq': ['error', 'always', { null: 'ignore' }],
      'no-var': 'error',
      'prefer-const': 'warn',
      'no-duplicate-imports': 'error',
      'no-restricted-imports': ['error', {
        patterns: [
          {
            group: ['**/hooks/useApi', '**/hooks/useApi.js', '**/hooks/useApi.jsx'],
            message: 'Do not import the legacy hooks/useApi entry in business code; use the newer API hook entrypoint instead.',
          },
        ],
      }],
    },
  },
  {
    files: ['src/crm/hooks/orchestrators/runtime/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [
          {
            group: ['**/runtime-kernel', '**/runtime-kernel/**'],
            message: 'runtime files must not import from runtime-kernel; keep imports one-way from runtime-kernel to runtime.',
          },
          {
            group: ['**/hooks/useApi', '**/hooks/useApi.js', '**/hooks/useApi.jsx'],
            message: 'Do not import the legacy hooks/useApi entry in business code; use the newer API hook entrypoint instead.',
          },
        ],
      }],
    },
  },
  {
    files: ['**/__tests__/**/*.{js,jsx,ts,tsx}', '**/*.test.{js,jsx,ts,tsx}', '**/*.spec.{js,jsx,ts,tsx}'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    rules: {
      'no-console': 'off',
      'no-unused-vars': 'off',
      'no-restricted-imports': 'off',
    },
  },
])
