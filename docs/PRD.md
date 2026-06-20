# Product Requirements Document (PRD)
## Personal Expense Tracker & Finance Dashboard

| Field | Value |
|-------|-------|
| **Product Name** | FinTrack (working title) |
| **Document Type** | Product Requirements Document |
| **Version** | 1.0 (MVP) |
| **Status** | Draft |
| **Owner** | *[{Prachi Bhari & Rahul }]* |
| **Last Updated** | June 2026 |
| **Related Docs** | `TRD.md` (Technical Requirements Document) |

---

## 1. Overview

### 1.1 Purpose
This document defines the product vision, scope, users, and functional requirements for **FinTrack**, a personal finance web application that lets individuals record income and expenses, organize them into categories, and understand their financial habits through an analytics dashboard.

The product is being built primarily as a **learning and portfolio project** to demonstrate full-stack engineering competence (Java/Spring Boot backend, React/TypeScript frontend, PostgreSQL, Docker) suitable for internship and placement interviews. The PRD is written as if for a real product so that scope, prioritization, and trade-offs can be discussed confidently in a technical interview setting.

### 1.2 Problem Statement
Most young people have no reliable picture of where their money goes. Bank statements are noisy, spreadsheets require manual discipline, and full-featured finance apps (with bank integrations, budgets, and subscriptions) are heavier than what a student or fresh graduate actually needs.

Users need a **lightweight, fast, visual** way to log transactions and immediately see meaningful summaries — total spend, spend by category, and trends over time — without onboarding friction or recurring cost.

### 1.3 Vision
> A clean, fast, and approachable personal finance dashboard that turns scattered transactions into clear insight, so a student or young professional can answer "where is my money going?" in under ten seconds.

### 1.4 Product Principles
1. **Clarity over completeness** — show the few numbers that matter, not every possible metric.
2. **Speed of entry** — logging a transaction should take seconds, not a form-filling chore.
3. **Privacy by default** — every user only ever sees their own data; no sharing in the MVP.
4. **Maintainable simplicity** — prefer a well-built monolith over premature distributed complexity.

---

## 2. Goals & Success Metrics

### 2.1 Product Goals
| ID | Goal |
|----|------|
| G1 | Allow a user to securely register, log in, and manage a private account. |
| G2 | Allow full CRUD management of expenses, income, and categories. |
| G3 | Provide an at-a-glance dashboard with summary cards and charts. |
| G4 | Enable searching and filtering of transactions by date, category, and amount. |
| G5 | Ship as a containerized, deployable, documented application. |

### 2.2 Learning / Resume Goals
This product doubles as proof of skill. By completion it should demonstrate: full-stack development, JWT-based authentication & authorization, layered REST API design, relational database modelling, Docker containerization, modern React frontend development, and engineering best practices (DTOs, validation, API docs, version control).

### 2.3 Success Metrics (illustrative)
Because this is a portfolio project, "success" is partly qualitative, but measurable targets keep scope honest:

| Metric | Target |
|--------|--------|
| Time to log a single transaction | < 10 seconds |
| Dashboard initial load (warm) | < 2 seconds |
| Core API p95 latency | < 300 ms |
| Backend unit/integration test coverage (service layer) | ≥ 60% |
| Setup-from-clone to running app | Single `docker compose up` |
| Critical user flows working end-to-end | 100% (auth, CRUD, dashboard) |

### 2.4 Non-Goals (Explicitly Out of Scope for MVP)
To protect scope and keep the architecture learnable, the MVP **will not** include: microservices, message queues (Kafka), caching layers (Redis), event-driven architecture, AI/ML features, push/email notifications, payment gateways, multi-currency conversion, bank/Plaid integrations, shared or family accounts, or a native mobile app. These are captured in §9 as future enhancements.

---

## 3. Target Users & Personas

### 3.1 Primary Users
Students, fresh graduates, and young professionals who manage modest, mostly manual finances and want lightweight visibility.

### 3.2 Secondary Users
Freelancers with irregular income and individuals who want simple, private expense tracking.

### 3.3 Personas

**Persona A — "Aisha, the Final-Year Student"**
- 21, lives on an allowance plus a part-time stipend.
- Pain: runs out of money mid-month and doesn't know why.
- Needs: fast entry on a laptop, a clear "this month you spent X on food" view.
- Success: opens the dashboard and immediately sees her top spending category.

