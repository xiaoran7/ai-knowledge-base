# API 接口定义 🔌

## 1. 接口规范

### 1.1 基础信息

- **Base URL**: `http://localhost:8080`（前端通过 `/api` 代理转发，Vite 会去除 `/api` 前缀）
- **认证方式**: JWT Bearer Token
- **数据格式**: JSON（请求体与响应体均为 JSON）
- **字符编码**: UTF-8

### 1.2 响应格式

所有接口**直接返回数据**，没有统一外层包装。

**成功响应示例（列表）：**
```json
[
  { "id": "uuid", "name": "..." }
]
```

**成功响应示例（单对象）：**
```json
{ "id": "uuid", "title": "..." }
```

**错误响应（HTTP 400）：**
```
纯文本错误描述，如："用户名已存在"
```

**未认证（HTTP 401）：** 无响应体

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

**响应（201）：**
```json
{
  "accessToken": "jwt_token",
  "refreshToken": "jwt_token",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "username": "string",
    "email": "string"
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

**响应（200）：**
```json
{
  "accessToken": "jwt_token",
  "refreshToken": "jwt_token",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "username": "string",
    "email": "string"
  }
}
```

---

### 2.3 登出

```
POST /auth/logout
```

将 access token 加入 Redis 黑名单，使其立即失效。

**响应（200）：**
```json
{ "message": "登出成功" }
```

---

### 2.4 刷新 Token

```
POST /auth/refresh
```

**请求:**
```json
{ "refreshToken": "jwt_token" }
```

**响应（200）：**
```json
{
  "accessToken": "new_jwt_token",
  "expiresIn": 86400
}
```

---

### 2.5 获取当前用户信息

```
GET /user/me
Authorization: Bearer <token>
```

**响应（200）：**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "avatarUrl": "string|null",
  "assistantAvatarUrl": "string|null",
  "bio": "string|null",
  "createdAt": "2026-04-16T00:00:00"
}
```

---

### 2.6 更新个人资料

```
PUT /user/me
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

| 字段 | 类型 | 说明 |
|------|------|------|
| username | String | 用户名 |
| bio | String | 个人简介 |
| avatarFile | File | 用户头像文件 |
| assistantAvatarFile | File | AI 助手头像文件 |

---

## 3. 知识库模块

### 3.1 获取知识库列表

```
GET /knowledge-base
Authorization: Bearer <token>
```

**响应（200）：**
```json
[
  {
    "id": "uuid",
    "name": "string",
    "description": "string",
    "documentCount": 5,
    "createdAt": "2026-04-16T00:00:00"
  }
]
```

---

### 3.2 创建知识库

```
POST /knowledge-base
Authorization: Bearer <token>
```

**请求:**
```json
{
  "name": "string",
  "description": "string"
}
```

**响应（200）：** 返回创建的知识库对象

---

### 3.3 删除知识库

```
DELETE /knowledge-base/{id}
Authorization: Bearer <token>
```

---

### 3.4 获取分类树

```
GET /knowledge-base/{kbId}/categories
Authorization: Bearer <token>
```

**响应（200）：**
```json
[
  {
    "id": "uuid",
    "name": "string",
    "parentId": null,
    "children": [
      { "id": "uuid", "name": "string", "parentId": "uuid", "children": [] }
    ]
  }
]
```

---

### 3.5 创建分类

```
POST /knowledge-base/{kbId}/categories
Authorization: Bearer <token>
```

**请求:**
```json
{
  "name": "string",
  "parentId": "uuid|null"
}
```

---

### 3.6 更新分类

```
PUT /knowledge-base/{kbId}/categories/{categoryId}
Authorization: Bearer <token>
```

**请求:**
```json
{ "name": "string" }
```

---

### 3.7 删除分类

```
DELETE /knowledge-base/{kbId}/categories/{categoryId}
Authorization: Bearer <token>
```

---

## 4. 文档模块

### 4.1 上传文档

```
POST /documents/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文档文件 |
| knowledgeBaseId | String | 是 | 知识库 ID |
| categoryId | String | 否 | 分类 ID |
| title | String | 否 | 文档标题（默认取文件名） |

**响应（200）：**
```json
{
  "id": "uuid",
  "title": "string",
  "status": "UPLOADED"
}
```

---

### 4.2 获取文档列表

```
GET /documents?knowledgeBaseId={kbId}&page=1&size=10
Authorization: Bearer <token>
```

**响应（200）：**
```json
{
  "list": [
    {
      "id": "uuid",
      "title": "string",
      "fileType": "pdf",
      "fileSize": 102400,
      "status": "COMPLETED",
      "stage": "INDEXING",
      "createdAt": "2026-04-16T00:00:00"
    }
  ],
  "total": 10,
  "page": 1,
  "size": 10
}
```

---

### 4.3 获取文档详情

```
GET /documents/{id}
Authorization: Bearer <token>
```

**响应（200）：** 返回文档详情，包含 parsedText、summaryText、summaryMode、failureReason 等字段。

---

### 4.4 更新文档

```
PUT /documents/{id}
Authorization: Bearer <token>
```

**请求（支持更新标题、解析文本、总结文本、分类等）：**
```json
{
  "title": "string",
  "parsedText": "string",
  "summaryText": "string",
  "summaryMode": "manual",
  "categoryId": "uuid|null"
}
```

---

### 4.5 删除文档

```
DELETE /documents/{id}
Authorization: Bearer <token>
```

