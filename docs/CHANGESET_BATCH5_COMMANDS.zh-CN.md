# 批次5提交命令草案

> 适用范围：仅限批次5（页面组件与页面入口迁移）文件。  
> 目标：提供可直接复制执行的提交命令，避免混入其他批次改动。

## 1. 建议的 `git add`

```bash
git add apps/web/src/crm/components/MainContent.jsx \
        apps/web/src/crm/components/MainContentPanels.jsx \
        apps/web/src/crm/components/ServerPager.jsx \
        apps/web/src/crm/components/pages
```

## 2. 建议的 `git commit`

```bash
git commit -m "refactor(pages): 批次5 页面组件与入口迁移收口"
```

可选：

```bash
git commit -m "refactor(ui-pages): batch5 页面入口与导出结构整理"
```

## 3. 提交前验证命令

```bash
git status --short
git diff --check -- apps/web/src/crm/components/MainContent.jsx \
                     apps/web/src/crm/components/MainContentPanels.jsx \
                     apps/web/src/crm/components/ServerPager.jsx \
                     apps/web/src/crm/components/pages
npm run lint --workspace apps/web
npm run test --workspace apps/web -- --run src/crm/__tests__/pagesBarrelExports.test.js src/crm/__tests__/routeConfig.test.js
npm run test:e2e:runner -- --grep "dashboard|leads|quotes|tenants"
git diff --cached --name-only
```

## 4. 提交后验证命令

```bash
git show --stat --oneline --name-only HEAD
git status --short
```

## 5. 注意事项

- 不要混入 `apps/web/src/crm/hooks/**`、`apps/api/**`、`docs/**` 与 `apps/web/tests/**` 的非批次5改动。
- 暂存区若混入其他文件，先执行 `git restore --staged <file>` 后再按批次5范围重新 `git add`。
- 若只做流程演练，可先跑验证命令，不必立刻提交。

