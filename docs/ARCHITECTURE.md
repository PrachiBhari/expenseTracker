# Architecture — FinTrack

---

## System Overview

FinTrack is a **three-tier web application**: a React single-page app in the browser talks to a Spring Boot REST API over HTTPS, and the API persists data to a PostgreSQL relational database.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Browser (SPA)                                │
│                                                                     │
│   React 19 + TypeScript                                             │
│   Vite 8 · Tailwind CSS v3 · React Router v6                        │
│   Axios (JWT interceptor) · React Hook Form + Zod · Recharts        │
└───────────────────────────┬─────────────────────────────────────────┘
                            │  HTTPS / JSON
                            │  Authorization: Bearer <jwt>
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Spring Boot REST API                            │
│                                                                     │
│   Java 21 · Spring Boot 3.3 · Spring Security 6                     │
│   Spring Data JPA · Hibernate · Flyway · MapStruct · Lombok         │
│   SpringDoc (Swagger UI)                                            │
└───────────────────────────┬─────────────────────────────────────────┘
                            │  JDBC / Hibernate
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL 16                                │
│                                                                     │
│   5 tables: users, categories, expenses, incomes, refresh_tokens    │
│   Schema managed by Flyway (V1__init.sql)                           │
└─────────────────────────────────────────────────────────────────────┘
```

**Auth flow:** the client sends credentials → backend verifies and returns a signed JWT → client stores token in localStorage → every subsequent request carries `Authorization: Bearer <token>` → a Spring Security filter validates the token before the request reaches any controller.

---

## Backend Architecture

The backend follows a **strict layered architecture** with one-directional dependency flow. Each layer has one job.

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────┐
│         Spring Security Filter Chain    │  ← JwtAuthFilter validates token,
│         (runs before DispatcherServlet) │    populates SecurityContext
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│             Controller Layer            │  ← Maps HTTP to Java method calls
│  @RestController, @RequestMapping       │    Validates @RequestBody with @Valid
│  Returns DTOs + HTTP status codes       │    No business logic
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│              Service Layer              │  ← All business logic lives here
│  @Service, @Transactional               │    Ownership checks (user can only
│  Orchestrates repos + mappers           │    access their own data)
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│           Repository Layer              │  ← Data access only
│  extends JpaRepository<Entity, Long>    │    Spring Data generates SQL from
│  Custom @Query for complex filters      │    method names or JPQL annotations
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│             Entity Layer                │  ← JPA-mapped domain objects
│  @Entity, @Table, @Column               │    Extend BaseEntity (id, timestamps)
│  Never serialized directly to clients   │    Business invariants enforced here
└─────────────────────────────────────────┘

        MapStruct Mappers convert Entity ↔ DTO
        at the Controller/Service boundary
```

### Why DTOs instead of exposing entities directly?

1. **Security** — entities may have fields you never want to expose (e.g. `passwordHash`)
2. **Stability** — changing an entity (DB column rename) doesn't break the API contract
3. **Shape control** — response DTOs can flatten, rename, or compute fields

---

## Package Structure (Backend)

```
com.fintrack/
├── FinTrackApplication.java           # Entry point
│
├── config/
│   ├── SecurityConfig.java            # Spring Security rules, CORS, JWT filter wiring
│   ├── JpaAuditingConfig.java         # Enables @CreatedDate / @LastModifiedDate
│   └── OpenApiConfig.java             # Swagger UI configuration
│
├── common/
│   ├── BaseEntity.java                # id, createdAt, updatedAt (inherited by all entities)
│   ├── dto/
│   │   ├── ApiErrorResponse.java      # Standard error envelope returned to clients
│   │   └── PageResponse.java          # Wrapper for paginated list responses
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice — maps all exceptions to HTTP
│   │   ├── ResourceNotFoundException.java # → 404
│   │   ├── DuplicateResourceException.java# → 409
│   │   └── CategoryInUseException.java    # → 409 (can't delete in-use category)
│   └── filter/
│       └── RequestLoggingFilter.java  # Adds request ID to MDC for log tracing
│
├── security/
│   ├── JwtService.java                # Creates, parses, and validates JWTs (JJWT library)
│   ├── JwtAuthFilter.java             # OncePerRequestFilter — validates token per request
│   ├── CustomUserDetailsService.java  # Loads user from DB by email for Spring Security
│   ├── SecurityUtils.java             # Helper to get current user ID from SecurityContext
│   ├── RefreshToken.java              # @Entity for refresh_tokens table
│   ├── RefreshTokenRepository.java
│   └── RefreshTokenCleanupTask.java   # @Scheduled task — purges expired refresh tokens
│
├── user/
│   ├── User.java                      # @Entity
│   ├── Role.java                      # enum USER / ADMIN
│   ├── UserRepository.java
│   ├── UserService.java               # Registration, login, profile
│   ├── AuthController.java            # POST /auth/register, /auth/login, /auth/refresh
│   ├── UserController.java            # GET /users/me
│   └── dto/                           # RegisterRequest, LoginRequest, AuthResponse, UserResponse
│
├── category/
│   ├── Category.java
│   ├── CategoryType.java              # enum INCOME / EXPENSE
│   ├── CategoryRepository.java
│   ├── CategoryService.java           # Blocks deletion of in-use categories
│   ├── CategoryController.java        # GET/POST /categories, GET/PUT/DELETE /categories/{id}
│   ├── CategoryMapper.java            # MapStruct: Category ↔ CategoryResponse
│   └── dto/                           # CategoryRequest, CategoryResponse
│
├── expense/
│   ├── Expense.java
│   ├── ExpenseSpecification.java      # JPA Specifications for dynamic filtering
│   ├── ExpenseRepository.java
│   ├── ExpenseService.java
│   ├── ExpenseController.java
│   ├── ExpenseMapper.java
│   └── dto/                           # ExpenseRequest, ExpenseResponse
│
├── income/
│   └── (mirrors expense package)
│
└── analytics/
    ├── AnalyticsService.java          # Aggregate queries: summary, by-category, trends
    ├── AnalyticsController.java       # GET /analytics/summary, /by-category, /trends
    └── dto/                           # SummaryResponse, CategoryBreakdownItem, TrendPoint
```

