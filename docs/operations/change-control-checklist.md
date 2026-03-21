# 变更控制清单 | Change Control Checklist

---

## 发布前 | Before Release

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 将变更关联到工单/功能及负责人 | Link change to ticket/feature and owner. |
| 2 | 确认受影响的 API/路由及回滚路径 | Confirm impacted API/routes and rollback path. |
| 3 | 完成质量门禁: `npm run lint`, `npm run build`, `npm run test:e2e`, `npm run test:backend`, `npm run test:full`, `npm run preflight:prod` | Complete gate: `npm run lint`, `npm run build`, `npm run test:e2e`, `npm run test:backend`, `npm run test:full`, `npm run preflight:prod`. |
| 4 | 如行为变更，确认运维手册引用已更新 | Confirm runbook references updated if behavior changed. |
| 5 | 准备发布快照 (`npm run release:snapshot`) | Prepare release snapshot (`npm run release:snapshot`). |

---

## 发布窗口期 | Release Window

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 通知值班人员及利益相关方频道 | Notify on-duty and stakeholder channel. |
| 2 | 部署 `apps/api` + `apps/web` 产物 | Deploy `apps/api` + `apps/web` artifacts. |
| 3 | 验证 `/api/health/ready` | Validate `/api/health/ready`. |
| 4 | 执行冒烟路径 (登录、仪表板、客户、报价) | Run smoke path (login, dashboard, customers, quotes). |
| 5 | 记录 requestId 样本用于可追溯性 | Record requestId samples for traceability. |

---

## 发布后 | After Release

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 观察 SLO 指标 30 分钟 | Observe SLO indicators for 30 minutes. |
| 2 | 检查审计导出指标及错误日志 | Check audit export metrics and error logs. |
| 3 | 标记发布完成或触发回滚 | Mark release completed or trigger rollback. |
