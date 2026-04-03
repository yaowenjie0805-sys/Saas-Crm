# 批次4可执行提交流程说明

> 适用范围：仅 `batch4`（前端运行时内核）相关文件，不包含页面组件、后端、e2e 与无关脚本。  
> 目标：把运行时内核改动拆成可执行、可验证、可回滚的提交流程，降低大改动一次性提交风险。

## 1. 建议 `git add` 路径

按批次4边界只暂存运行时内核相关文件。

```bash
git add apps/web/src/crm/shared.js \
        apps/web/src/crm/hooks/useApi.js \
        apps/web/src/crm/hooks/useAppCrudActions.js \
        apps/web/src/crm/hooks/useAppPageActions.js \
        apps/web/src/crm/hooks/orchestrators \
        apps/web/src/crm/hooks/orchestrators/runtime-kernel \
        apps/web/src/crm/hooks/orchestrators/runtime
```

如果本次只涉及其中一部分，可以按文件/目录进一步收窄。

## 2. 提交信息模板（中文）

推荐使用“类型 + 范围 + 批次 + 摘要”的格式。

```text
refactor(runtime): 批次4 运行时内核重构收口
```

可选版本：

```text
refactor(runtime-kernel): batch4 内核入口与依赖整理
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

如需完整检查提交差异：

```bash
git show HEAD
```

## 5. 回滚建议

- 仅撤销暂存：`git restore --staged <file>`
- 保留改动取消本地提交：`git reset --soft HEAD~1`
- 已推送分支优先反向提交：`git revert <commit_sha>`
- 建议按 `shared.js -> 基础 hooks -> orchestrators` 顺序分层回滚，避免依赖断裂。

## 6. 推荐执行顺序

```bash
git status --short
git add <batch4 files>
npm run check:runtime-kernel-imports
npm run test:frontend:unit -- --run apps/web/src/crm/__tests__/useApi.test.jsx apps/web/src/crm/__tests__/routeConfig.test.jsx
npm run lint --workspace apps/web
git diff --cached --name-only
git commit -m "refactor(runtime): 批次4 运行时内核重构收口"
git show --stat --oneline --name-only HEAD
```

