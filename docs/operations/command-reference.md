# 命令参考 | Command Reference

---

## 本地开发 | Local Development

| 命令 | 中文说明 | English Description |
|------|----------|---------------------|
| `npm run dev` | 启动 Web 应用 (`apps/web`) Vite 开发服务器 | start web app (`apps/web`) with Vite dev server. |
| `npm run dev:backend` | 启动 Spring Boot API (`apps/api`) | start Spring Boot API (`apps/api`). |
| `npm run db:init` | 初始化本地 MySQL 数据库并填充演示数据 | initialize local MySQL database and seed demo data. |

---

## 构建与测试 | Build and Test

| 命令 | 中文说明 | English Description |
|------|----------|---------------------|
| `npm run lint` | 前端代码检查 | lint frontend workspace. |
| `npm run build` | 前端构建 | build frontend workspace. |
| `npm run test:backend` | 运行后端测试 | run backend tests. |
| `npm run test:e2e` | 运行完整 Playwright E2E 流程 (后端 + 前端 + 套件) | run full Playwright E2E flow (backend + frontend + suite). |
| `npm run test:e2e:mobile` | 运行移动端/响应式 E2E 套件 | run mobile/responsive E2E suite. |
| `npm run test:api` | 后端打包 + API 冒烟测试 | backend package + API smoke test. |

---

## 环境验证 | Environment Validation

| 命令 | 中文说明 | English Description |
|------|----------|---------------------|
| `npm run validate:env` | 验证开发环境配置 | validate development env hints. |
| `npm run validate:env:staging` | 验证预发部署环境 | validate staging deployment env. |
| `npm run validate:env:prod` | 严格生产环境验证 | strict production env validation. |
| `npm run optimize:audit` | 生成自动优化审计报告到 `logs/optimization/` | generate automatic optimization audit report under `logs/optimization/`. |
| `npm run optimize:guard` | 严格优化门禁 (文档编码/归档违规时失败) | strict optimization guard (fails on docs encoding/archive violations). |
| `npm run optimize:css:split` | 将过大的 `App.css` 拆分为安全的 PostCSS 块 | split oversized `App.css` into safe PostCSS chunks. |
| `npm run optimize:auto` | 优化审计 + 布局契约 + 代码检查 | optimization audit + layout contract + lint. |

---

## CI 门禁 | CI Gates

| 命令 | 中文说明 | English Description |
|------|----------|---------------------|
| `npm run repo:check:layout` | 验证根布局契约 (`apps/` 优先，无遗留根条目) | verify root layout contract (`apps/`-first, no legacy root entries). |
| `npm run ci:pr` | 快速 PR 门禁 (代码检查 + 构建 + 环境检查 + 冒烟 E2E) | fast PR gate (lint + build + env check + smoke e2e). |
| `npm run ci:frontend` | 前端完整门禁 | frontend full gate. |
| `npm run ci:backend` | 后端完整门禁 | backend full gate. |
| `npm run ci:separated` | 分离契约门禁 | separation contract gate. |
| `npm run ci:gate` | 完整发布门禁 | full release gate. |
