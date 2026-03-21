# 变更请求模板 | Change Request Template

---

## 变更摘要 | Change Summary

| 字段 | 中文 | English |
|------|------|---------|
| 变更标题 | Change title: | Change title: |
| 负责人 | Owner: | Owner: |
| 计划窗口 | Planned window: | Planned window: |
| 相关提交/PR | Related commit/PR: | Related commit/PR: |

---

## 风险与影响 | Risk and Impact

| 字段 | 中文 | English |
|------|------|---------|
| 影响范围 | Scope of impact: | Scope of impact: |
| 租户/用户影响 | Tenant/user impact: | Tenant/user impact: |
| 回滚触发阈值 | Rollback trigger threshold: | Rollback trigger threshold: |
| 回滚负责人 | Rollback owner: | Rollback owner: |

---

## 验证 | Verification

| 字段 | 中文 | English |
|------|------|---------|
| 必需门禁 | 必需门禁: `npm run ci:frontend`, `npm run ci:backend`, `npm run test:full`, `npm run perf:gate`, `npm run sre:daily-check`, `npm run sre:alert-check`, `npm run security:scan`, `npm run preflight:prod` | Required gates: `npm run ci:frontend`, `npm run ci:backend`, `npm run test:full`, `npm run perf:gate`, `npm run sre:daily-check`, `npm run sre:alert-check`, `npm run security:scan`, `npm run preflight:prod` |
| 预发环境证据包 | Staging evidence bundle: | Staging evidence bundle: |
| 发布后检查 | Post-release checks: | Post-release checks: |

---

## 审批 | Approval

| 角色 | 中文 | English |
|------|------|---------|
| 工程审批人 | Engineering approver: | Engineering approver: |
| 产品审批人 | Product approver: | Product approver: |
| 运维/值班审批人 | Ops/on-call approver: | Ops/on-call approver: |
