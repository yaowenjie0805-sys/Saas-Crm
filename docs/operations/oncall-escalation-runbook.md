# 值班升级手册 | Oncall Escalation Runbook

---

## 告警级别 | Alert Levels

| 级别 | 中文 | English |
|------|------|---------|
| `P1` | 影响客户的服务中断或阻断发布的故障。需要立即响应。 | customer-impacting outage or release-blocking fault. Immediate response required. |
| `P2` | 显著降级但未完全中断。当班内响应。 | significant degradation with no complete outage. Response in current shift. |
| `P3` | 警告级别趋势或阈值漂移。24小时内跟踪解决。 | warning-level trend or threshold drift. Track and resolve within 24h. |

---

## 升级路径 | Escalation Path

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 主值班人员确认告警并启动事故记录 | Primary oncall acknowledges alert and starts incident record. |
| 2 | 如 15 分钟 (`P1`) 或 60 分钟 (`P2`) 内未缓解，升级至 SRE 负责人 | If no mitigation in 15 minutes (`P1`) or 60 minutes (`P2`), escalate to SRE Lead. |
| 3 | 如第二个 SLA 窗口后仍未解决，升级至工程经理 | If unresolved after second SLA window, escalate to Engineering Manager. |
| 4 | 对于跨租户/安全事件，立即包含安全负责人 | For cross-tenant/safety incidents, include Security owner immediately. |

---

## 必需证据 | Required Evidence

| 中文 | English |
|------|---------|
| `requestId`, `tenantId`, 受影响路由及首次发现时间戳 | `requestId`, `tenantId`, impacted route, and first seen timestamp. |
| `logs/sre/alerts-latest.json` 包含级别/原因/建议 | `logs/sre/alerts-latest.json` with level/reasons/recommendation. |
| 回滚决策及验证结果 (`staging:verify` / 健康检查) | rollback decision and validation result (`staging:verify` / health checks). |

---

## 值班交接清单 | Shift Handover Checklist

| 中文 | English |
|------|---------|
| 活跃告警及负责人 | active alerts and owners |
| 缓解状态及下一检查点 | mitigation status and next checkpoint |
| 回滚状态及待发布决策 | rollback state and pending release decisions |
