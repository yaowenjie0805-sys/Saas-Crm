# 预发部署手册 (Docker Compose) | Staging Deploy Runbook (Docker Compose)

---

## 适用范围 | Scope

| 中文 | English |
|------|---------|
| 预发环境的标准部署流程 | Standard deployment process for staging environment. |

---

## 前置条件 | Prerequisites

| 中文 | English |
|------|---------|
| 预发主机 SSH 访问权限 | Staging host SSH access. |
| 已安装 Docker 和 Docker Compose | Docker and Docker Compose installed. |
| 有效的环境文件和产物版本 | Valid environment file and artifact versions. |

---

## 部署步骤 | Deploy Steps

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 拉取最新批准的代码/产物 | Pull latest approved code/artifacts. |
| 2 | 应用环境变量 | Apply environment variables. |
| 3 | 启动服务: `docker compose -f infra/staging/docker-compose.yml up -d` | Start services: `docker compose -f infra/staging/docker-compose.yml up -d` |
| 4 | 验证容器健康状态 | Verify containers are healthy. |
| 5 | 运行预发验证脚本: `npm run staging:verify` | Run staging verification script: `npm run staging:verify` |

---

## 验证清单 | Validation Checklist

| 中文 | English |
|------|---------|
| 后端健康端点全部正常 | Backend health endpoints all green. |
| 前端加载且核心页面可访问 | Frontend loads and core pages are reachable. |
| 冒烟流程通过 (认证、仪表板、客户列表、合同) | Smoke flows pass (auth, dashboard, customer list, contracts). |

---

## 回滚 | Rollback

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 停止当前 compose 栈 | Stop current compose stack. |
| 2 | 重新部署之前的产物版本 | Re-deploy previous artifact version. |
| 3 | 重新运行健康和冒烟验证 | Re-run health and smoke verification. |

---

## 证据归档 | Evidence

| 中文 | English |
|------|---------|
| 部署命令输出 | deploy command output |
| 验证输出 | verification output |
| 最终状态和操作员签名 | final status and operator signature |
