# 批次 2 提交就绪清单

## 目标

统一 CI、脚本和配置类改动的说明口径，先把“怎么跑、怎么验、怎么发”相关的入口收拢清楚，再进入下一轮代码提交。

## 范围

- `.github/workflows/*.yml`
- `package.json`
- `apps/web/package.json`
- `apps/web/eslint.config.js`
- `apps/web/vite.config.js`
- `apps/web/vitest.config.js`
- `apps/web/playwright.config.js`
- `apps/web/postcss.config.js`
- `apps/web/tailwind.config.js`
- `apps/web/test-suites.config.js`
- `scripts/*.mjs`
- `scripts/*.ps1`
- `scripts/*.sh`
- `apps/web/scripts/*.mjs`

## 排除项

- `apps/api/src/main/java/**`
- `apps/api/src/test/java/**`
- `apps/web/src/**`
- `apps/web/tests/**`
- 任何页面、组件、业务逻辑、接口实现文件
- 任何纯功能新增的测试内容，除非它是 CI / 脚本 / 配置直接相关的断言或门禁

## 验证命令

```bash
git diff --check -- .github/workflows package.json apps/web/package.json apps/web/*.config.js apps/web/test-suites.config.js scripts apps/web/scripts
npm run validate:env
npm run repo:check:layout
npm run lint
npm run test:frontend:unit
npm run test:backend:unit
npm run ci:pr
npm run ci:separated
npm run test:e2e
npm run test:e2e:runner
npm run test:full
```

## 风险

- CI 配置改动最容易出现“本地没问题，线上失败”的情况，尤其是环境变量、runner 预装工具和 secrets 依赖。
- `package.json` 里的脚本名或参数一旦变动，往往会连带影响 workflow、文档和其他脚本入口。
- shell、PowerShell、Node 脚本在 Windows 和 Linux 上的行为可能不完全一致，路径分隔符、换行符和环境变量写法都要留意。

## 回滚

- 如果只是配置或脚本调整，优先回退对应文件，不要混入业务代码。
- 如果已经影响到发布入口或门禁，先恢复脚本签名，再恢复 workflow 调用关系。
- 若某个改动拆散到多个文件，建议按 workflow、根脚本、前端配置、辅助脚本四类分别回滚。

## 建议提交信息

- `docs: unify batch2 ready checklist`
