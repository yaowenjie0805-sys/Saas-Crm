# 发布策略 | Release Strategy

---

## 产物策略 | Artifact Strategy

| 中文 | English |
|------|---------|
| 前端产物: `apps/web/dist/assets/*` | Frontend artifact: `apps/web/dist/assets/*`. |
| 后端产物: `apps/api/target/crm-backend-1.0.0.jar` | Backend artifact: `apps/api/target/crm-backend-1.0.0.jar`. |
| 快照映射: `npm run release:snapshot` 写入 commit -> 产物 -> 配置摘要 | Snapshot mapping: `npm run release:snapshot` writes commit -> artifact -> config summary. |

---

## 版本管理 | Versioning

| 中文 | English |
|------|---------|
| 每个发布候选使用语义化提交和标签 | Use semantic commit and tag per release candidate. |
| 将回滚目标保持为带有匹配快照文件的最后一个稳定标签 | Keep rollback target as last stable tag with matching snapshot file. |

---

## 回滚策略 | Rollback Strategy

| 项目 | 中文 | English |
|------|------|---------|
| 触发条件 | SLO 违规、认证回归、租户隔离回归或就绪检查失败 | Trigger: SLO breach, auth regression, tenant isolation regression, or failed readiness. |
| 回滚操作 | 回滚到最后稳定的产物对 (前端 + 后端) 并恢复配置快照 | Rollback to last stable artifact pair (frontend + backend) and restore config snapshot. |
| 验证 | 使用 `/api/health/ready` + 冒烟路径验证 | Validate with `/api/health/ready` + smoke path. |

---

## 证据保留 | Evidence

| 中文 | English |
|------|---------|
| 保留发布清单、快照 JSON 和发布后验证日志用于审计 | Keep release checklist, snapshot JSON, and post-release verification logs for audit. |
