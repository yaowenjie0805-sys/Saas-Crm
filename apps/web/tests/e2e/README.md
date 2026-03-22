# CRM E2E 测试套件

基于 Playwright 的端到端测试，覆盖 CRM 系统的核心功能模块。

## 测试文件

| 文件 | 描述 |
|------|------|
| `smoke.spec.js` | 冒烟测试 - 核心功能快速验证 |
| `auth-flow.spec.js` | 认证流程测试 |
| `dashboard.spec.js` | 仪表盘测试 |
| `sidebar-navigation.spec.js` | 侧边栏导航测试 |
| `leads-management.spec.js` | 线索管理测试 |
| `toast-notifications.spec.js` | 通知提示测试 |
| `api-integration.spec.js` | API 集成测试 |
| `topbar-quotes-interaction.spec.js` | 顶部栏和报价交互测试 |
| `tenants-layout.spec.js` | 租户布局测试 |
| `mobile-responsive.spec.js` | 移动端响应式测试 |

## 运行测试

### 前提条件
1. 启动后端服务 (端口 8080)
2. 启动前端服务 (端口 5173 或 14173)

### 运行所有测试
```bash
npm run test:e2e
```

### 运行特定测试套件
```bash
# 冒烟测试
npx playwright test --config=test-suites.config.js --project=smoke

# 认证测试
npx playwright test --config=test-suites.config.js --project=auth

# 仪表盘测试
npx playwright test --config=test-suites.config.js --project=dashboard

# 导航测试
npx playwright test --config=test-suites.config.js --project=navigation

# API 测试
npx playwright test --config=test-suites.config.js --project=api
```

### 运行单个测试文件
```bash
npx playwright test tests/e2e/auth-flow.spec.js
```

### 交互模式（调试）
```bash
npx playwright test --ui
```

### 查看报告
```bash
npx playwright show-report
```

## 测试数据

测试数据生成器位于 `tests/e2e/helpers/testData.js`：
- `generateUniqueCustomerName()` - 生成唯一客户名
- `generateUniqueLeadName()` - 生成唯一线索名
- `generateUniqueOpportunityName()` - 生成唯一商机名
- `generateUniqueQuoteName()` - 生成唯一报价名
- `generateNumericValue()` - 生成随机数值

## 环境变量

| 变量 | 默认值 | 描述 |
|------|--------|------|
| `E2E_BASE_URL` | `http://127.0.0.1:14173` | 前端 URL |
| `E2E_API_BASE_URL` | `http://127.0.0.1:8080/api` | 后端 API URL |
| `CI` | - | CI 环境标识 |

## 测试覆盖范围

### 已覆盖
- [x] 用户登录/登出
- [x] 会话持久化
- [x] 仪表盘展示
- [x] 统计卡片显示
- [x] 图表渲染
- [x] 侧边栏导航
- [x] 页面跳转
- [x] 客户 CRUD
- [x] 线索管理
- [x] Toast 通知
- [x] API 接口测试
- [x] 租户隔离
- [x] 移动端响应式

### 待覆盖
- [ ] 报价管理完整流程
- [ ] 合同管理完整流程
- [ ] 产品管理
- [ ] 商机漏斗
- [ ] 报表导出
- [ ] 多语言切换
- [ ] 权限控制
- [ ] 性能基准测试

## 最佳实践

1. **使用 data-testid** - 优先使用 `data-testid` 属性进行元素定位
2. **独立性** - 每个测试应该独立运行，不依赖其他测试的状态
3. **清理** - 测试后清理创建的数据
4. **等待** - 使用适当的等待机制避免 flaky tests
5. **断言** - 提供清晰的断言消息

## 调试技巧

```javascript
// 在测试中添加断点
await page.pause()

// 截图
await page.screenshot({ path: 'debug.png' })

// 查看控制台日志
const logs = []
page.on('console', msg => logs.push(msg.text()))

// 查看网络请求
const requests = []
page.on('request', req => requests.push(req.url()))
```
