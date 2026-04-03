# 批次1提交命令草案

> 适用范围：仅限 `docs/` 下的批次1文档文件。  
> 目标：给出一套可以直接复制执行的 `git add`、`git commit`、验证命令，避免把其他批次、代码文件或无关文档一起带入提交。

## 1. 建议的 `git add`

先只暂存本批次相关文档，确保提交边界清晰。

```bash
git add docs/CHANGESET_BATCH1_READY.zh-CN.md \
        docs/CHANGESET_BATCH1_COMMIT_STEPS.zh-CN.md \
        docs/CHANGESET_BATCH1_COMMANDS.zh-CN.md
```

如果你当前工作区里还有其他 `docs/` 文件改动，但它们不属于批次1，这里不要一起加进来。

## 2. 建议的 `git commit`

提交信息建议保持简短，并直接说明是批次1的文档整理。

```bash
git commit -m "docs: batch1 提交命令草案"
```

如果你更偏好更明确的约定式前缀，也可以用下面这个版本：

```bash
git commit -m "docs(batch1): 提交命令草案"
```

## 3. 提交前验证命令

在执行 `git commit` 之前，建议先确认暂存内容和格式都只落在批次1文档上。

```bash
git status --short -- docs
git diff --check -- docs/CHANGESET_BATCH1_READY.zh-CN.md \
                     docs/CHANGESET_BATCH1_COMMIT_STEPS.zh-CN.md \
                     docs/CHANGESET_BATCH1_COMMANDS.zh-CN.md
git diff --cached --name-only
git diff --cached -- docs/CHANGESET_BATCH1_READY.zh-CN.md \
                     docs/CHANGESET_BATCH1_COMMIT_STEPS.zh-CN.md \
                     docs/CHANGESET_BATCH1_COMMANDS.zh-CN.md
```

如果你想更严格一点，可以把最后一条替换成下面这个，直接查看暂存差异内容：

```bash
git diff --cached -- docs/CHANGESET_BATCH1_READY.zh-CN.md \
                     docs/CHANGESET_BATCH1_COMMIT_STEPS.zh-CN.md \
                     docs/CHANGESET_BATCH1_COMMANDS.zh-CN.md
```

## 4. 提交后验证命令

提交完成后，建议立即确认提交只包含预期的批次1文档。

```bash
git show --stat --oneline --name-only HEAD
git status --short
```

如果需要进一步核对提交内容，也可以查看完整提交详情：

```bash
git show HEAD
```

## 5. 注意事项

- 这份命令草案只面向 `docs/CHANGESET_BATCH1_*.zh-CN.md` 这类批次1文档，不要把 `README`、代码、配置或其他批次文档一起混进来。
- 如果你已经暂存了别的文件，先用 `git restore --staged <文件>` 退回，再重新执行这里的 `git add`。
- 如果 `git diff --check` 报错，先修正文档里的空格、换行或补丁格式问题，再提交。
- 如果你当前只是想“演练命令”，可以先执行验证命令，不必立刻运行 `git commit`。

