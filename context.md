# IbnFirnas Backend — Context

## What this is
Spring Boot 3.5.16 (Java 21) REST API for an e-commerce site with a company/
services/portfolio layer bundled in. PostgreSQL + JPA/Hibernate, JWT-based
auth, Swagger/OpenAPI docs, transactional email via Gmail SMTP.

- Group/artifact: `com.ibnfirnas:ibnfirnas-backend`
- Entry point: `src/main/java/com/ibnfirnas/IbnfirnasBackendApplication.java`
- Config: `src/main/resources/application.yaml`
- Default port: 8080 (`SERVER_PORT` env var)
- Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/api-docs`

## Architecture
Standard layered structure:
- `controller/` — REST endpoints (Auth, Product, Cart, Order, Wishlist,
  Category, Company, Service, Gallery, Banner, Inquiry, Newsletter,
  Notification, ProductReview, Profile, Dashboard, Contact)
- `service/` — business logic, one per domain area, plus `EmailService`,
  `FileStorageService`
- `repository/` — Spring Data JPA repositories
- `entity/` — JPA entities + `entity/enums` (OrderStatus, PaymentStatus,
  InquiryStatus, TokenType, UserRole)
- `dto/request` / `dto/response` — request/response payloads, kept separate
  from entities
- `security/` — JWT provider, auth filter, `CustomUserDetailsService`
- `config/` — CORS, JWT, Security, Swagger config
- `exception/` — `GlobalExceptionHandler`, custom exceptions

## Domain model
E-commerce: `User`, `Product` (+ `ProductImage`, `Category`, `ProductReview`),
`Cart`/`CartItem`, `Order`/`OrderItem`, `Wishlist`.
Site/company content: `Company`, `ServiceEntity`, `Gallery`, `Banner`.
Lead gen / comms: `Inquiry`, `Newsletter`, `Notification`,
`PasswordResetToken`.

Not all of these are in scope for v1 — Cart, Wishlist, Product Reviews,
Newsletter, Order, and Notification aren't called by anything in the v1
client (mobile app + admin panel), but stay in the codebase intentionally
rather than being deleted. See [project_context.md](project_context.md)
for the actual v1 scope and [architecture.md](architecture.md) for the
full module-by-module breakdown and reasoning.

## Auth
JWT-based (`io.jsonwebtoken` / jjwt 0.11.5). Login/register/forgot-password/
reset-password live in `AuthController` + `AuthService` +
`PasswordResetService`. Token validation via `JwtAuthenticationFilter` →
`JwtTokenProvider`, user lookup via `CustomUserDetailsService`.

## Configuration & secrets
`application.yaml` now reads secrets from environment variables (previously
they were hardcoded and committed — see note below):

| Env var | Purpose | Required |
|---|---|---|
| `DB_URL`, `DB_USERNAME` | Postgres connection | no (defaults to local) |
| `DB_PASSWORD` | Postgres password | **yes** |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail SMTP (app password) | **yes** |
| `JWT_SECRET` | JWT signing key | **yes** |
| `JWT_EXPIRATION` | Token TTL in ms | no (default 86400000 = 24h) |
| `SERVER_PORT` | HTTP port | no (default 8080) |

Local dev: copy `.env.example` to `.env` (gitignored) and fill in real
values, or export the env vars directly. Spring Boot picks up `.env` via
`spring.config.import: optional:file:.env[.properties]` in
`application.yaml`.

**⚠️ Action needed:** the original `application.yaml` committed in git
history (commits `5cac7c6`, `2b16928`) contains a real DB password and a
live Gmail app password in plaintext. Since git history isn't rewritten
here, that Gmail app password should be revoked/rotated in the Google
account regardless of this refactor.

## Build / run
```
./mvnw spring-boot:run
```
Requires a running Postgres instance matching `DB_URL`/`DB_USERNAME`/
`DB_PASSWORD`, and the `.env` file (or equivalent env vars) in place.
`ddl-auto: update` — Hibernate auto-migrates the schema on startup, no
separate migration tool (no Flyway/Liquibase) is wired in yet.

## Known runtime defects (confirmed by running the server)
Full detail, reproduction steps, and fixes in `architecture.md`'s
"Confirmed defects" section:
- `GET /api/auth/me` and `GET`/`PUT /api/profile` return the raw `User`
  entity, leaking the BCrypt password hash in every response.
- `GET /api/categories` throws `LazyInitializationException` (500) the
  moment any category has a child — same pattern will hit `Product` once
  its write endpoints exist.
- Several write endpoints (`Category`, `Service`, `Banner`) are
  unauthenticated right now due to a `permitAll()` path-matcher bug in
  `SecurityConfig` that covers all HTTP methods, not just GET.

## Open questions
- No `application-{profile}.yaml` files — dev/prod aren't split into
  Spring profiles yet.
- No CI config found in the repo.
- This repo has multiple branches (`main`, `developer-2`, plus a few
  `feature/*` branches on `origin`) with diverging history — see
  "Branches" in `project_context.md`. Not yet reconciled.
