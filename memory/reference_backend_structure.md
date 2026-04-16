---
name: backend_structure
description: 后端目录结构和开发顺序参考
type: reference
---

## 后端目录结构

```
backend/
├── pom.xml
└── src/main/
    ├── java/com.ai.kb/
    │   ├── BackendApplication.java      ✅ 启动类
    │   ├── config/
    │   │   └── SecurityConfig.java      ✅ 安全配置
    │   ├── controller/
    │   │   ├── AuthController.java      ✅ 认证接口
    │   │   ├── KnowledgeBaseController.java ✅ 知识库接口
    │   │   └── GlobalExceptionHandler.java  ✅ 异常处理
    │   ├── dto/
    │   │   ├── LoginRequest.java        ✅
    │   │   ├── RegisterRequest.java     ✅
    │   │   ├── AuthResponse.java        ✅
    │   │   ├── KnowledgeBaseRequest.java ✅
    │   │   └── KnowledgeBaseResponse.java ✅
    │   ├── entity/
    │   │   ├── User.java                ✅ 用户实体
    │   │   └── KnowledgeBase.java       ✅ 知识库实体
    │   ├── repository/
    │   │   ├── UserRepository.java      ✅
    │   │   └── KnowledgeBaseRepository.java ✅
    │   ├── service/
    │   │   ├── AuthService.java         ✅
    │   │   └── KnowledgeBaseService.java ✅
    │   └── security/
    │   │   ├── JwtTokenProvider.java    ✅
    │   │   └── JwtAuthenticationFilter.java ✅
    └── resources/
        └── application.yml              ✅ 配置文件
```

## 已完成的 API 接口

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 登录 | `/auth/login` | POST | 返回 JWT Token |
| 注册 | `/auth/register` | POST | 创建用户 |
| 登出 | `/auth/logout` | POST | 清除 Token |
| 知识库列表 | `/knowledge-base` | GET | 获取用户知识库 |
| 创建知识库 | `/knowledge-base` | POST | 创建新知识库 |
| 删除知识库 | `/knowledge-base/{id}` | DELETE | 删除知识库 |

## 开发顺序（后续）

1. Document 实体 + Repository
2. 文档上传接口（multipart/form-data）
3. Tika 文档解析
4. 向量数据库配置
5. Embedding + 相似度搜索
6. AI 问答接口