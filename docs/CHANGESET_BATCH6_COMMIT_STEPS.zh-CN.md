# 批次6可执行提交流程说明

> 适用范围：仅 `batch6`（测试补齐与回归收口）相关文件。  
> 目标：把测试改动作为独立批次提交，做到可验证、可追溯、可回滚。

## 1. 建议 `git add` 路径

```bash
git add apps/web/src/crm/__tests__ \
        apps/web/tests/e2e \
        apps/api/src/test/java/com/yao/crm
```

若本次只涉及部分测试文件，请按文件名进一步收窄。

## 2. 提交信息模板（中文）

```text
test(batch6): 测试补齐与回归收口
```

可选：

```text
test(regression): batch6 前后端测试用例收口
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

## 5. 回滚建议

- 撤销暂存：`git restore --staged <file>`
- 保留改动取消本地提交：`git reset --soft HEAD~1`
- 已推送分支使用：`git revert <commit_sha>`
- 建议按 `unit -> e2e -> backend` 顺序分层回滚。

## 6. 推荐执行顺序

```bash
git status --short
git add <batch6 files>
npm run test:frontend:unit
npm run test:e2e
npm run test:backend:unit
git diff --cached --name-only
git commit -m "test(batch6): 测试补齐与回归收口"
git show --stat --oneline --name-only HEAD
```

