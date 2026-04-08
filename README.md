<div align="center">

# 💰 Finance Dashboard Backend

**A production-ready REST API for personal and organizational financial records**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Swagger](https://img.shields.io/badge/Swagger-UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)

Role-based access control · JWT authentication · Analytics · Rate limiting · Modular monolith

</div>

---

## 🏗️ Architecture

A **modular monolith** — one deployable Spring Boot JAR organized into self-contained domain modules with strict package-level boundaries.

```
  HTTP Request
       │
       ▼
  ┌─────────────────────────────────────────────┐
  │  RateLimitFilter                            │
  │  Rejects /api/auth/** after 5 req/min/IP    │
  └──────────────────┬──────────────────────────┘
                     │
                     ▼
  ┌─────────────────────────────────────────────┐
  │  JwtAuthFilter                              │
  │  Validates Bearer token · Sets Security     │
  │  Context                                    │
  └──────────────────┬──────────────────────────┘
                     │
                     ▼
  ┌─────────────────────────────────────────────┐
  │  SecurityFilterChain                        │
  │  Role checks via @PreAuthorize              │
  └──────────────────┬──────────────────────────┘
                     │
                     ▼
  ┌─────────────────────────────────────────────┐
  │  Controller  ·  parse request, call service │
  └──────────────────┬──────────────────────────┘
                     │
                     ▼
  ┌─────────────────────────────────────────────┐
  │  Service  ·  business logic + ownership     │
  └──────────────────┬──────────────────────────┘
                     │
                     ▼
  ┌─────────────────────────────────────────────┐
  │  Repository  ·  JPA + JPQL + Specifications │
  └──────────────────┬──────────────────────────┘
                     │
                     ▼
               PostgreSQL
```

### Module boundaries

```
com.finance.dashboard/
├── auth/        Users · Roles · JWT tokens · Refresh tokens
├── records/     Financial records CRUD · Filtering
├── dashboard/   Aggregation queries · Analytics
└── shared/      Security filters · Exception handler · Utilities
```

> `records` and `dashboard` may read from `auth` repositories to resolve user context.
> `auth` never imports from `records` or `dashboard`. `shared` never imports from any domain module.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | [Java 17](https://openjdk.org/projects/jdk/17/) |
| Framework | [Spring Boot 3.5](https://spring.io/projects/spring-boot) |
| Security | [Spring Security 6](https://spring.io/projects/spring-security) — stateless JWT |
| Persistence | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) + Hibernate 6 |
| Database | [PostgreSQL 14+](https://www.postgresql.org/) |
| Migrations | [Flyway](https://flywaydb.org/) |
| JWT Library | [jjwt 0.12.3](https://github.com/jwtk/jjwt) |
| DTO Mapping | [MapStruct 1.5.5](https://mapstruct.org/) |
| Boilerplate | [Lombok](https://projectlombok.org/) |
| Rate Limiting | [Bucket4j 8.7.0](https://github.com/bucket4j/bucket4j) |
| API Docs | [SpringDoc OpenAPI 2.3.0](https://springdoc.org/) |
| Testing DB | H2 in-memory (test scope only) |
| Testing | [JUnit 5](https://junit.org/junit5/) + MockMvc |
| Build | [Maven](https://maven.apache.org/) |

---

## 📁 Project Structure

```
src/
├── main/java/com/finance/dashboard/
│   ├── DashboardApplication.java
│   │
│   ├── auth/
│   │   ├── controller/
│   │   │   ├── AuthController.java              (POST /api/auth/**)
│   │   │   └── UserController.java              (GET/PATCH/POST/DELETE /api/users/**)
│   │   ├── service/
│   │   │   ├── AuthService.java                 (register, login, refresh, logout)
│   │   │   └── UserService.java                 (CRUD, role assign/revoke)
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── RoleRepository.java
│   │   │   └── RefreshTokenRepository.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Role.java
│   │   │   ├── RefreshToken.java
│   │   │   ├── RoleName.java                    (ROLE_VIEWER, ROLE_ANALYST, ROLE_ADMIN)
│   │   │   └── UserStatus.java                  (ACTIVE, INACTIVE)
│   │   └── dto/
│   │       ├── RegisterRequest.java
│   │       ├── LoginRequest.java
│   │       ├── TokenResponse.java
│   │       └── UserResponse.java
│   │
│   ├── records/
│   │   ├── controller/
│   │   │   └── FinancialRecordController.java
│   │   ├── service/
│   │   │   └── FinancialRecordService.java
│   │   ├── repository/
│   │   │   ├── FinancialRecordRepository.java   (JPQL aggregation queries)
│   │   │   └── RecordSpecification.java         (JPA Specification filters)
│   │   ├── model/
│   │   │   ├── FinancialRecord.java
│   │   │   ├── RecordType.java                  (INCOME, EXPENSE)
│   │   │   └── RecordCategory.java              (SALARY, RENT, FOOD, UTILITIES, INVESTMENT, OTHER)
│   │   └── dto/
│   │       ├── RecordRequest.java
│   │       ├── RecordResponse.java
│   │       └── RecordFilterRequest.java
│   │
│   ├── dashboard/
│   │   ├── controller/
│   │   │   └── DashboardController.java
│   │   ├── service/
│   │   │   └── DashboardService.java
│   │   └── dto/
│   │       ├── DashboardSummaryResponse.java
│   │       └── TrendPoint.java
│   │
│   └── shared/
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   └── OpenApiConfig.java
│       ├── security/
│       │   ├── JwtAuthFilter.java
│       │   ├── JwtService.java
│       │   ├── AppUserDetails.java
│       │   ├── AppUserDetailsService.java
│       │   └── RateLimitFilter.java
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java
│       │   ├── ResourceNotFoundException.java
│       │   └── ErrorResponse.java
│       └── util/
│           └── SecurityUtils.java
│
└── test/java/com/finance/dashboard/
    ├── auth/controller/
    │   └── AuthControllerTest.java              (11 tests)
    ├── records/service/
    │   └── FinancialRecordServiceTest.java      (10 tests)
    ├── dashboard/service/
    │   └── DashboardServiceTest.java            (8 tests)
    ├── DashboardApplicationTests.java           (1 test)
    └── shared/
        ├── BaseIntegrationTest.java
        ├── TestDataSeeder.java
        └── TokenHelper.java
```

---

## 🗄️ Database Schema

```
users             (id, email, password, full_name, status, created_at)
roles             (id, name)
user_roles        (user_id → users.id,  role_id → roles.id)

financial_records (id, created_by → users.id, amount,
                   type     [INCOME | EXPENSE],
                   category [SALARY, RENT, FOOD, UTILITIES, INVESTMENT, OTHER],
                   date, notes, deleted, created_at, updated_at)

refresh_tokens    (id, user_id → users.id, token, expires_at, created_at)
```

Indexes on `financial_records`:

| Index | Column |
|---|---|
| `idx_records_user` | `created_by` |
| `idx_records_date` | `date` |
| `idx_records_type` | `type` |
| `idx_records_deleted` | `deleted` |

---

## 🚀 Getting Started

### Prerequisites

- [Java 17+](https://openjdk.org/projects/jdk/17/)
- [PostgreSQL 14+](https://www.postgresql.org/download/) — or Docker
- [Maven 3.8+](https://maven.apache.org/download.cgi)

### Option A — Docker (fastest)

```bash
git clone <repo-url>
cd dashboard

docker-compose up -d

export JWT_SECRET=my-very-long-secret-key-at-least-32-characters-long
./mvnw spring-boot:run
```

### Option B — Local PostgreSQL

```bash
psql -U postgres -c "CREATE DATABASE finance_db;"

export DB_URL=jdbc:postgresql://localhost:5432/finance_db
export DB_USER=postgres
export DB_PASSWORD=secret
export JWT_SECRET=my-very-long-secret-key-at-least-32-characters-long

./mvnw spring-boot:run
```

App starts on **port 8080**. Flyway applies all migrations on startup — no manual schema setup needed.

### Promote the first admin

New users get `VIEWER` by default. Promote to `ADMIN` directly in the DB, then log in again for a fresh token:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'your@email.com'
  AND r.name  = 'ROLE_ADMIN';
```

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

## 🔑 Environment Variables

| Variable | Default | Required | Description |
|---|---|---|---|
| `JWT_SECRET` | — | ✅ | HMAC signing key — minimum 32 characters |
| `DB_URL` | `jdbc:postgresql://localhost:5432/finance_db` | — | Full JDBC connection string |
| `DB_USER` | `postgres` | — | Database username |
| `DB_PASSWORD` | `secret` | — | Database password |

---

## 🧪 Running Tests

Tests use H2 in-memory — no PostgreSQL needed.

```bash
# Run all 30 tests
./mvnw test

# Run a specific class
./mvnw test -Dtest=AuthControllerTest
./mvnw test -Dtest=FinancialRecordServiceTest
./mvnw test -Dtest=DashboardServiceTest
```

| Class | Tests | What is verified |
|---|---|---|
| `AuthControllerTest` | 11 | Register, login, refresh, validation, inactive user |
| `FinancialRecordServiceTest` | 10 | CRUD, ownership isolation, soft delete, filters |
| `DashboardServiceTest` | 8 | Aggregation totals, scoping, trend, access control |
| `DashboardApplicationTests` | 1 | Context loads |

---

## 📡 API Reference

Interactive docs at [`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html)

`🔒` = requires `Authorization: Bearer <token>`

**Auth**

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | — | Register — returns JWT immediately |
| `POST` | `/api/auth/login` | — | Login — returns JWT + refresh token |
| `POST` | `/api/auth/refresh` | — | Swap refresh token for new tokens |

**User Management** — `ADMIN` only

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/users` | List all users |
| `PATCH` | `/api/users/{id}/status` | Activate or deactivate |
| `POST` | `/api/users/{id}/roles` | Assign role |
| `DELETE` | `/api/users/{id}/roles/{role}` | Revoke role |

**Financial Records**

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| `GET` | `/api/records` | ALL | List — paginated and filterable |
| `POST` | `/api/records` | ANALYST, ADMIN | Create new record |
| `GET` | `/api/records/{id}` | ALL | Get one — own or admin |
| `PUT` | `/api/records/{id}` | ANALYST, ADMIN | Update — own or admin |
| `DELETE` | `/api/records/{id}` | ANALYST, ADMIN | Soft delete — own or admin |

Query parameters for `GET /api/records`:

| Parameter | Type | Example | Description |
|---|---|---|---|
| `from` | date | `2025-04-01` | Start of date range (ISO 8601) |
| `to` | date | `2025-04-30` | End of date range |
| `type` | enum | `INCOME` | `INCOME` or `EXPENSE` |
| `category` | enum | `SALARY` | `SALARY` `RENT` `FOOD` `UTILITIES` `INVESTMENT` `OTHER` |
| `page` | int | `0` | Zero-based page index |
| `size` | int | `20` | Page size — max 100 |

**Dashboard Analytics** — `ANALYST` and `ADMIN`

| Method | Endpoint | Params | Description |
|---|---|---|---|
| `GET` | `/api/dashboard/summary` | `from`, `to` | Full snapshot — income, expense, net |
| `GET` | `/api/dashboard/categories` | `from`, `to` | Totals grouped by category |
| `GET` | `/api/dashboard/trend` | `since` | Monthly series for income and expense |
| `GET` | `/api/dashboard/recent` | — | 10 most recent records |

<details>
<summary>Paginated response shape</summary>

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

</details>

<details>
<summary>Dashboard summary response shape</summary>

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
  "recentActivity": []
}
```

</details>

<details>
<summary>Error response shape</summary>

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

| Status | Code | When |
|---|---|---|
| `400` | `VALIDATION_FAILED` | Bean validation failed on request body |
| `400` | `BAD_REQUEST` | Business rule violation (duplicate email, etc.) |
| `401` | `UNAUTHORIZED` | Missing, invalid, or expired JWT |
| `403` | `FORBIDDEN` | Valid JWT but insufficient role or ownership |
| `404` | `NOT_FOUND` | Resource not found or soft-deleted |
| `429` | `RATE_LIMIT_EXCEEDED` | More than 5 auth requests per minute per IP |
| `500` | `INTERNAL_ERROR` | Unexpected server error |

</details>

---

## 🔐 Roles and Permissions

New users are assigned `VIEWER` by default. An `ADMIN` must explicitly promote them. The first admin must be created via a direct database insert (see [Getting Started](#-getting-started)).

| Action | VIEWER | ANALYST | ADMIN |
|---|---|---|---|
| Register / Login | ✅ Public | ✅ Public | ✅ Public |
| View own records | ✅ | ✅ | ✅ |
| View all records | ❌ | ❌ | ✅ |
| Create records | ❌ | ✅ | ✅ |
| Edit own records | ❌ | ✅ | ✅ |
| Delete own records | ❌ | ✅ | ✅ |
| Edit or delete any record | ❌ | ❌ | ✅ |
| Dashboard analytics | ❌ | ✅ Own data | ✅ All data |
| Manage users | ❌ | ❌ | ✅ |

---

## 🛡️ Security Model

### JWT Authentication

```
Authorization: Bearer <access_token>
```

| Token | Expiry | Notes |
|---|---|---|
| Access token | 24 hours | Sent on every protected request |
| Refresh token | 7 days | Rotated on every use — old token immediately invalidated |

### Rate Limiting

`/api/auth/**` is limited to **5 requests per minute per IP**. The filter reads `X-Forwarded-For` so it works correctly behind a reverse proxy or load balancer.

```
HTTP 429 Too Many Requests
X-Rate-Limit-Retry-After-Seconds: 60
```

### CORS

CORS is configured in `SecurityConfig`. Update the allowed origins before deploying to production.

---

## 💡 Design Decisions

<details>
<summary><strong>Modular monolith over microservices</strong></summary>

One deployable JAR with strict package-level module boundaries. Each domain owns its own controllers, services, repositories, models, and DTOs. Cross-module access goes through service interfaces only — never direct repository access across module boundaries. This gives the same separation discipline as microservices with zero network overhead, and each package is a natural migration boundary if the system needs to scale independently later.

</details>

<details>
<summary><strong>Soft delete over hard delete</strong></summary>

Financial records are never physically removed. Setting `deleted = true` hides them from all API responses while preserving them in the database. Dashboard aggregations for past periods remain accurate, records can be audited or restored by an admin, and analytics history is never silently corrupted.

</details>

<details>
<summary><strong>JPA Specifications for dynamic filtering</strong></summary>

The records list endpoint supports four optional filters composed at runtime. JPA Specifications let each filter predicate be an independent null-safe method that composes cleanly with `.and()`. The alternative — a hand-written JPQL query with multiple optional clauses — produces fragile string concatenation that is hard to test and easy to break.

</details>

<details>
<summary><strong>JPQL over native SQL for aggregations</strong></summary>

Dashboard queries use JPQL `@Query` annotations. This keeps queries database-agnostic (testable against H2), benefits from Hibernate's type safety, and avoids mapping raw `ResultSet` rows manually. The tradeoff is that `FUNCTION('YEAR', r.date)` is less readable than PostgreSQL's `EXTRACT(YEAR FROM date)`, but it is portable.

</details>

<details>
<summary><strong>H2 for tests, PostgreSQL for production</strong></summary>

Integration tests run against H2 with `ddl-auto: create-drop`, starting in under 5 seconds with no external dependencies. The tradeoff is that PostgreSQL-specific SQL in migration files is not exercised by the test suite — keep migrations simple and database-agnostic to mitigate this.

</details>

<details>
<summary><strong>Refresh token rotation</strong></summary>

Every refresh token use issues a new pair and invalidates the old token. This limits the exposure window if a refresh token is stolen — the legitimate user's next refresh immediately invalidates the attacker's copy.

</details>

<details>
<summary><strong>Bucket4j in-memory rate limiting</strong></summary>

Rate buckets are stored in a `ConcurrentHashMap` keyed by IP address — correct for a single-instance deployment. With multiple instances behind a load balancer, the effective limit becomes `5 × instance_count`. The fix is to replace the map with a Bucket4j Redis backend — the filter code does not change.

</details>

---

## ⚠️ Known Limitations

- **No logout endpoint** — access tokens remain valid until expiry. Logout requires a token blacklist table and a check in `JwtAuthFilter` on every request.
- **Rate limiting is single-instance** — see design decisions above.
- **No password reset** — users who forget their password cannot recover access without admin intervention.
- **No email verification** — any email address can be registered without confirmation.
- **No audit log** — there is no record of who changed what and when beyond `created_at` and `updated_at` timestamps on records.
