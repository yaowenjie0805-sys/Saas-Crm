# 二开热点文件地图（Development Hotspots）

这份文档用于“快速找到该改哪里”，适合新同学接手或你后续排查功能时直接跳转。

## 1. 启动与配置入口

- 后端主入口：`apps/api/src/main/java/com/yao/crm/CrmApplication.java`
- 后端配置主文件：`apps/api/src/main/resources/application.properties`
- 本地环境变量自动加载：`scripts/run-maven.mjs`
- 本地后端环境模板：`.env.backend.local.example`

## 2. API 分层与责任

- Controller 层（路由入口）：`apps/api/src/main/java/com/yao/crm/controller/`
- Service 层（业务编排）：`apps/api/src/main/java/com/yao/crm/service/`
- Repository 层（数据访问）：`apps/api/src/main/java/com/yao/crm/repository/`
- Entity 层（表映射）：`apps/api/src/main/java/com/yao/crm/entity/`

定位原则：
- “接口 404/参数绑定问题”优先看 Controller。
- “业务规则不对/通知未发”优先看 Service。
- “查询慢/结果不准”优先看 Repository + migration 索引。

## 3. Flyway 与数据库迁移热点

- 迁移目录：`apps/api/src/main/resources/db/migration/`
- 当前重点版本：`V11` ~ `V17`
- 一键修复脚本：
  - Windows: `scripts/flyway-repair-dev.ps1`
  - Linux/macOS: `scripts/flyway-repair-dev.sh`

建议：
- 已发布 migration 不再改结构语义；若必须调整，优先新增更高版本脚本。
- 本地历史库出现 checksum mismatch 时，先备份再 repair/migrate。

## 4. 三方通知集成热点（微信/钉钉/飞书）

- 核心发送逻辑：`apps/api/src/main/java/com/yao/crm/service/IntegrationWebhookService.java`
- 集成入口接口：
  - `apps/api/src/main/java/com/yao/crm/controller/V1IntegrationController.java`
  - `apps/api/src/main/java/com/yao/crm/controller/V2IntegrationConnectorController.java`
- 连通性脚本：`scripts/test-webhooks.ps1`

飞书当前推荐路径：
- 优先使用 App 模式（App ID/App Secret -> tenant_access_token -> im/v1/messages）。
- webhook 作为兜底，不建议作为主通道。

## 5. 导入导出与任务链路热点

- 导入导出服务：`apps/api/src/main/java/com/yao/crm/service/DataImportExportService.java`
- 通知分发：`apps/api/src/main/java/com/yao/crm/service/NotificationDispatchService.java`
- 通知任务：`apps/api/src/main/java/com/yao/crm/service/NotificationJobService.java`

排查建议：
- 先看任务状态与日志，再看外部调用返回码。
- 失败重试问题优先检查任务表状态与定时触发条件。

## 6. 前端页面与状态管理热点

- 前端应用主目录：`apps/web/src/crm/`
- 页面组件：`apps/web/src/crm/components/pages/`
- 通用组件：`apps/web/src/crm/components/common/`
- API Hook：`apps/web/src/crm/hooks/useApi.js`
- 全局状态：`apps/web/src/crm/store/appStore.js`
- 打包分块策略：`apps/web/vite.config.js`

排查建议：
- 页面空白/跳转异常：先看 Router 与页面懒加载导入。
- 数据不刷新：先看 store 更新，再看 useApi 调用参数。
- 构建告警：优先看 `vite.config.js` 的 `manualChunks`。

## 7. 常用“先看这里”清单

1. 启动失败：`docs/PROJECT_TROUBLESHOOTING.md`
2. 功能流向不清：`docs/PROJECT_FLOW_MAP.md`
3. 命令不会用：`docs/operations/command-reference.md`
4. 环境变量不确定：`docs/operations/environment-matrix.md`

## 8. 二开变更建议

1. 业务改动尽量只落在 Service + 新 migration，减少对旧脚本回改。
2. 涉及三方集成时，先补脚本化连通性检查，再改业务发送逻辑。
3. 每次改完至少跑：`npm run test:backend` + `npm run build`。
