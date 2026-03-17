# 架构方案

## 摘要
采用“拦截器日志 + 健康检查控制器 + 任务服务指标 + 运维脚本”组合实现，不改变现有 API 业务契约。

## 核心设计
- `ApiDiagnosticsInterceptor` 输出统一结构化请求日志。
- `HealthController` 新增 `/health/live`、`/health/ready`、`/health/deps`。
- `AuditExportJobService` 增加内存态运行指标快照，`AuditController` 暴露查询接口。
- `scripts/preflight-prod.mjs` 聚合上线前配置和依赖检查。
- `scripts/db-backup.ps1` / `scripts/db-restore.ps1` 作为最小可执行恢复工具。
