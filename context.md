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
  Notification, ProductReview, Profile, Dashboard, Contact, User,
  DeviceToken)
- `service/` — business logic, one per domain area, plus `EmailService`,
  `FileStorageService` (local-disk file storage, not yet Cloudinary)
- `repository/` — Spring Data JPA repositories
- `entity/` — JPA entities + `entity/enums` (OrderStatus, PaymentStatus,
  InquiryStatus, TokenType, UserRole)
- `dto/request` / `dto/response` — request/response payloads, kept separate
  from entities (a real DTO layer now covers most modules — see
  `changelog.md` for what moved off raw-entity responses and when)
- `security/` — JWT provider, auth filter, `CustomUserDetailsService`
- `config/` — CORS, JWT, Security, Swagger, `FirebaseConfig` (initializes
  the Firebase Admin SDK from `firebase-service-account.json`, gitignored
  and not present in the repo — non-fatal if missing)
- `exception/` — `GlobalExceptionHandler`, custom exceptions

## Domain model
E-commerce: `User`, `Product` (+ `ProductImage`, `Category`, `ProductReview`),
`Cart`/`CartItem`, `Order`/`OrderItem`, `Wishlist`.
Site/company content: `Company`, `ServiceEntity`, `Gallery`, `Banner`.
Lead gen / comms: `Inquiry`, `Newsletter`, `Notification`, `DeviceToken`
(FCM push tokens), `PasswordResetToken`.

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

**OTP verification** (added 2026-07-15, `OtpController`/`OtpService`) is
a separate, standalone module — email OTP (generated + tracked in our
DB, 10-min TTL) and SMS OTP (delegated to Twilio Verify via the new
`twilio` SDK dependency and `TwilioConfig`). `POST /api/otp/send` and
`POST /api/otp/verify` are public. It is **not called by anything in
`AuthController`** yet — register/login/forgot-password don't check it.
See `architecture.md`'s "OTP module" section for the full API contract
and known gaps.

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

**Getting an admin account locally:** there's no public "register as
admin" endpoint by design — `POST /api/auth/register` always creates
`ROLE_USER` (see `AuthService.register()`), and there's no seed data.
To test `hasRole("ADMIN")` endpoints, register/login a normal account,
then flip its role directly in Postgres:
```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'you@example.com';
```
No new token needed afterward — `JwtAuthenticationFilter` looks up the
user's role fresh from the DB on every request rather than reading it
from the JWT itself, so an already-issued token picks up the new role
immediately.

## Known runtime defects (updated 2026-07-17)
Full detail, reproduction steps, and fixes in `architecture.md`'s
"Confirmed defects" section; dated history in `changelog.md`.

**Fixed 2026-07-17:**
- **Auth failure responses were an empty `403` body regardless of cause**
  (missing token, invalid token, or wrong role all looked identical to a
  client). Added `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`
  — now `401`/`403` return the same JSON envelope as every other
  response. See `architecture.md`'s "Auth error responses" note.
- **A stale/deleted-user token could break `permitAll` (public)
  endpoints**, not just protected ones — `JwtAuthenticationFilter` threw
  an uncaught `UsernameNotFoundException` that aborted the whole filter
  chain before Spring Security's authorization check ever ran. Now
  caught; the request just proceeds as anonymous.

**Still open:**
- **`GET /api/notifications` — leaks the creating admin's password
  hash, and has no auth gate at all (any logged-in user can call it).**
  Found 2026-07-16 in a full-codebase audit. Same bug class as the
  Gallery leak below, just not caught until now — this is currently the
  most severe open issue in the backend.
- **`GET /api/orders/{id}` has no ownership check (IDOR)** — any
  logged-in user can view any other user's order by ID.
- **`POST /api/auth/refresh-token` throws a `500` instead of a `401`**
  when called anonymously (same root cause `/me` had — sits under a
  `permitAll` matcher but relies on `@AuthenticationPrincipal`
  internally, so an anonymous call NPEs).
