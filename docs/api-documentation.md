# CRM API 接口文档

## 概述

本文档描述了 CRM 系统的所有 REST API 接口，包括国际标准功能和国内特色功能。

**基础URL**: `https://api.example.com/api/v2`

**认证方式**: Bearer Token (JWT)

**多租户**: 使用 `X-Tenant-Id` 请求头

---

## 目录

1. [通用说明](#通用说明)
2. [工作流 API](#工作流-api)
3. [数据导入导出 API](#数据导入导出-api)
4. [审批 API](#审批-api)
5. [协作 API](#协作-api)
6. [全局搜索 API](#全局搜索-api)
7. [数据可视化 API](#数据可视化-api)
8. [权限管理 API](#权限管理-api)
9. [通知推送 API](#通知推送-api)
10. [错误码](#错误码)

---

## 通用说明

### 请求头

| Header | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer {token} |
| X-Tenant-Id | String | 是 | 租户ID |
| Content-Type | String | 是 | application/json |
| Accept-Language | String | 否 | zh-CN / en-US |

### 通用响应格式

```json
{
  "success": true,
  "data": { ... },
  "message": "操作成功",
  "timestamp": "2026-03-22T10:00:00Z"
}
```

### 分页响应格式

```json
{
  "success": true,
  "data": {
    "items": [...],
    "page": 1,
    "size": 20,
    "totalPages": 10,
    "total": 200
  }
}
```

---

## 工作流 API

### 工作流管理

#### 创建工作流

```
POST /api/v2/workflows
```

**请求体**:
```json
{
  "name": "客户跟进工作流",
  "description": "当客户状态变更时自动创建跟进任务",
  "category": "customer",
  "owner": "user123"
}
```

**响应**:
```json
{
  "id": "wf_abc123",
  "name": "客户跟进工作流",
  "status": "DRAFT",
  "version": 1,
  "createdAt": "2026-03-22T10:00:00Z"
}
```

#### 获取工作流详情

```
GET /api/v2/workflows/{id}
```

**响应**:
```json
{
  "workflow": {
    "id": "wf_abc123",
    "name": "客户跟进工作流",
    "status": "ACTIVE",
    "nodes": [...],
    "connections": [...]
  }
}
```

#### 激活工作流

```
POST /api/v2/workflows/{id}/activate
```

**请求体**:
```json
{
  "activatedBy": "user123"
}
```

#### 停用工作流

```
POST /api/v2/workflows/{id}/deactivate
```

#### 验证工作流

```
POST /api/v2/workflows/{id}/validate
```

**响应**:
```json
{
  "valid": true,
  "errors": [],
  "warnings": ["建议添加结束节点"]
}
```

### 工作流节点管理

#### 添加节点

```
POST /api/v2/workflows/{id}/nodes
```

**请求体**:
```json
{
  "nodeType": "TRIGGER",
  "nodeSubtype": "RECORD_CREATED",
  "name": "客户创建触发",
  "positionX": 100,
  "positionY": 200,
  "configJson": "{\"entityType\": \"Customer\"}"
}
```

**支持的节点类型**:

| nodeType | 说明 | nodeSubtype |
|----------|------|-------------|
| TRIGGER | 触发器 | RECORD_CREATED, RECORD_UPDATED, FIELD_CHANGED, MANUAL |
| ACTION | 动作 | CREATE_TASK, UPDATE_FIELD, SEND_EMAIL, CREATE_RECORD |
| CONDITION | 条件 | IF, SWITCH |
| NOTIFICATION | 通知 | EMAIL, WECHAT_WORK, DINGTALK, IN_APP, SMS |
| APPROVAL | 审批 | SINGLE, SERIAL, PARALLEL |
| WAIT | 等待 | DELAY, CONDITION |
| CC | 抄送 | EMAIL_CC, WECHAT_CC |
| AUXILIARY | 辅助 | START, END |

#### 添加连接

```
POST /api/v2/workflows/{id}/connections
```

**请求体**:
```json
{
  "sourceNodeId": "node_1",
  "targetNodeId": "node_2",
  "connectionType": "DEFAULT",
  "label": "true"
}
```

### 工作流执行

#### 启动执行

```
POST /api/v2/workflows/{id}/execute
```

**请求体**:
```json
{
  "triggerType": "MANUAL",
  "triggerSource": "user123",
  "payload": {
    "customerId": "cust_123",
    "action": "update_status"
  }
}
```

**响应**:
```json
{
  "success": true,
  "executionId": "exec_xyz789",
  "status": "RUNNING",
  "startedAt": "2026-03-22T10:00:00Z"
}
```

#### 获取执行详情

```
GET /api/v2/workflows/executions/{executionId}
```

**响应**:
```json
{
  "execution": {
    "id": "exec_xyz789",
    "workflowId": "wf_abc123",
    "status": "RUNNING",
    "currentNodeId": "node_2",
    "startedAt": "2026-03-22T10:00:00Z"
  },
  "context": {
    "variables": {...},
    "nodeResults": {...}
  }
}
```

#### 获取执行历史

```
GET /api/v2/workflows/{id}/executions?status=RUNNING&page=0&size=20
```

#### 取消执行

```
POST /api/v2/workflows/executions/{executionId}/cancel
```

**请求体**:
```json
{
  "cancelledBy": "user123"
}
```

#### 重试执行

```
POST /api/v2/workflows/executions/{executionId}/retry
```

#### 审批回调

```
POST /api/v2/workflows/executions/{executionId}/approval-callback
```

**请求体**:
```json
{
  "nodeId": "node_approval_1",
  "action": "APPROVE",
  "approverId": "user456",
  "comments": "同意此申请"
}
```

### 获取节点类型

```
GET /api/v2/workflows/node-types
```

**响应**:
```json
{
  "TRIGGER": [
    {"value": "RECORD_CREATED", "label": "记录创建", "icon": "📝"},
    {"value": "RECORD_UPDATED", "label": "记录更新", "icon": "✏️"}
  ],
  "ACTION": [...],
  "CONDITION": [...],
  "NOTIFICATION": [
    {"value": "EMAIL", "label": "邮件通知", "icon": "📧"},
    {"value": "WECHAT_WORK", "label": "企业微信", "icon": "💬"},
    {"value": "DINGTALK", "label": "钉钉通知", "icon": "🔔"}
  ],
  "APPROVAL": [...]
}
```

---

## 数据导入导出 API

### 创建导入任务

```
POST /api/v2/import/jobs
Content-Type: multipart/form-data
```

**表单参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| file | File | 导入文件 (xlsx, csv, json) |
| entityType | String | Customer, Contact, Lead, Product |
| operator | String | 操作人ID |

**响应**:
```json
{
  "jobId": "imp_abc123",
  "status": "RUNNING",
  "totalRows": 1000,
  "processedRows": 0,
  "successCount": 0,
  "failCount": 0,
  "progressPercent": 0
}
```

### 获取导入任务状态

```
GET /api/v2/import/jobs/{jobId}
```

**响应**:
```json
{
  "jobId": "imp_abc123",
  "status": "RUNNING",
  "totalRows": 1000,
  "processedRows": 500,
  "successCount": 480,
  "failCount": 20,
  "progressPercent": 50,
  "errors": [
    {
      "rowNumber": 15,
      "errorMessage": "客户名称不能为空",
      "rawData": ",,13800138000,email@example.com"
    }
  ]
}
```

### 取消导入任务

```
DELETE /api/v2/import/jobs/{jobId}
```

### 获取导入模板

```
GET /api/v2/import/templates/{entityType}?format=xlsx
```

**支持的 entityType**: Customer, Contact, Lead, Product

**支持的 format**: xlsx, csv

### 创建导出任务

```
POST /api/v2/export/jobs
```

**请求体**:
```json
{
  "entityType": "Customer",
  "format": "xlsx",
  "fields": ["name", "industry", "phone", "status"],
  "filters": {
    "status": "ACTIVE",
    "industry": "互联网"
  }
}
```

### 获取导出任务状态

```
GET /api/v2/export/jobs/{jobId}
```

### 下载导出文件

```
GET /api/v2/export/jobs/{jobId}/download
```

---

## 审批 API

### 审批模板管理

#### 创建审批模板

```
POST /api/v2/approval/templates
```

**请求体**:
```json
{
  "name": "合同审批",
  "description": "金额大于10万的合同需要审批",
  "entityType": "Contract",
  "triggerCondition": "amount > 100000",
  "approvers": [
    {"userId": "manager1", "type": "USER", "order": 1},
    {"userId": "manager2", "type": "USER", "order": 2}
  ],
  "slaHours": 48,
  "notificationEnabled": true
}
```

#### 提交审批

```
POST /api/v2/approval/instances
```

**请求体**:
```json
{
  "templateId": "tmpl_abc123",
  "entityId": "contract_xyz",
  "entityType": "Contract",
  "applicantId": "user123",
  "reason": "需要签订新客户合同",
  "payload": {
    "amount": 150000,
    "customerName": "示例公司"
  }
}
```

#### 审批操作

```
POST /api/v2/approval/tasks/{taskId}/action
```

**请求体**:
```json
{
  "action": "APPROVE",
  "comments": "同意，请继续",
  "delegateTo": null
}
```

**action 可选值**: APPROVE, REJECT, RETURN_TO_APPLICANT, TRANSFER

### 获取待我审批

```
GET /api/v2/approval/tasks/pending?page=0&size=20
```

### 获取我发起的审批

```
GET /api/v2/approval/instances/my?page=0&size=20
```

### 获取审批历史

```
GET /api/v2/approval/instances/{instanceId}/history
```

### 审批委托 API

#### 委托审批任务

```
POST /api/v2/collaboration/approval/tasks/{taskId}/delegate
```

**请求体**:
```json
{
  "fromUserId": "user123",
  "toUserId": "user456",
  "reason": "出差期间无法处理"
}
```

**响应**:
```json
{
  "success": true,
  "result": {
    "taskId": "task_abc123",
    "delegationId": "del_xyz789",
    "message": "审批任务已委托给 user456"
  }
}
```

#### 加签审批

```
POST /api/v2/collaboration/approval/tasks/{taskId}/add-sign
```

**请求体**:
```json
{
  "approverId": "user123",
  "addSignUserId": "user789",
  "reason": "需要财务复核",
  "type": "AFTER"
}
```

**type 可选值**: BEFORE（前加签）, AFTER（后加签）

#### 转交审批任务

```
POST /api/v2/collaboration/approval/tasks/{taskId}/transfer
```

**请求体**:
```json
{
  "fromUserId": "user123",
  "toUserId": "user456",
  "reason": "工作调整"
}
```

#### 获取委托历史

```
GET /api/v2/collaboration/approval/tasks/{taskId}/delegations
```

#### 获取转交历史

```
GET /api/v2/collaboration/approval/tasks/{taskId}/transfers
```

#### 撤回委托

```
POST /api/v2/collaboration/approval/delegations/{delegationId}/recall?userId={userId}
```

#### 获取可委托的用户列表

```
GET /api/v2/collaboration/approval/tasks/delegatable-users?currentUserId={userId}
```

---

## 协作 API

### 评论功能

#### 添加评论

```
POST /api/v2/collaboration/comments
```

**请求体**:
```json
{
  "entityType": "Customer",
  "entityId": "cust_123",
  "authorId": "user123",
  "authorName": "张三",
  "content": "这个客户很有潜力，@李四 跟进一下。#重点客户#",
  "parentCommentId": null,
  "metadata": {}
}
```

**content 支持**:
- @提及：使用 `@用户名` 格式
- #标签#：使用 `#标签名#` 格式

**响应**:
```json
{
  "success": true,
  "comment": {
    "id": "cmt_abc123",
    "content": "这个客户很有潜力，@李四 跟进一下。#重点客户#",
    "authorId": "user123",
    "authorName": "张三",
    "mentions": ["李四"],
    "tags": ["重点客户"],
    "likeCount": 0,
    "replyCount": 0,
    "createdAt": "2026-03-22T10:00:00Z"
  }
}
```

#### 回复评论

```
POST /api/v2/collaboration/comments/{commentId}/reply
```

**请求体**:
```json
{
  "authorId": "user456",
  "authorName": "李四",
  "content": "好的，我马上跟进"
}
```

#### 删除评论

```
DELETE /api/v2/collaboration/comments/{commentId}?userId={userId}
```

#### 点赞/取消点赞评论

```
POST /api/v2/collaboration/comments/{commentId}/like?userId={userId}
```

#### 编辑评论

```
PUT /api/v2/collaboration/comments/{commentId}
```

**请求体**:
```json
{
  "userId": "user123",
  "newContent": "更新后的评论内容"
}
```

### 获取评论

#### 获取实体的评论列表

```
GET /api/v2/collaboration/entities/{entityType}/{entityId}/comments?page=0&size=20&includeReplies=true
```

**响应**:
```json
{
  "success": true,
  "comments": [...],
  "total": 50,
  "page": 0,
  "size": 20,
  "totalPages": 3
}
```

#### 获取@提及我的评论

```
GET /api/v2/collaboration/mentions?userId={userId}&page=0&size=20
```

#### 获取我参与的讨论

```
GET /api/v2/collaboration/discussions?userId={userId}&limit=20
```

**响应**:
```json
{
  "success": true,
  "discussions": [
    {
      "entityType": "Customer",
      "entityId": "cust_123",
      "commentCount": 10,
      "latestCommentAt": "2026-03-22T10:00:00Z",
      "latestCommentAuthor": "张三",
      "latestCommentPreview": "这个客户很有潜力..."
    }
  ]
}
```

#### 搜索评论

```
GET /api/v2/collaboration/comments/search?keyword={keyword}&page=0&size=20
```

#### 获取评论统计

```
GET /api/v2/collaboration/entities/{entityType}/{entityId}/comments/stats
```

**响应**:
```json
{
  "success": true,
  "stats": {
    "totalComments": 50,
    "topLevelComments": 20,
    "replies": 30,
    "totalLikes": 100,
    "uniqueParticipants": 15
  }
}
```

### 团队协作

#### 创建团队

```
POST /api/v2/collaboration/teams
```

**请求体**:
```json
{
  "name": "销售一部",
  "description": "负责北区销售",
  "leaderId": "user123",
  "memberIds": ["user456", "user789"]
}
```

#### 获取团队列表

```
GET /api/v2/collaboration/teams
GET /api/v2/collaboration/teams?userId={userId}
```

#### 添加团队成员

```
POST /api/v2/collaboration/teams/{teamId}/members
```

**请求体**:
```json
{
  "userId": "user999",
  "role": "MEMBER"
}
```

#### 移除团队成员

```
DELETE /api/v2/collaboration/teams/{teamId}/members/{userId}
```

---

## 全局搜索 API

### 全局搜索

```
GET /api/v2/search?q={query}&types=Customer,Lead,Contact&page=0&size=20
```

**参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| q | String | 搜索关键词（支持拼音） |
| types | String | 搜索类型，多个用逗号分隔 |
| page | Int | 页码 |
| size | Int | 每页数量 |

**支持的搜索类型**: Customer, Lead, Contact, Opportunity, Product, Contract

**响应**:
```json
{
  "items": [
    {
      "id": "cust_123",
      "type": "Customer",
      "title": "示例公司",
      "subtitle": "互联网 | 北京市朝阳区",
      "highlight": "示例<span class='highlight'>公司</span>",
      "pinyin": "shi li gong si",
      "score": 0.95
    }
  ],
  "total": 50,
  "page": 0,
  "size": 20
}
```

### 保存搜索

```
POST /api/v2/search/saved
```

**请求体**:
```json
{
  "name": "重点客户",
  "query": "status:VIP",
  "types": ["Customer"]
}
```

### 获取保存的搜索

```
GET /api/v2/search/saved
```

---

## 数据可视化 API

### 获取仪表盘配置

```
GET /api/v2/dashboards/{dashboardId}
```

### 保存仪表盘配置

```
PUT /api/v2/dashboards/{dashboardId}
```

**请求体**:
```json
{
  "name": "销售仪表盘",
  "layout": [...],
  "filters": {...}
}
```

### 获取图表数据

```
POST /api/v2/charts/query
```

**请求体**:
```json
{
  "chartType": "BAR",
  "entityType": "Opportunity",
  "xAxis": "owner",
  "yAxis": ["amount"],
  "groupBy": "stage",
  "filters": {
    "createdAt": {
      "start": "2026-01-01",
      "end": "2026-03-31"
    }
  }
}
```

**chartType 可选值**: BAR, LINE, PIE, FUNNEL, TABLE

### 销售预测

```
GET /api/v2/forecasts?salesRepId={id}&quarter=Q1&year=2026
```

**响应**:
```json
{
  "forecast": {
    "amount": 5000000,
    "weightedAmount": 3500000,
    "confidence": 0.75,
    "minAmount": 3000000,
    "maxAmount": 6000000
  },
  "byStage": [
    {"stage": "需求分析", "amount": 1000000, "weight": 0.2},
    {"stage": "方案报价", "amount": 2000000, "weight": 0.4},
    {"stage": "合同谈判", "amount": 1500000, "weight": 0.6},
    {"stage": "签单完成", "amount": 500000, "weight": 1.0}
  ],
  "trend": [
    {"month": "2026-01", "amount": 1000000},
    {"month": "2026-02", "amount": 1500000},
    {"month": "2026-03", "amount": 2500000}
  ]
}
```

---

## 权限管理 API

### 字段级权限

#### 获取字段权限配置

```
GET /api/v2/permissions/field/{entityType}
```

**响应**:
```json
{
  "fields": [
    {
      "fieldName": "phone",
      "label": "电话",
      "required": false,
      "readable": true,
      "writable": true,
      "sensitive": true,
      "masked": true,
      "maskPattern": "***-****-****"
    }
  ]
}
```

#### 更新字段权限

```
PUT /api/v2/permissions/field/{entityType}/{fieldName}
```

**请求体**:
```json
{
  "readable": true,
  "writable": false,
  "required": false,
  "sensitive": true,
  "masked": true,
  "maskPattern": "***-****-****"
}
```

### 团队管理

#### 创建团队

```
POST /api/v2/teams
```

**请求体**:
```json
{
  "name": "销售一部",
  "description": "负责北区销售",
  "leaderId": "user123",
  "memberIds": ["user456", "user789"]
}
```

#### 获取团队列表

```
GET /api/v2/teams
```

#### 添加团队成员

```
POST /api/v2/teams/{teamId}/members
```

**请求体**:
```json
{
  "userId": "user999",
  "role": "MEMBER"
}
```

---

## 通知推送 API

### 发送推送

```
POST /api/v2/push/send
```

**请求体**:
```json
{
  "recipientId": "user123",
  "channel": "WECHAT_WORK",
  "title": "新任务提醒",
  "content": "您有一个待处理的任务：客户拜访",
  "data": {
    "taskId": "task_abc",
    "type": "task_reminder"
  }
}
```

**channel 可选值**:
- IN_APP: 站内通知
- EMAIL: 邮件
- SMS: 短信
- WECHAT_WORK: 企业微信
- DINGTALK: 钉钉

### 获取推送历史

```
GET /api/v2/push/history?recipientId={userId}&page=0&size=20
```

### 更新推送配置

```
PUT /api/v2/push/settings/{userId}
```

**请求体**:
```json
{
  "emailEnabled": true,
  "smsEnabled": false,
  "wechatWorkEnabled": true,
  "dingtalkEnabled": true,
  "quietHours": {
    "enabled": true,
    "start": "22:00",
    "end": "08:00"
  }
}
```

---

## 错误码

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求过于频繁 |
| 500 | 服务器错误 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| WF_001 | 工作流不存在 |
| WF_002 | 工作流未激活 |
| WF_003 | 工作流验证失败 |
| WF_004 | 执行中无法停用 |
| IMP_001 | 导入文件为空 |
| IMP_002 | 导入行数超限 |
| IMP_003 | 不支持的格式 |
| IMP_004 | 并发导入超限 |
| EXP_001 | 导出文件生成失败 |
| APR_001 | 审批模板不存在 |
| APR_002 | 审批任务不存在 |
| APR_003 | 无审批权限 |
| APR_004 | 审批已超时 |
| PERM_001 | 字段无权限 |
| PERM_002 | 字段必填 |
| PUSH_001 | 推送配置错误 |
| PUSH_002 | 推送服务不可用 |

---

## 国内特色功能说明

### 1. 企业微信集成

工作流通知和站内信支持推送到企业微信：
- 自动同步用户信息
- 支持@提醒指定人员
- 支持点击跳转到CRM详情页

### 2. 钉钉集成

支持钉钉作为通知渠道：
- 支持钉钉群机器人
- 支持工作通知消息
- 支持审批回调

### 3. 短信通知

支持国内短信通知：
- 模板化管理
- 签名配置
- 发送状态追踪

### 4. 拼音搜索

全局搜索支持拼音首字母和全拼搜索：
- 输入 "xsgs" 可以搜索到 "销售公司"
- 输入 "xiao shou gong si" 也可以搜索到

### 5. 敏感数据脱敏

电话、身份证等敏感字段自动脱敏显示：
- 手机号: 138****8000
- 身份证: 110101********1234

### 6. 审批委托

支持将审批任务委托给他人处理。

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-03-22 | 初始版本，包含工作流、数据导入导出、审批等核心功能 |
