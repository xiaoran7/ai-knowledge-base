# 架构设计 🏗️

## 1. 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                         用户层                               │
│                    (浏览器/移动端)                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        前端层                                │
│         Vue 3 + Element Plus + Pinia + Vue Router 5         │
│                    (Axios API 调用)                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       后端层                                 │
│           Spring Boot 4 (Java 21) + Spring Security 6       │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │ 用户模块 │ 文档模块 │ 知识库   │ AI 问答  │ 检索调试 │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       数据层                                 │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  │
│  │      PostgreSQL         │  │         Redis           │  │
│  │  (用户 + 文档 + 向量)   │  │   (Token 黑名单 / 缓存)  │  │
│  └─────────────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      AI 服务层                               │
│     外部 LLM API（DeepSeek / MiniMax 等可配置 Provider）     │
│     本地内置 Embedding 模型（ONNX Runtime + DJL Tokenizer）  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 技术栈详情

### 2.1 后端技术

| 组件 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 运行时 | Java | 21 | LTS，虚拟线程支持 |
| 框架 | Spring Boot | 4.0.5 | Web / DI / 自动配置 |
| 安全 | Spring Security | 6 | 认证授权 |
| ORM | Spring Data JPA | — | 数据库访问 |
| 数据库 | PostgreSQL | 14+ | 关系型 + pgvector 向量存储 |
| 缓存 | Redis | 6+ | Token 黑名单 / 结果缓存 |
| Token | jjwt | 0.12.3 | JWT 生成与校验 |
| 文档解析 | Apache Tika | 2.9.1 | PDF/Word/Excel/Markdown 解析 |
| Embedding | ONNX Runtime | 1.20.0 | 本地向量化推理 |
| Tokenizer | DJL HuggingFace | 0.31.1 | 本地分词 |
| PDF 导出 | Apache PDFBox | — | 会话导出为 PDF |
| 构建 | Maven | 3.9+ | 项目管理 |

### 2.2 前端技术

| 组件 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Vue | 3.5 | UI 框架 |
| 构建 | Vite | 8 | 构建工具 |
| 语言 | TypeScript | 6 | 类型安全 |
| UI | Element Plus | 2.13 | 组件库 |
| 状态 | Pinia | 3 | 全局状态管理 |
| 请求 | Axios | 1.14 | HTTP 客户端（含拦截器） |
| 路由 | Vue Router | 5 | SPA 路由（含 auth guard） |
| Markdown | markdown-it | 14 | 消息内容渲染 |

---

## 3. 核心模块设计

### 3.1 用户模块

```
User
├── id (UUID)
├── username
├── email
├── passwordHash (BCrypt)
├── avatarUrl
├── assistantAvatarUrl
├── bio
├── created_at
└── updated_at

LlmConfig
├── id (UUID)
├── user_id (FK)
├── provider (deepseek / minimax / ...)
├── apiKey
├── model
├── baseUrl
├── isDefault
├── isEnabled
└── created_at

JWT Token
├── access_token (24h)
└── refresh_token (7d，Redis 黑名单管理)
```

### 3.2 知识库与分类模块

```
KnowledgeBase
├── id (UUID)
├── user_id (FK)
├── name
├── description
└── created_at

Category（树形，支持多级）
├── id (UUID)
├── knowledge_base_id (FK)
├── name
├── parent_id (自关联，可为 null)
└── created_at
```

### 3.3 文档模块

```
Document
├── id (UUID)
├── knowledge_base_id (FK)
├── category_id (FK，可为 null)
├── user_id (FK)
├── title
├── fileName / filePath / fileType / fileSize
├── status (UPLOADED / PROCESSING / SUMMARIZED / FAILED / CANCELED)
├── parsedText (TEXT，Tika 解析后原文)
├── summaryText (TEXT，AI 生成摘要)
├── summaryMode (ai / heuristic / manual)
├── failureReason
└── created_at / updated_at

DocumentChunk（向量切片）
├── id (UUID)
├── document_id (FK)
├── content (TEXT)
├── chunkType (raw / summary)
├── chunkIndex
├── embeddingVector (pgvector / Redis 存储)
└── created_at

DocumentTask（异步任务记录）
├── id (UUID)
├── document_id (FK)
├── taskType (INGESTION / SUMMARIZE)
├── status (PENDING / RUNNING / COMPLETED / FAILED / CANCELED)
├── stage (PARSING / SUMMARIZING / INDEXING / COMPLETED)
├── errorMessage
├── startedAt / completedAt
└── created_at
```

### 3.4 对话模块

```
Conversation
├── id (UUID)
├── user_id (FK)
├── knowledge_base_id (FK)
├── title（自动生成）
├── messageCount
├── sessionSummary (TEXT，滚动摘要）
├── sessionFacts (TEXT，长期事实）
├── titleGenerated (Boolean)
└── created_at / updated_at

Message
├── id (UUID)
├── conversation_id (FK)
├── role (user / assistant)
├── content (TEXT)
├── thinking (TEXT，<think> 内容）
├── sources (JSON，引用来源列表）
└── created_at
```

