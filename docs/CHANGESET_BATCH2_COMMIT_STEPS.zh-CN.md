# 批次2可执行提交流程说明

> 适用范围：仅 `batch2`（CI/脚本/配置）相关文件。  
> 目标：把工具链与门禁相关改动独立提交，降低“看起来没问题但跑不起来”的风险。

## 1. 建议 `git add` 路径

```bash
git add .github/workflows \
        package.json \
        apps/web/package.json \
        apps/web/eslint.config.js \
        apps/web/vite.config.js \
        apps/web/vitest.config.js \
        apps/web/playwright.config.js \
        apps/web/scripts/run-playwright-e2e.mjs \
        scripts
```

## 2. 提交信息模板（中文）

```text
chore(batch2): CI与脚本配置收口
```

可选：

```text
chore(toolchain): batch2 门禁与运行脚本整理
```

## 3. 提交前验证命令

```bash
git status --short
git diff --check -- .github/workflows package.json apps/web/package.json apps/web/eslint.config.js apps/web/vite.config.js apps/web/vitest.config.js apps/web/playwright.config.js apps/web/scripts/run-playwright-e2e.mjs scripts
npm run lint --workspace apps/web
npm run test:frontend:unit
npm run test:backend:unit
git diff --cached --name-only
```

## 4. 提交后验证命令

```bash
git show --stat --oneline --name-only HEAD
git status --short
```

## 5. 回滚建议

- 撤销暂存：`git restore --staged <file>`
- 保留改动取消本地提交：`git reset --soft HEAD~1`
- 已推送分支使用：`git revert <commit_sha>`
- 建议先回滚 workflow，再回滚脚本与配置。

