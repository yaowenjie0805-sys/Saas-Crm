# 批次 4 提交就绪清单

## 目标

聚焦前端运行时内核重构，把基础工具、hook 入口和 orchestrator 相关改动收敛成一个清晰边界，减少循环依赖、导入路径错误和运行时状态错配的风险。

## 范围

- `apps/web/src/crm/shared.js`
- `apps/web/src/crm/hooks/useApi.js`
- `apps/web/src/crm/hooks/useAppCrudActions.js`
- `apps/web/src/crm/hooks/useAppPageActions.js`
- `apps/web/src/crm/hooks/orchestrators/**`
- `apps/web/src/crm/hooks/orchestrators/runtime-kernel/**`
- `apps/web/src/crm/hooks/orchestrators/runtime/**`
- `apps/web/src/crm/hooks/**/index.js`

## 排除项

- `apps/api/**`
- `apps/web/src/crm/components/**`
- `apps/web/src/crm/components/pages/**`
- `apps/web/tests/**`
- `.github/workflows/**`
- `scripts/**`
- `package.json`
- `apps/web/package.json`
- `apps/web/src/crm/__tests__/**` 中与本次运行时内核无直接关系的测试
- 任何页面迁移、UI 重排、后端接口和纯测试补齐内容

## 验证命令

```bash
git diff --check -- docs/CHANGESET_BATCH4_READY.zh-CN.md
git diff --check -- apps/web/src/crm/shared.js apps/web/src/crm/hooks/useApi.js apps/web/src/crm/hooks/useAppCrudActions.js apps/web/src/crm/hooks/useAppPageActions.js apps/web/src/crm/hooks/orchestrators apps/web/src/crm/hooks/orchestrators/runtime-kernel apps/web/src/crm/hooks/orchestrators/runtime apps/web/src/crm/hooks
npm run check:runtime-kernel-imports
npm run test:frontend:unit -- --run apps/web/src/crm/__tests__/useApi.test.jsx apps/web/src/crm/__tests__/routeConfig.test.jsx
npm run lint
```

## 风险

- 这是整个前端的基础层，任何导入关系或公共工具的变化都可能向上放大到页面、路由和测试入口。
- 运行时内核改动最容易引入循环依赖、路径别名失配、重复导出或初始化顺序问题。
- 如果 `shared.js`、`useApi` 和 orchestrator 一起改，表面上可能都能编译，但实际运行时状态已经不一致。

## 回滚

- 若尚未推送，优先回退运行时内核相关文件，保持 `shared.js`、hook 入口和 orchestrator 的相互依赖关系一致。
- 若已经推送，优先通过反向提交回滚单个运行时内核改动，避免直接改历史记录。
- 若改动拆散到多个文件，建议按 `shared.js`、基础 hook、orchestrator 目录三类分别回滚，减少回退面。

## 建议提交信息

- `docs: unify batch4 ready checklist`
