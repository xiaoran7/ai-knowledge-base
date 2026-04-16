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
│              Vue 3 + Element Plus + Pinia                   │
│                    (axios API 调用)                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       后端层                                 │
│              Spring Boot 3 + Spring Security                │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐ │
│  │  用户模块   │  文档模块   │  知识库模块  │   AI 模块    │ │
│  └─────────────┴─────────────┴─────────────┴─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       数据层                                 │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │   PostgreSQL    │  │      Redis      │                  │
│  │  (用户 + 文档 + 向量)│  │   (缓存 + 会话)    │                  │
│  └─────────────────┘  └─────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      AI 服务层                               │
│              DeepSeek API (嵌入 + LLM)                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 技术栈详情

### 2.1 后端技术

| 组件 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Spring Boot | 3.2+ | Web 框架 |
| AI | Spring AI | 1.0+ | LLM 集成 |
| 数据库 | PostgreSQL | 14+ | 关系型 + 向量存储 |
| 缓存 | Redis | 6+ | 缓存/会话 |
| 安全 | Spring Security | 6+ | 认证授权 |
| Token | JWT | - | 无状态认证 |
| 文档解析 | Apache Tika | 2.x | 多格式解析 |
| 构建 | Maven | 3.9+ | 项目管理 |

### 2.2 前端技术

| 组件 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Vue | 3.4+ | UI 框架 |
| 构建 | Vite | 5.0+ | 构建工具 |
| 语言 | TypeScript | 5.3+ | 类型安全 |
| UI | Element Plus | 2.4+ | 组件库 |
| 状态 | Pinia | 2.1+ | 状态管理 |
| 请求 | Axios | 1.6+ | HTTP 客户端 |
| 路由 | Vue Router | 4.2+ | 路由管理 |
| Markdown | markdown-it | 14+ | Markdown 渲染 |

---

## 3. 核心模块设计

### 3.1 用户模块

```
User (用户表)
├── id (UUID)
├── username
├── email
├── password (BCrypt)
├── created_at
└── updated_at

JWT Token
├── access_token (24h)
└── refresh_token (7d)
```

### 3.2 知识库模块

```
KnowledgeBase (知识库表)
├── id (UUID)
├── user_id (FK)
├── name
├── description
└── created_at

Category (分类表)
├── id (UUID)
├── knowledge_base_id (FK)
├── name
├── parent_id (自关联)
└── created_at
```

### 3.3 文档模块

```
Document (文档表)
├── id (UUID)
├── knowledge_base_id (FK)
├── category_id (FK)
├── user_id (FK)
├── title
├── file_path
├── file_type
├── file_size
├── status (pending/processed/failed)
├── created_at
└── updated_at

DocumentChunk (文档切片表)
├── id (UUID)
├── document_id (FK)
├── content
├── embedding (pgvector)
├── chunk_index
└── created_at
```

### 3.4 对话模块

```
Conversation (对话表)
├── id (UUID)
├── user_id (FK)
├── knowledge_base_id (FK)
├── title
├── created_at
└── updated_at

Message (消息表)
├── id (UUID)
├── conversation_id (FK)
├── role (user/assistant)
├── content
├── sources (JSON - 引用来源)
└── created_at
```

---

## 4. 核心流程

### 4.1 文档上传流程

```
1. 用户上传文件 → 后端
2. 后端保存文件 → 本地存储
3. Apache Tika 解析文档 → 提取文本
4. 文本切片 → 按段落/固定长度
5. 调用 DeepSeek API → 生成向量嵌入
6. 存储到 PostgreSQL (pgvector)
7. 更新文档状态 → processed
```

### 4.2 AI 问答流程 (RAG)

```
1. 用户提问 → 后端
2. 调用 DeepSeek API → 生成问题向量
3. PostgreSQL 向量检索 → 查找相似文档切片 (Top-K)
4. 构建 Prompt → 系统提示 + 检索内容 + 用户问题
5. 调用 DeepSeek LLM → 生成回答
6. 流式返回 → 前端
7. 缓存结果 → Redis (可选)
8. 保存对话 → 数据库
```

### 4.3 用户认证流程

```
1. 用户登录 → 提交用户名/密码
2. 后端验证 → BCrypt 校验
3. 生成 JWT Token → access + refresh
4. 存储 Token 到 Redis (可选黑名单)
5. 返回 Token → 前端
6. 前端存储 → localStorage
7. 后续请求 → Header 携带 Token
```

---

## 5. 目录结构

### 5.1 后端

```
backend/
├── src/main/java/com/ai/kb/
│   ├── com.ai.kb.controller/       # REST 控制器
│   ├── com.ai.kb.service/          # 业务逻辑
│   ├── repository/       # 数据访问
│   ├── com.ai.kb.entity/           # 实体类
│   ├── com.ai.kb.dto/              # 数据传输对象
│   ├── com.ai.kb.config/           # 配置类
│   ├── com.ai.kb.security/         # 安全相关
│   └── ai/               # AI 相关
├── src/main/resources/
│   ├── application.yml   # 配置文件
│   └── application-dev.yml
└── pom.xml
```

### 5.2 前端

```
frontend/
├── src/
│   ├── views/            # 页面
│   │   ├── Login.vue
│   │   ├── Dashboard.vue
│   │   ├── KnowledgeBase.vue
│   │   ├── Chat.vue
│   │   └── Settings.vue
│   ├── components/       # 组件
│   │   ├── DocumentUpload.vue
│   │   ├── ChatBox.vue
│   │   └── DocumentList.vue
│   ├── api/              # API 封装
│   ├── stores/           # Pinia stores
│   ├── router/           # 路由配置
│   ├── utils/            # 工具函数
│   └── styles/           # 全局样式
├── public/
├── package.json
└── vite.com.ai.kb.config.ts
```

---

## 6. 部署架构

### 6.1 本地开发

```
localhost:5173 (前端 Vite)
localhost:8080 (后端 Spring Boot)
localhost:5432 (PostgreSQL)
localhost:6379 (Redis)
```

### 6.2 服务器部署

```
Nginx (反向代理)
├── / → frontend (静态文件)
└── /api → backend:8080

Docker Compose
├── backend (Spring Boot)
├── postgres (PostgreSQL + pgvector)
└── redis (Redis)
```

---

*最后更新：2026-04-08* 🐾
