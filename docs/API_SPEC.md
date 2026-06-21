# API Specification — FinTrack

| Field | Value |
|---|---|
| **Base URL (Production)** | `https://expensetracker-jdzs.onrender.com/api/v1` |
| **Base URL (Local)** | `http://localhost:8080/api/v1` |
| **Auth** | Bearer JWT — `Authorization: Bearer <token>` |
| **Format** | JSON (`Content-Type: application/json`) |
| **Interactive Docs** | `/api/v1/swagger-ui.html` |

---

## Standard Error Response

All errors return this envelope:

```json
{
  "timestamp": "2026-06-20T10:15:30Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "amount must be greater than 0",
  "path": "/api/v1/expenses",
  "fieldErrors": [
    { "field": "amount", "message": "must be greater than 0" }
  ]
}
```

| Status | Meaning |
|---|---|
| `200` | OK |
| `201` | Created |
| `204` | Deleted (No Content) |
| `400` | Validation error |
| `401` | Missing or invalid JWT |
| `403` | Valid JWT but insufficient permission |
| `404` | Resource not found |
| `409` | Conflict (e.g. duplicate email or category name) |

---

## Auth Endpoints

### POST `/auth/register`
Create a new account. Returns JWT immediately — no separate login required.

**Request**
```json
{
  "fullName": "Prachi Bhari",
  "email": "prachi@example.com",
  "password": "mypassword123"
}
```

**Validation**
- `fullName` — required, min 2 characters
- `email` — required, valid email format
- `password` — required, min 8 characters

**Response `201`**
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "fullName": "Prachi Bhari",
    "email": "prachi@example.com",
    "role": "USER"
  }
}
```

**Errors**
- `409` — email already registered

---

### POST `/auth/login`
Authenticate an existing user.

**Request**
```json
{
  "email": "prachi@example.com",
  "password": "mypassword123"
}
```

**Response `200`** — same shape as register

**Errors**
- `401` — `"Invalid email or password"` (generic — intentionally doesn't reveal which field is wrong)

---

### POST `/auth/refresh`
Exchange a refresh token for a new access token.

**Request**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response `200`**
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "new-uuid-here",
  "tokenType": "Bearer"
}
```

**Errors**
- `401` — refresh token expired or not found

---

## User Endpoints

### GET `/users/me` 🔒
Returns the currently authenticated user's profile.

**Response `200`**
```json
{
  "id": 1,
  "fullName": "Prachi Bhari",
  "email": "prachi@example.com",
  "role": "USER"
}
```

---

## Category Endpoints

All endpoints are scoped to the authenticated user — users never see each other's categories.

### GET `/categories` 🔒
List all categories. Filter by type with `?type=EXPENSE` or `?type=INCOME`.

**Query Params**
| Param | Example | Description |
|---|---|---|
| `type` | `EXPENSE` | Filter by `EXPENSE` or `INCOME`. Omit for all. |

**Response `200`**
```json
[
  {
    "id": 1,
    "name": "Food & Dining",
    "type": "EXPENSE",
    "color": "#F8C8DC",
    "icon": "utensils"
  }
]
```

---

### POST `/categories` 🔒
Create a category.

**Request**
```json
{
  "name": "Food & Dining",
  "type": "EXPENSE",
  "color": "#F8C8DC",
  "icon": "utensils"
}
```

**Validation**
- `name` — required, max 80 characters, unique per user per type
- `type` — required, must be `EXPENSE` or `INCOME`
- `color` — optional, max 20 characters (hex color)
- `icon` — optional, max 40 characters (icon name)

**Response `201`** — the created category object

**Errors**
- `409` — category with same name + type already exists for this user

---

### GET `/categories/{id}` 🔒
Get a single category by ID.

**Response `200`** — category object
**Errors** — `404` if not found or doesn't belong to user

---

### PUT `/categories/{id}` 🔒
Update a category.

**Request** — same fields as POST (all optional on update)

**Response `200`** — updated category object

---

### DELETE `/categories/{id}` 🔒
Delete a category. Blocked if any expenses or incomes use it.

**Response `204`** — no body

**Errors**
- `409` — category is in use by existing transactions

---

## Expense Endpoints

### GET `/expenses` 🔒
List expenses with optional filters. Results are paginated, most recent first.

**Query Params**
| Param | Example | Description |
|---|---|---|
| `from` | `2026-06-01` | Start date (inclusive) |
| `to` | `2026-06-30` | End date (inclusive) |
| `categoryId` | `4` | Filter by category |
| `q` | `groceries` | Search in description |
| `page` | `0` | Page number (0-indexed) |
| `size` | `10` | Page size |
| `sort` | `expenseDate,desc` | Sort field and direction |

