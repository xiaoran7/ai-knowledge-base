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
## 2026-04-18 进展补记

### 本轮新增完成
- 聊天工具层新增无 LLM 配置时的工具结果降级回复能力。
- 聊天工具扩展了知识库重命名、删除、统计、文档存在性校验、分类重命名与删除。
- 知识库页面现已支持编辑名称和描述，前后端接口链路完整可用。

### 当前优先缺口
- 聊天工具意图识别仍偏规则化，缺少更稳定的 tool registry 与 schema 执行框架。
- 文档任务治理还缺少更好的失败诊断、筛选和历史归档能力。
- 文档解析与预览层对 PDF / Office 的结构化支持仍需继续增强。

## 2026-04-18 进展补记：聊天工具层继续增强

### 本轮新增完成
- 聊天工具层新增文档移动到分类、移出分类的能力。
- 聊天工具层新增按多个文档名批量删除的能力。
- 文档服务层补齐按标题检索、批量改分类、批量删除，形成可复用的服务底座。

### 下一步建议
- 给聊天工具层补更强的歧义消解和执行前确认。
- 继续扩展批量类动作，例如批量标签、批量重命名、批量归档。

## 2026-04-18 进展补记：Retrieval Debug 模块

### 本轮新增完成
- 修复了 Retrieval Debug 在 Redis 向量反序列化上的兼容性问题。
- Retrieval Debug 页面已重做为更完整的调试工作台，包含清晰文案、错误态、命中统计和结果分组。

### 当前剩余缺口
- 还缺少更深的检索调参能力，例如阈值、排序策略、召回来源占比。
- 还没有把“改写前后差异”做成更强的 explainable 对比视图。
## 2026-04-18 工具层补强记录：确认与歧义保护

### 本轮新增完成
- 聊天工具层新增删除确认门槛；删除知识库、删除分类、批量删除文档都需要用户明确说出“确认删除 / 确定删除”类表达才会执行。
- 聊天工具层新增目标歧义检测；当知识库、分类或文档短名称匹配到多个候选时，工具会返回候选列表并要求用户说得更具体。
- 文档服务层新增按知识库枚举全部文档的能力，专门支撑工具层做更稳的批量操作解析。

### 当前价值
- 工具层从“命中就执行”提升为“先确认风险、先消解歧义、再执行”，更接近可代理办事的产品体验。

### 下一步建议
- 继续把规则式确认和匹配抽象成统一的 tool registry / permission / schema 体系。
- 继续补全非破坏性但高频的智能动作，例如批量重命名、批量打标签、跨分类整理建议。

## 2026-04-18 工具层补强记录：状态语义结构化

### 本轮新增完成
- 聊天工具层新增轻量级 `ToolPolicy` 注册信息，用于统一描述工具是否破坏性、是否需要显式确认。
- 删除类工具现在可以返回 `blocked` 状态，语义上表示“不是执行失败，而是在等待确认”。
- 聊天页工具卡片已适配 `blocked` 状态，用户可以直接看出当前是待确认而不是报错。

### 当前价值
- 工具层开始从“分支堆逻辑”转向“带规则的执行层”，后续继续接 schema、registry、permission 会更顺手。

## 2026-04-18 工具层补强记录：执行分发拆段

### 本轮新增完成
- `ChatToolService.execute()` 已拆成知识库、文档、分类三个处理段，降低了后续继续抽 registry / schema 时的改造阻力。
- 前端聊天接口把工具状态收敛成联合类型，`blocked` 状态已经成为明确的产品语义，而不是松散字符串。
- 当前聊天页已通过 `vue-tsc -b` 校验。

### 下一步建议
- 下一轮可以继续把工具定义、权限策略、参数解析抽成独立结构，逐步脱离“规则直接写在 service 里”的模式。

## 2026-04-18 工具层补强记录：工具定义收口

### 本轮新增完成
- 聊天工具层新增统一 `ToolDefinition` 映射，把工具名、标题、策略放进同一处维护。
- 现有工具回写卡片时已经可以通过定义自动解析标题，减少后续新增工具时的重复样板代码。

### 下一步建议
- 继续把参数解析和目标匹配从 `ChatToolService` 主体中拆出去，逐步形成 definition + parser + executor 的结构。