---

### 4.6 重新总结文档

```
POST /documents/{id}/summarize
Authorization: Bearer <token>
```

触发异步重新生成文档摘要并重建向量索引。

---

### 4.7 重试失败文档

```
POST /documents/{id}/retry
Authorization: Bearer <token>
```

---

## 5. 文档任务模块

### 5.1 获取任务列表

```
GET /document-tasks?knowledgeBaseId={kbId}&status=FAILED&page=1&size=20
Authorization: Bearer <token>
```

**响应（200）：**
```json
{
  "list": [
    {
      "id": "uuid",
      "documentId": "uuid",
      "documentTitle": "string",
      "taskType": "INGESTION",
      "status": "COMPLETED",
      "stage": "COMPLETED",
      "errorMessage": null,
      "startedAt": "2026-04-16T00:00:00",
      "completedAt": "2026-04-16T00:01:00",
      "createdAt": "2026-04-16T00:00:00"
    }
  ],
  "total": 5
}
```

---

### 5.2 取消任务

```
POST /document-tasks/{id}/cancel
Authorization: Bearer <token>
```

---

## 6. AI 问答模块

### 6.1 发送消息

```
POST /chat
Authorization: Bearer <token>
```

**请求:**
```json
{
  "knowledgeBaseId": "uuid",
  "message": "用户问题",
  "conversationId": "uuid（可选，不传则新建会话）"
}
```

**响应（200）：**
```json
{
  "conversationId": "uuid",
  "messageId": "uuid",
  "title": "会话标题",
  "content": "AI 回答内容",
  "thinking": "思考过程（如有）",
  "sources": [
    {
      "documentId": "uuid",
      "documentTitle": "string",
      "content": "命中片段",
      "score": 0.95
    }
  ]
}
```

---

## 7. 会话模块

### 7.1 获取会话列表

```
GET /conversations?knowledgeBaseId={kbId}&page=1&size=10
Authorization: Bearer <token>
```

**响应（200）：**
```json
[
  {
    "id": "uuid",
    "title": "string",
    "messageCount": 10,
    "updatedAt": "2026-04-16T00:00:00"
  }
]
```

---

### 7.2 获取会话详情（含消息列表）

```
GET /conversations/{id}
```

**响应（200）：**
```json
{
  "id": "uuid",
  "title": "string",
  "sessionSummary": "string|null",
  "sessionFacts": "string|null",
  "messages": [
    {
      "id": "uuid",
      "role": "user",
      "content": "用户问题",
      "thinking": null,
      "sources": [],
      "createdAt": "2026-04-16T00:00:00"
    },
    {
      "id": "uuid",
      "role": "assistant",
      "content": "AI 回答",
      "thinking": "思考过程",
      "sources": [...],
      "createdAt": "2026-04-16T00:00:00"
    }
  ]
}
```

---

### 7.3 更新会话记忆

```
PUT /conversations/{id}/memory
Authorization: Bearer <token>
```

**请求:**
```json
{
  "sessionSummary": "string|null",
  "sessionFacts": "string|null"
}
```

**响应（200）：** 返回更新后的会话详情

---

### 7.4 导出会话

```
GET /conversations/{id}/export?format=markdown
GET /conversations/{id}/export?format=pdf
```

返回文件流，Content-Disposition 为 attachment。

---

### 7.5 删除会话

```
DELETE /conversations/{id}
Authorization: Bearer <token>
```

---

## 8. 检索调试模块

### 8.1 调试检索

```
POST /chat/debug-retrieval
Authorization: Bearer <token>
```

**请求:**
```json
{
  "knowledgeBaseId": "uuid",
  "message": "调试问题",
  "topK": 8
}
```

**响应（200）：**
```json
{
  "originalQuery": "原始问题",
  "rewrittenQuery": "改写后问题（未触发则为空）",
  "usedQuery": "最终采用的问题",
  "originalHits": [...],
  "rewrittenHits": [...],
  "finalHits": [
    {
      "chunkId": "uuid",
      "documentId": "uuid",
      "documentTitle": "string",
      "content": "命中片段",
      "chunkType": "raw|summary",
      "chunkIndex": 0,
      "score": 0.92
    }
  ]
}
```

---

## 9. LLM 配置模块

### 9.1 获取 Provider 列表

```
GET /llm/providers
```

**响应（200）：** 返回系统支持的 LLM Provider 列表（provider 标识 + 展示名）

---

### 9.2 获取用户 LLM 配置列表

```
GET /llm/configs
Authorization: Bearer <token>
```

---

### 9.3 创建/更新 LLM 配置

```
POST /llm/configs
Authorization: Bearer <token>
```

**请求:**
```json
{
  "provider": "deepseek",
  "apiKey": "sk-...",
  "model": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com",
  "isDefault": true
}
```

---

### 9.4 删除 LLM 配置

```
DELETE /llm/configs/{id}
Authorization: Bearer <token>
```

---

### 9.5 启用/禁用 LLM 配置

```
PUT /llm/configs/{id}/enable
PUT /llm/configs/{id}/disable
Authorization: Bearer <token>
```

---

## 10. 错误说明

| HTTP 状态 | 含义 |
|-----------|------|
| 200 | 成功 |
| 400 | 请求参数错误（响应体为纯文本错误描述） |
| 401 | 未认证 / Token 无效或已过期 |
| 403 | 无权限操作 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

*最后更新：2026-04-16* 🐾