**Persona B — "Ravi, the Fresh Graduate"**
- 23, first salaried job, wants to start saving.
- Pain: salary arrives, disappears, no record of the leaks.
- Needs: income vs. expense comparison, monthly trend.
- Success: sees a month-over-month trend chart and identifies a creeping subscription/eating-out habit.

**Persona C — "Meera, the Freelancer"** *(secondary)*
- 27, irregular project income.
- Pain: lumpy cash flow, hard to know a "good" vs "bad" month.
- Needs: income records with sources, expense categorization, net balance.
- Success: filters transactions by date range to reconcile a project period.

---

## 4. User Stories & Acceptance Criteria

User stories are grouped by **epic**. Each story uses the format *As a [user], I want [capability], so that [benefit]*, with testable acceptance criteria (AC). Priority uses **MoSCoW** (Must / Should / Could).

### Epic 1 — Authentication & Account

**US-1.1 (Must) — Registration**
*As a new user, I want to register with email and password, so that I can have a private account.*
- AC1: Given a unique email and a valid password, registration succeeds and returns a success state.
- AC2: Duplicate email returns a clear, non-revealing error.
- AC3: Password must meet minimum strength (≥ 8 chars); weak passwords are rejected with a message.
- AC4: Passwords are never stored in plaintext (hashed server-side).

**US-1.2 (Must) — Login**
*As a registered user, I want to log in, so that I can access my data.*
- AC1: Valid credentials return an authentication token (JWT) and the user is routed to the dashboard.
- AC2: Invalid credentials return a generic "invalid email or password" error (no user enumeration).
- AC3: The token is attached to subsequent API requests automatically.

**US-1.3 (Must) — Session persistence & logout**
*As a logged-in user, I want my session to persist on refresh and to be able to log out.*
- AC1: Refreshing the page keeps me logged in until the token expires.
- AC2: Logout clears the token and redirects to login.
- AC3: Accessing a protected route without a valid token redirects to login.

**US-1.4 (Should) — View profile**
*As a user, I want to see my basic profile (name, email), so that I can confirm my account.*

### Epic 2 — Expense Management

**US-2.1 (Must) — Create expense**
*As a user, I want to add an expense with amount, category, date, and note, so that I can track spending.*
- AC1: Amount is required, positive, and stored with two-decimal precision.
- AC2: Category and date are required; description is optional.
- AC3: On save the expense appears in the transaction list and dashboard totals update.

**US-2.2 (Must) — View / list expenses**
*As a user, I want to see my expenses in a sortable, paginated table.*
- AC1: Most recent first by default.
- AC2: List is paginated (page size ~10–20).
- AC3: I only ever see my own expenses.

**US-2.3 (Must) — Edit expense**
*As a user, I want to edit an existing expense, so that I can correct mistakes.*

**US-2.4 (Must) — Delete expense**
*As a user, I want to delete an expense, with a confirmation step, so that I avoid accidental loss.*

### Epic 3 — Income Management

**US-3.1 (Must) — Create income**
*As a user, I want to record income with amount, source, category, and date.*

**US-3.2 (Must) — List / edit / delete income**
*As a user, I want full CRUD over income records, mirroring expense management.*

### Epic 4 — Category Management

**US-4.1 (Must) — Create category**
*As a user, I want to create categories (e.g., Food, Rent, Salary) with a type (income/expense) and a color, so that I can organize transactions.*
- AC1: Category name is required and unique per user per type.
- AC2: Each category has a type (INCOME or EXPENSE) and an optional color/icon.

**US-4.2 (Must) — List / edit / delete category**
*As a user, I want to manage my categories.*
- AC1: Deleting a category that is in use is either blocked with a message, or reassigns transactions to an "Uncategorized" default (decision recorded in TRD).

**US-4.3 (Should) — Default categories on signup**
*As a new user, I want a starter set of sensible default categories, so that I can begin immediately.*

### Epic 5 — Dashboard & Analytics

