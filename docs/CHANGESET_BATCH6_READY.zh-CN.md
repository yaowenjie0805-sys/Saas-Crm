# 批次 6 提交就绪清单

## 目标

聚焦测试补齐与回归收口，将单测、E2E 与后端测试调整独立成批，确保测试改动不混入主体业务逻辑提交，降低排障成本。

## 范围

- `apps/web/src/crm/__tests__/*.test.jsx`
- `apps/web/src/crm/__tests__/*.test.js`
- `apps/web/tests/e2e/*.spec.js`
- `apps/web/tests/e2e/helpers/*.js`
- `apps/api/src/test/java/com/yao/crm/**/*.java`

## 排除项

- `apps/api/src/main/**`
- `apps/web/src/crm/components/**`
- `apps/web/src/crm/hooks/**`
- `apps/web/src/crm/shared.js`
- `.github/workflows/**`
- `scripts/**`
- `package.json`
- `apps/web/package.json`
- `apps/web/src/main/**` 与测试无关的实现代码改动

## 验证命令

```bash
git diff --check -- docs/CHANGESET_BATCH6_READY.zh-CN.md
npm run test:frontend:unit
npm run test --workspace apps/web -- --run src/crm/__tests__/useApi.test.jsx src/crm/__tests__/pagesBarrelExports.test.js src/crm/__tests__/routeConfig.test.js
npm run test:e2e
npm run test:backend:unit
```

## 风险

- 测试断言调整容易把“旧行为变化”误判成“修复”，导致回归风险被掩盖。
- E2E 中登录、网络兜底和噪音过滤规则若改动过宽，可能掩盖真实线上异常。
- 后端测试和前端测试同时改动时，若没有分层验证，很难快速定位失败归因。

## 回滚

- 未推送优先 `git reset --soft HEAD~1`，保留测试改动重新拆分。
- 已推送优先 `git revert <commit_sha>`，避免改写共享历史。
- 建议按 `web unit -> e2e -> api test` 三层回滚，保留可工作的最小验证链路。

## 建议提交信息

- `test(batch6): 测试补齐与回归收口`

