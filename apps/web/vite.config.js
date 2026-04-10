import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Code split groups
const CHUNK_GROUPS = {
  vendor: [
    { test: /node_modules\/react/, name: 'vendor-react' },
    { test: /node_modules\/react-dom/, name: 'vendor-react' },
    { test: /node_modules\/react-router/, name: 'vendor-router' },
    { test: /node_modules\/zustand/, name: 'vendor-state' },
    { test: /node_modules\/antd/, name: 'vendor-antd' },
    { test: /node_modules\/@ant-design/, name: 'vendor-antd' },
    { test: /node_modules\/echarts/, name: 'vendor-charts' },
    { test: /node_modules\/echarts-for-react/, name: 'vendor-charts' },
    { test: /node_modules\//, name: 'vendor-misc' },
  ],
  appRuntime: [
    'useCoreListDomainLoaders',
    'useWorkbenchDomainLoaders',
    'useCommerceDomainLoaders',
    'useGovernanceDomainLoaders',
    'useApprovalDomainLoaders',
    'useReportingAuditDomainLoaders',
    'useLeadImportDomainLoaders',
    '/orchestrators/',
  ],
  routes: {
    DashboardPanel: 'route-dashboard',
    customer360: 'route-customers-detail',
    CustomersPanel: 'route-sales-core',
    CustomersPanelRuntime: 'route-sales-core',
    CustomersPanelContainer: 'route-sales-core',
    useBatchActions: 'route-sales-core',
    QuotesPanel: 'route-commerce',
    OrdersPanel: 'route-commerce',
    // Keep commerce detail modules together to avoid circular chunk warnings.
    quotes: 'route-commerce',
    orders: 'route-commerce',
    PipelinePanel: 'route-sales-core',
    ApprovalsPageContainer: 'route-approvals',
    GovernancePageContainer: 'route-governance',
    ReportDesignerPanel: 'route-report-designer',
    reportDesigner: 'route-report-designer-detail',
  },
  i18n: {
    'i18n/common/en': 'crm-i18n-en',
    'i18n/common/zh': 'crm-i18n-zh',
    'i18n/namespaces/market-dashboard': 'crm-i18n-ns-dashboard',
    'i18n/namespaces/market-governance': 'crm-i18n-ns-governance',
    'i18n/namespaces/opportunity-workbench': 'crm-i18n-ns-opportunity-workbench',
    'i18n/namespaces/report-designer': 'crm-i18n-ns-report-designer',
    'i18n/namespaces/': 'crm-i18n-ns',
    'i18n.js': 'crm-i18n-core',
  },
  shell: ['/components/shell/', 'SidebarNav'],
}

function getChunkName(id) {
  for (const { test, name } of CHUNK_GROUPS.vendor) {
    if (test.test(id)) return name
  }

  for (const pattern of CHUNK_GROUPS.appRuntime) {
    if (id.includes(pattern)) return 'app-runtime-core'
  }

  for (const pattern of CHUNK_GROUPS.shell) {
    if (id.includes(pattern)) return 'app-shell'
  }

  for (const [pattern, name] of Object.entries(CHUNK_GROUPS.routes)) {
    if (id.includes(pattern)) return name
  }

  for (const [pattern, name] of Object.entries(CHUNK_GROUPS.i18n)) {
    if (id.includes(pattern)) return name
  }

  return undefined
}

export default defineConfig({
  plugins: [react()],
  build: {
    sourcemap: false,
    target: 'es2015',
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },
    rollupOptions: {
      output: {
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
        manualChunks(id) {
          const byRule = getChunkName(id)
          if (byRule) return byRule
          if (id.includes('node_modules/antd') || id.includes('node_modules/@ant-design')) {
            return 'vendor-antd'
          }
          if (id.includes('node_modules/echarts')) {
            return 'vendor-charts'
          }
          return undefined
        },
      },
    },
    cssCodeSplit: true,
    chunkSizeWarningLimit: 500,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 4173,
  },
  resolve: {
    alias: {
      '@': '/src',
    },
  },
})
