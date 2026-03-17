# 任务拆解：Staging 真部署闭环 + 发布治理收口

## P0（本轮必须完成）
- 新增 `staging:deploy`：制品上传、远端 Compose 拉起、健康检查、部署证据输出。
- 新增 `staging:rollback`：回滚到上一可用版本并完成健康检查。
- 强化 `staging-release` 工作流：审批环境、门禁顺序、失败回滚建议、证据上传命名规范。
- `preflight:prod` 默认在主干发布分支要求 staging 证据（可通过环境变量覆盖）。
- 更新 `staging:release:summary`：补齐 `commit -> artifact -> target -> gate -> rollbackRecommendation -> operator/approval`.

## P1（本轮同时收口）
- 新增 `docs/operations/staging-deploy-runbook.md`，固化部署/回滚/证据流程。
- 更新 README 命令入口与证据路径。
- 更新 `.meta/execution.json`：`stagingDeployed` 与 `stagingRollbackReady` 标记为完成。

## QA Gate（串行）
`lint -> build -> test:e2e -> test:backend -> test:full -> perf:baseline -> perf:gate -> sre:daily-check -> security:scan -> preflight:prod`

## Staging 发布验收
- `staging:deploy` 成功且 health `live/ready/deps` 全部 `UP`。
- `staging:verify` 通过并生成 `logs/staging/staging-verify-latest.json`。
- 手动错误版本演练后 `staging:rollback` 可恢复，并再次通过 `staging:verify`。
