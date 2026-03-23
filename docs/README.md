# 文档索引（Docs Index）

本目录存放项目运维、发布、质量与治理文档。

## 快速入口

- 项目根文档：`README.md`
- 结构说明：`docs/PROJECT_STRUCTURE.md`
- 全景调用图：`docs/PROJECT_FLOW_MAP.md`
- 命令总览：`docs/operations/command-reference.md`
- 环境配置：`docs/operations/environment-matrix.md`

## 运维文档（operations）

### 发布与环境

- `docs/operations/release-strategy.md`
- `docs/operations/release-window-policy.md`
- `docs/operations/staging-deploy-runbook.md`
- `docs/operations/release-rollback-runbook.md`
- `docs/operations/environment-matrix.md`

### 可靠性与性能

- `docs/operations/sre-slo-baseline.md`
- `docs/operations/error-budget-policy.md`
- `docs/operations/perf-baseline.md`
- `docs/operations/perf-bundle-budget.json`

### 安全与审计

- `docs/operations/audit-retention-policy.md`
- `docs/operations/backup-restore-runbook.md`
- `docs/operations/oncall-escalation-runbook.md`

### 模板与清单

- `docs/operations/change-control-checklist.md`
- `docs/operations/change-request-template.md`
- `docs/operations/incident-postmortem-template.md`
- `docs/operations/weekly-oncall-alert-review-template.md`

## 历史归档

- `docs/operations/archive/`

---

建议从 `README.md` 的“快速开始”和“迁移修复”章节开始，再按需要深入 `docs/operations`。

## 新增导航

- `docs/DEVELOPMENT_HOTSPOTS.md`：二开时“改哪里”的热点地图。
- `docs/PROJECT_FLOW_MAP.md`：页面/API/数据表的全景调用关系。
- `docs/PROJECT_TROUBLESHOOTING.md`：启动失败、集成失败等常见故障处理。
- `docs/MODULE_CAPABILITY_MATRIX.md`：页面、接口、实体三层功能矩阵。
- `docs/API_ENDPOINT_CATALOG.md`：按 Controller 分组的接口清单（含示例请求）。
- `docs/postman/crm-api.postman_collection.json`：本地联调 Collection。
- `docs/postman/crm-local.postman_environment.json`：本地联调 Environment。
