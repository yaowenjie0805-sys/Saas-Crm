# CRM 项目规范文档

## 1. 项目概述

### 1.1 项目基本信息
- **项目名称**: Aster CRM (企业级客户关系管理系统)
- **项目类型**: 全栈单体仓库（前后端分离架构）
- **版本**: 1.0.0
- **开发语言**: Java (JDK 17) + JavaScript (Node.js 18+)
- **架构模式**: RESTful API + SPA (单页应用)

### 1.2 项目定位
企业级 CRM 系统，提供完整的客户关系管理解决方案，涵盖销售流程管理、审批流程、团队协作、数据报表、导入导出、多租户治理与审计运维等功能。

### 1.3 核心特性
- **多租户架构**: 基于 X-Tenant-Id 的租户隔离机制
- **权限管理**: 字段级、角色级、团队级权限控制
- **审批流程**: 灵活的审批模板和流程引擎
- **数据分析**: 仪表盘和报表设计器
- **企业集成**: 支持企业微信、钉钉、飞书等第三方集成
- **审计日志**: 完整的操作审计和数据变更追踪
- **AI 智能**: 基于大语言模型的内容生成、销售预测、线索分类等功能（需配置 `ai.openai.api-key`）

## 2. 技术架构

### 2.1 整体架构
```
┌─────────────────────────────────────────────────────────────┐
│                         前端层 (React)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  页面组件    │  │  业务组件    │  │  通用组件    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                  ↓                  ↓              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  状态管理    │  │  自定义Hooks │  │  路由管理    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTP/HTTPS
┌─────────────────────────────────────────────────────────────┐
│                     API 网关层 (Spring)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  安全认证    │  │  租户隔离    │  │  限流保护    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    应用服务层 (Spring Boot)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  控制器层    │  │  业务服务层  │  │  数据访问层  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      数据存储层                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   MySQL      │  │    Redis     │  │  RabbitMQ    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 前端技术栈

#### 2.2.1 核心框架
- **React**: 19.2.0 - UI 框架
- **Vite**: 7.3.1 - 构建工具和开发服务器
- **React Router DOM**: 7.9.4 - 路由管理
- **Zustand**: 5.0.12 - 状态管理

#### 2.2.2 UI 和样式
- **Tailwind CSS**: 4.2.2 - 原子化 CSS 框架
- **Classnames**: 2.5.1 - CSS 类名工具
- **ECharts**: 5.4.3 - 图表库
- **echarts-for-react**: 3.0.2 - ECharts React 封装

#### 2.2.3 开发工具
- **ESLint**: 9.39.1 - 代码检查
- **Vitest**: 4.1.0 - 单元测试框架
- **Playwright**: 1.58.2 - E2E 测试框架
- **PostCSS**: 8.5.8 - CSS 处理工具

### 2.3 后端技术栈

#### 2.3.1 核心框架
- **Spring Boot**: 2.7.18 - 应用框架
- **Spring Data JPA**: 数据访问抽象
- **Spring Security**: 安全认证框架
- **Spring Boot Actuator**: 监控和管理
- **springdoc-openapi**: 统一生成 OpenAPI 规范与 Swagger UI（`/swagger-ui.html`）

#### 2.3.2 数据存储
- **MySQL**: 8.0.33 - 主数据库
- **Redis + Caffeine**: Redis 作为集中缓存与会话存储，Caffeine 作为本地内存缓存（用于热点数据、10K 上限 + LRU 淘汰策略）
- **Flyway**: 9.22.3 - 数据库版本管理

#### 2.3.3 消息队列
- **RabbitMQ**: 异步消息处理
- **Spring AMQP**: RabbitMQ 集成

#### 2.3.4 工具库
- **Apache POI**: 5.2.3 - Excel 文件处理
- **Spring Validation**: 数据校验
- **H2 Database**: 测试数据库

### 2.4 开发环境
- **JDK**: 17
- **Maven**: 3.9+
- **Node.js**: 18+
- **MySQL**: 8+
- **Redis**: (可选，推荐)
- **RabbitMQ**: (可选，推荐)

## 3. 项目结构

### 3.1 目录结构规范
```
crm/
├── apps/                          # 应用程序主目录
│   ├── api/                       # Spring Boot 后端
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/yao/crm/
│   │   │   │   │   ├── controller/      # REST 控制器层
│   │   │   │   │   ├── service/         # 业务服务层
│   │   │   │   │   ├── entity/          # JPA 实体层
│   │   │   │   │   ├── repository/      # 数据访问层
│   │   │   │   │   ├── dto/             # 数据传输对象（入参/出参 DTO，带 JSR-303 校验注解）
│   │   │   │   │   ├── security/        # 安全认证模块
│   │   │   │   │   ├── config/          # Spring 配置类（安全头、安全启动校验、MDC 日志、AOP 性能、OpenAPI 等）
│   │   │   │   │   ├── integration/     # 第三方集成
│   │   │   │   │   ├── workflow/        # 工作流引擎
│   │   │   │   │   ├── exception/       # 业务异常体系（BusinessException + ErrorCode + 各类业务异常）
│   │   │   │   │   ├── enums/           # 业务常量枚举（UserRole, DataScope, EntityStatus 等）
│   │   │   │   │   ├── event/           # 领域事件基础设施（DomainEvent, Publisher, 业务事件监听器）
│   │   │   │   │   └── audit/           # 审计日志
│   │   │   │   └── resources/
│   │   │   │       ├── application.properties
│   │   │   │       ├── application-dev.properties
│   │   │   │       ├── application-prod.properties
│   │   │   │       └── db/migration/    # Flyway 迁移脚本
│   │   │   └── test/                    # 测试代码
│   │   ├── pom.xml                      # Maven 配置
│   │   └── target/                      # 构建输出
│   │
│   └── web/                       # React 前端
│       ├── src/
│       │   ├── crm/
│       │   │   ├── components/          # 组件目录
│       │   │   │   ├── pages/          # 页面组件
│       │   │   │   ├── common/         # 通用组件
│       │   │   │   ├── layout/         # 布局组件
│       │   │   │   ├── approvals/      # 审批相关
│       │   │   │   ├── charts/         # 图表组件
│       │   │   │   ├── collaboration/  # 协作组件
│       │   │   │   ├── search/         # 搜索组件
│       │   │   │   ├── workflow/       # 工作流组件
│       │   │   │   └── shell/          # 应用外壳
│       │   │   ├── hooks/              # 自定义 Hooks
│       │   │   │   ├── crudActions/    # CRUD 操作
│       │   │   │   ├── orchestrators/  # 数据编排
│       │   │   │   ├── pageActions/    # 页面操作
│       │   │   │   └── pageModels/     # 页面模型
│       │   │   ├── store/              # Zustand 状态管理
│       │   │   ├── i18n/               # 国际化配置
│       │   │   ├── context/            # React Context
│       │   │   ├── shared.js           # 共享工具函数
│       │   │   └── runtimeConfig.js    # 运行时配置
│       │   ├── main.jsx                # 前端入口
│       │   ├── App.jsx                 # 应用根组件
│       │   └── index.html              # HTML 模板
│       ├── tests/
│       │   └── e2e/                    # E2E 测试
│       ├── public/                     # 静态资源
│       ├── package.json                # 前端依赖配置
│       ├── vite.config.js              # Vite 配置
│       ├── tailwind.config.js          # Tailwind 配置
│       └── playwright.config.js        # Playwright 配置
│
├── scripts/                       # 辅助脚本
│   ├── init-db.ps1                # 数据库初始化
│   ├── flyway-repair-dev.ps1      # Flyway 修复
│   ├── start-backend.ps1          # 后端启动
│   ├── run-maven.mjs              # Maven 运行器
│   └── test-webhooks.ps1          # Webhook 测试
│
├── docs/                          # 项目文档
│   ├── README.md                  # 文档索引
│   ├── PROJECT_STRUCTURE.md       # 项目结构
│   ├── PROJECT_FLOW_MAP.md        # 调用关系图
│   ├── PROJECT_TROUBLESHOOTING.md # 故障处理
│   ├── DEVELOPMENT_HOTSPOTS.md    # 开发热点
│   ├── MODULE_CAPABILITY_MATRIX.md # 功能模块矩阵
│   ├── API_ENDPOINT_CATALOG.md    # API 目录
│   ├── api-documentation.md       # API 文档
│   ├── COMPONENT_STRUCTURE.md     # 组件结构
│   ├── operations/                # 运维文档
│   └── postman/                   # Postman 集合
│
├── packages/                      # 共享包 (预留)
├── infra/                         # 基础设施配置
│   ├── production/                # 生产环境配置
│   └── staging/                   # 预发布环境配置
│
├── backups/                       # 数据库备份
├── logs/                          # 日志文件
├── evaluation/                    # 评估相关文件
│
├── .env.example                   # 环境配置模板
├── .env.backend.local.example     # 后端配置模板
├── .gitignore                     # Git 忽略配置
├── package.json                   # 根项目配置
└── README.md                      # 项目主文档
```

### 3.2 命名规范

#### 3.2.1 文件命名规范
- **Java 类文件**: 大驼峰命名法，如 `CustomerController.java`
- **React 组件文件**: 大驼峰命名法，如 `CustomerPanel.jsx`
- **工具函数文件**: 小写+连字符，如 `api-utils.js`
- **样式文件**: 小写+连字符，如 `customer-styles.css`
- **测试文件**: 与被测文件同名+`.test.js`，如 `CustomerPanel.test.jsx`

#### 3.2.2 代码命名规范
- **类名**: 大驼峰，如 `CustomerService`
- **方法名**: 小驼峰，如 `getCustomerById`
- **变量名**: 小驼峰，如 `customerId`
- **常量名**: 全大写+下划线，如 `MAX_RETRY_COUNT`
- **数据库表名**: 小写+下划线，如 `customer_contacts`
- **数据库字段名**: 小写+下划线，如 `customer_id`

## 4. 开发规范

### 4.1 前端开发规范

#### 4.1.1 组件开发规范
- **组件结构**: 每个组件应该是一个独立的文件
- **组件职责**: 单一职责原则，一个组件只做一件事
- **Props 验证**: 使用 PropTypes 或 TypeScript 进行类型检查
- **状态管理**: 优先使用 Zustand，避免过度使用 Context
- **样式**: 使用 Tailwind CSS，避免内联样式

#### 4.1.2 Hooks 使用规范
- **自定义 Hooks**: 放置在 `hooks/` 目录下
- **Hooks 命名**: 以 `use` 开头，如 `useCustomerData`
- **依赖管理**: 正确声明 useEffect 依赖项
- **性能优化**: 合理使用 useMemo、useCallback

#### 4.1.3 状态管理规范
- **Store 结构**: 按功能模块划分 Store
- **状态更新**: 使用不可变数据更新
- **异步操作**: 在 Store 中处理异步逻辑
- **状态订阅**: 避免过度订阅，只订阅需要的数据

#### 4.1.4 路由管理规范
- **路由配置**: 集中在路由配置文件
- **路由守卫**: 使用组件进行权限控制
- **路由参数**: 使用 useParams 获取参数
- **页面懒加载**: 使用 React.lazy 进行代码分割

### 4.2 后端开发规范

#### 4.2.1 分层架构规范
- **Controller 层**: 处理 HTTP 请求，参数校验，调用 Service
- **Service 层**: 业务逻辑处理，事务管理
- **Repository 层**: 数据访问，使用 Spring Data JPA
- **Entity 层**: 数据库实体映射
 
#### 4.2.2 API 设计规范
- **RESTful 风格**: 遵循 RESTful API 设计原则
- **HTTP 方法**: 正确使用 GET、POST、PUT、DELETE
- **状态码**: 使用合适的 HTTP 状态码
- **版本控制**: API 路径包含版本号，如 `/api/v1/customers`
- **参数校验**: Controller 层接收的 DTO 必须使用 JSR-303 注解（如 `@NotNull`, `@Size` 等），并在方法签名中通过 `@Valid` 触发校验，禁止在 Service 层重复编写入参校验逻辑。
- **DTO 规范**: 输入 DTO 与输出 DTO 分离，避免直接暴露实体；字段命名与前端保持一致，必要时通过 `@JsonProperty` 映射；为每个公开 API 定义清晰的请求和响应模型。

#### 4.2.3 数据库规范
- **表命名**: 小写+下划线，如 `customer_contacts`
- **字段命名**: 小写+下划线，如 `customer_id`
- **主键**: 使用 `id` 作为主键，类型为 BIGINT
- **外键**: 关联字段命名为 `关联表_id`，如 `customer_id`
- **索引**: 合理创建索引，避免过度索引
- **时间字段**: 统一使用 `created_at`、`updated_at`

#### 4.2.4 异常处理规范
- **异常分类**: 区分业务异常和系统异常
- **异常层级**: 所有业务异常统一继承 `BusinessException` 基类，通过 `ErrorCode` 枚举（包含稳定的业务错误码、国际化消息 key、HTTP 状态码映射）进行归类管理。
- **异常处理**: 使用 `@ControllerAdvice` + 全局异常处理器，将 `BusinessException`、校验异常（如 `MethodArgumentNotValidException`）和系统异常映射为统一错误响应。
- **错误响应**: 统一的错误响应格式，至少包含 `traceId`、`errorCode`、`message`、`timestamp`；前端据此进行文案与兜底处理。
- **日志记录**: 重要异常必须记录日志，对系统异常记录 `ERROR` 级别，对可预期的业务异常记录 `WARN` 级别，结合 MDC（tenantId/userId/traceId）便于排查。

#### 4.2.5 事务管理规范
- **查询事务**: 所有只读查询 Service 方法必须标记为 `@Transactional(readOnly = true)`，确保不会误产生写入，并优化底层事务与缓存行为。
- **写入事务**: 所有写操作（新增、更新、删除）必须在 Service 层开启事务，统一添加 `timeout = 30` 秒的超时约束，避免长事务阻塞数据库连接池。
- **事务边界**: 禁止在 Controller 层直接开启事务；事务边界应与领域服务操作保持一致，一个事务仅覆盖必要的聚合修改范围。
- **异常与回滚**: 业务异常（`BusinessException`）与系统运行时异常默认触发回滚；如需要跨服务补偿，需设计显式的补偿流程而不是关闭回滚。

#### 4.2.6 事件驱动架构
- **领域事件模型**: 通过 `DomainEvent` 抽象表达领域内的重要状态变更（如审批通过、报表导出完成、缓存失效等），避免在 Service 之间直接强耦合调用。
- **发布与订阅**: 统一通过 `DomainEventPublisher` 接口发布事件，由 Spring 实现类（如 `SpringDomainEventPublisher`）适配到应用事件机制，监听器通过 `@EventListener` 订阅处理。
- **典型用例**: 使用 `CacheInvalidationListener` 等监听器，在实体变更后自动触发 Caffeine/Redis 缓存失效；后续可以扩展用于审计扩展、异步通知等。
- **规范要求**: 新增跨聚合协作逻辑优先考虑基于领域事件实现，避免在一个 Service 中直接注入过多其他 Service 造成“上帝服务”。

### 4.3 数据库迁移规范
- **迁移脚本**: 使用 Flyway 管理数据库版本
- **脚本命名**: `V{版本号}__{描述}.sql`，如 `V1__init_schema.sql`
- **不可修改**: 已执行的迁移脚本不能修改
- **回滚策略**: 提供回滚脚本或说明
- **测试环境**: 先在测试环境验证迁移脚本

## 5. 配置管理

### 5.1 环境配置
- **开发环境**: `application-dev.properties`
- **生产环境**: `application-prod.properties`
- **本地配置**: `.env.backend.local` (不提交到版本控制)
- **配置模板**: `.env.backend.local.example`

#### 5.2 配置项说明
```properties
# 服务器配置
server.port=8080
spring.profiles.active=dev

