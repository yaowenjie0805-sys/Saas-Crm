# Aster CRM

轻量级 CRM 单体仓库（`web + api`），支持多租户、审批、报表与集成能力。

## 快速开始

### 环境要求
- `JDK 17+`
- `Node.js 18+`
- `MySQL 8+`

### 启动步骤
1. 复制环境模板文件：
   - `.env.example` -> `.env`
   - `.env.backend.local.example` -> `.env.backend.local`
2. 填写关键配置：
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   - `AUTH_TOKEN_SECRET`（建议至少 32 字符）
   - `APP_SEED_TENANT_ID`（见下方 Seed 租户说明）
   - `AUTH_BOOTSTRAP_DEFAULT_PASSWORD`（开发默认 `admin123`）
   - `VITE_DEFAULT_TENANT`（登录默认租户）
3. 启动后端：`npm run dev:backend`
4. 安装并启动前端：
   - `npm install`
   - `npm run dev`

### 访问地址
- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`

## Seed 租户说明

- 当 `APP_SEED_ENABLED=true` 时，`APP_SEED_TENANT_ID` 必须非空。
- 推荐配置：
  - `dev`: `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_default`, `APP_SEED_DEMO_ENABLED=true`, `AUTH_BOOTSTRAP_DEFAULT_PASSWORD=admin123`
  - `test`: `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_test`
  - `prod`: `APP_SEED_ENABLED=false`，除非执行 seed 否则不要设置 `APP_SEED_TENANT_ID`，且不要使用弱默认密码

常见报错：登录 500 或 `seed_tenant_id_required`，通常是 seed 开启但租户 ID 为空。

## 文档导航

- 文档索引（中文主）：[`docs/README.md`](./docs/README.md)
- 项目结构：[`docs/PROJECT_STRUCTURE.md`](./docs/PROJECT_STRUCTURE.md)
- 全景调用图：[`docs/PROJECT_FLOW_MAP.md`](./docs/PROJECT_FLOW_MAP.md)
- 开发约定：[`docs/DEVELOPMENT_CONVENTIONS.md`](./docs/DEVELOPMENT_CONVENTIONS.md)

## 备注

- 未启用 Redis Repository 时，相关 warning 可忽略。
- 启用 AI 功能时，需要配置 `AI_OPENAI_API_KEY` 或 `AI_ANTHROPIC_API_KEY`。
