# 批次1可执行提交流程说明

> 适用范围：仅 `docs/` 范围内的变更，不包含业务代码、配置或依赖文件。
> 目标：把“可提交”的动作拆成一套可执行、可回滚、可验证的提交流程，便于后续按批次整理变更。

## 1. 建议 `git add` 路径

优先只加入本批次明确相关的文档文件，避免把其他未完成的文档一起带进去。

```bash
git add docs/CHANGESET_PLAN.zh-CN.md \
        docs/CHANGESET_BATCH1_READY.zh-CN.md \
        docs/CHANGESET_BATCH1_COMMIT_STEPS.zh-CN.md \
        docs/README.md \
        docs/PROJECT_FLOW_MAP.md \
        docs/PROJECT_STRUCTURE.md
```

如果本批次实际只涉及其中一部分文档，可以进一步收窄为：

```bash
git add docs/<具体文件1> docs/<具体文件2>
```

## 2. 提交信息模板（中文）

建议采用“类型 + 范围 + 批次 + 说明”的格式，保持后续追溯清晰。

### 模板 A

```text
docs: 批次1 - <一句话说明本次文档整理内容>
```

### 模板 B

```text
docs(changeset): 批次1 提交流程说明与文档整理
```

### 模板 C

```text
docs: 完成批次1 文档清单与提交验证指引
```

建议正文补充两点：

```text
变更范围：仅 docs/
验证结果：已通过 git diff --check / git status 检查
```

## 3. 提交前验证命令

提交前先确认工作区只包含预期的 docs 改动，并且没有格式问题。

```bash
git status --short
git diff --check -- docs
git diff -- docs
```

如果希望更严格一点，可以再核对拟提交文件列表：

```bash
git diff --cached --name-only
```

在执行 `git commit` 之前，建议确认下面三件事：

1. 只有 `docs/` 相关文件进入暂存区。
2. `git diff --check` 没有空格、换行或补丁格式错误。
3. 文档内容已经覆盖“建议 `git add` 路径、提交信息模板、提交前后验证命令、回滚建议”四项要求。

## 4. 提交后验证命令

提交完成后，立即确认提交内容、影响范围和提交摘要。

```bash
git show --stat --oneline --name-only HEAD
git status --short
```

如果需要核对最近一次提交的完整差异，可以执行：

```bash
git show HEAD
```

如果后续还要继续分批整理，可以再确认最近提交是否只包含 docs：

```bash
git log -1 --stat --name-only
```

## 5. 回滚建议

回滚时优先按“是否已经提交、是否已经推送”来选择命令。

### 情况 A：只想撤销暂存区

```bash
git restore --staged docs/<文件名>
```

### 情况 B：想保留修改但取消本地提交

```bash
git reset --soft HEAD~1
```

### 情况 C：已经提交，但还没推送

如果需要重新整理提交内容，可以先软回退，再重新 `git add`：

```bash
git reset --soft HEAD~1
git status --short
```

### 情况 D：已经推送到共享分支

优先使用反向提交，避免改写共享历史：

```bash
git revert <commit_sha>
```

## 6. 推荐执行顺序

```bash
git status --short
git add docs/<本批次文件>
git diff --cached --check
git diff --cached --name-only
git commit -m "docs(changeset): 批次1 提交流程说明与文档整理"
git show --stat --oneline --name-only HEAD
```

## 7. 备注

- 本文件只描述提交流程，不代表已经执行提交。
- 本批次若新增或调整 docs 文件，优先更新这里的 `git add` 路径示例。
- 如果后续批次改成包含非 docs 文件，应另起文档，不要混用当前流程说明。