**US-5.1 (Must) — Summary cards**
*As a user, I want top-line cards (total income, total expense, net balance, this month's spend), so that I get instant context.*
- AC1: Cards reflect the selected period (default: current month).
- AC2: Net balance = income − expense for the period.

**US-5.2 (Must) — Spending by category chart**
*As a user, I want a pie/donut chart of expenses by category, so that I can see where money goes.*

**US-5.3 (Must) — Trend chart**
*As a user, I want a line/bar chart of income vs. expense over time (e.g., last 6 months), so that I can see trends.*

**US-5.4 (Should) — Period selector**
*As a user, I want to switch the dashboard period (this month, last month, last 6 months, custom range).*

### Epic 6 — Search & Filter

**US-6.1 (Must) — Filter transactions**
*As a user, I want to filter transactions by date range, category, and type.*
- AC1: Filters combine (AND logic) and update the table without a full reload.

**US-6.2 (Should) — Search by text & amount range**
*As a user, I want to search descriptions and filter by min/max amount.*

### Epic 7 — Platform / Non-Functional

**US-7.1 (Must) — Responsive UI**
*As a user, I want the app to work on a laptop and adapt gracefully to smaller screens.*

**US-7.2 (Must) — API documentation**
*As a developer/interviewer, I want interactive API docs (Swagger), so that the API is explorable.*

**US-7.3 (Must) — One-command run**
*As a developer, I want `docker compose up` to start the full stack.*

---

## 5. Functional Requirements (Feature Specification)

### 5.1 Authentication Module
- Email + password registration with server-side validation and password hashing.
- Stateless JWT authentication; token returned on login and sent as a Bearer token.
- Protected routes both on the API (Spring Security filter) and the frontend (route guards).
- Token expiry handling with redirect to login; optional refresh token (see TRD trade-off).

### 5.2 Transactions (Expense & Income)
- Full CRUD for expenses and income.
- Required fields: amount (positive, 2-decimal), date, category; optional: description, payment method (expense), source (income).
- Server-side validation and ownership enforcement (a user can only mutate their own records).
- Pagination and sorting on list endpoints.

### 5.3 Categories
- Full CRUD, scoped per user, typed as INCOME or EXPENSE.
- Color/icon metadata for chart and UI rendering.
- Starter defaults seeded on registration.
- Safe deletion policy (block-if-in-use **or** reassign-to-Uncategorized).

### 5.4 Dashboard & Analytics
- Summary aggregates: total income, total expense, net balance, current-month spend.
- Spending-by-category breakdown (for the donut chart).
- Time-series trend (income vs. expense per month).
- All analytics are computed server-side and scoped to the authenticated user and selected period.

### 5.5 Search & Filter
- Query parameters for date range, category, type, amount range, and free-text description.
- Results paginated and sorted; reflected live in the transaction table.

### 5.6 Documentation & Tooling
- Swagger/OpenAPI UI for the backend.
- Postman collection for manual API testing.
- README with setup, architecture overview, and screenshots.

---

## 6. Non-Functional Requirements (NFRs)

| Category | Requirement |
|----------|-------------|
| **Security** | Hashed passwords (BCrypt), JWT auth, per-user data isolation, input validation, no secrets in source control, CORS configured for the known frontend origin. |
| **Performance** | Core endpoints p95 < 300 ms on a small dataset; dashboard load < 2 s; indexed queries on `user_id` and date columns. |
| **Usability** | Clean dashboard layout, < 10 s to log a transaction, clear empty/error/loading states. |
| **Reliability** | Graceful API error responses with consistent error schema; no unhandled 500s on validation failures. |
| **Maintainability** | Layered architecture, DTO separation, consistent naming, meaningful tests on the service layer. |
| **Portability** | Fully containerized; runs identically via Docker Compose on any host. |
| **Accessibility** | Sufficient color contrast (pastel palette must still meet readable contrast), keyboard-focusable controls, semantic HTML. |
| **Observability** | Sensible application logging; readable startup and error logs. |

---

## 7. UX & Design Direction

### 7.1 Layout (modern SaaS dashboard)
- **Left sidebar:** primary navigation — Dashboard, Transactions, Income, Expenses, Categories, (Profile/Logout at bottom).
- **Top bar:** page title, period/date-range selector, user menu.
- **Main content:** summary stat cards (row) → analytics charts (grid) → transactions table.
- **Responsive:** sidebar collapses to a drawer / icons on narrow screens; cards stack vertically.

### 7.2 Visual style — pastel professional
A calm, approachable aesthetic that still reads as a serious analytics tool.

| Token | Suggested use |
|-------|---------------|
| Cream / off-white | App background |
| Pastel pink | Primary accent / expenses |
| Butter yellow | Secondary accent / highlights |
| Lavender | Tertiary accent / charts |
| Mint | Positive values / income / success |
| Charcoal/slate | Text and high-contrast elements |

Design rules: generous whitespace, rounded cards with soft shadows, one accent per element (avoid rainbow overload), and **contrast-checked** text so pastels never compromise readability. Exact tokens are specified in the TRD frontend section.

### 7.3 Key Screens
1. **Auth** — Register and Login (centered card, minimal).
2. **Dashboard** — summary cards + spending-by-category donut + income/expense trend + recent transactions.
3. **Transactions** — filterable, paginated table with add/edit modal.
4. **Categories** — list + create/edit with color picker.
5. **Profile** — basic account info, logout.

---

## 8. Development Phases (Product Roadmap)

A phased build keeps each milestone demoable — ideal for incremental learning and for showing progression.

| Phase | Theme | Deliverables | Outcome |
|-------|-------|--------------|---------|
| **0** | Foundation | Repo, backend & frontend scaffolds, DB schema, Docker Compose skeleton | App boots, DB connects |
| **1** | Auth | Registration, login, JWT, protected routes, route guards | A user can sign up and reach an empty dashboard |
| **2** | Core CRUD | Categories → Expenses → Income CRUD (API + UI) | A user can record and manage transactions |
| **3** | Dashboard | Summary cards, category breakdown, trend charts | Insightful dashboard |
| **4** | Search & Filter | Query params, filter UI, pagination polish | Powerful transaction views |
| **5** | Hardening | Validation, error states, tests, Swagger, responsive polish | Production-feel quality |
| **6** | DevOps & Deploy | Compose finalization, README, deployment to a free platform | Publicly demoable, resume-ready |

> Build order rationale: **Categories before transactions** (transactions depend on categories), and **CRUD before analytics** (analytics need data to aggregate).

---

## 9. Future Enhancements (Post-MVP Backlog)

Captured to show product thinking and a credible growth path — explicitly **not** in the MVP.

- **Budgets & alerts:** monthly budgets per category with progress indicators.
- **Recurring transactions:** auto-create rent/subscriptions on a schedule.
- **Multi-currency** with conversion.
- **Export/Import:** CSV/PDF export, CSV import from bank statements.
- **Notifications:** email/push for budget overruns (would introduce a notification service).
- **AI insights:** natural-language summaries or auto-categorization (Claude-powered).
- **Bank integration:** read-only transaction sync (Plaid-style).
- **Shared/family accounts** and role-based sharing.
- **Mobile app** (React Native) sharing the same API.
- **Scalability track:** caching (Redis), async processing (queues), and service extraction — the moment the monolith genuinely needs it, not before.

---

## 10. Assumptions, Constraints & Risks

**Assumptions**
- Single-currency per account in the MVP.
- All transaction entry is manual (no imports/integrations).
- Users access primarily via desktop/laptop browsers.

**Constraints**
- Solo developer / learning timeline.
- Free-tier hosting for deployment.
- Tech stack fixed by learning goals (Java 21, Spring Boot 3, React/TS, PostgreSQL).

**Risks & Mitigations**
| Risk | Mitigation |
|------|------------|
| Scope creep into "cool" features | Strict non-goals list (§2.4); features land in §9 backlog. |
| Money handled as floating point → rounding bugs | Use fixed-precision decimal types end-to-end (specified in TRD). |
| Auth implemented insecurely | Follow Spring Security conventions; BCrypt; never log tokens/passwords. |
| Over-engineering for scale | Deliberate monolith; revisit only on real need. |

---

## 11. Glossary

| Term | Definition |
|------|------------|
| **MVP** | Minimum Viable Product — the smallest releasable version delivering core value. |
| **CRUD** | Create, Read, Update, Delete. |
| **JWT** | JSON Web Token — a signed, stateless authentication token. |
| **DTO** | Data Transfer Object — a shape used to move data across API boundaries. |
| **Net balance** | Income minus expense for a period. |
| **Uncategorized** | Fallback category for transactions whose category was removed. |

---

*End of PRD. See `TRD.md` for system architecture, database design, API contracts, and implementation detail.*
