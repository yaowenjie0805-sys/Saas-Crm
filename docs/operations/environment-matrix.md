# 环境矩阵 | Environment Matrix

---

## 环境定义 | Environments

| 环境 | 中文 | English |
|------|------|---------|
| Local | 开发机器，用于编码和快速验证 | developer machine for coding and fast validation. |
| Staging | 预生产环境，用于发布验证 | pre-production environment for release verification. |
| Production | 面向客户的环境 | customer-facing environment. |

---

## 环境用途 | Purpose by Environment

| 环境 | 中文 | English |
|------|------|---------|
| Local | 功能开发，代码检查/构建/E2E 迭代 | feature development, lint/build/e2e iteration. |
| Staging | 集成检查，发布候选验证 | integration checks, release candidate verification. |
| Production | 稳定服务运行 | stable service operation. |

---

## 必需输入 | Required Inputs

| 中文 | English |
|------|---------|
| `VITE_API_BASE_URL` 用于前端 API 目标地址 | `VITE_API_BASE_URL` for frontend API target. |
| 后端数据库连接配置 | DB connection settings for backend. |
| 用于 `/api/v1/**` 验证的租户感知认证头 | Tenant-aware auth headers for `/api/v1/**` verification. |

---

## 晋升规则 | Promotion Rules

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 本地检查通过 (`lint`, `build`, smoke e2e) | Local checks pass (`lint`, `build`, smoke e2e). |
| 2 | 预发部署和验证通过 | Staging deploy and verification pass. |
| 3 | 变更控制清单完成 | Change-control checklist is complete. |
| 4 | 获得发布窗口批准 | Release window approval is granted. |

---

## 禁止实践 | Forbidden Practices

| 中文 | English |
|------|---------|
| 生产发布前跳过预发验证 | Skipping staging verification before production. |
| 无回滚计划的情况下在生产环境应用数据库/索引变更 | Applying DB/index changes in production without rollback plan. |
| 混用环境凭证 | Mixing environment credentials. |

---

## 快速命令 | Quick Commands

| 命令 | 中文 | English |
|------|------|---------|
| `npm run validate:env` | 验证开发环境 | Validate env |
| `npm run validate:env:staging` | 验证预发环境 | Validate staging env |
| `npm run validate:env:prod` | 验证生产环境 | Validate production env |
