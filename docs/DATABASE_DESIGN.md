# Database Design — FinTrack

| Field | Value |
|---|---|
| **Database** | PostgreSQL 16 |
| **Migration tool** | Flyway |
| **Schema file** | `Backend/src/main/resources/db/migration/V1__init.sql` |

---

## Design Decisions

### Why PostgreSQL?
Relational data (users own categories, categories classify transactions) is a natural fit. ACID compliance ensures financial data is never partially written. PostgreSQL specifically because it is the industry standard for serious applications and is free.

### Why NUMERIC(12,2) for money?
`FLOAT` and `DOUBLE` are binary floating-point types — they cannot represent most decimal fractions exactly. `0.1 + 0.2` in a float gives `0.30000000000000004`. For financial data this is unacceptable. `NUMERIC(12,2)` is an exact decimal type: 12 total digits, exactly 2 decimal places. Maximum storable value: `9,999,999,999.99`.

### Why separate `expenses` and `incomes` tables?
They have different fields (`payment_method` vs `source`) and are always queried independently. A unified `transactions` table would need nullable columns for type-specific fields, which is messier. The trade-off is that analytics queries (`UNION ALL`) are slightly more verbose — worth it for clarity.

### Why Flyway for migrations?
`ddl-auto=update` (Hibernate auto-DDL) is dangerous in production — it can silently alter or drop columns. Flyway version-controls every schema change as a SQL file, making it reproducible, reviewable in PRs, and safe in production. Once applied, a migration is never run again.

### Why BIGSERIAL (auto-increment) instead of UUID?
Simpler, smaller (8 bytes vs 16), and sequential — better for B-tree index performance. UUIDs are better when IDs must be non-guessable across distributed systems. The MVP uses BIGINT PKs; UUIDs would be the upgrade path if the API became public.

---

## Entity-Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                              users                                  │
│  id (PK) │ email (UK) │ password_hash │ full_name │ role │ enabled  │
└──────────┬──────────────────────────────────────────────────────────┘
           │ 1
           │
           ├──────────────── owns ─────────────────────────────────────┐
           │                                                           │
           │ N                                                         │ N
┌──────────┴────────────────────────────┐         ┌───────────────────┴────────────────────┐
│            categories                 │         │              expenses                   │
│  id │ user_id (FK) │ name │ type      │         │  id │ user_id (FK) │ category_id (FK)  │
│     │              │      │ color     │         │     │              │ amount             │
│     │              │      │ icon      │         │     │              │ description        │
└──────────┬────────────────────────────┘         │     │              │ expense_date       │
           │ 1                                    │     │              │ payment_method     │
           │                                      └───────────────────────────────────────┘
           │ N
┌──────────┴────────────────────────────┐
│              incomes                  │
│  id │ user_id (FK) │ category_id (FK) │
│     │              │ amount           │
│     │              │ source           │
│     │              │ description      │
│     │              │ income_date      │
└───────────────────────────────────────┘

All tables also have: created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**Relationship rules:**
- `users` → `categories`: one-to-many, ON DELETE CASCADE (delete user → delete their categories)
- `users` → `expenses`: one-to-many, ON DELETE CASCADE
- `users` → `incomes`: one-to-many, ON DELETE CASCADE
- `categories` → `expenses`: one-to-many, **NO cascade** — service layer blocks deletion of categories that are in use
- `categories` → `incomes`: same as above
- `incomes.category_id` is **nullable** — income can be uncategorized

---

## Table Definitions

### `users`
```sql
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(120),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);
```

| Column | Notes |
|---|---|
| `email` | Login identifier. Unique constraint ensures no duplicate accounts. |
| `password_hash` | BCrypt hash. Plaintext password is never stored anywhere. |
| `role` | `USER` or `ADMIN`. MVP only uses `USER`. |
| `enabled` | Soft-disable without deleting data. Currently always `true`. |

---

### `categories`
```sql
CREATE TABLE categories (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    name       VARCHAR(80) NOT NULL,
    type       VARCHAR(10) NOT NULL,
    color      VARCHAR(20),
    icon       VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_categories_user         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_category_user_name_type UNIQUE (user_id, name, type),
    CONSTRAINT chk_category_type          CHECK (type IN ('INCOME', 'EXPENSE'))
);
```