# 连接池配置（生产环境建议使用 HikariCP）
spring.datasource.type=com.zaxxer.hikari.HikariDataSource
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# 认证配置
auth.token.secret=crm-secret-change-me
auth.token.ttl-ms=86400000
auth.cookie.name=CRM_SESSION

# 安全配置
security.cors.allowed-origins=http://localhost:5173
security.rate-limit.enabled=true
security.rate-limit.max-requests=180

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/crm_local
spring.datasource.username=root
spring.datasource.password=root

# Redis 配置
spring.redis.host=127.0.0.1
spring.redis.port=6379

# RabbitMQ 配置
spring.rabbitmq.host=127.0.0.1
spring.rabbitmq.port=5672

# 集成配置
integration.webhooks.providers=WECOM,DINGTALK,FEISHU
integration.feishu.app-id=cli_xxx
integration.feishu.app-secret=xxx
```

### 5.3 敏感信息管理
- **环境变量**: 敏感信息通过环境变量配置
- **加密存储**: 密码等敏感信息加密存储
- **访问控制**: 限制配置文件的访问权限
- **版本控制**: 敏感配置不提交到版本控制

## 6. 测试规范

### 6.1 前端测试规范
- **单元测试**: 使用 Vitest 进行组件测试
- **E2E 测试**: 使用 Playwright 进行端到端测试
- **测试覆盖率**: 核心业务代码覆盖率 > 80%
- **测试组织**: 测试文件与源码文件同级

### 6.2 后端测试规范
- **单元测试**: 使用 JUnit 进行单元测试
- **集成测试**: 使用 Spring Boot Test 进行集成测试
- **测试数据**: 使用 H2 内存数据库进行测试
- **Mock 对象**: 使用 Mockito 进行对象模拟

### 6.3 测试命令
```bash
# 前端测试
npm run test                    # 运行单元测试
npm run test:e2e               # 运行 E2E 测试
npm run test:coverage          # 生成测试覆盖率报告

