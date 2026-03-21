# SRE SLO 基线 | SRE SLO Baseline

---

## 目标 | Objective

| 中文 | English |
|------|---------|
| 定义核心 CRM 可用性和延迟的基线 SLO | Define baseline SLOs for core CRM availability and latency. |

---

## 服务等级目标 | Service Level Objectives

| 指标 | 中文 | English |
|------|------|---------|
| API 可用性 | >= 99.9% 月度 | API availability: >= 99.9% monthly. |
| P95 延迟 - `/api/dashboard` | <= 400ms | `/api/dashboard` <= 400ms |
| P95 延迟 - `/api/customers/search` | <= 500ms | `/api/customers/search` <= 500ms |
| P95 延迟 - `/api/v1/reports/overview` | <= 700ms | `/api/v1/reports/overview` <= 700ms |
| 错误率 | <= 1% 持续 10 分钟 | Error rate: <= 1% sustained over 10 minutes. |

---

## 告警阈值 | Alert Thresholds

| 级别 | 中文 | English |
|------|------|---------|
| P1 | 持续中断或严重 API 故障 | sustained outage or severe API failure. |
| P2 | 影响关键工作流的主要降级 | major degradation affecting critical workflows. |
| P3 | 需要跟进的趋势警告 | trend warning requiring follow-up. |

---

## 测量窗口 | Measurement Windows

| 窗口 | 中文 | English |
|------|------|---------|
| 实时检查 | 1分钟/5分钟 窗口 | Real-time checks: 1m/5m windows. |
| 每日审核 | 前 24 小时 | Daily review: previous 24h. |
| 每周审核 | 趋势 + 错误预算燃烧 | Weekly review: trend + error budget burn. |

---

## 运维操作 | Operational Actions

| 中文 | English |
|------|---------|
| 如 SLO 燃烧率高，冻结非关键发布 | If SLO burn rate is high, freeze non-critical releases. |
| 优先处理可靠性修复而非功能发布 | Prioritize reliability fixes over feature rollout. |
| 记录事故时间线和缓解措施 | Document incident timeline and mitigation. |

---

## 报告 | Reporting

| 中文 | English |
|------|---------|
| 使用值班周审模板并在发布审核中发布摘要 | Use weekly on-call template and publish summary in release review. |
