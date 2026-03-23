# 功能模块矩阵（Page -> API -> Data）

用于快速理解“页面对应哪些后端能力与数据实体”。

## 1. 核心业务模块

| 模块 | 主要前端页面/组件 | 主要后端接口 | 主要实体/表 | 备注 |
|---|---|---|---|---|
| 认证与会话 | `AuthShell` / 应用入口壳层 | `/api/auth/*`, `/api/v1/auth/*` | `user_accounts`, `user_invitations`, `audit_logs` | 支持登录、会话、邀请、MFA/OIDC 回调 |
| 仪表盘 | `DashboardPanel.jsx` | `/api/dashboard` | 客户/商机/订单等聚合查询 | 首页概览与关键指标 |
| 客户管理 | `CustomersPanel.jsx` | `/api/customers*`, `/api/v1/customers/batch-actions` | `customers` | 支持搜索、新增、删除、批量动作 |
| 联系人与跟进 | `ContactsPanel.jsx`, `FollowUpsPanel.jsx` | `/api/contacts*`, `/api/follow-ups*` | `contacts`, `follow_ups` | 客户关系维护链路 |
| 商机管道 | `PipelinePanel.jsx` | `/api/opportunities*`, `/api/v1/opportunities/batch-actions` | `opportunities` | 销售阶段推进与批量分配 |
| 任务管理 | `TasksPanel.jsx` | `/api/tasks*` | `tasks` | 任务查询、状态更新、创建 |
| 线索中心 | `LeadsPanel.jsx` | `/api/v1/leads*`, `/api/v1/leads/assignment-rules*` | `leads`, `lead_assignment_rules`, `lead_import_*` | 包含导入模板、异步导入、分配规则 |
| 商品与价目表 | `ProductsPanel.jsx`, `PriceBooksPanel.jsx` | `/api/v1/products*`, `/api/v1/price-books*` | `products`, `price_books`, `price_book_items` | 商业化基础数据 |
| 报价与版本 | `QuotesPanel.jsx` | `/api/v1/quotes*` | `quotes`, `quote_items`, `quote_versions` | 报价提交、接受、转订单 |
| 订单/合同/回款 | `OrdersPanel.jsx`, `ContractsPanel.jsx`, `PaymentsPanel.jsx` | `/api/v1/orders*`, `/api/contracts*`, `/api/payments*` | `order_records`, `contract_records`, `payment_records` | 履约与回款闭环 |

## 2. 增强模块（V13-V17）

| 模块 | 主要前端页面/组件 | 主要后端接口 | 主要实体/表 | 备注 |
|---|---|---|---|---|
| 图表与可视化 | `components/charts/*` | `/api/v2/charts/*` | `chart_templates` | 图表数据、模板、克隆 |
| 全局搜索与快筛 | `components/search/*` | `/api/v2/search/*`, `/api/v2/filters/quick*` | `search_index`, `saved_searches`, `quick_filters` | 搜索建议、历史、保存筛选 |
| 工作流引擎 | `components/workflow/*` | `/api/v2/workflows/*`（含执行相关） | `workflow_definitions`, `workflow_nodes`, `workflow_connections`, `workflow_executions` | 定义与执行分离 |
| 协作与审批增强 | `components/collaboration/*`, `ApprovalsPageContainer.jsx` | `/api/v2/collaboration/*`, `/api/v1/approval/*` | `comments`, `activity_shares`, `teams`, `approval_*` | 评论、@提及、委派、加签、转办 |
| 权限与敏感字段 | `PermissionsPanel.jsx`, `GovernancePageContainer.jsx` | `/api/permissions/*`, `/api/v2/compliance/*` | `field_permissions`, `sensitive_field_configs` | 字段级权限与合规治理 |

## 3. 报表与运营治理

| 模块 | 主要前端页面/组件 | 主要后端接口 | 主要实体/表 | 备注 |
|---|---|---|---|---|
| 报表总览 | `ReportDesignerPanel.jsx` + 报表相关组件 | `/api/reports/*`, `/api/v1/reports/*` | `report_designer_templates`, 导出任务表 | 总览、漏斗、导出作业 |
| 审计与运维 | `AuditPanel.jsx`, 治理页 | `/api/audit-logs*`, `/api/v1/ops/*` | `audit_logs` + 运营指标聚合 | 审计检索、导出与健康指标 |

## 4. 三方集成能力

| 渠道 | 接入方式 | 主要接口/服务 | 配置来源 |
|---|---|---|---|
| 企业微信 | webhook | `/api/v1/integrations/webhooks/wecom` + `IntegrationWebhookService` | `INTEGRATION_WECOM_WEBHOOK_URL` |
| 钉钉 | webhook（可签名） | `/api/v1/integrations/webhooks/dingtalk` + `IntegrationWebhookService` | `INTEGRATION_DINGTALK_WEBHOOK_URL`, `INTEGRATION_DINGTALK_SECRET` |
| 飞书 | App 模式优先，webhook 兜底 | `/api/v1/integrations/webhooks/feishu` + `IntegrationWebhookService` | `INTEGRATION_FEISHU_APP_ID`, `INTEGRATION_FEISHU_APP_SECRET`, `INTEGRATION_FEISHU_RECEIVE_ID` |

## 5. 代码阅读顺序建议

1. 前端入口与面板装配：`apps/web/src/crm/components/MainContentPanels.jsx`
2. 对应页面：`apps/web/src/crm/components/pages/*`
3. API 调用封装：`apps/web/src/crm/hooks/useApi.js`
4. 后端路由层：`apps/api/src/main/java/com/yao/crm/controller/*`
5. 业务实现：`apps/api/src/main/java/com/yao/crm/service/*`
6. 迁移与表结构：`apps/api/src/main/resources/db/migration/*`

## 6. 与本矩阵配套阅读

- 全景调用关系：`docs/PROJECT_FLOW_MAP.md`
- 热点文件地图：`docs/DEVELOPMENT_HOTSPOTS.md`
- 故障定位手册：`docs/PROJECT_TROUBLESHOOTING.md`
