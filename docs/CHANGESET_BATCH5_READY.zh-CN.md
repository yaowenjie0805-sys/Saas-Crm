# 批次 5 提交就绪清单

## 目标

聚焦页面组件与页面入口迁移，把 `MainContent`、`MainContentPanels` 与 `pages` 目录的导出/入口改动收敛成清晰边界，降低路由、面板懒加载和页面装配错配风险。

## 范围

- `apps/web/src/crm/components/MainContent.jsx`
- `apps/web/src/crm/components/MainContentPanels.jsx`
- `apps/web/src/crm/components/ServerPager.jsx`
- `apps/web/src/crm/components/pages/**`
- `apps/web/src/crm/components/pages/*/index.js`
- `apps/web/src/crm/components/pages/*/sections.js`
- 各页面目录下 `sections/*.jsx`

## 排除项

- `apps/api/**`
- `apps/web/src/crm/hooks/**`
- `apps/web/src/crm/shared.js`
- `apps/web/tests/**`
- `apps/web/src/crm/__tests__/**`（仅为页面迁移直接新增/修复的测试除外）
- `.github/workflows/**`
- `scripts/**`
- `package.json`
- `apps/web/package.json`
- 任何后端接口、运行时内核与纯测试收口批次内容

## 验证命令

```bash
git diff --check -- docs/CHANGESET_BATCH5_READY.zh-CN.md
git diff --check -- apps/web/src/crm/components/MainContent.jsx apps/web/src/crm/components/MainContentPanels.jsx apps/web/src/crm/components/ServerPager.jsx apps/web/src/crm/components/pages
npm run lint --workspace apps/web
npm run test --workspace apps/web -- --run src/crm/__tests__/pagesBarrelExports.test.js src/crm/__tests__/routeConfig.test.js
npm run test:e2e:runner -- --grep "dashboard|leads|quotes|tenants"
```

## 风险

- 页面入口、懒加载映射和 `pages/*` barrel 是串联关系，轻微导出错误会放大为整页白屏或“组件未挂载”问题。
- `sections.js` 与 `index.js` 改动若不同步，容易出现“测试通过但运行路径缺失”的隐性问题。
- 移动端和窄屏布局依赖页面组合结构，目录迁移时容易引发布局回归。

## 回滚

- 若未推送，优先按页面域分组回退（例如 `dashboard`、`leads`、`quotes`），避免一次性全量回退。
- 若已推送，使用 `git revert <commit_sha>` 反向提交，不改共享历史。
- 建议先回滚 `MainContentPanels` 映射，再回滚对应 `pages/*` 目录导出，降低依赖断裂风险。

## 建议提交信息

- `refactor(pages): batch5 页面组件与入口迁移收口`

