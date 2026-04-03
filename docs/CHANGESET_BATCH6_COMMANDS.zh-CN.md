# 批次6提交命令草案

> 适用范围：仅限批次6（测试补齐与回归收口）文件。  
> 目标：提供可直接复制执行的测试批次提交命令，避免混入业务代码改动。

## 1. 建议的 `git add`

```bash
git add apps/web/src/crm/__tests__ \
        apps/web/tests/e2e \
        apps/api/src/test/java/com/yao/crm
```

## 2. 建议的 `git commit`

```bash
git commit -m "test(batch6): 测试补齐与回归收口"
```

可选：

```bash
git commit -m "test(regression): batch6 前后端测试用例收口"
```

## 3. 提交前验证命令

```bash
git status --short
git diff --check -- apps/web/src/crm/__tests__ apps/web/tests/e2e apps/api/src/test/java/com/yao/crm
npm run test:frontend:unit
npm run test:e2e
npm run test:backend:unit
git diff --cached --name-only
```

## 4. 提交后验证命令

```bash
git show --stat --oneline --name-only HEAD
git status --short
```

## 5. 注意事项

- 不要混入 `apps/web/src/crm/components/**`、`apps/web/src/crm/hooks/**`、`apps/api/src/main/**` 与 `docs/**` 的改动。
- 若暂存区混入其他文件，先 `git restore --staged <file>` 再重新按范围 `git add`。
- E2E 失败先定位是环境问题还是断言问题，不建议为过测扩大忽略规则。

