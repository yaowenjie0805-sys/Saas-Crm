import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 代码分割配置
const CHUNK_GROUPS = {
  vendor: [
    { test: /node_modules\/react/, name: 'vendor-react' },
    { test: /node_modules\/react-dom/, name: 'vendor-react' },
    { test: /node_modules\/react-router/, name: 'vendor-router' },
    { test: /node_modules\/zustand/, name: 'vendor-state' },
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
    'DashboardPanel': 'route-dashboard',
    'customer360': 'route-customers-detail',
    'CustomersPanel': 'route-sales-core',
    'CustomersPanelRuntime': 'route-sales-core',
    'CustomersPanelContainer': 'route-sales-core',
    'useBatchActions': 'route-sales-core',
    'QuotesPanel': 'route-commerce',
    'OrdersPanel': 'route-commerce',
    'quotes': 'route-commerce-detail',
    'orders': 'route-commerce-detail',
    'PipelinePanel': 'route-sales-core',
    'ApprovalsPageContainer': 'route-approvals',
    'GovernancePageContainer': 'route-governance',
    'ReportDesignerPanel': 'route-report-designer',
    'reportDesigner': 'route-report-designer-detail',
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
  // 1. Vendor chunks
  for (const { test, name } of CHUNK_GROUPS.vendor) {
    if (test.test(id)) return name
  }

  // 2. App runtime core (avoid circular deps)
  for (const pattern of CHUNK_GROUPS.appRuntime) {
    if (id.includes(pattern)) return 'app-runtime-core'
  }

  // 3. Shell chunks
  for (const pattern of CHUNK_GROUPS.shell) {
    if (id.includes(pattern)) return 'app-shell'
  }

  // 4. Route chunks
  for (const [pattern, name] of Object.entries(CHUNK_GROUPS.routes)) {
    if (id.includes(pattern)) return name
  }

  // 5. i18n chunks
  for (const [pattern, name] of Object.entries(CHUNK_GROUPS.i18n)) {
    if (id.includes(pattern)) return name
  }

  return undefined
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    sourcemap: false,
    rollupOptions: {
      output: {
        chunkFileNames: 'assets/[name]-[hash].js',
        manualChunks: getChunkName,
      },
    },
  },
})
