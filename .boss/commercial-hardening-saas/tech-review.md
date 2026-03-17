# Tech Review（收口版）

## 结论
当前代码具备商用化改造基础，主要风险不在业务逻辑，而在“样式冲突堆叠”和“安全策略遗漏”。本轮通过统一覆盖层与自动化闸门可控收口。

## 主要风险与处理
- 风险：多轮 CSS 覆盖导致视觉不一致。
  - 处理：追加单一“商业基线覆盖层”，统一 Topbar/Sidebar/Panel/Task/Table 规则。
- 风险：legacy 路径遗漏 tenant 条件。
  - 处理：控制器统一 `currentTenant`，仓储层 tenant-aware 查询。
- 风险：生产错误配置导致安全倒退。
  - 处理：`prod` fail-fast 守卫，弱配置直接拒绝启动。

## 质量门禁
- 必跑：`npm run lint`、`npm run build`、`npm run test:e2e`、`npm run test:backend`、`npm run test:full`。
- 通过后允许单次全量提交。
