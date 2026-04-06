## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running Tests](#running-tests)
- [API Reference](#api-reference)
- [Roles and Permissions](#roles-and-permissions)
- [Security Model](#security-model)
- [Design Decisions](#design-decisions)
- [Known Limitations](#known-limitations)

---

## Architecture

This project uses a **modular monolith** — one deployable Spring Boot application  
organized into self-contained domain modules with strict package-level boundaries.


HTTP Request
↓
RateLimitFilter → rejects /api/auth/** after 5 req/min per IP
↓
JwtAuthFilter → validates Bearer token, sets SecurityContext
↓
SecurityFilterChain → role checks via @PreAuthorize
↓
Controller → HTTP only: parse request, call one service
↓
Service → all business logic and ownership rules
↓
Repository → JPA + JPQL queries, Specifications for filters
↓
PostgreSQL


### Module boundaries


auth/ → users, roles, JWT tokens, refresh tokens
records/ → financial records CRUD and filtering
dashboard/ → aggregation and analytics queries
shared/ → security filters, exception handler, utilities


---

## Tech Stack

| Layer             | Technology                        |
|------------------|----------------------------------|
| Language         | Java 17                          |
| Framework        | Spring Boot 3.5                  |
| Security         | Spring Security 6 (stateless JWT)|
| Persistence      | Spring Data JPA + Hibernate 6    |
| Database         | PostgreSQL 14+                   |
| Migrations       | Flyway                           |
| JWT Library      | jjwt 0.12.3                      |
| DTO Mapping      | MapStruct 1.5.5                  |
| Boilerplate      | Lombok                           |
| Rate Limiting    | Bucket4j 8.7.0                   |
| API Docs         | SpringDoc OpenAPI 2.3.0          |
| Testing DB       | H2 (in-memory)                   |
| Testing Framework| JUnit 5 + MockMvc                |
| Build Tool       | Maven                            |

---

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL 14 or higher
- Maven 3.8 or higher

### Setup with Local PostgreSQL

```bash
# Clone the repo
git clone <repo-url>
cd dashboard

# Create database
psql -U postgres -c "CREATE DATABASE finance_db;"

# Environment variables
export DB_URL=jdbc:postgresql://localhost:5432/finance_db
export DB_USER=postgres
export DB_PASSWORD=secret
export JWT_SECRET=my-very-long-secret-key-at-least-32-characters-long

# Run application
./mvnw spring-boot:run

The application starts on port 8080.
Flyway automatically applies migrations on startup.

Environment Variables
Variable	Default	Required	Description
JWT_SECRET	—	Yes	HMAC signing key (min 32 chars)
DB_URL	jdbc:postgresql://localhost:5432/finance_db	No	JDBC connection string
DB_USER	postgres	No	Database username
DB_PASSWORD	secret	No	Database password
Running Tests

Tests use an in-memory H2 database (no PostgreSQL required).

./mvnw test
API Reference
```
## API Reference

Interactive Swagger UI:

http://localhost:8080/swagger-ui.html

---

## Roles and Permissions

| Action              | VIEWER | ANALYST | ADMIN |
|---------------------|--------|---------|-------|
| View own records    | Yes    | Yes     | Yes   |
| View all records    | No     | No      | Yes   |
| Create records      | No     | Yes     | Yes   |
| Edit own records    | No     | Yes     | Yes   |
| Delete own records  | No     | Yes     | Yes   |
| Manage users        | No     | No      | Yes   |

---

## Security Model

### JWT Authentication

All protected endpoints require:
Authorization: Bearer <access_token>

- Access token expiry: **24 hours**
- Refresh token expiry: **7 days**

### Rate Limiting

- `/api/auth/**` → **5 requests per minute per IP**

---

## Design Decisions

- **Modular monolith** → clean separation without microservice overhead  
- **Soft delete** → preserves analytics integrity  
- **JPA Specifications** → flexible filtering  
- **JPQL** → database-agnostic queries  
- **H2 for testing** → fast CI  
- **Refresh token rotation** → improved security  
