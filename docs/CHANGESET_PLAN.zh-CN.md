# 可分批提交清单

> 目标：基于当前工作区的变更，给出一个更稳妥的提交拆分方案。  
> 原则：尽量让每个批次“改动目标单一、回归范围可控、验证命令明确”。  
> 说明：下表中的文件范围是“建议归类”，不是硬性唯一归属；如果某些文件已经被别的批次强依赖，优先按依赖关系调整。

## 推荐提交顺序

| 批次 | 建议目标 | 涉及文件范围 | 主要风险 | 建议验证命令 |
|---|---|---|---|---|
| 1 | 仓库说明与文档同步 | `README.md`、`README.en.md`、`README.zh-CN.md`、`docs/*.md` | 风险最低，但要避免文档描述与实际脚本/目录不一致 | `git diff --check` |
| 2 | CI / 脚本 / 运行时配置收口 | `.github/workflows/*.yml`、`package.json`、`apps/web/package.json`、`apps/web/eslint.config.js`、`apps/web/vite.config.js`、`apps/web/vitest.config.js`、`apps/web/playwright.config.js`、`scripts/*.mjs`、`apps/web/scripts/run-playwright-e2e.mjs`、`scripts/local-e2e.mjs`、`scripts/api-smoke-test.mjs` | 可能影响本地、PR 和主干门禁；配置类改动最容易“看起来没问题，实际跑不起来” | `npm run lint`、`npm run test:frontend:unit`、`npm run test:backend:unit` |
| 3 | 后端 API 与服务行为调整 | `apps/api/src/main/java/com/yao/crm/controller/*.java`、`apps/api/src/main/java/com/yao/crm/service/DataImportExportService.java`、`apps/api/src/test/java/com/yao/crm/**/*.java` | 后端接口或导出逻辑变化会直接影响前端和回归测试，建议单独提交便于定位 | `npm run test:backend`、`npm run test:api` |
| 4 | 前端运行时内核重构 | `apps/web/src/crm/shared.js`、`apps/web/src/crm/hooks/useApi.js`、`apps/web/src/crm/hooks/useAppCrudActions.js`、`apps/web/src/crm/hooks/useAppPageActions.js`、`apps/web/src/crm/hooks/orchestrators/**`、`apps/web/src/crm/hooks/orchestrators/runtime-kernel/**`、`apps/web/src/crm/hooks/orchestrators/runtime/**`、`apps/web/src/crm/hooks/**/index.js` | 这是整个前端的基础层，改动面最大，最容易引入循环依赖、导入路径错误或运行时状态错配 | `npm run check:runtime-kernel-imports`、`npm run test:frontend:unit -- --run apps/web/src/crm/__tests__/useApi.test.jsx apps/web/src/crm/__tests__/routeConfig.test.jsx`、`npm run lint` |
| 5 | 页面组件与页面入口迁移 | `apps/web/src/crm/components/MainContent.jsx`、`apps/web/src/crm/components/MainContentPanels.jsx`、`apps/web/src/crm/components/ServerPager.jsx`、`apps/web/src/crm/components/pages/**`、`apps/web/src/crm/components/pages/*/index.js`、`apps/web/src/crm/components/pages/*/sections.js`、各页面下的 `sections/*.jsx` | 路由、面板和页面导出串联较紧，若和运行时内核拆开提交，能更快确认是“数据层问题”还是“视图层问题” | `npm run test:frontend:unit`、`npm run test:e2e:runner -- --grep "dashboard|leads|quotes|tenants"` |
| 6 | 测试补齐与回归收口 | `apps/web/src/crm/__tests__/*.test.jsx`、`apps/web/tests/e2e/*.spec.js`、`apps/web/tests/e2e/helpers/*.js`、`apps/api/src/test/java/com/yao/crm/**/*.java` | 测试改动通常不改生产逻辑，但断言变化多，容易把“旧行为”误判成回归；建议放在主体代码之后 | `npm run test:frontend:unit`、`npm run test:backend:unit`、`npm run test:e2e` |

## 拆分建议

1. 先提交文档和仓库说明，作为“辅助性改动”的独立批次，后面回看更清晰。
2. 再提交 CI / 脚本 / 配置，这一批能先把验证入口稳定下来，避免后续代码批次被工具链噪音干扰。
3. 后端 API 和前端运行时内核建议分开，原因是它们虽然会互相影响，但排错维度不同。
4. 页面组件迁移和测试收口可以继续拆细；如果某个页面改动很小，允许并入对应页面批次。
5. 若时间紧张，至少保证“运行时内核重构”和“页面组件迁移”不要和“测试更新”混在一个提交里。

## 额外说明

- 当前变更中存在大量 `apps/web/src/crm/components/pages/**` 与 `apps/web/src/crm/hooks/orchestrators/**` 的新增和重排，建议优先检查导出层、路由层、hook 入口是否都已对齐。
- 如果后续还要继续拆更细，可按以下三个维度再切：
  - `shared.js` / `useApi` 等基础工具先单独提交。
  - 页面级容器和 section 组件单独提交。
  - 单测和 E2E 断言单独提交。
- 这个文档只负责“提交拆分建议”，不包含代码修改本身。
