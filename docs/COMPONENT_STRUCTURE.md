# 组件目录结构规范

## 当前结构

```
components/pages/
├── approvals/
│   ├── ApprovalStatsCards.jsx
│   ├── ApprovalTaskSection.jsx
│   ├── ApprovalTemplateRows.jsx
│   ├── ApprovalNodeConfigPanel.jsx
│   └── ApprovalVersionHistoryPanel.jsx
├── customers/
│   ├── customers/
│   │   ├── customer360/
│   │   │   └── Customer360View.jsx
│   │   ├── sections/
│   │   │   ├── CustomerRow.jsx
│   │   │   ├── CustomerFormBar.jsx
│   │   │   ├── CustomerBatchToolbar.jsx
│   │   │   ├── CustomersTableSection.jsx
│   │   │   ├── CustomerDetailDrawerSection.jsx
│   │   │   └── CustomerFilterBar.jsx
│   │   ├── hooks/
│   │   │   ├── index.js
│   │   │   ├── useCustomersDetailNavigation.js
│   │   │   ├── useCustomersListModel.js
│   │   │   └── useCustomerBatchOperations.js
│   │   ├── index.js
│   │   ├── CustomersPanelContainer.jsx
│   │   ├── CustomersPanelRuntime.jsx
│   │   ├── CustomersPanelSections.jsx
│   │   └── shared.js
│   ├── CustomersPanel.jsx  (根目录)
│   └── ...
├── quotes/
│   ├── QuotePanelRows.jsx
│   ├── QuoteItemsSection.jsx
│   ├── QuoteEditorModal.jsx
│   └── quotePanelHelpers.js
├── orders/
│   ├── OrderPanelRows.jsx
│   ├── OrderEditorModal.jsx
│   └── orderPanelHelpers.js
├── pipeline/
│   └── PipelineRows.jsx
├── leads/
│   ├── LeadPanelRows.jsx
│   ├── LeadImportSection.jsx
│   └── LeadListSection.jsx
├── reportDesigner/
│   └── ReportDesignerPreviewSection.jsx
├── governance/
│   ├── UsersGovernanceSection.jsx
│   ├── SalesAutomationSection.jsx
│   └── TenantsGovernanceSection.jsx
├── dashboard/
├── ApprovalsPageContainer.jsx  (根目录)
├── AuditPanel.jsx              (根目录)
├── ContactsPanel.jsx           (根目录)
├── ContractsPanel.jsx          (根目录)
├── DashboardPanel.jsx          (根目录)
├── FollowUpsPanel.jsx          (根目录)
├── GovernancePageContainer.jsx (根目录)
├── LeadsPanel.jsx              (根目录)
├── OrdersPanel.jsx             (根目录)
├── PaymentsPanel.jsx            (根目录)
├── PermissionsPanel.jsx        (根目录)
├── PipelinePanel.jsx           (根目录)
├── PriceBooksPanel.jsx         (根目录)
├── ProductsPanel.jsx           (根目录)
├── QuotesPanel.jsx             (根目录)
├── ReportDesignerPanel.jsx     (根目录)
└── TasksPanel.jsx             (根目录)
```

## 建议的改进

### 1. 统一 Panel 组件位置

对于没有子目录的 Panel，建议迁移到对应的子目录：

```
# 建议迁移
AuditPanel.jsx → audit/AuditPanel.jsx
ContactsPanel.jsx → contacts/ContactsPanel.jsx
ContractsPanel.jsx → contracts/ContractsPanel.jsx
FollowUpsPanel.jsx → follow-ups/FollowUpsPanel.jsx
PaymentsPanel.jsx → payments/PaymentsPanel.jsx
PermissionsPanel.jsx → permissions/PermissionsPanel.jsx
PriceBooksPanel.jsx → price-books/PriceBooksPanel.jsx
ProductsPanel.jsx → products/ProductsPanel.jsx
TasksPanel.jsx → tasks/TasksPanel.jsx
```

### 2. 目录命名规范

| 当前 | 建议 |
|------|------|
| `followUps/` | `follow-ups/` |
| `priceBooks/` | `price-books/` |

## 注意事项

1. **不强制要求立即迁移** - 现有结构可以正常工作
2. **如需迁移** - 需要更新所有相关的 import 路径
3. **保持一致性** - 新增的 Panel 组件应遵循建议的目录结构

## 建议的文件组织模式

每个功能域应遵循以下模式：

```
domain/
├── DomainPanel.jsx          # 主面板容器
├── components/              # 子组件
│   ├── DomainRow.jsx
│   ├── DomainEditorModal.jsx
│   └── DomainDetailSection.jsx
├── hooks/                  # 领域特定 hooks
│   └── useDomainLoader.js
└── helpers/                # 辅助函数
    └── domainHelpers.js
```

## 性能优化模式

### React.memo 优化

表格行组件（`*Row.jsx`）和列表项组件推荐使用 `React.memo` 包裹，避免因父组件状态变化导致的不必要重渲染。

适用场景：
- 数据量大、渲染频繁的列表项
- 接收复杂对象 props 的组件
- 纯展示型组件

示例：
```jsx
const CustomerRow = React.memo(function CustomerRow({ customer, onEdit }) {
  return (
    <tr>
      <td>{customer.name}</td>
      <td>{customer.email}</td>
      <td><button onClick={() => onEdit(customer)}>编辑</button></td>
    </tr>
  );
});
```

### Vite 分块策略

`vite.config.js` 中的 `manualChunks` 配置将第三方库拆分为独立 chunk，优化首屏加载：

| Chunk 名称 | 包含内容 | 加载时机 |
|------------|----------|----------|
| `vendor-antd` | Ant Design 组件库 | 使用 Antd 组件时按需加载 |
| `vendor-charts` | 图表库（echarts/recharts） | 使用图表组件时按需加载 |
| `vendor-react` | React 核心库 | 首屏必须 |

配置示例：
```javascript
manualChunks: {
  'vendor-react': ['react', 'react-dom', 'react-router-dom'],
  'vendor-antd': ['antd', '@ant-design/icons'],
  'vendor-charts': ['echarts', 'echarts-for-react'],
}
```

优化效果：
- 减少首屏包体积
- 利用浏览器缓存（vendor 包变化频率低）
- 并行加载多个 chunk