| Column | Notes |
|---|---|
| `type` | CHECK constraint enforces only `INCOME` or `EXPENSE` — no invalid values possible at the DB level. |
| `color` | Hex string e.g. `#F8C8DC`. Used for chart rendering. |
| `icon` | Icon name string e.g. `utensils`. Maps to a Lucide icon in the frontend. |
| Unique `(user_id, name, type)` | A user can have "Food" as both INCOME and EXPENSE, but not two EXPENSE categories named "Food". |

---

### `expenses`
```sql
CREATE TABLE expenses (
    id             BIGSERIAL      PRIMARY KEY,
    user_id        BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL,
    description    VARCHAR(255),
    expense_date   DATE           NOT NULL,
    payment_method VARCHAR(40),
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_expenses_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_expense_amount   CHECK (amount > 0)
);
```

| Column | Notes |
|---|---|
| `amount` | `NUMERIC(12,2)` — exact decimal, never float. CHECK ensures positive values. |
| `expense_date` | `DATE` type — date only, no time. Users enter the calendar date, not a timestamp. |
| `payment_method` | Free text: UPI, Cash, Card, Net Banking, etc. |
| FK `category_id` | No ON DELETE CASCADE — intentional. The service blocks category deletion if this FK would be violated. |

---

### `incomes`
```sql
CREATE TABLE incomes (
    id          BIGSERIAL      PRIMARY KEY,
    user_id     BIGINT         NOT NULL,
    category_id BIGINT,
    amount      NUMERIC(12, 2) NOT NULL,
    source      VARCHAR(120),
    description VARCHAR(255),
    income_date DATE           NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_incomes_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_incomes_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_income_amount   CHECK (amount > 0)
);
```

| Column | Notes |
|---|---|
| `category_id` | **Nullable** — income can be uncategorized. |
| `source` | Where the money came from: employer name, client name, etc. |

---

### `refresh_tokens`
```sql
CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_token       UNIQUE (token)
);
```

| Column | Notes |
|---|---|
| `token` | UUID string. Unique, random, unguessable. Checked on every `/auth/refresh` call. |
| `expires_at` | 7-day TTL. Expired tokens are rejected. A background job (`RefreshTokenCleanupTask`) purges expired rows. |

---

## Indexes

```sql
-- Email lookup on every login
CREATE INDEX idx_users_email           ON users (email);

-- Loading a user's category dropdown
CREATE INDEX idx_categories_user_id    ON categories (user_id);

-- Most common query: a user's expenses in a date range
-- Composite covers WHERE user_id=? AND expense_date BETWEEN ? AND ?
CREATE INDEX idx_expenses_user_date    ON expenses (user_id, expense_date);

-- Category breakdown analytics: GROUP BY category_id
CREATE INDEX idx_expenses_category_id  ON expenses (category_id);

-- Same patterns for income
CREATE INDEX idx_incomes_user_date     ON incomes (user_id, income_date);
CREATE INDEX idx_incomes_category_id   ON incomes (category_id);

-- Refresh token lookup on every /auth/refresh call
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
```

**Why these indexes?** Without indexes, PostgreSQL does a full table scan for every query — fine at 100 rows, very slow at 100,000. `(user_id, expense_date)` is a composite index that covers the most common query pattern (all of a user's transactions in a date range) with a single index scan.

---

## Auditing

`created_at` and `updated_at` are automatically managed by Spring Data JPA auditing (`@EntityListeners(AuditingEntityListener.class)` on `BaseEntity`). No need to set them manually — they're populated on insert and updated on every save.

---

## Migration Strategy

| File | Description |
|---|---|
| `V1__init.sql` | Full initial schema — all 5 tables + all indexes |

Adding a new column in the future: create `V2__add_column_name.sql`. Flyway runs it exactly once and records it in `flyway_schema_history`. The `ddl-auto: validate` setting in JPA then verifies that Hibernate's entity model matches the actual DB schema on every startup.
