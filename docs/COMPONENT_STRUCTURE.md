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
