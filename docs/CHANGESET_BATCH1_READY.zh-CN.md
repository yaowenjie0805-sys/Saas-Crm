# 批次 1 提交就绪清单

## 目标

统一 `README/README.en/README.zh-CN/docs` 范围内的文档口径，补齐和整理说明内容，不涉及业务代码。

## 范围

- `README.md`
- `README.en.md`
- `README.zh-CN.md`
- `docs/README.md`
- `docs/CHANGESET_BATCH1_READY.zh-CN.md`

## 排除项

- `apps/**`
- `scripts/**`
- 任何业务代码、测试代码、构建配置和 CI 配置

## 验证命令

```bash
git diff --check -- README.md docs/README.md docs/CHANGESET_BATCH1_READY.zh-CN.md
git status --short -- README.en.md README.zh-CN.md docs/CHANGESET_BATCH1_READY.zh-CN.md
```

## 风险

- 文档口径不一致时，容易让后续提交范围判断偏差。
- 如果只改了部分 README，而没有同步 `docs` 目录，可能会出现说明前后不一致。

## 回滚

- 直接回退本批次对 `README*` 和 `docs/README.md` 的文档改动。
- 如果已经拆分成多个提交，优先按文件粒度回滚，避免影响其他批次。

## 建议提交信息

- `docs: unify batch1 ready checklist`
