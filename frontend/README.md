# 前端开发日志 🎨

**开发者**: xiaoran 🔆  
**开始日期**: 2026-04-08  
**技术栈**: Vue 3 + Vite + TypeScript + Element Plus

---

## 📋 开发进度

### ✅ 阶段 1: 项目初始化 (2026-04-08)

**完成内容:**

1. **项目创建**
   - 使用 Vite 创建 Vue 3 + TypeScript 项目
   - 安装核心依赖

2. **依赖安装**
   ```json
   {
     "vue": "^3.4.x",
     "vue-router": "^4.2.x",
     "pinia": "^2.1.x",
     "element-plus": "^2.4.x",
     "axios": "^1.6.x",
     "markdown-it": "^14.x",
     "@element-plus/icons-vue": "^2.x"
   }
   ```

3. **目录结构**
   ```
   frontend/
   ├── src/
   │   ├── api/              # API 封装
   │   │   ├── index.ts      # Axios 实例 + 拦截器
   │   │   ├── auth.ts       # 用户认证接口
   │   │   ├── knowledge.ts  # 知识库接口
   │   │   ├── document.ts   # 文档接口
   │   │   └── chat.ts       # AI 问答接口
   │   ├── stores/           # Pinia 状态管理
   │   │   ├── user.ts       # 用户状态
   │   │   └── knowledge.ts  # 知识库状态
   │   ├── router/           # 路由配置
   │   │   └── index.ts      # 路由守卫 + 页面路由
   │   ├── views/            # 页面组件
   │   │   ├── Layout.vue    # 布局 (侧边栏 + 顶栏)
   │   │   ├── Login.vue     # 登录页
   │   │   ├── Register.vue  # 注册页
   │   │   ├── Dashboard.vue # 首页
   │   │   ├── KnowledgeBase.vue  # 知识库管理
   │   │   ├── Chat.vue      # AI 问答页
   │   │   ├── Documents.vue # 文档管理页
   │   │   └── Settings.vue  # 设置页
   │   ├── components/       # 可复用组件 (待开发)
   │   ├── styles/           # 全局样式
   │   │   └── main.css
   │   ├── utils/            # 工具函数 (待开发)
   │   ├── assets/           # 静态资源
   │   ├── App.vue           # 根组件
   │   └── main.ts           # 入口文件
   ├── public/
   ├── package.json
   ├── vite.com.ai.kb.config.ts        # Vite 配置 (代理/API)
   └── tsconfig.json
   ```

4. **核心配置**
   - `vite.com.ai.kb.config.ts`: 配置了 `/api` 代理到 `http://localhost:8080`
   - `main.ts`: 注册了 Element Plus、Pinia、Router、图标
   - `router/index.ts`: 实现了登录守卫 (requiresAuth)

5. **API 封装**
   - 统一的 Axios 实例
   - 请求拦截器 (自动添加 Token)
   - 响应拦截器 (错误处理 + 401 跳转)
   - 封装方法：`get`, `post`, `put`, `del`, `upload`

6. **状态管理 (Pinia)**
   - `user.ts`: 用户登录状态、Token 管理
   - `knowledge.ts`: 知识库列表、当前知识库、分类列表

7. **页面开发**
   - ✅ `Layout.vue` - 侧边栏导航 + 顶栏用户信息
   - ✅ `Login.vue` - 登录表单 + 表单验证
   - ✅ `Register.vue` - 注册表单 + 密码确认验证
   - ✅ `Dashboard.vue` - 数据统计卡片 + 快速开始引导
   - ✅ `KnowledgeBase.vue` - 知识库列表表格
   - ✅ `Chat.vue` - 对话列表 + 聊天界面 + Markdown 渲染
   - ✅ `Documents.vue` - 文档列表 + 上传 + 筛选 + 分页
   - ✅ `Settings.vue` - 个人信息 + AI 设置

---

## 🚧 待开发内容

### 阶段 2: 组件细化
- [ ] `DocumentUpload.vue` - 独立的文档上传组件 (拖拽 + 进度条)
- [ ] `ChatBox.vue` - 可复用的聊天框组件
- [ ] `DocumentList.vue` - 可复用的文档列表组件
- [ ] `CategoryTree.vue` - 分类树形选择器

### 阶段 3: 功能完善
- [ ] 流式输出支持 (SSE)
- [ ] 对话历史加载
- [ ] 文档预览功能
- [ ] 知识库创建/编辑表单
- [ ] 分类管理 (增删改)

### 阶段 4: 优化
- [ ] 响应式布局 (移动端适配)
- [ ] 加载状态优化
- [ ] 错误边界处理
- [ ] 性能优化 (虚拟滚动、懒加载)

---

## 🔧 开发命令

```bash
# 进入项目目录
cd D:\aworkspace\ai-knowledge-base\frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build

# 预览构建结果
npm run preview
```

---

## 📝 接口对接说明

**后端需要实现的接口 (详见 `/docs/api.md`):**

### 用户模块
- `POST /api/auth/login` - 登录
- `POST /api/auth/register` - 注册
- `POST /api/auth/logout` - 登出
- `GET /api/user/me` - 获取用户信息

### 知识库模块
- `GET /api/knowledge-base` - 获取知识库列表
- `POST /api/knowledge-base` - 创建知识库
- `GET /api/knowledge-base/{id}/categories` - 获取分类列表
- `POST /api/knowledge-base/{id}/categories` - 创建分类

### 文档模块
- `POST /api/documents/upload` - 上传文档
- `GET /api/documents` - 获取文档列表
- `GET /api/documents/{id}` - 获取文档详情
- `DELETE /api/documents/{id}` - 删除文档

### AI 问答模块
- `POST /api/ai/chat` - AI 问答 (支持流式)
- `GET /api/conversations` - 获取对话列表
- `GET /api/conversations/{id}` - 获取对话详情
- `DELETE /api/conversations/{id}` - 删除对话

---

## 🐾 开发笔记

1. **Element Plus 图标**: 使用 `@element-plus/icons-vue`，已全局注册所有图标
2. **Markdown 渲染**: 使用 `markdown-it`，在 Chat 页面已集成
3. **文件上传**: 使用 Element Plus 的 `el-upload` 组件，自定义 `http-request`
4. **路由守卫**: 已实现基于 Token 的登录验证
5. **API 代理**: Vite 配置了 `/api` 代理到后端 `localhost:8080`

---

## 📞 联系后端 (Tang)

**需要确认的事项:**
1. 接口路径是否按照 `/docs/api.md` 实现？
2. 流式输出用 SSE 还是 WebSocket？(目前前端按 SSE 实现)
3. 文件上传的最大限制是多少？
4. 用户密码的加密方式？(前端传输用明文，后端 BCrypt)

---

*最后更新：2026-04-08*  
*前端脚手架已完成，可以开始对接后端接口喵！* 🐾