**Response `200`**
```json
{
  "content": [
    {
      "id": 1,
      "amount": 350.00,
      "category": {
        "id": 4,
        "name": "Food & Dining",
        "color": "#F8C8DC",
        "icon": "utensils"
      },
      "description": "Groceries from D-Mart",
      "expenseDate": "2026-06-18",
      "paymentMethod": "UPI",
      "createdAt": "2026-06-18T09:12:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 29,
  "totalPages": 3,
  "last": false
}
```

---

### POST `/expenses` 🔒
Create an expense.

**Request**
```json
{
  "amount": 350.00,
  "categoryId": 4,
  "description": "Groceries from D-Mart",
  "expenseDate": "2026-06-18",
  "paymentMethod": "UPI"
}
```

**Validation**
- `amount` — required, > 0, max 10 integer digits, 2 decimal places
- `categoryId` — required, must be an EXPENSE category owned by the user
- `expenseDate` — required, format `YYYY-MM-DD`
- `description` — optional, max 255 characters
- `paymentMethod` — optional, max 40 characters

**Response `201`** — the created expense object

---

### GET `/expenses/{id}` 🔒
Get a single expense.

**Response `200`** — expense object
**Errors** — `404` if not found or belongs to another user

---

### PUT `/expenses/{id}` 🔒
Update an expense. Send only the fields you want to change.

**Response `200`** — updated expense object

---

### DELETE `/expenses/{id}` 🔒
Delete an expense.

**Response `204`** — no body

---

## Income Endpoints

Identical structure to expenses. Key differences:
- Field `incomeDate` instead of `expenseDate`
- Field `source` (max 120 chars) instead of `paymentMethod`
- `categoryId` is **optional** (income can be uncategorized)

### GET `/incomes` 🔒
Same query params as `/expenses`.

### POST `/incomes` 🔒
**Request**
```json
{
  "amount": 65000.00,
  "categoryId": 5,
  "source": "TechCorp Pvt Ltd",
  "description": "June salary",
  "incomeDate": "2026-06-30"
}
```

### PUT `/incomes/{id}` 🔒 | DELETE `/incomes/{id}` 🔒
Same pattern as expenses.

---

## Analytics Endpoints

All analytics are scoped to the authenticated user. Date ranges default to the current calendar month when omitted.

### GET `/analytics/summary` 🔒
Total income, expense, and net balance for a period.

**Query Params**
| Param | Example | Description |
|---|---|---|
| `from` | `2026-06-01` | Start date. Defaults to first day of current month. |
| `to` | `2026-06-30` | End date. Defaults to last day of current month. |

**Response `200`**
```json
{
  "from": "2026-06-01",
  "to": "2026-06-30",
  "totalIncome": 50000.00,
  "totalExpense": 23100.00,
  "netBalance": 26900.00
}
```

---

### GET `/analytics/by-category` 🔒
Spending (or income) breakdown per category, sorted by total descending. Drives the donut chart.

**Query Params**
| Param | Example | Description |
|---|---|---|
| `type` | `EXPENSE` | `EXPENSE` (default) or `INCOME` |
| `from` | `2026-06-01` | Start date |
| `to` | `2026-06-30` | End date |

**Response `200`**
```json
[
  { "categoryId": 1, "name": "Housing & Rent", "color": "#FFD6A5", "icon": "home",     "total": 8000.00 },
  { "categoryId": 2, "name": "Food & Dining",  "color": "#F8C8DC", "icon": "utensils", "total": 5800.00 },
  { "categoryId": 3, "name": "Shopping",       "color": "#C8B6E2", "icon": "shopping-bag", "total": 4000.00 }
]
```

---

### GET `/analytics/trends` 🔒
Monthly income vs expense for the last N months. Always returns contiguous months — months with no transactions have `0` values.

**Query Params**
| Param | Example | Description |
|---|---|---|
| `months` | `6` | Number of months to look back (default 6, max 24) |

**Response `200`**
```json
[
  { "month": "2026-01", "income": 0.00,      "expense": 0.00 },
  { "month": "2026-02", "income": 0.00,      "expense": 0.00 },
  { "month": "2026-03", "income": 0.00,      "expense": 0.00 },
  { "month": "2026-04", "income": 60000.00,  "expense": 17700.00 },
  { "month": "2026-05", "income": 53000.00,  "expense": 21300.00 },
  { "month": "2026-06", "income": 50000.00,  "expense": 23100.00 }
]
```

---

## Health Check

### GET `/actuator/health`
Public endpoint for uptime monitoring. No auth required.

**Response `200`**
```json
{ "status": "UP" }
```
