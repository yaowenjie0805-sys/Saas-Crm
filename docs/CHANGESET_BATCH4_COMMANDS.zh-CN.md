# 批次4提交命令草案

> 适用范围：仅限批次4（前端运行时内核）文件。  
> 目标：给出可直接复制执行的 `git add`、`git commit`、验证命令，避免混入其他批次改动。

## 1. 建议的 `git add`

```bash
git add apps/web/src/crm/shared.js \
        apps/web/src/crm/hooks/useApi.js \
        apps/web/src/crm/hooks/useAppCrudActions.js \
        apps/web/src/crm/hooks/useAppPageActions.js \
        apps/web/src/crm/hooks/orchestrators \
        apps/web/src/crm/hooks/orchestrators/runtime-kernel \
        apps/web/src/crm/hooks/orchestrators/runtime
```

## 2. 建议的 `git commit`

```bash
git commit -m "refactor(runtime): 批次4 运行时内核重构收口"
```

可选：

```bash
git commit -m "refactor(runtime-kernel): batch4 内核入口与依赖整理"
```

## 3. 提交前验证命令

```bash
git status --short
git diff --check -- apps/web/src/crm/shared.js \
                     apps/web/src/crm/hooks/useApi.js \
                     apps/web/src/crm/hooks/useAppCrudActions.js \
                     apps/web/src/crm/hooks/useAppPageActions.js \
                     apps/web/src/crm/hooks/orchestrators \
                     apps/web/src/crm/hooks/orchestrators/runtime-kernel \
                     apps/web/src/crm/hooks/orchestrators/runtime
npm run check:runtime-kernel-imports
npm run test:frontend:unit -- --run apps/web/src/crm/__tests__/useApi.test.jsx apps/web/src/crm/__tests__/routeConfig.test.jsx
npm run lint --workspace apps/web
git diff --cached --name-only
```

## 4. 提交后验证命令

```bash
git show --stat --oneline --name-only HEAD
git status --short
```

## 5. 注意事项

- 批次4不要混入 `apps/web/src/crm/components/**`、`apps/api/**`、`apps/web/tests/**` 与 `docs/**` 变更。
- 若暂存区混入其他文件，先执行 `git restore --staged <file>` 再重新 `git add`。
- 若只想演练流程，可先执行验证命令，不必立刻提交。

