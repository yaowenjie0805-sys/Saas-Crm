
# Aster CRM

> 面向中小团队的全栈 CRM 单体仓库（Monorepo），内置多租户、审批流、报表与自动化能力。

<p align="center">
  <a href="./README.en.md">English</a> | <strong>简体中文</strong>
</p>

## 项目概览

Aster CRM 提供从线索到回款的完整业务链路，默认包含：

- 客户、联系人、线索、商机、报价、合同、订单、回款管理
- 多租户隔离与 RBAC 权限控制
- 可配置审批流与审计日志
- 仪表盘与报表导出（CSV/Excel）
- 前后端一体化工程结构与 CI/CD 流水线

## 仓库结构

- `apps/web`：React + Vite 前端
- `apps/api`：Spring Boot 后端
- `docs`：架构、运维、规范与操作手册
- `scripts`：本地开发、测试、部署辅助脚本

更多见：[PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md)

## 环境要求

- Node.js `>= 18`
- JDK `>= 17`
- MySQL `>= 8.0`
- Redis（推荐）
- RabbitMQ（推荐）

## 快速开始

1. 安装依赖

```bash
npm install
```

2. 配置环境变量

```bash
cp .env.example .env
cp .env.backend.local.example .env.backend.local
```

3. 初始化数据库

```bash
npm run db:init
```

4. 启动后端

```bash
npm run dev:backend
```

5. 启动前端（新终端）

```bash
npm run dev
```

## 常用命令

```bash
# 代码检查
npm run lint

# 前端单测
npm run test:frontend

# 后端测试
npm run test:backend

# E2E 测试
npm run test:e2e

# 构建前后端
npm run build
npm run build:backend
```

## 默认访问

- 前端：`http://localhost:5173`
- 后端 API：`http://localhost:8080/api`
- Swagger：`http://localhost:8080/swagger-ui.html`
- 默认账号：`admin`
- 默认密码：`admin123`

生产环境请务必覆盖默认密码与 `AUTH_TOKEN_SECRET`。

## Webhook 安全建议

- 优先配置 `WORKFLOW_APPROVAL_CALLBACK_TOKENS`（逗号分隔）用于保护审批回调入口，示例：`replace-with-long-random-token-a,replace-with-long-random-token-b`。
- 当 `WORKFLOW_APPROVAL_CALLBACK_TOKENS` 非空时，其优先级高于 `WORKFLOW_APPROVAL_CALLBACK_TOKEN`；仅在未配置多值时才回退单值。
- 配置 `WORKFLOW_APPROVAL_CALLBACK_TOKEN` 用于保护审批回调入口，建议使用高强度随机字符串，示例：`replace-with-long-random-token`。
- 配置 `INTEGRATION_WEBHOOK_ALLOWED_HOST_SUFFIXES` 为允许的目标域名后缀白名单（逗号分隔），如：`.example.com,.internal.example`；开发环境可留空，生产环境建议显式限制。
- 避免将这两个值提交到仓库，推荐通过本地环境变量或密钥管理服务注入。

## 文档导航

- [docs/README.md](./docs/README.md)：文档总入口
- [DEVELOPMENT_CONVENTIONS.md](./docs/DEVELOPMENT_CONVENTIONS.md)：开发规范
- [PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md)：故障排查
- [operations/staging-deploy-runbook.md](./docs/operations/staging-deploy-runbook.md)：部署手册

## 许可证

本项目采用 [AGPL-3.0](./LICENSE) 许可证。
