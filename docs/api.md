# API 接口定义 🔌

## 1. 接口规范

### 1.1 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token
- **数据格式**: JSON
- **字符编码**: UTF-8

### 1.2 响应格式

**成功响应:**
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**错误响应:**
```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

### 1.3 认证 Header

```
Authorization: Bearer <access_token>
```

---

## 2. 用户模块

### 2.1 注册

```
POST /auth/register
```

**请求:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**响应:**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": "uuid",
    "username": "string"
  }
}
```

---

### 2.2 登录

```
POST /auth/login
```

**请求:**
```json
{
  "username": "string",
  "password": "string"
}
```

**响应:**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "jwt_token",
    "refreshToken": "jwt_token",
    "expiresIn": 86400,
    "user": {
      "userId": "uuid",
      "username": "string",
      "email": "string"
    }
  }
}
```

---

### 2.3 登出

```
POST /auth/logout
```

**请求:** (Header 携带 Token)

**响应:**
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

---

### 2.4 刷新 Token

```
POST /auth/refresh
```

**请求:**
```json
{
  "refreshToken": "jwt_token"
}
```

**响应:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "jwt_token",
    "expiresIn": 86400
  }
}
```

---

### 2.5 获取当前用户信息

```
GET /user/me
```

**响应:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "uuid",
    "username": "string",
    "email": "string",
    "createdAt": "2026-04-08T00:00:00Z"
  }
}
```

---

## 3. 知识库模块

### 3.1 获取知识库列表

```
GET /knowledge-base
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": "uuid",
        "name": "string",
        "description": "string",
        "documentCount": 0,
        "createdAt": "2026-04-08T00:00:00Z"
      }
    ],
    "total": 1
  }
}
```

---

### 3.2 创建知识库

```
POST /knowledge-base
```

**请求:**
```json
{
  "name": "string",
  "description": "string"
}
```

---

### 3.3 获取分类列表

```
GET /knowledge-base/{kbId}/categories
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": "uuid",
        "name": "string",
        "parentId": "uuid|null",
        "documentCount": 0
      }
    ]
  }
}
```

---

### 3.4 创建分类

```
POST /knowledge-base/{kbId}/categories
```

**请求:**
```json
{
  "name": "string",
  "parentId": "uuid|null"
}
```

---

## 4. 文档模块

### 4.1 上传文档

```
POST /documents/upload
```

**请求:** (multipart/form-data)

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文档文件 |
| knowledgeBaseId | String | 是 | 知识库 ID |
| categoryId | String | 否 | 分类 ID |
| title | String | 否 | 文档标题 |

**响应:**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "documentId": "uuid",
    "title": "string",
    "status": "pending"
  }
}
```

---

### 4.2 获取文档列表

```
GET /documents
```

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| knowledgeBaseId | String | 是 | 知识库 ID |
| categoryId | String | 否 | 分类 ID |
| page | Integer | 否 | 页码 (默认 1) |
| size | Integer | 否 | 每页数量 (默认 10) |

**响应:**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": "uuid",
        "title": "string",
        "fileType": "pdf",
        "fileSize": 102400,
        "status": "processed",
        "createdAt": "2026-04-08T00:00:00Z"
      }
    ],
    "total": 10,
    "page": 1,
    "size": 10
  }
}
```

---

### 4.3 删除文档

```
DELETE /documents/{id}
```

**响应:**
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

### 4.4 获取文档详情

```
GET /documents/{id}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "id": "uuid",
    "title": "string",
    "fileType": "pdf",
    "fileSize": 102400,
    "status": "processed",
    "content": "string (可选预览)",
    "category": {
      "id": "uuid",
      "name": "string"
    },
    "createdAt": "2026-04-08T00:00:00Z"
  }
}
```

---

## 5. AI 问答模块

### 5.1 智能问答

```
POST /ai/chat
```

**请求:**
```json
{
  "knowledgeBaseId": "uuid",
  "message": "用户问题",
  "conversationId": "uuid (可选，续对话)",
  "stream": true (是否流式输出)
}
```

**响应 (非流式):**
```json
{
  "code": 200,
  "data": {
    "conversationId": "uuid",
    "messageId": "uuid",
    "content": "AI 回答内容",
    "sources": [
      {
        "documentId": "uuid",
        "documentTitle": "string",
        "content": "引用片段",
        "score": 0.95
      }
    ]
  }
}
```

**响应 (流式):** (SSE)
```
data: {"type": "start", "conversationId": "uuid"}

data: {"type": "content", "content": "部"}

data: {"type": "content", "content": "分"}

data: {"type": "content", "content": "回"}

data: {"type": "content", "content": "答"}

data: {"type": "sources", "sources": [...]}

data: {"type": "end", "messageId": "uuid"}
```

---

### 5.2 获取对话历史

```
GET /conversations
```

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| knowledgeBaseId | String | 是 | 知识库 ID |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |

**响应:**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": "uuid",
        "title": "string",
        "messageCount": 10,
        "lastMessageAt": "2026-04-08T00:00:00Z"
      }
    ],
    "total": 5
  }
}
```

---

### 5.3 获取对话详情

```
GET /conversations/{id}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "id": "uuid",
    "title": "string",
    "messages": [
      {
        "id": "uuid",
        "role": "user",
        "content": "用户问题",
        "createdAt": "2026-04-08T00:00:00Z"
      },
      {
        "id": "uuid",
        "role": "assistant",
        "content": "AI 回答",
        "sources": [...],
        "createdAt": "2026-04-08T00:00:00Z"
      }
    ]
  }
}
```

---

### 5.4 删除对话

```
DELETE /conversations/{id}
```

---

## 6. 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证/Token 失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | AI 服务不可用 |

---

## 7. WebSocket (可选扩展)

### 7.1 连接

```
ws://localhost:8080/ws?token=<access_token>
```

### 7.2 消息格式

**订阅:**
```json
{
  "action": "subscribe",
  "channel": "chat:{conversationId}"
}
```

**推送:**
```json
{
  "type": "content",
  "content": "部分回答"
}
```

---

*最后更新：2026-04-08* 🐾