---

## 4. 核心流程

### 4.1 文档处理流水线（异步）

```
1. 用户上传文件 → DocumentController 保存文件、创建 Document（UPLOADED）
2. 异步任务启动：DocumentService.processDocumentAsync()
   ├── Stage PARSING：Apache Tika 解析 → 存入 parsedText
   ├── Stage SUMMARIZING：LLM 生成摘要 → 存入 summaryText
   └── Stage INDEXING：分块 → 本地 Embedding → 写入向量存储（raw + summary chunk）
3. 状态更新：COMPLETED / FAILED（含 failureReason）
4. 支持取消（CANCELED）、失败重试
```

### 4.2 AI 问答流程（RAG）

```
1. 用户发送消息 → ChatController → ChatService.chat()
2. 向量化原始 query（本地 Embedding 或外部 API）
3. pgvector 语义检索 → topK 候选 chunk（raw + summary 分层）
4. Query Rewrite：LLM 改写问题 → 再次检索 → 取更优结果
5. 构建 Prompt：会话记忆（sessionSummary / sessionFacts）+ 检索上下文 + 用户问题
6. 调用 LLM API → 提取 <think> 内容 → 返回回答
7. 保存消息（含 thinking / sources）
8. 异步更新会话摘要、长期事实、会话标题（满 4 条消息后触发）
```

### 4.3 检索调试流程

```
1. 用户输入 query → ChatService.debugRetrieval()
2. 原始 query 检索 → 展示原始命中
3. Query Rewrite → 改写后检索 → 展示改写命中
4. 对比两轮命中，选出最优 → 展示最终命中
5. 返回：originalQuery / rewrittenQuery / usedQuery / 各轮 hits（含 chunkType / score）
```

### 4.4 用户认证流程

```
1. 注册/登录 → AuthService → BCrypt 校验
2. 生成 JWT（access 24h + refresh 7d）
3. 前端存入 localStorage
4. 后续请求：Axios 拦截器附加 Authorization: Bearer <token>
5. 后端：JwtAuthenticationFilter 校验 Token → SecurityContext
6. 登出：access token 加入 Redis 黑名单
```

---

## 5. 目录结构

### 5.1 后端

```
backend/src/main/java/com.ai.kb/
├── BackendApplication.java
├── config/
│   ├── JacksonConfig.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ChatController.java
│   ├── ConversationController.java
│   ├── DocumentController.java
│   ├── GlobalExceptionHandler.java
│   ├── KnowledgeBaseController.java
│   ├── LlmConfigController.java
│   └── UserController.java
├── dto/                       # 请求/响应 DTO（record 类型）
├── entity/                    # JPA 实体（UUID 主键）
├── repository/                # Spring Data JPA 仓库
├── security/
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
└── service/
    ├── AuthService.java
    ├── CategoryService.java
    ├── ChatService.java       # 对话 / RAG / 检索调试 / 导出
    ├── DocumentService.java   # 上传 / 异步处理 / 任务管理
    ├── EmbeddingService.java  # 向量化（本地 + 外部）
    ├── KnowledgeBaseService.java
    ├── LlmConfigService.java
    ├── LlmService.java        # LLM 调用 / query rewrite / 摘要 / 标题生成
    └── VectorStoreService.java
```

### 5.2 前端

```
frontend/src/
├── api/
│   ├── index.ts              # Axios 实例 + 拦截器
│   ├── auth.ts
│   ├── chat.ts
│   ├── document.ts
│   ├── knowledge.ts
│   └── llm.ts
├── router/index.ts           # 路由 + auth guard
├── stores/
│   ├── user.ts               # 用户信息 / Token
│   └── knowledge.ts          # 当前知识库
├── styles/main.css
└── views/
    ├── Login.vue / Register.vue
    ├── Dashboard.vue
    ├── KnowledgeBase.vue / KnowledgeBaseDetail.vue
    ├── Documents.vue          # 文档列表 + 工作台（侧边预览）
    ├── DocumentTasks.vue      # 文档任务中心
    ├── Chat.vue               # AI 问答 + 会话记忆抽屉 + 检索调试抽屉
    ├── RetrievalDebug.vue     # 独立检索调试工作台
    └── Settings.vue           # LLM 配置 + 个人资料
```

---

## 6. 部署架构

### 6.1 本地开发

```
localhost:5173   前端 Vite 开发服务器（/api → :8080 代理）
localhost:8080   后端 Spring Boot
localhost:5432   PostgreSQL（需 pgvector 扩展）
localhost:6379   Redis
```

### 6.2 服务器部署（推荐）

```
Nginx（反向代理）
├── /        → frontend 静态文件
└── /api     → backend:8080（去掉 /api 前缀）

Docker Compose（建议）
├── backend  Spring Boot JAR
├── postgres PostgreSQL + pgvector
└── redis    Redis
```

---

*最后更新：2026-04-16* 🐾
