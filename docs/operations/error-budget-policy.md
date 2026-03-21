# 错误预算策略 | Error Budget Policy

---

## 预算窗口 | Budget Windows

| 中文 | English |
|------|---------|
| **日预算**: `2%` API 错误率当量 (`ops.error-budget.daily-max`) | **Daily budget**: `2%` API error-rate equivalent (`ops.error-budget.daily-max`). |
| **周预算**: `5%` 累计当量 (`ops.error-budget.weekly-max`) | **Weekly budget**: `5%` cumulative equivalent (`ops.error-budget.weekly-max`). |

---

## 发布决策规则 | Release Decision Rules

| 规则 | 中文 | English |
|------|------|---------|
| 阻断发布 | 当 `alertsLevel=P1` 时 | Block release if `alertsLevel=P1`. |
| 阻断发布 | 当日或周错误预算超限时 | Block release if daily or weekly error budget is exceeded. |
| 阻断发布 | 当当前值班主责人或升级链缺失时 | Block release if current oncall primary or escalation chain is missing. |
| 谨慎发布 | 当仅存在 `P2/P3` 建议性告警时 | Allow release with caution when only `P2/P3` advisory alerts exist. |

---

## 门禁真实来源 | Gate Source of Truth

| 中文 | English |
|------|---------|
| `GET /api/v1/ops/slo-snapshot`: | `GET /api/v1/ops/slo-snapshot`: |
| - `errorBudget.daily/weekly` | - `errorBudget.daily/weekly` |
| - `alertsLevel` 和 `alertsDetailed` | - `alertsLevel` and `alertsDetailed` |
| - `oncall` | - `oncall` |
| `logs/sre/alerts-latest.json` 作为预检可审计快照 | `logs/sre/alerts-latest.json` as auditable snapshot for preflight. |

---

## 恢复预期 | Recovery Expectations

| 中文 | English |
|------|---------|
| 缓解或回滚后，重新运行: | After mitigation or rollback, rerun: |
| - `npm run sre:daily-check` | - `npm run sre:daily-check` |
| - `npm run sre:alert-check` | - `npm run sre:alert-check` |
| - `npm run preflight:prod` | - `npm run preflight:prod` |
