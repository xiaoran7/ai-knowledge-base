---
name: project_status
description: AI 知识库项目当前开发进度和状态
type: project
---

## 项目当前状态（2026-04-14）

### 已完成 ✅

#### 认证模块
- ✅ application.yml 配置（数据库、Redis、JWT）
- ✅ pom.xml 依赖配置（Spring Boot、JWT、Tika 等）
- ✅ BackendApplication 启动类
- ✅ SecurityConfig 安全配置（CORS、CSRF、JWT 过滤器）
- ✅ JwtTokenProvider JWT 工具类（Token 生成/验证）
- ✅ JwtAuthenticationFilter JWT 认证过滤器
- ✅ PasswordEncoder Bean（BCrypt 加密）
- ✅ User 实体类（UUID 主键、时间戳）
- ✅ UserRepository 数据访问层
- ✅ DTO 类（LoginRequest、RegisterRequest、AuthResponse）
- ✅ AuthService 认证业务逻辑（登录/注册）
- ✅ AuthController 登录/注册/登出接口
- ✅ GlobalExceptionHandler 统一异常处理

#### 前端联调
- ✅ API 拦截器（Token 携带、错误处理）
- ✅ Vite 代理配置（/api 转发后端）
- ✅ 登录页面（表单验证、登录跳转）
- ✅ 注册页面（表单验证、错误提示）
- ✅ Layout 布局（侧边栏、顶栏、退出按钮）
- ✅ 路由守卫（未登录跳转登录页）

#### 知识库模块
- ✅ KnowledgeBase 实体类
- ✅ KnowledgeBaseRepository 数据访问层
- ✅ KnowledgeBaseService 业务逻辑（CRUD）
- ✅ KnowledgeBaseController REST API
- ✅ 前端知识库页面（列表、创建、删除）
- ✅ 知识库 Store（状态管理）

### 待开发 ⏳

#### 文档管理
- ⏳ Document 实体类
- ⏳ 文档上传接口
- ⏳ 文档解析（Tika）
- ⏳ 文档列表/删除

#### AI 问答
- ⏳ 向量数据库配置
- ⏳ 文档 Embedding
- ⏳ 相似度搜索
- ⏳ AI 问答接口

#### 其他
- ⏳ Category 分类实体
- ⏳ Dashboard 首页数据展示
- ⏳ Chat 聊天页面

### 技术栈

**后端**：Java 21 + Spring Boot 4 + PostgreSQL + Redis + JWT

**前端**：Vue 3 + Vite + Element Plus + Pinia + Axios

### 开发团队
- 后端：Tang
- 前端：xiaoran

### 开发模式
- VibeCoding：用户不想自己写代码，AI 直接生成完整代码