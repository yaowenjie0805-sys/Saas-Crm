# CRM E2E 测试说明

本目录包含基于 Playwright 的端到端测试，用于验证核心用户路径在真实浏览器中的行为。

## 目录与用例

- `smoke.spec.js`：核心链路冒烟测试
- `auth-flow.spec.js`：登录/鉴权流程
- `dashboard.spec.js`：仪表盘展示与关键交互
- `sidebar-navigation.spec.js`：侧边栏导航与路由切换
- `leads-management.spec.js`：线索管理流程
- `toast-notifications.spec.js`：全局通知提示
- `api-integration.spec.js`：接口联动验证
- `topbar-quotes-interaction.spec.js`：顶部入口与报价交互
- `tenants-layout.spec.js`：多租户布局与隔离展示
- `mobile-responsive.spec.js`：移动端响应式行为

## 运行前准备

1. 启动后端服务（默认 `:8080`）
2. 启动前端服务（默认 `:5173` 或 `:14173`）

## 常用命令

在仓库根目录执行：

```bash
# 全量 E2E
npm run test:e2e

# 移动端 E2E
npm run test:e2e:mobile

# AI 快捷入口专项
npm run test:e2e:ai-shortcut

# 直接调用 Playwright runner
npm run test:e2e:runner
```

在 `apps/web` 工作区执行：

```bash
# 指定单文件
npx playwright test --config playwright.config.js tests/e2e/auth-flow.spec.js

# UI 调试模式
npx playwright test --config playwright.config.js --ui

# 查看测试报告
npx playwright show-report
```

## 环境变量

- `E2E_BASE_URL`：前端地址，默认 `http://127.0.0.1:14173`
- `E2E_API_BASE_URL`：后端 API 地址，默认 `http://127.0.0.1:8080/api`
- `CI`：CI 环境标记（自动由流水线注入）

## 测试数据工具

测试数据辅助函数位于 `tests/e2e/helpers/testData.js`，用于生成唯一名称与随机数据，避免并发测试冲突。

## 编写规范

1. 优先使用 `data-testid` 作为定位器。
2. 每个用例必须可独立运行，不依赖执行顺序。
3. 用例应包含必要清理逻辑，避免污染后续测试。
4. 对异步场景使用显式等待，减少 flaky。
5. 断言信息应清晰，方便失败定位。
