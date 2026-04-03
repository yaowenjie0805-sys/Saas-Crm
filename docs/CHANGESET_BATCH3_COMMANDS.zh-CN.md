# 批次3提交命令草案

> 适用范围：仅限批次3（后端 API 与服务行为调整）文件。  
> 目标：提供可直接执行命令，确保后端接口调整独立提交。

## 1. 建议的 `git add`

```bash
git add apps/api/src/main/java/com/yao/crm/controller \
        apps/api/src/main/java/com/yao/crm/service/DataImportExportService.java \
        apps/api/src/test/java/com/yao/crm
```

## 2. 建议的 `git commit`

```bash
git commit -m "feat(batch3): 后端API与服务行为调整"
```

可选：

```bash
git commit -m "refactor(api): batch3 controller与service收口"
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

## 5. 注意事项

- 不要混入 `apps/web/**` 和 `docs/**` 的改动。
- 若本次涉及接口契约变化，提交后应同步触发前端 API 相关回归。

