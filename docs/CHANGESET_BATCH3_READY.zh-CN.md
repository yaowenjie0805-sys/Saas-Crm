# 批次 3 提交就绪清单

## 目标

聚焦后端 API 与服务层的行为变更，把接口、服务和对应测试拆成一个清晰边界，便于回归验证和独立提交。

## 范围

- `apps/api/src/main/java/com/yao/crm/controller/*.java`
- `apps/api/src/main/java/com/yao/crm/service/*.java`
- `apps/api/src/main/java/com/yao/crm/service/impl/*.java`
- `apps/api/src/main/java/com/yao/crm/dto/*.java`
- `apps/api/src/main/java/com/yao/crm/dto/request/*.java`
- `apps/api/src/main/java/com/yao/crm/exception/*.java`
- `apps/api/src/test/java/com/yao/crm/controller/*.java`
- `apps/api/src/test/java/com/yao/crm/service/*.java`
- `apps/api/src/test/java/com/yao/crm/**/*.java` 中与本次接口或服务行为直接相关的测试

## 排除项

- `apps/web/src/**`
- `apps/web/tests/**`
- `.github/workflows/**`
- `scripts/**`
- `package.json`
- `apps/web/package.json`
- `apps/api/src/main/resources/**`
- `apps/api/src/main/java/com/yao/crm/config/**`
- `apps/api/src/main/java/com/yao/crm/security/**`
- `apps/api/src/main/java/com/yao/crm/repository/**`
- `apps/api/src/main/java/com/yao/crm/entity/**`
- `docs/**`

## 验证命令

```bash
git diff --check -- docs/CHANGESET_BATCH3_READY.zh-CN.md
git diff --check -- apps/api/src/main/java/com/yao/crm/controller apps/api/src/main/java/com/yao/crm/service apps/api/src/main/java/com/yao/crm/dto apps/api/src/test/java/com/yao/crm
git status --short
npm run test:backend
npm run test:api
```

## 风险

- API 返回字段、错误码、分页字段或排序语义变化后，前端和外部调用方都可能受到影响。
- 服务层行为变化通常会牵动状态流转、权限判断、审计日志、异步任务或数据聚合逻辑，回归风险高于纯文档或纯配置改动。
- 如果同时改了 controller 和 service，最容易出现“接口看起来没变，但内部行为已经变了”的情况，必须补足针对性测试。

## 回滚

- 若尚未推送，优先回退接口边界改动，保持 controller / DTO / test 的一致性。
- 若已推送，优先通过反向提交回滚单个后端改动，避免直接改历史记录。
- 若已经联动前端或其他后端分支，先确认调用方是否也需要同步回退，避免下游继续报错。

## 建议提交信息

- `docs: unify batch3 ready checklist`