---

## Frontend Architecture

```
src/
├── main.tsx                           # React root, mounts AuthProvider + App
├── App.tsx                            # React Router config, lazy-loaded routes
│
├── auth/
│   ├── AuthContext.tsx                # React Context: user, token, login(), logout()
│   ├── useAuth.ts                     # Hook to consume AuthContext
│   ├── authService.ts                 # loginUser(), registerUser() API calls
│   ├── ProtectedRoute.tsx             # Redirects to /login if not authenticated
│   ├── LoginPage.tsx
│   └── RegisterPage.tsx
│
├── lib/
│   ├── api.ts                         # Single Axios instance with interceptors
│   └── utils.ts                       # Shared helpers (formatCurrency, formatDate, etc.)
│
├── types/                             # TypeScript interfaces mirroring API response shapes
│   ├── auth.types.ts
│   ├── expense.types.ts
│   ├── income.types.ts
│   ├── category.types.ts
│   ├── analytics.types.ts
│   └── api.types.ts
│
├── features/
│   ├── dashboard/
│   │   ├── DashboardPage.tsx          # Composes all dashboard widgets
│   │   ├── SummaryCards.tsx           # 3 stat cards: income, expense, balance
│   │   ├── CategoryDonut.tsx          # Recharts PieChart
│   │   ├── TrendChart.tsx             # Recharts BarChart (income vs expense by month)
│   │   ├── periodUtils.ts             # Date range helpers for period selector
│   │   └── hooks/
│   │       ├── useSummary.ts          # GET /analytics/summary
│   │       ├── useCategoryBreakdown.ts# GET /analytics/by-category
│   │       └── useTrends.ts           # GET /analytics/trends
│   │
│   ├── transactions/
│   │   ├── TransactionsPage.tsx       # Orchestrates table + filters + form
│   │   ├── TransactionTable.tsx       # Paginated table with edit/delete actions
│   │   ├── TransactionFilters.tsx     # Date, category, search filters
│   │   └── TransactionForm.tsx        # Add/edit modal (React Hook Form + Zod)
│   │
│   ├── expenses/
│   │   ├── expenseSchema.ts           # Zod schema for expense form validation
│   │   └── hooks/useExpenses.ts       # GET/POST/PUT/DELETE /expenses
│   │
│   ├── income/
│   │   ├── incomeSchema.ts
│   │   └── hooks/useIncomes.ts
│   │
│   └── categories/
│       ├── CategoriesPage.tsx
│       ├── CategoryForm.tsx
│       ├── categorySchema.ts
│       └── hooks/useCategories.ts
│
└── components/
    ├── ui/
    │   ├── Button.tsx
    │   ├── Input.tsx
    │   ├── Card.tsx
    │   ├── Modal.tsx
    │   ├── Select.tsx
    │   ├── Badge.tsx
    │   ├── Skeleton.tsx               # Loading placeholder
    │   ├── EmptyState.tsx
    │   ├── ConfirmDialog.tsx          # "Are you sure?" before delete
    │   ├── ErrorBoundary.tsx          # Class component catching render errors
    │   └── PageLoader.tsx             # Suspense fallback spinner
    └── layout/
        ├── Sidebar.tsx
        ├── Topbar.tsx
        └── DashboardLayout.tsx        # Sidebar + Topbar + <Outlet /> shell
```

---

## Authentication Flow

