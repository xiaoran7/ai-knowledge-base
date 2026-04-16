# AI 知识库 📚🤖

基于 Spring Boot 4 + Vue 3 的个人知识库系统，通过 RAG（检索增强生成）技术，让 AI 基于你上传的文档回答问题。

## 🔆 项目定位

个人学习资料管理工具。上传 PDF/Word/Markdown 等文档后，系统自动解析、分块、向量化，并支持语义检索与多轮 AI 问答。

## 🚀 核心功能

### 📤 文档管理
- 支持 PDF / Word / Excel / Markdown / TXT 等多种格式上传
- 异步处理流水线：解析 → 分块 → 总结 → 向量化索引
- 文档工作台：查看解析文本、编辑总结、AI 自动生成摘要、失败重试
- 文档任务中心：按状态筛选任务、取消进行中任务、跳转文档工作台

### 💬 AI 问答
- 基于知识库内容的 RAG 问答（含 query rewrite）
- 消息引用来源展示（文档标题 + 命中片段 + 相关度）
- `<think>` 思考过程折叠展示
- 多轮对话支持：自动生成会话摘要与长期事实（sessionSummary / sessionFacts）
- 自动生成会话标题
- 停止生成按钮（前端中断请求）
- 会话导出：Markdown / PDF

### 🔍 检索调试
- 独立检索调试页：输入问题 → 查看 query rewrite 前后对比 → 展示 topK 命中 chunk
- 聊天页内嵌检索调试抽屉

### 📁 知识库与分类
- 创建/删除知识库
- 树形分类管理（支持多级分类）

### 👤 用户系统
- 注册 / 登录 / 登出（JWT 认证）
- 个人资料：用户名、头像、AI 助手头像、简介
- LLM 配置：支持配置多个 Provider（含 MiniMax）、设置默认模型

## 🛠️ 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行时 |
| Spring Boot | 4.0.5 | Web 框架 |
| Spring Security | 6 | 认证授权 |
| PostgreSQL + pgvector | 14+ | 关系数据库 + 向量存储 |
| Redis | 6+ | 缓存 / Token 黑名单 |
| Apache Tika | 2.9.1 | 多格式文档解析 |
| ONNX Runtime + DJL | 1.20.0 / 0.31.1 | 本地内置 Embedding 模型 |
| jjwt | 0.12.3 | JWT 生成与校验 |
| Apache PDFBox | — | 会话导出 PDF |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| Vite | 8 | 构建工具 |
| TypeScript | 6 | 类型安全 |
| Element Plus | 2.13 | 组件库 |
| Pinia | 3 | 状态管理 |
| Axios | 1.14 | HTTP 客户端 |
| Vue Router | 5 | 路由 |
| markdown-it | 14 | Markdown 渲染 |

## 📁 项目结构

```
ai-knowledge-base/
├── README.md
├── docs/
│   ├── requirements.md        # 需求文档
│   ├── architecture.md        # 架构设计
│   ├── api.md                 # 接口定义
│   ├── v1-gap-tracker.md      # V1 完成度追踪
│   └── v1-progress-*.md       # 各轮开发进度记录
├── backend/                   # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com.ai.kb/
│       ├── config/            # Security / Redis / Jackson 配置
│       ├── controller/        # REST 控制器
│       ├── dto/               # 请求/响应 DTO
│       ├── entity/            # JPA 实体
│       ├── repository/        # Spring Data 仓库
│       ├── security/          # JWT Filter / Provider
│       └── service/           # 业务逻辑
└── frontend/                  # Vue 3 前端
    └── src/
        ├── api/               # Axios 封装（auth / chat / document / knowledge / llm）
        ├── router/            # Vue Router（含 auth guard）
        ├── stores/            # Pinia（user / knowledge）
        ├── styles/            # 全局样式
        └── views/             # 页面组件
            ├── Login.vue / Register.vue
            ├── Dashboard.vue
            ├── KnowledgeBase.vue / KnowledgeBaseDetail.vue
            ├── Documents.vue        # 文档管理 + 工作台
            ├── DocumentTasks.vue    # 文档任务中心
            ├── Chat.vue             # AI 问答 + 会话记忆
            ├── RetrievalDebug.vue   # 检索调试工作台
            └── Settings.vue         # LLM 配置 / 个人资料
```

## ⚡ 快速开始

### 前置依赖

- Java 21+
- Maven 3.9+
- PostgreSQL 14+（需启用 pgvector 扩展）
- Redis 6+
- Node.js 18+

### 后端启动

```bash
cd backend
# 在 src/main/resources/application.yml 中配置数据库、Redis 连接
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
# 前端运行在 http://localhost:5173（/api 代理到 8080）
```

### 使用流程

1. 注册账号并登录
2. 在「设置」页配置 LLM API Key（支持 DeepSeek / MiniMax 等）
3. 创建知识库
4. 上传文档，等待异步处理完成
5. 在「AI 问答」页选择知识库，开始对话

## 📋 开发进度

### ✅ 已完成

- [x] 用户注册 / 登录 / 登出（JWT）
- [x] 知识库 CRUD
- [x] 树形分类管理
- [x] 文档上传（PDF/Word/Excel/Markdown/TXT）
- [x] 异步文档处理流水线（解析 → 分块 → 总结 → 向量索引）
- [x] 文档工作台（预览 / 编辑解析文本 / 总结管理 / 重试）
- [x] 文档任务中心（状态筛选 / 取消 / 重试 / 跳转工作台）
- [x] AI 问答（RAG + query rewrite + 来源引用）
- [x] 多轮对话（会话摘要 / 长期事实 / 自动标题）
- [x] 思考过程（`<think>` 提取与折叠展示）
- [x] 会话导出（Markdown / PDF）
- [x] 停止生成
- [x] 检索调试（独立页 + 聊天内嵌抽屉）
- [x] LLM 多 Provider 配置（含 MiniMax）
- [x] 本地内置 Embedding 模型（ONNX）
- [x] 个人资料（头像 / AI 助手头像 / 简介）

### 🚧 部分完成

- [ ] 后端任务取消（检查点式，非硬中断）
- [ ] 检索调试产品化（目前偏向研发视图）
- [ ] 会话导出入口与模板优化
- [ ] 个人资料扩展（语言 / 主题 / AI 偏好设置）

### ⏳ 待实现

- [ ] 会话历史清理策略（用户可配置）
- [ ] Function Calling 工具层
- [ ] 更强 PDF/Office 结构化解析（表格 / 公式 / 扫描件）
- [ ] 文档自动分类与自动标签
- [ ] 多模态处理（OCR / 音频转写）
- [ ] 系统级缓存（热门查询 / 文档列表缓存）
- [ ] 定时归档与清理

---
*Let's make knowledge accessible!* 🐾