- The new OTP module (`/api/otp/send`, `/api/otp/verify` — added
  2026-07-15, see "Auth" below) is **not wired into
  register/login/forgot-password**, and its `purpose` field is
  case-sensitive on `/verify` but not `/send`, which can produce a raw
  `500` instead of a clean `400`.

**In progress, uncommitted (2026-07-16):** `RestAuthenticationEntryPoint`
/`RestAccessDeniedHandler` now give `401`/`403` responses a proper JSON
body (previously empty) app-wide, and `JwtAuthenticationFilter` no
longer crashes on a token referencing a deleted user. Local changes on
`developer-2` only, not yet committed — see `architecture.md`'s
"Security gap" section.

**Fixed 2026-07-15:**
- **`GET /api/gallery`'s public password-hash leak** — was fully
  public, no auth needed at all, and embedded the uploader's full
  `User` record including the password hash in every gallery item.
  Fixed by wiring the (previously unused) `GalleryResponse` DTO into
  `GalleryController`.
- **`GET /api/auth/me`'s password-hash leak** — now returns a
  `UserResponse` DTO, same pattern `ProfileController` already used.
- **Gallery deletes orphaning Cloudinary assets** — `DELETE
  /api/gallery/{id}` now cleans up the Cloudinary asset before removing
  the DB row.
- **The CORS conflict** — two competing CORS configs existed
  (`CorsConfig.java`'s standalone filter vs. `SecurityConfig`'s own),
  with `SecurityConfig`'s version having drifted to allow any origin
  *with* credentials. Consolidated to one config in `SecurityConfig`,
  now driven by a `CORS_ALLOWED_ORIGINS` env var.

**Fixed same day (2026-07-10):**
- **Write endpoints for `Category`, `Service`, `Banner`, `Product`,
  `Gallery`, and `Company` are no longer unauthenticated.** The
  `permitAll()` path-matcher bug in `SecurityConfig` (covered all HTTP
  methods, not just GET) is fixed for all six — `SecurityConfig` now
  splits each into GET-permitAll / write-`hasRole("ADMIN")`, the same
  pattern `Inquiry` already used. Verified live with anonymous,
  `ROLE_USER`, and `ROLE_ADMIN` tokens.
- **`POST /api/inquiries` now requires a logged-in user (any role) to
  submit an inquiry.** This is a deliberate scope change (not a bug
  fix) — it reverses the earlier "public, no account needed"
  requirement from the v1 brief, so it's worth confirming with the
  client. A new `GET /api/inquiries/my` (authenticated) lets a user see
  their own past inquiries, separate from the admin-only full list.
- `POST /api/inquiries` (the public inquiry form) briefly required a
  login token earlier in the day — a regression introduced by the
  2026-07-10 `main` merge, fixed and verified live at the time (before
  the later decision, above, to require login again on purpose).
- `Company` had no fields for social media/website links, despite the
  v1 brief requiring them under Contact Us. Added `websiteUrl`,
  `facebookUrl`, `instagramUrl`, `twitterUrl` (no `linkedinUrl`).

**Fixed** (by the 2026-07-10 `main` merge, re-verified live):
- `GET /api/categories` no longer 500s once a category has children
  (`Category.children` is now `FetchType.EAGER` + `@JsonIgnore`).

## Open questions
- No `application-{profile}.yaml` files — dev/prod aren't split into
  Spring profiles yet.
- No CI config found in the repo.
- `origin` also has `develop`, `feature/authentication`,
  `feature/product-api` branches. `origin/main` had diverged
  significantly from `developer-2` since the 2026-07-10 sync (a
  teammate's OTP feature + security fixes landed there) — reconciled
  again 2026-07-16 by merging `origin/main` into `developer-2`
  (`0e30e3c`). See "Branches" in `project_context.md` for full detail.
  `developer-2` is **9 commits ahead of `origin/developer-2`** as of
  this writing — merge committed locally but **not yet pushed**.
