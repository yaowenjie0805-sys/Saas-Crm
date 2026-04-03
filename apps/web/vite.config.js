import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 代码分割配置
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
    'DashboardPanel': 'route-dashboard',
    'customer360': 'route-customers-detail',
    'CustomersPanel': 'route-sales-core',
    'CustomersPanelRuntime': 'route-sales-core',
    'CustomersPanelContainer': 'route-sales-core',
    'useBatchActions': 'route-sales-core',
    'QuotesPanel': 'route-commerce',
    'OrdersPanel': 'route-commerce',
    // Keep commerce detail modules in the same chunk to avoid circular chunk warnings
    'quotes': 'route-commerce',
    'orders': 'route-commerce',
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
    // 生产环境禁用 sourcemap
    sourcemap: false,
    // 目标浏览�?
    target: 'es2015',
    // 代码压缩
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,    // 生产环境移除 console
        drop_debugger: true,   // 移除 debugger
      },
    },
    // 分包配置
    rollupOptions: {
      output: {
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
        manualChunks(id) {
          const byRule = getChunkName(id)
          if (byRule) return byRule
          // 大型库单独分�?
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
    // CSS 代码分割
    cssCodeSplit: true,
    // 启用chunk分层
    chunkSizeWarningLimit: 500,
  },
  // 开发服务器优化
  server: {
    port: 5173,
    // 代理配置
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  // 预览服务器配�?
  preview: {
    port: 4173,
  },
  // 路径解析优化
  resolve: {
    alias: {
      '@': '/src',
    },
  },
});

