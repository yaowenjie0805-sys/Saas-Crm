# 接口分组清单（API Endpoint Catalog）

本清单用于联调与二开定位，按后端 Controller 分组给出核心端点与最小请求示例。

## 1. 通用约定

- Base URL：`http://localhost:8080`
- 通用前缀：`/api`、`/api/v1`、`/api/v2`
- 常用请求头：
  - `Authorization: Bearer <token>`
  - `X-Tenant-Id: <tenantId>`
  - `Content-Type: application/json`

## 2. 认证与会话

Controller：`AuthController`、`V1AuthController`

核心端点：
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `GET /api/auth/session`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/mfa/verify`
- `POST /api/v1/auth/oidc/callback`

示例：
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 3. CRM 基础对象

Controller：`CustomerController`、`ContactController`、`OpportunityController`、`TaskController`、`FollowUpController`

核心端点：
- `GET /api/customers`
- `GET /api/customers/search`
- `POST /api/customers`
- `DELETE /api/customers/{id}`
- `GET /api/contacts/search`
- `POST /api/contacts`
- `GET /api/opportunities`
- `POST /api/opportunities`
- `GET /api/tasks`
- `POST /api/tasks`
- `GET /api/follow-ups/search`

示例：
```bash
curl -X GET "http://localhost:8080/api/customers?size=20&page=0" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 1"
```

## 4. 商业化链路（商品/报价/订单/回款）

Controller：`V1CommerceController`、`ContractController`、`PaymentController`

核心端点：
- `GET /api/v1/products`
- `GET /api/v1/price-books`
- `GET /api/v1/quotes`
- `POST /api/v1/quotes/{id}/submit`
- `POST /api/v1/quotes/{id}/to-order`
- `GET /api/v1/orders`
- `POST /api/v1/orders/{id}/confirm`
- `GET /api/contracts/search`
- `GET /api/payments/search`

## 5. 线索与导入

Controller：`V1LeadController`、`V1LeadAssignmentController`

核心端点：
- `GET /api/v1/leads`
- `POST /api/v1/leads`
- `POST /api/v1/leads/{id}/convert`
- `POST /api/v1/leads/{id}/assign`
- `GET /api/v1/leads/import-template`
- `POST /api/v1/leads/import-jobs`（multipart）
- `GET /api/v1/leads/import-jobs/{jobId}`
- `POST /api/v1/leads/import-jobs/{jobId}/retry`

示例（导入任务创建）：
```bash
curl -X POST "http://localhost:8080/api/v1/leads/import-jobs" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 1" \
  -F "file=@./leads.xlsx"
```

## 6. 审批与协作

Controller：`V1ApprovalController`、`CollaborationController`

核心端点：
- `POST /api/v1/approval/templates`
- `GET /api/v1/approval/templates`
- `POST /api/v1/approval/instances/{bizType}/{bizId}/submit`
- `POST /api/v1/approval/tasks/{taskId}/approve`
- `POST /api/v1/approval/tasks/{taskId}/reject`
- `POST /api/v2/collaboration/comments`
- `POST /api/v2/collaboration/comments/{commentId}/reply`
- `GET /api/v2/collaboration/entities/{entityType}/{entityId}/comments`
- `POST /api/v2/collaboration/approval/tasks/{taskId}/delegate`

## 7. 搜索、筛选、图表、工作流

Controller：`SearchController`、`QuickFilterController`、`ChartController`、`WorkflowController`

核心端点：
- `GET /api/v2/search`
- `GET /api/v2/search/suggestions`
- `POST /api/v2/search/saved`
- `GET /api/v2/filters/quick`
- `POST /api/v2/filters/quick`
- `GET /api/v2/charts/data`
- `POST /api/v2/charts/preview`
- `GET /api/v2/charts/templates`
- `GET /api/v2/workflows/*`（定义与执行相关）

## 8. 报表、审计、运维

Controller：`ReportController`、`V1ReportController`、`V1ReportDesignerController`、`AuditController`、`V1OpsController`

核心端点：
- `GET /api/reports/overview`
- `POST /api/reports/export-jobs`
- `GET /api/v1/reports/funnel`
- `POST /api/v1/reports/designer/datasets/query`
- `GET /api/audit-logs/search`
- `GET /api/v1/ops/health`
- `GET /api/v1/ops/metrics/summary`

## 9. 三方集成（微信/钉钉/飞书）

Controller：`V1IntegrationController`、`V2IntegrationConnectorController`、`V1NotificationJobController`

核心端点：
- `POST /api/v1/integrations/webhooks/wecom`
- `POST /api/v1/integrations/webhooks/dingtalk`
- `POST /api/v1/integrations/webhooks/feishu`
- `GET /api/v1/integrations/notifications/jobs`
- `POST /api/v1/integrations/notifications/jobs/{jobId}/retry`

示例（飞书发送）：
```bash
curl -X POST "http://localhost:8080/api/v1/integrations/webhooks/feishu" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 1" \
  -d '{"text":"CRM 联调测试消息"}'
```

## 10. 健康检查与启动验证

Controller：`HealthController`

核心端点：
- `GET /api/health`
- `GET /api/health/live`
- `GET /api/health/ready`
- `GET /api/health/deps`

示例：
```bash
curl "http://localhost:8080/api/health"
```

## 11. 建议联调顺序

1. 先通 `GET /api/health`。
2. 再通登录拿 token。
3. 先跑客户/线索等主链路。
4. 最后联调审批、报表、三方通知。

## 12. 配套文档

- `docs/MODULE_CAPABILITY_MATRIX.md`
- `docs/PROJECT_FLOW_MAP.md`
- `docs/PROJECT_TROUBLESHOOTING.md`
- `docs/DEVELOPMENT_HOTSPOTS.md`

## 13. Postman 快速联调

- Collection: `docs/postman/crm-api.postman_collection.json`
- Environment: `docs/postman/crm-local.postman_environment.json`
- 推荐顺序：`01 Auth -> 00 Health -> 02+`（先登录自动写入 token）
