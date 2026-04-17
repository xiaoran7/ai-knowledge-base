# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

AI Knowledge Base (AI 知识库) - A personal knowledge management system with RAG-based AI Q&A capabilities. Built with Spring Boot 4 (Java 21) backend and Vue 3 frontend.

## Build & Run Commands

### Backend (Spring Boot)

```bash
# Run backend (from backend directory)
cd backend
mvn spring-boot:run

# Run tests
mvn test

# Or in IDEA: Run BackendApplication.java
```

Backend runs on port 8080. Requires PostgreSQL (localhost:5432) and Redis (localhost:6379) to be running.

### Frontend (Vue 3)

```bash
# Install dependencies (first time)
cd frontend
npm install

# Run frontend dev server
npm run dev

# Build for production
npm run build
```

Frontend runs on port 5173 (or next available port). Proxies `/api` requests to backend at `http://localhost:8080`.

## Architecture

### Technology Stack

**Backend:**
- Java 21, Spring Boot 4.0.5, Spring Security 6
- Spring AI 2.0.0-M4 (for RAG/LLM integration)
- PostgreSQL + pgvector (relational + vector storage)
- Redis (vector store + caching)
- Apache Tika 2.9.1 (document parsing: PDF/Word/Excel/Markdown)
- JWT (jjwt 0.12.3) for authentication

**Frontend:**
- Vue 3.5, Vite 8, TypeScript 6
- Element Plus 2.13 (UI components)
- Pinia 3 (state management), Axios (HTTP)
- Vue Router 5 with auth guard

### Backend Structure

```
backend/src/main/java/com.ai.kb/
├── BackendApplication.java      # Spring Boot entry point
├── config/
│   └── SecurityConfig.java      # JWT + CORS config, PasswordEncoder bean
├── controller/
│   ├── AuthController.java      # /auth/login, /auth/register, /auth/logout
│   ├── KnowledgeBaseController.java  # /knowledge-base CRUD
│   └── GlobalExceptionHandler.java   #统一异常处理,返回纯文本错误消息
├── dto/                         # Request/Response DTOs
├── entity/                      # JPA entities (UUID primary keys)
├── repository/                  # Spring Data JPA repositories
├── service/                     # Business logic layer
└── security/
    ├── JwtTokenProvider.java    # Token generation/validation
    └── JwtAuthenticationFilter.java  # Request filter for JWT
```

### Frontend Structure

```
frontend/src/
├── api/
│   ├── index.ts                 # Axios instance with interceptors
│   ├── auth.ts                  # Auth API calls
│   ├── knowledge.ts             # Knowledge base API
│   ├── document.ts              # Document API (upload/list)
│   └── chat.ts                  # Chat/AI API
├── stores/                      # Pinia stores (user, knowledge)
├── views/                       # Vue components (Login, Register, KnowledgeBase, Layout, Chat, Documents, Dashboard, Settings)
├── router/index.ts              # Vue Router with auth guard
```

### Key Patterns

**Backend:**
- Package path uses `.` notation: `com.ai.kb` (not `com/ai/kb`)
- Entities use UUID primary keys with `@GeneratedValue(strategy = GenerationType.UUID)`
- All responses from controllers return pure data (no wrapper)
- Exceptions throw `IllegalArgumentException` for user-facing errors
- `GlobalExceptionHandler` returns HTTP 400 with plain text error message
- Document upload flow: Tika parses → chunk text → generate embeddings → store in Redis vector store

**Frontend:**
- API interceptor in `api/index.ts` handles errors and shows `ElMessage.error()`
- Views should NOT duplicate error handling (catch block should be empty)
- Store methods handle state updates; views use computed properties
- Vite proxy strips `/api` prefix before forwarding to backend
- Router auth guard: checks `access_token` in localStorage, redirects to `/login` if missing

**Authentication Flow:**
1. Login → backend returns `{ accessToken, refreshToken, expiresIn, user }`
2. Frontend stores tokens in localStorage
3. Axios interceptor adds `Authorization: Bearer <token>` to all requests
4. Backend `JwtAuthenticationFilter` validates token
5. Token invalid → 401 → frontend redirects to /login

## API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/auth/login` | POST | No | Login, returns JWT |
| `/auth/register` | POST | No | Create user |
| `/auth/logout` | POST | No | Logout (client clears token) |
| `/knowledge-base` | GET | Yes | List user's knowledge bases |
| `/knowledge-base` | POST | Yes | Create knowledge base |
| `/knowledge-base/{id}` | DELETE | Yes | Delete knowledge base |

**Note:** `docs/api.md` defines a wrapped response format `{code, message, data}`, but actual implementation returns raw data directly. Error responses are plain text (HTTP 400), not wrapped JSON.

## Development Notes

- User prefers **VibeCoding mode**: AI should generate complete code directly, not just provide guidance
- Backend directory structure uses `com.ai.kb` as folder name (not standard Java package path)
- When creating new entities, follow User.java pattern with UUID id and timestamp fields
- Frontend uses Element Plus components; follow existing page patterns