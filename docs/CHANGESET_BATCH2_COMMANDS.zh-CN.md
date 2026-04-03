# 批次2提交命令草案

> 适用范围：仅限批次2（CI/脚本/配置）文件。  
> 目标：提供可直接执行的命令，避免混入业务改动。

## 1. 建议的 `git add`

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

## 2. 建议的 `git commit`

```bash
git commit -m "chore(batch2): CI与脚本配置收口"
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

## 5. 注意事项

- 不要混入 `apps/api/src/main/**`、`apps/web/src/crm/**` 与 `docs/**` 的无关改动。
- workflow 与脚本建议同批次提交，避免配置与执行入口版本不一致。

