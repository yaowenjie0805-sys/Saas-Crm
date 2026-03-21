# 发布/回滚手册 | Release / Rollback Runbook

---

## 适用范围 | Scope

| 中文 | English |
|------|---------|
| 预发/生产环境的部署和回滚运维手册 | Operational runbook for deployment and rollback in staging/production. |

---

## 发布前置条件 | Release Preconditions

| 中文 | English |
|------|---------|
| CI 门禁通过 | CI gates pass. |
| 变更请求已批准 | Change request approved. |
| 回滚产物和流程已准备 | Rollback artifact and procedure prepared. |
| 值班人员和利益相关方已通知 | On-call and stakeholders notified. |

---

## 发布步骤 | Release Steps

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 确认目标版本和产物校验和 | Confirm target version and artifact checksums. |
| 2 | 部署到目标环境 | Deploy to target environment. |
| 3 | 运行部署后检查: 健康端点、API 冒烟测试、关键页面冒烟检查 | Run post-deploy checks: health endpoints, API smoke tests, key page smoke checks |
| 4 | 观察指标和告警至少 15 分钟 | Observe metrics and alerts for at least 15 minutes. |

---

## 回滚触发条件 | Rollback Triggers

| 中文 | English |
|------|---------|
| P1 中断或重复 P2 降级 | P1 outage or repeated P2 degradation. |
| 核心 API 错误率超过阈值 | Core API error rate over threshold. |
| 数据正确性风险已确认 | Data correctness risk confirmed. |

---

## 回滚步骤 | Rollback Steps

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 停止当前发布并冻结新变更 | Stop current rollout and freeze new changes. |
| 2 | 部署之前稳定的后端产物 | Deploy previous stable backend artifact. |
| 3 | 如需要，回滚前端产物 | Roll back frontend artifact if needed. |
| 4 | 重新运行健康和冒烟检查 | Re-run health and smoke checks. |
| 5 | 宣布回滚完成 | Announce rollback completion. |

---

## 回滚后 | Post-Rollback

| 中文 | English |
|------|---------|
| 开启事故报告 | Open incident report. |
| 记录根因和纠正措施 | Document root cause and corrective actions. |
| 下次发布前更新发布清单 | Update release checklist before next rollout. |