# 后端测试
npm run test:backend           # 运行后端测试
npm run test:backend:unit      # 运行单元测试

# 全链路测试
npm run test:full              # 运行完整测试套件
```

## 7. 部署规范

### 7.1 构建规范
```bash
# 前端构建
npm run build                  # 构建前端项目
npm run build:frontend         # 构建前端项目

# 后端构建
npm run build:backend          # 构建后端项目
```

### 7.2 部署流程
1. **代码审查**: 代码必须通过 Code Review
2. **测试验证**: 所有测试必须通过
3. **环境准备**: 准备部署环境配置
4. **数据库迁移**: 执行数据库迁移脚本
5. **应用部署**: 部署应用服务
6. **健康检查**: 验证应用健康状态
7. **监控告警**: 配置监控和告警

### 7.3 环境管理
- **开发环境**: 用于日常开发
- **测试环境**: 用于功能测试
- **预发布环境**: 用于发布前验证
- **生产环境**: 正式运行环境

## 8. 安全规范

### 8.1 认证授权
- **认证方式**: JWT Token + Session
- **权限控制**: 基于角色的访问控制 (RBAC)
- **租户隔离**: 基于 X-Tenant-Id 的租户隔离
- **密码安全**: 密码加密存储，使用 BCrypt

### 8.2 数据安全
- **SQL 注入防护**: 使用参数化查询
- **XSS 防护**: 输入输出过滤
- **CSRF 防护**: 使用 CSRF Token
- **敏感数据**: 敏感数据加密存储

### 8.3 网络安全
- **HTTPS**: 生产环境强制使用 HTTPS
- **CORS**: 配置合理的 CORS 策略
- **限流保护**: API 请求限流
- **安全头部**: 配置安全响应头

## 9. 性能优化

### 9.1 前端性能优化
- **代码分割**: 使用动态导入进行代码分割
- **懒加载**: 路由和组件懒加载
- **缓存策略**: 合理使用浏览器缓存
- **资源压缩**: 压缩 JavaScript、CSS、图片
- **CDN 加速**: 静态资源使用 CDN

### 9.2 后端性能优化
- **数据库优化**: 索引优化、查询优化
- **缓存策略**: 使用 Redis 缓存热点数据
- **异步处理**: 使用消息队列处理异步任务
- **连接池**: 合理配置数据库连接池
- **慢查询监控**: 监控和优化慢查询

## 10. 监控和运维

### 10.1 日志管理
- **日志级别**: DEBUG、INFO、WARN、ERROR
- **日志格式**: 统一的日志格式
- **日志归档**: 定期归档和清理日志
- **日志分析**: 使用日志分析工具

### 10.2 监控指标
- **应用监控**: CPU、内存、磁盘使用率
- **接口监控**: 请求量、响应时间、错误率
- **数据库监控**: 连接数、慢查询、锁等待
- **缓存监控**: 命中率、内存使用

### 10.3 告警机制
- **告警规则**: 配置合理的告警规则
- **告警渠道**: 支持多种告警渠道
- **告警级别**: 区分告警级别
- **告警响应**: 建立告警响应机制

## 11. 文档规范

### 11.1 代码文档
- **JavaDoc**: 为公共 API 编写 JavaDoc
- **JSDoc**: 为 JavaScript 函数编写 JSDoc
- **注释**: 复杂逻辑必须添加注释
- **README**: 每个模块应该有 README 文件

### 11.2 项目文档
- **架构文档**: 描述系统架构设计
- **API 文档**: 完整的 API 接口文档
- **部署文档**: 详细的部署步骤说明
- **运维文档**: 运维操作手册

## 12. 团队协作

### 12.1 版本控制
- **分支策略**: 使用 Git Flow 分支策略
- **提交规范**: 遵循 Conventional Commits 规范
- **Code Review**: 所有代码必须经过 Code Review
- **合并策略**: 使用 Pull Request 合并代码

### 12.2 代码质量
- **代码规范**: 遵循项目代码规范
- **静态检查**: 使用 ESLint、Checkstyle 进行静态检查
- **单元测试**: 核心代码必须有单元测试
- **技术债务**: 定期清理技术债务

## 13. 附录

### 13.1 常用命令
```bash
# 安装依赖
npm install

# 初始化数据库
npm run db:init

# 启动后端
npm run dev:backend
npm run dev:backend:stable

# 启动前端
npm run dev

# 构建项目
npm run build

# 运行测试
npm run test:full

# 代码检查
npm run lint
```

### 13.2 相关资源
- **项目文档**: `docs/` 目录
- **API 文档**: `docs/api-documentation.md`
- **故障处理**: `docs/PROJECT_TROUBLESHOOTING.md`
- **开发指南**: `docs/DEVELOPMENT_HOTSPOTS.md`

### 13.3 联系方式
- **技术支持**: 技术支持邮箱
- **问题反馈**: 问题反馈渠道
- **文档更新**: 文档更新记录

---

**文档版本**: 1.0.0
**最后更新**: 2026-03-24
**维护人员**: 开发团队
