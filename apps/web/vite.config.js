import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    sourcemap: false,
    rollupOptions: {
      output: {
        chunkFileNames: 'assets/[name]-[hash].js',
        manualChunks(id) {
          if (id.includes('node_modules/react') || id.includes('node_modules/react-dom')) {
            return 'vendor-react'
          }
          if (id.includes('node_modules/react-router')) {
            return 'vendor-router'
          }
          if (id.includes('node_modules/zustand')) {
            return 'vendor-state'
          }
          if (id.includes('node_modules')) {
            return 'vendor-misc'
          }
          // Domain loaders share circular deps with orchestrators,
          // so they must stay in the same chunk to avoid circular chunk warnings.
          if (id.includes('/src/crm/hooks/useCoreListDomainLoaders')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/hooks/useWorkbenchDomainLoaders')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/hooks/useCommerceDomainLoaders')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/hooks/useGovernanceDomainLoaders')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/hooks/useApprovalDomainLoaders')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/hooks/useReportingAuditDomainLoaders')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/hooks/useLeadImportDomainLoaders')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/hooks/orchestrators/')) {
            return 'app-runtime-core'
          }
          if (id.includes('/src/crm/components/shell/')) {
            return 'app-shell'
          }
          if (id.includes('/src/crm/components/SidebarNav')) {
            return 'app-shell'
          }
          if (id.includes('/src/crm/components/pages/DashboardPanel')) {
            return 'route-dashboard'
          }
          if (id.includes('/src/crm/components/pages/customers/customer360/')) {
            return 'route-customers-detail'
          }
          if (
            id.includes('/src/crm/components/pages/CustomersPanel')
            || id.includes('/src/crm/components/pages/customers/CustomersPanelContainer')
            || id.includes('/src/crm/components/pages/customers/CustomersPanelRuntime')
            || id.includes('/src/crm/hooks/useBatchActions')
          ) {
            return 'route-customers'
          }
          if (id.includes('/src/crm/components/pages/quotes/') || id.includes('/src/crm/components/pages/orders/')) {
            return 'route-commerce-detail'
          }
          if (id.includes('/src/crm/components/pages/QuotesPanel') || id.includes('/src/crm/components/pages/OrdersPanel')) {
            return 'route-commerce'
          }
          if (id.includes('/src/crm/components/pages/PipelinePanel')) {
            return 'route-pipeline'
          }
          if (id.includes('/src/crm/components/pages/ApprovalsPageContainer')) {
            return 'route-approvals'
          }
          if (id.includes('/src/crm/components/pages/GovernancePageContainer')) {
            return 'route-governance'
          }
          if (id.includes('/src/crm/components/pages/ReportDesignerPanel')) {
            return 'route-report-designer'
          }
          if (id.includes('/src/crm/components/pages/reportDesigner/')) {
            return 'route-report-designer-detail'
          }
          if (id.includes('/src/crm/i18n/common/en')) {
            return 'crm-i18n-en'
          }
          if (id.includes('/src/crm/i18n/common/zh')) {
            return 'crm-i18n-zh'
          }
          if (id.includes('/src/crm/i18n/namespaces/market-dashboard')) {
            return 'crm-i18n-ns-dashboard'
          }
          if (id.includes('/src/crm/i18n/namespaces/market-governance')) {
            return 'crm-i18n-ns-governance'
          }
          if (id.includes('/src/crm/i18n/namespaces/opportunity-workbench')) {
            return 'crm-i18n-ns-opportunity-workbench'
          }
          if (id.includes('/src/crm/i18n/namespaces/report-designer')) {
            return 'crm-i18n-ns-report-designer'
          }
          if (id.includes('/src/crm/i18n/namespaces/')) {
            return 'crm-i18n-ns'
          }
          if (id.includes('/src/crm/i18n.js')) {
            return 'crm-i18n-core'
          }
          return undefined
        },
      },
    },
  },
})
