# 批次3可执行提交流程说明

> 适用范围：仅 `batch3`（后端 API 与服务行为调整）相关文件。  
> 目标：把后端 controller/service 与其测试改动独立提交，便于接口回归定位。

## 1. 建议 `git add` 路径

```bash
git add apps/api/src/main/java/com/yao/crm/controller \
        apps/api/src/main/java/com/yao/crm/service/DataImportExportService.java \
        apps/api/src/test/java/com/yao/crm
```

## 2. 提交信息模板（中文）

```text
feat(batch3): 后端API与服务行为调整
```

可选：

```text
refactor(api): batch3 controller与service收口
```

## 3. 提交前验证命令

```bash
git status --short
git diff --check -- apps/api/src/main/java/com/yao/crm/controller apps/api/src/main/java/com/yao/crm/service/DataImportExportService.java apps/api/src/test/java/com/yao/crm
npm run test:backend
npm run test:api
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
- 先回滚 API 主体，再回滚测试，避免临时不一致。

