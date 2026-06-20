-- =============================================================================
-- V1__init.sql  — Initial schema for FinTrack
--
-- Flyway naming convention:  V{version}__{description}.sql
--   V1    = version number (applied in order)
--   __    = double underscore (separator)
--   init  = description
--
-- Rules followed:
--   • BIGSERIAL    → PostgreSQL auto-increment BIGINT primary key
--   • NUMERIC(12,2)→ Exact decimal for money (never float/double)
--   • TIMESTAMPTZ  → Timestamp WITH time zone; stored as UTC in PostgreSQL
--   • DATE         → Date only, no time component (for expense/income dates)
--   • Named constraints → Easier to debug violation errors in logs
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE: users
-- Stores registered user accounts.
-- email is the unique login identifier.
-- password_hash: BCrypt hash — plaintext password is NEVER stored.
-- role: single role per user for simplicity (USER or ADMIN).
-- enabled: soft-disable account without deleting data.
-- ─────────────────────────────────────────────────────────────────────────────
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

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE: categories
-- User-owned categories. Each category belongs to exactly one user.
-- type: either 'INCOME' or 'EXPENSE' — enforced by CHECK constraint.
-- Unique constraint: a user cannot have two categories with the same name + type.
-- ON DELETE CASCADE on user_id: deleting a user removes their categories.
--
-- NOTE: We do NOT cascade delete on categories → expenses/incomes.
--       The service layer blocks category deletion if any transactions use it.
-- ─────────────────────────────────────────────────────────────────────────────
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

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE: expenses
-- Individual expense records.
-- amount: NUMERIC(12,2) supports values up to 9,999,999,999.99.
-- category_id FK has NO cascade — blocks deletion of in-use categories.
-- payment_method: optional free text (UPI, Cash, Card, etc.)
-- ─────────────────────────────────────────────────────────────────────────────
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

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE: incomes
-- Individual income records.
-- category_id is nullable (NULL means uncategorized income).
-- source: free text for income source (e.g. "Freelance Project", "Employer")
-- ─────────────────────────────────────────────────────────────────────────────
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

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE: refresh_tokens
-- Stores long-lived refresh tokens issued on login.
-- token: UUID string — random, unique, unguessable.
-- expires_at: checked on every /auth/refresh call.
-- ON DELETE CASCADE: deleting a user removes all their refresh tokens.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_token       UNIQUE (token)
);

-- =============================================================================
-- INDEXES
-- Why: PostgreSQL uses B-tree indexes by default.
--      Without an index, every query on user_id requires a full table scan.
--      With millions of rows, this goes from milliseconds to seconds.
-- =============================================================================

-- Email lookup on login — used in every authentication check
CREATE INDEX idx_users_email
    ON users (email);

-- Listing a user's categories (dropdown in UI)
CREATE INDEX idx_categories_user_id
    ON categories (user_id);

-- A user's expenses filtered by date range — the most common query pattern
-- Composite: (user_id, expense_date) covers WHERE user_id=? AND expense_date BETWEEN ? AND ?
CREATE INDEX idx_expenses_user_date
    ON expenses (user_id, expense_date);

-- Category breakdown analytics: JOIN expenses ON category_id + GROUP BY category_id
CREATE INDEX idx_expenses_category_id
    ON expenses (category_id);

-- A user's incomes filtered by date range
CREATE INDEX idx_incomes_user_date
    ON incomes (user_id, income_date);

-- Category breakdown for income analytics
CREATE INDEX idx_incomes_category_id
    ON incomes (category_id);

-- Refresh token lookup by token value — called on every /auth/refresh request
CREATE INDEX idx_refresh_tokens_token
    ON refresh_tokens (token);

-- Find all tokens for a user (used during logout to revoke them all)
CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);