```
1. User fills Login form
         │
         ▼
2. LoginPage calls loginUser(email, password) via authService.ts
         │
         ▼
3. Axios POST /auth/login
         │
         ▼
4. Backend: finds user → BCrypt.matches() → generates JWT → returns { accessToken, user }
         │
         ▼
5. flushSync(() => login(accessToken, user))
   ├── localStorage.setItem('token', accessToken)
   ├── setToken(accessToken)       → React state update
   └── setUser(user)               → React state update
   flushSync forces these to apply BEFORE navigate() fires
         │
         ▼
6. navigate('/', { replace: true })
         │
         ▼
7. ProtectedRoute checks isAuthenticated → true → renders Dashboard
         │
         ▼
8. Every Axios request thereafter: interceptor reads localStorage
   and adds Authorization: Bearer <token> header automatically
```

**Key detail — why `flushSync`?** React 18+ batches state updates asynchronously. Without `flushSync`, `navigate('/')` fires before `setToken`/`setUser` flush, so `ProtectedRoute` reads `isAuthenticated=false` and redirects straight back to `/login`. `flushSync` forces synchronous state application before navigation.

---

## Routing

```
/login          → LoginPage      (public)
/register       → RegisterPage   (public)
/               → DashboardPage  (protected via ProtectedRoute)
/transactions   → TransactionsPage
/categories     → CategoriesPage
/profile        → ProfilePage
```

All protected routes are wrapped in `<ProtectedRoute>`. If `isAuthenticated` is false and auth is no longer loading, the user is redirected to `/login`. On page refresh, `AuthContext` re-validates the stored token against `GET /users/me` before deciding.

---

## Deployment Architecture

```
GitHub (main branch)
    │
    ├── push → Netlify auto-build
    │          Base dir: Frontend/
    │          npm run build → /dist
    │          Env: VITE_API_BASE_URL=https://expensetracker-jdzs.onrender.com/api/v1
    │          URL: https://fintrack-fin.netlify.app
    │          netlify.toml: all routes → index.html (React Router SPA support)
    │
    └── push → Render auto-build
               Root dir: Backend/
               mvn clean package -DskipTests
               java -jar target/*.jar --server.port=$PORT
               URL: https://expensetracker-jdzs.onrender.com
               │
               └── Render PostgreSQL (internal network, Singapore region)
                   Host: dpg-d8rhotf7f7vs73e2np1g-a
                   DB:   fintrack_jk98
```

**Local development** uses Docker Compose:
- `db` (PostgreSQL) → `backend` (Spring Boot, port 8081 on host) → `frontend` (Nginx, port 80 on host)
- Nginx proxies `/api/` → `backend:8080` so the frontend uses a relative URL (`/api/v1`) with no CORS

---

## Security Design

| Concern | Implementation |
|---|---|
| **Password storage** | BCrypt (cost factor 12) — salted, slow, adaptive |
| **Authentication** | Stateless JWT (HMAC-SHA384), 15-minute access token |
| **Session extension** | Refresh tokens (UUID, 7-day TTL) stored in DB, revocable |
| **Authorization** | Every service method scopes queries to `userId` from SecurityContext |
| **Input validation** | Bean Validation (`@NotNull`, `@DecimalMin`, `@Size`) on all request DTOs |
| **SQL injection** | Impossible — JPA uses parameterized queries, never string concatenation |
| **XSS** | React escapes all content by default; no `dangerouslySetInnerHTML` |
| **CORS** | Restricted to known frontend origin via `CORS_ALLOWED_ORIGINS` env var |
| **Secrets** | JWT secret and DB credentials injected via environment variables only |
| **Error messages** | Auth errors are generic (`"Invalid email or password"`) to prevent user enumeration |
| **Stack traces** | Never exposed to clients — `include-stacktrace: never` in all profiles |

---

## Key Technical Decisions

### 1. Modular monolith over microservices
For the MVP scale (one developer, one domain, simple data model), a monolith is the correct choice. Microservices would add distributed systems complexity (network calls, eventual consistency, service discovery) with no benefit at this scale.

### 2. Package-by-feature over package-by-layer
`com.fintrack.expense` contains the entity, repository, service, controller, and DTOs together — not split across `controllers/`, `services/`, `repositories/`. This makes each feature self-contained and easier to navigate.

### 3. `ddl-auto: validate` instead of `update`
Hibernate's `ddl-auto=update` is tempting but dangerous in production — it can silently drop and recreate columns. `validate` mode makes Hibernate check that the schema matches entities but never touch it. Flyway owns all DDL.

### 4. JPA Specifications for dynamic filtering
The `/expenses` and `/incomes` list endpoints support multiple optional filters (date range, category, search term). JPA Specifications allow these to be composed dynamically with AND logic — no need for a large number of custom `@Query` methods for every filter combination.

### 5. React lazy loading
Each page component is loaded with `React.lazy()` and rendered inside `<Suspense>`. This reduces the initial JavaScript bundle from 774KB to 383KB, meaning faster first paint for users.

### 6. Axios interceptors for auth
Rather than adding the JWT header to every API call manually, a single request interceptor reads from localStorage and attaches it. A single response interceptor handles `401` globally — clears the stale token and redirects to login — so individual hooks don't need to handle auth expiry.
