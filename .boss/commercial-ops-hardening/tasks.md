# 任务拆解：SLO 告警与值班闭环

## 提交 1（观测与告警数据面）
- 扩展 `/api/v1/ops/slo-snapshot`：补齐 `errorBudget`、分级 `alerts`、`oncall` 字段（兼容追加）。
- 新增 `npm run sre:alert-check`：读取 snapshot 与运维证据，输出 `logs/sre/alerts-*.json` 和 `alerts-latest.json`。
- 主干与 staging 工作流在 `sre:daily-check` 后强制执行 `sre:alert-check`。

## 提交 2（值班与发布决策闭环）
- 扩展 `preflight:prod`：新增错误预算、P1 告警、值班覆盖阻断项。
- 补齐运行文档：
  - `docs/operations/oncall-escalation-runbook.md`
  - `docs/operations/error-budget-policy.md`
  - `docs/operations/weekly-oncall-alert-review-template.md`
- 同步 README 与 Boss 产物状态字段。

## QA Gate（串行）
`lint -> build -> test:e2e -> test:backend -> test:full -> perf:baseline -> perf:gate -> sre:daily-check -> sre:alert-check -> security:scan -> preflight:prod`

## 验收场景
- 触发错误率阈值时 `sre:alert-check` 非 0 退出并输出 P1/P2/P3 建议。
- 预算超阈值或无 oncall 信息时 `preflight:prod` 稳定阻断。
- 证据包可检索 alert verdict + error budget + oncall 三类字段。
