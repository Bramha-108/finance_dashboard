
# Finance Dashboard Backend

A production-ready REST API for managing personal and organizational financial records
with role-based access control, JWT authentication, analytics, and rate limiting.
Built with Spring Boot 3.5 following a modular monolith architecture.

---

## Table of Contents

- Architecture
- Tech Stack
- Project Structure
- Database Schema
- Getting Started
- Environment Variables
- Running Tests
- API Reference
- Roles and Permissions
- Security Model
- Design Decisions
- Known Limitations

---

## Architecture

This project uses a **modular monolith** — one deployable Spring Boot application
organized into self-contained domain modules with strict package-level boundaries.

HTTP Request

↓
RateLimitFilter
- Rejects /api/auth/** after 5 req/min per IP

↓
JwtAuthFilter
- Validates Bearer token
- Sets SecurityContext

↓
SecurityFilterChain
- Role checks via @PreAuthorize

↓
Controller
- HTTP only: parse request, call one service

↓
Service
- All business logic and ownership rules

↓
Repository
- JPA + JPQL queries
- Specifications for filters

↓
PostgreSQL

### Module boundaries

auth/
- Users
- Roles
- JWT tokens
- Refresh tokens

records/
- Financial records CRUD
- Filtering

dashboard/
- Aggregation queries
- Analytics

shared/
- Security filters
- Exception handler
- Utilities

The rule enforced throughout: `records` and `dashboard` may read from `auth`
repositories to resolve user context. `auth` never imports from `records` or
`dashboard`. `shared` never imports from any domain module.

---

## Tech Stack

| Layer              | Technology                        |
|--------------------|-----------------------------------|
| Language           | Java 17                           |
| Framework          | Spring Boot 3.5                   |
| Security           | Spring Security 6 (stateless JWT) |
| Persistence        | Spring Data JPA + Hibernate 6     |
| Database           | PostgreSQL 14+                    |
| Migrations         | Flyway                            |
| JWT Library        | jjwt 0.12.3                       |
| DTO Mapping        | MapStruct 1.5.5                   |
| Boilerplate        | Lombok                            |
| Rate Limiting      | Bucket4j 8.7.0                    |
| API Documentation  | SpringDoc OpenAPI 2.3.0           |
| Testing DB         | H2 (in-memory, test scope only)   |
| Testing Framework  | JUnit 5 + MockMvc                 |
| Build Tool         | Maven                             |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/finance/dashboard/
│   │   ├── DashboardApplication.java
│   │
│   │   ├── auth/
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java              (POST /api/auth/**)
│   │   │   │   └── UserController.java              (GET/PATCH/POST/DELETE /api/users/**)
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java                 (register, login, refresh, logout)
│   │   │   │   └── UserService.java                 (CRUD, role assign/revoke)
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── RoleRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   ├── model/
│   │   │   │   ├── User.java
│   │   │   │   ├── Role.java
│   │   │   │   ├── RefreshToken.java
│   │   │   │   ├── RoleName.java                    (ROLE_VIEWER, ROLE_ANALYST, ROLE_ADMIN)
│   │   │   │   └── UserStatus.java                  (ACTIVE, INACTIVE)
│   │   │   └── dto/
│   │   │       ├── RegisterRequest.java
│   │   │       ├── LoginRequest.java
│   │   │       ├── TokenResponse.java
│   │   │       └── UserResponse.java
│   │
│   │   ├── records/
│   │   │   ├── controller/
│   │   │   │   └── FinancialRecordController.java
│   │   │   ├── service/
│   │   │   │   └── FinancialRecordService.java
│   │   │   ├── repository/
│   │   │   │   ├── FinancialRecordRepository.java   (JPQL aggregation queries)
│   │   │   │   └── RecordSpecification.java         (JPA Specification filters)
│   │   │   ├── model/
│   │   │   │   ├── FinancialRecord.java
│   │   │   │   ├── RecordType.java                  (INCOME, EXPENSE)
│   │   │   │   └── RecordCategory.java              (SALARY, RENT, FOOD, UTILITIES, INVESTMENT, OTHER)
│   │   │   └── dto/
│   │   │       ├── RecordRequest.java
│   │   │       ├── RecordResponse.java
│   │   │       └── RecordFilterRequest.java
│   │
│   │   ├── dashboard/
│   │   │   ├── controller/
│   │   │   │   └── DashboardController.java
│   │   │   ├── service/
│   │   │   │   └── DashboardService.java
│   │   │   └── dto/
│   │   │       ├── DashboardSummaryResponse.java
│   │   │       └── TrendPoint.java
│   │
│   │   └── shared/
│   │       ├── config/
│   │       │   ├── SecurityConfig.java
│   │       │   └── OpenApiConfig.java
│   │       ├── security/
│   │       │   ├── JwtAuthFilter.java
│   │       │   ├── JwtService.java
│   │       │   ├── AppUserDetails.java
│   │       │   ├── AppUserDetailsService.java
│   │       │   └── RateLimitFilter.java
│   │       ├── exception/
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   ├── ResourceNotFoundException.java
│   │       │   └── ErrorResponse.java
│   │       ├── dto/
│   │       │   └── PagedResponse.java
│   │       └── util/
│   │           └── SecurityUtils.java
│   │
│   └── resources/
│       ├── application.yml
│       └── db/migration/
│           ├── V1__init_users_roles.sql
│           └── V2__financial_records.sql
│
└── test/
    ├── java/com/finance/dashboard/
    │   ├── auth/controller/
    │   │   └── AuthControllerTest.java              (11 tests)
    │   ├── records/service/
    │   │   └── FinancialRecordServiceTest.java      (10 tests)
    │   ├── dashboard/service/
    │   │   └── DashboardServiceTest.java            (8 tests)
    │   ├── DashboardApplicationTests.java           (1 test)
    │   └── shared/
    │       ├── BaseIntegrationTest.java
    │       ├── TestDataSeeder.java
    │       └── TokenHelper.java
    │
    └── resources/
        └── application.yml                          (H2 config, Flyway disabled)
        
```
---

## Database Schema
```
users (id, email, password, full_name, status, created_at)
roles (id, name)
user_roles (user_id → users.id, role_id → roles.id)

financial_records (
  id, created_by → users.id, amount,
  type [INCOME | EXPENSE],
  category [SALARY, RENT, ...],
  date, notes, deleted, created_at, updated_at
)

refresh_tokens (
  id, user_id → users.id,
  token, expires_at, created_at
)
```

Indexes on financial_records: idx_records_user (created_by) idx_records_date (date) idx_records_type (type) idx_records_deleted (deleted)


---

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL 14 or higher (or Docker)
- Maven 3.8 or higher

### Option A — Docker (fastest)
```bash
# Clone the repo
git clone <repo-url>
cd dashboard

# Start PostgreSQL
docker-compose up -d

# Set the required JWT secret
export JWT_SECRET=my-very-long-secret-key-at-least-32-characters-long

# Run the application
./mvnw spring-boot:run
```

### Option B — Local PostgreSQL
```bash
# Create the database
psql -U postgres -c "CREATE DATABASE finance_db;"

# Export environment variables
export DB_URL=jdbc:postgresql://localhost:5432/finance_db
export DB_USER=postgres
export DB_PASSWORD=secret
export JWT_SECRET=my-very-long-secret-key-at-least-32-characters-long

# Run
./mvnw spring-boot:run
```

The application starts on **port 8080**. Flyway automatically applies
all migrations on startup. No manual schema setup is needed.

### Promote the first admin

New users get VIEWER by default. Promote yourself to ADMIN directly in the DB:
```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'your@email.com'
  AND r.name = 'ROLE_ADMIN';
```

Then log in again to get a fresh token with the ADMIN role baked in.

### docker-compose.yml
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: finance_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
volumes:
  postgres_data:
```

---

## Environment Variables

| Variable      | Default                                     | Required | Description                              |
|---------------|---------------------------------------------|----------|------------------------------------------|
| JWT_SECRET    | —                                           | Yes      | HMAC signing key. Minimum 32 characters. |
| DB_URL        | jdbc:postgresql://localhost:5432/finance_db | No       | Full JDBC connection string              |
| DB_USER       | postgres                                    | No       | Database username                        |
| DB_PASSWORD   | secret                                      | No       | Database password                        |

---

## Running Tests

Tests use H2 in-memory database. No PostgreSQL needed.
```bash
# Run all 30 tests
./mvnw test

# Run one test class
./mvnw test -Dtest=AuthControllerTest
./mvnw test -Dtest=FinancialRecordServiceTest
./mvnw test -Dtest=DashboardServiceTest
```

### Test coverage

| Class                       | Tests | What is verified                                    |
|-----------------------------|-------|-----------------------------------------------------|
| AuthControllerTest          | 11    | Register, login, refresh, validation, inactive user |
| FinancialRecordServiceTest  | 10    | CRUD, ownership isolation, soft delete, filters     |
| DashboardServiceTest        | 8     | Aggregation totals, scoping, trend, access control  |
| DashboardApplicationTests   | 1     | Context loads                                       |

---

## API Reference

Full interactive documentation available at:
````

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

````

### Authentication

| Method | Endpoint            | Auth     | Description                        |
|--------|---------------------|----------|------------------------------------|
| POST   | /api/auth/register  | Public   | Register — returns JWT immediately |
| POST   | /api/auth/login     | Public   | Login — returns JWT + refresh token|
| POST   | /api/auth/refresh   | Public   | Swap refresh token for new tokens  |

### User Management — ADMIN only

| Method | Endpoint                      | Description              |
|--------|-------------------------------|--------------------------|
| GET    | /api/users                    | List all users           |
| PATCH  | /api/users/{id}/status        | Activate or deactivate   |
| POST   | /api/users/{id}/roles         | Assign role              |
| DELETE | /api/users/{id}/roles/{role}  | Revoke role              |

### Financial Records

| Method | Endpoint          | Roles            | Description                           |
|--------|-------------------|------------------|---------------------------------------|
| GET    | /api/records      | ALL              | List — paginated and filterable       |
| POST   | /api/records      | ANALYST, ADMIN   | Create new record                     |
| GET    | /api/records/{id} | ALL              | Get one — own or admin                |
| PUT    | /api/records/{id} | ANALYST, ADMIN   | Update — own or admin                 |
| DELETE | /api/records/{id} | ANALYST, ADMIN   | Soft delete — own or admin            |

#### Query parameters for GET /api/records

| Parameter | Type   | Example    | Description                     |
|-----------|--------|------------|---------------------------------|
| from      | date   | 2025-04-01 | Start of date range (ISO 8601)  |
| to        | date   | 2025-04-30 | End of date range               |
| type      | enum   | INCOME     | INCOME or EXPENSE               |
| category  | enum   | SALARY     | See categories below            |
| page      | int    | 0          | Zero-based page index           |
| size      | int    | 20         | Page size — max 100             |

#### Record categories

`SALARY` `RENT` `FOOD` `UTILITIES` `INVESTMENT` `OTHER`

#### Paginated response shape
```json
{
  "content": [
    {
      "id": "uuid",
      "amount": 1500.00,
      "type": "INCOME",
      "category": "SALARY",
      "date": "2025-04-01",
      "notes": "April salary",
      "createdBy": "user@example.com",
      "createdAt": "2025-04-01T09:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

### Dashboard Analytics — ANALYST and ADMIN

| Method | Endpoint                  | Query Params | Description                         |
|--------|---------------------------|--------------|-------------------------------------|
| GET    | /api/dashboard/summary    | from, to     | Full snapshot — income, expense, net|
| GET    | /api/dashboard/categories | from, to     | Totals grouped by category          |
| GET    | /api/dashboard/trend      | since        | Monthly series for income and expense|
| GET    | /api/dashboard/recent     | —            | 10 most recent records              |

#### Summary response shape
```json
{
  "totalIncome":   4000.00,
  "totalExpenses":  800.00,
  "netBalance":    3200.00,
  "byCategory": {
    "SALARY":     3000.00,
    "INVESTMENT": 1000.00,
    "RENT":        800.00
  },
  "monthlyTrend": [
    { "year": 2025, "month": 3, "type": "INCOME",  "amount": 1000.00 },
    { "year": 2025, "month": 4, "type": "INCOME",  "amount": 3000.00 },
    { "year": 2025, "month": 4, "type": "EXPENSE", "amount":  800.00 }
  ],
  "recentActivity": [ ]
}
```

### Error response shape

Every error follows this structure:
```json
{
  "code":    "VALIDATION_FAILED",
  "message": "Input validation error",
  "details": {
    "amount": "must be greater than 0",
    "date":   "must not be null"
  }
}
```

| Status | Code                | When                                         |
|--------|---------------------|----------------------------------------------|
| 400    | VALIDATION_FAILED   | Bean validation failed on request body       |
| 400    | BAD_REQUEST         | Business rule violation (duplicate email etc)|
| 401    | UNAUTHORIZED        | Missing, invalid, or expired JWT             |
| 403    | FORBIDDEN           | Valid JWT but insufficient role or ownership |
| 404    | NOT_FOUND           | Resource not found or soft-deleted           |
| 429    | RATE_LIMIT_EXCEEDED | More than 5 auth requests per minute per IP  |
| 500    | INTERNAL_ERROR      | Unexpected server error                      |

---

## Roles and Permissions

New users are assigned VIEWER by default after registration.
An ADMIN must explicitly promote them. The first admin must be
created via a direct database insert (see Getting Started).

| Action                    | VIEWER | ANALYST     | ADMIN       |
|---------------------------|--------|-------------|-------------|
| Register / Login          | Public | Public      | Public      |
| View own records          | Yes    | Yes         | Yes         |
| View all records          | No     | No          | Yes         |
| Create records            | No     | Yes         | Yes         |
| Edit own records          | No     | Yes         | Yes         |
| Delete own records        | No     | Yes         | Yes         |
| Edit or delete any record | No     | No          | Yes         |
| Dashboard analytics       | No     | Own data    | All data    |
| Manage users              | No     | No          | Yes         |

---

## Security Model

### JWT Authentication

Every protected request needs:
````

Authorization: Bearer <access_token>

`````

Access tokens expire after **24 hours**. Refresh tokens expire after **7 days**
and are rotated on every use — using a refresh token once invalidates it and
issues a new pair.

### Rate Limiting

`/api/auth/**` endpoints are limited to **5 requests per minute per IP**.
The filter reads `X-Forwarded-For` headers so it works correctly behind
a reverse proxy or load balancer.
```

HTTP 429 Too Many Requests X-Rate-Limit-Retry-After-Seconds: 60

```

### CORS

CORS is configured in `SecurityConfig`. Update the allowed origins
before deploying to production.

---

## Design Decisions

### Modular monolith over microservices

One deployable JAR with strict package-level module boundaries. Each domain
owns its own controllers, services, repositories, models, and DTOs. Cross-module
access goes through service interfaces only — never direct repository access
across module boundaries. This gives the same separation discipline as
microservices with zero network overhead, and each package is a natural
migration boundary if the system needs to scale independently later.

### Soft delete over hard delete

Financial records are never physically removed. Setting `deleted = true` hides
them from all API responses while preserving them in the database. This means:
- Dashboard aggregations for past periods remain accurate after a user deletes a record
- Records can be audited or restored by an admin
- Analytics history is never silently corrupted

### JPA Specifications for dynamic filtering

The records list endpoint supports four optional filters composed at runtime.
JPA Specifications let each filter predicate be an independent null-safe method
that composes cleanly with `.and()`. The alternative — a hand-written JPQL query
with multiple optional clauses — produces fragile string concatenation that is
hard to test and easy to break.

### JPQL over native SQL for aggregations

Dashboard queries use JPQL `@Query` annotations. This keeps the queries
database-agnostic (testable against H2), benefits from Hibernate's type safety,
and avoids the impedance mismatch of mapping raw `ResultSet` rows manually.
The one tradeoff is that `FUNCTION('YEAR', r.date)` is less readable than
PostgreSQL's `EXTRACT(YEAR FROM date)`, but it is portable.

### H2 for tests, PostgreSQL for production

Integration tests run against H2 with `ddl-auto: create-drop`. This means
tests start in under 5 seconds with no external dependencies — ideal for CI.
The tradeoff is that PostgreSQL-specific SQL in migration files is not exercised
by the test suite. If a migration uses a PostgreSQL-only function, it will pass
tests but fail on startup in production. The mitigation is to keep migrations
simple and database-agnostic where possible.

### Refresh token rotation

Every refresh token use issues a new pair and invalidates the old token.
This limits the window of exposure if a refresh token is stolen — the legitimate
user's next refresh will immediately invalidate the attacker's copy.

### Bucket4j in-memory rate limiting

Rate buckets are stored in a `ConcurrentHashMap` keyed by IP address. This is
correct for a single-instance deployment. With multiple instances behind a
load balancer, each instance maintains independent counters and the effective
limit becomes `5 × instance_count`. The fix at that point is to replace the
map with a Bucket4j Redis backend — the filter code does not change.

---

## Known Limitations

- **No logout endpoint yet** — access tokens remain valid until expiry after the
  user stops using them. Adding logout requires a token blacklist table and a
  check in `JwtAuthFilter` on every request.
- **Rate limiting is single-instance** — see Design Decisions above.
- **No password reset** — users who forget their password cannot recover access
  without admin intervention.
- **No email verification** — any email address can be registered without confirmation.
- **Audit log missing** — there is no record of who changed what and when beyond
  `created_at` and `updated_at` timestamps on records.
