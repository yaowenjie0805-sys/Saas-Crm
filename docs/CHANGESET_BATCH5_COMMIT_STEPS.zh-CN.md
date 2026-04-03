# 批次5可执行提交流程说明

> 适用范围：仅 `batch5`（页面组件与页面入口迁移）相关文件。  
> 目标：将页面目录迁移改动按可执行、可验证、可回滚流程提交，减少路由与导出层回归风险。

## 1. 建议 `git add` 路径

```bash
git add apps/web/src/crm/components/MainContent.jsx \
        apps/web/src/crm/components/MainContentPanels.jsx \
        apps/web/src/crm/components/ServerPager.jsx \
        apps/web/src/crm/components/pages
```

如本次只涉及部分页面域（如 `leads`、`quotes`），请按子目录收窄暂存范围。

## 2. 提交信息模板（中文）

```text
refactor(pages): 批次5 页面组件与入口迁移收口
```

可选版本：

```text
refactor(ui-pages): batch5 页面入口与导出结构整理
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

如需完整核对：

```bash
git show HEAD
```

## 5. 回滚建议

- 仅撤销暂存：`git restore --staged <file>`
- 保留改动取消本地提交：`git reset --soft HEAD~1`
- 已推送优先反向提交：`git revert <commit_sha>`
- 建议按“入口映射 -> 页面域目录 -> section 组件”顺序分层回滚。

## 6. 推荐执行顺序

```bash
git status --short
git add <batch5 files>
npm run lint --workspace apps/web
npm run test --workspace apps/web -- --run src/crm/__tests__/pagesBarrelExports.test.js src/crm/__tests__/routeConfig.test.js
npm run test:e2e:runner -- --grep "dashboard|leads|quotes|tenants"
git diff --cached --name-only
git commit -m "refactor(pages): 批次5 页面组件与入口迁移收口"
git show --stat --oneline --name-only HEAD
```

