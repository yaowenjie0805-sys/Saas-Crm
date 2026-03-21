# 审计日志保留策略 | Audit Retention Policy

---

## 适用范围 | Scope

| 中文 | English |
|------|---------|
| 适用于 `/api/audit-logs/**` 及审计导出任务 | Applies to `/api/audit-logs/**` and audit export jobs. |

---

## 基线策略 | Baseline Policy

| 中文 | English |
|------|---------|
| **保留周期**: 在线保留 180 天 | **Retention period**: 180 days online. |
| **导出频率**: 每周导出前 7 天数据 | **Export cadence**: weekly export of previous 7 days. |
| **高优先级事件**: `TENANT_FORBIDDEN`、认证失败、导出失败 | **High-priority events**: `TENANT_FORBIDDEN`, auth failures, export failures. |
| **敏感字段**: 在 `details` 中脱敏处理密钥/令牌/密码 | **Sensitive fields**: redact secrets/tokens/passwords in `details`. |

---

## 导出失败处理 | Export Failure Handling

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 检查 `/api/audit-logs/export-metrics` 查看队列/失败趋势 | Check `/api/audit-logs/export-metrics` for queue/failure trend. |
| 2 | 通过 `/api/audit-logs/export-jobs/{jobId}/retry` 重试失败任务 | Retry failed job via `/api/audit-logs/export-jobs/{jobId}/retry`. |
| 3 | 如持续失败，发起事故并附带 requestId + tenantId 上下文 | If repeated failures continue, open incident with requestId + tenantId context. |

---

## 合规回溯 | Compliance Replay

| 中文 | English |
|------|---------|
| 使用 `requestId + tenantId + username + action` 进行时间线重建 | Use `requestId + tenantId + username + action` for timeline reconstruction. |
| 将导出的 CSV 文件保存在受限访问存储中 | Keep exported CSV artifacts in restricted access storage. |
