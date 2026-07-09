# IbnFirnas — Architecture

## Target architecture (client-approved)

```
                    ┌─────────────────┐
                    │   Admin Panel   │
                    │ React + Vite    │
                    └────────┬────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────┐
│              Spring Boot Backend                 │
│                                                    │
│ Authentication (JWT)                              │
│ Product Management                                │
│ Service Management                                │
│ Gallery Management                                │
│ Inquiry Management                                │
│ Order Management                                  │
│ Payment Integration                               │
│ Notification Service                              │
└───────┬─────────────┬─────────────┬───────────────┘
        │              │             │
        ▼              ▼             ▼
┌────────────┐ ┌──────────────┐ ┌─────────────┐
│ PostgreSQL │ │ Cloudinary   │ │ Firebase    │
│ Database   │ │ Image Store  │ │ FCM         │
└────────────┘ └──────────────┘ └─────────────┘
        ▲
        │
        ▼
┌─────────────────┐
│ React Native    │
│ Mobile App      │
└─────────────────┘
```

Three consumers (admin panel, mobile app) share one Spring Boot API backed
by PostgreSQL, Cloudinary (images), and Firebase FCM (push). Order/
Payment/Notification boxes are drawn for the full architecture but only
become load-bearing in v1.1 — see [project_context.md](project_context.md)
for what's actually in v1.

## How the admin panel actually talks to everything

One backend serves both clients — the admin panel does **not** get its
own backend, and it never talks to the database or to the mobile app
directly.

```
Admin panel (browser)          Mobile app (React Native)
      │  HTTP + JWT                   │  HTTP + JWT
      └───────────────┬───────────────┘
                       ▼
             Spring Boot backend   ← only this layer touches Postgres
                       │  JDBC / Hibernate
                       ▼
                  PostgreSQL
```

- Same REST endpoints, same `/api/auth/login`, same JWT mechanism for
  both clients. What makes a request "admin" isn't a separate system —
  it's that the caller's `User.role` is `ROLE_ADMIN` and `SecurityConfig`
  gates certain endpoints with `hasRole("ADMIN")` (today, inconsistently
  — see the security gap section below).
- Admin panel never holds DB credentials or a JDBC driver. It only knows
  how to call REST endpoints, same as the mobile app.
- Admin panel and mobile app never talk to each other directly. An admin
  creating a product just writes a row to Postgres; the mobile app sees
  it next time it calls `GET /api/products`. Push notifications (v1.1)
  route admin → backend → Firebase → device, still no direct channel.
- `CorsConfig` currently allows `allowedOriginPatterns: ["*"]` — fine for
  local dev with both frontends on different ports, but should be locked
  to the actual admin panel and mobile app origins before this goes near
  production, especially combined with the `permitAll()` gap below.

## Backend module → v1 requirement cross-check

Re-verified 2026-07-10 after merging `main`'s substantial backend work
into `developer-2` (Firebase/FCM plumbing, DTO layer, Product CRUD,
Inquiry admin endpoints, Company update endpoint). Most gaps from the
2026-07-09 audit are closed; three real issues remain or got newly
introduced by the merge — see "Confirmed defects" below.

| Module | v1 needs | Current state | Gap |
|---|---|---|---|
| Auth | Register, login, JWT, roles | `AuthController` has register/login/forgot-password/reset-password/refresh-token/me. `SecurityConfig` uses BCrypt + stateless JWT. `UserRole` = `ROLE_USER`/`ROLE_ADMIN`. | `GET /me` still leaks the password hash (see defects) — the rest is solid. |
| User profile | Get/update profile | `ProfileController` GET/PUT `/api/profile`, now returns a `UserResponse` DTO, password change flow added. | **Fixed** — no more entity leak here. |
| Dashboard | Product/service/inquiry counts | `DashboardService.getStats()` returns totalUsers, totalProducts, totalOrders, totalInquiries, totalServices. | None. |
| Product | Full CRUD + image upload (Cloudinary) | `ProductController`/`ProductService` now have full GET/POST/PUT/DELETE via `ProductRequest`/`ProductResponse`. `Product.images` is `FetchType.EAGER` now (fixes the lazy-load crash). | **Still missing:** pagination on the catalog GET, actual Cloudinary upload (still no image upload wiring — `ProductRequest` doesn't carry a file). |
| Service | Full CRUD | `ServiceController` has GET (all/featured/by-id) + POST create + DELETE. | **Still missing:** update (PUT) — unchanged from before. |
| Gallery | Upload/delete/list | `GalleryController` now has GET/POST/DELETE, but POST takes a raw JSON `Gallery` body (no multipart/file upload), no service layer, no Cloudinary. `cloudinaryPublicId` column added 2026-07-10 (see below) but nothing writes to it yet. | **Still missing:** actual Cloudinary integration — the column is ready, the upload flow isn't built. |
| Company | GET public / PUT admin | `CompanyController` now has GET/POST/PUT (`PUT /api/company/{id}`). | **Fixed** functionally — still unauthenticated, see security gap. |
| Inquiry | POST public / GET + status update admin | `InquiryController` now has POST/GET list/GET by id/PUT resolve. | **New regression:** `/api/inquiries` isn't in `SecurityConfig`'s `permitAll` list anymore, so the public submit form now requires a login token — breaks the v1 requirement that inquiry submission needs no account. See defects below. |
| Image storage | Cloudinary | `FileStorageService` still saves to local `./uploads`, unused by any controller now. No Cloudinary SDK in `pom.xml`. `Gallery.cloudinaryPublicId` column exists as of 2026-07-10 (Hibernate `ddl-auto: update` added it, verified live). | **Not started** — schema is ready, the actual `CloudinaryService` + SDK integration still needs building. |
| Order/Payment/Notification | Deferred to v1.1 | Order gained an admin `/api/orders/all` (properly `hasRole("ADMIN")` gated). Notification unchanged (no real FCM call yet, despite `firebase-admin` now in `pom.xml` and `FirebaseConfig` added). Payment has none. | Fine to leave as-is — not v1-blocking. |

## Full v1 API inventory (re-verified against a running instance, 2026-07-10)

Every endpoint that exists in the codebase today, for the modules that are
actually in v1. "Auth (actual)" is what `SecurityConfig` really enforces
right now, not what it should enforce — see "Security gap" below for the
difference. Updated after the `main` merge; changes from the 2026-07-09
version are marked.

### Auth — `/api/auth`
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| POST `/register` | public | `{fullName, email, password, phone}` | `{token, email, fullName, role}` | Mobile app |
| POST `/login` | public | `{email, password}` | `{token, email, fullName, role}` | Mobile app, Admin panel |
| POST `/forgot-password` | public | `{email}` | `void` | Mobile app |
| POST `/reset-password` | public | `{token, newPassword}` | `void` | Mobile app |
| POST `/refresh-token` | authenticated | — (Bearer token) | `{accessToken}` | Both |
| GET `/me` | authenticated | — | full `User` entity **(still leaks password hash — unchanged, see defects)** | Both |

### Profile — `/api/profile` — **fixed**
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| GET `/` | authenticated | — | `UserResponse` DTO (no password) | Mobile app |
| PUT `/` | authenticated | `UpdateProfileRequest {fullName, phone, avatarUrl, currentPassword, newPassword}` | `UserResponse` DTO (no password) | Mobile app |

### Product — `/api/products` — **CRUD now built**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<ProductResponse>`, no pagination | Mobile app | Built, still needs pagination |
| GET `/{id}` | public | — | `ProductResponse` | Mobile app | Built |
| GET `/featured` | public | — | `List<ProductResponse>` | Mobile app (home) | Built |
| POST `/` | **public — should be admin** | `ProductRequest` JSON, no file field | `ProductResponse` | Admin panel | Built, insecure, no image upload |
| PUT `/{id}` | **public — should be admin** | `ProductRequest` JSON | `ProductResponse` | Admin panel | Built, insecure |
| DELETE `/{id}` | **public — should be admin** | — | — | Admin panel | Built, insecure |
| image upload | — | — | — | Admin panel | **Still missing entirely** — no multipart handling, no Cloudinary |

### Category — `/api/categories` — **lazy-load crash fixed**
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| GET `/` | public | — | `List<Category>` (`children` now `FetchType.EAGER` + `@JsonIgnore` — no more 500) | Mobile app |
| GET `/{id}` | public | — | `Category` | Mobile app |
| POST `/` | **public, no role check** | raw `Category` entity | `Category` | Admin panel |
| DELETE `/{id}` | **public, no role check** | — | `void` | Admin panel |

### Service — `/api/services` — unchanged
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<ServiceEntity>` | Mobile app | Built |
| GET `/featured` | public | — | `List<ServiceEntity>` | Mobile app (home) | Built |
| GET `/{id}` | public | — | `ServiceEntity` | Mobile app | Built |
| POST `/` | **public, no role check** | raw `ServiceEntity` | `ServiceEntity` | Admin panel | Built (insecure) |
| PUT `/{id}` | — | — | — | Admin panel | **Still missing** |
| DELETE `/{id}` | **public, no role check** | — | `void` | Admin panel | Built (insecure) |

### Gallery — `/api/gallery` — **write endpoints added, still no Cloudinary**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<Gallery>`, ordered by `displayOrder` | Mobile app | Built |
| POST `/` | **public in `SecurityConfig`, but code requires `@AuthenticationPrincipal`** — an anonymous call throws a `NullPointerException` (500) rather than a clean 401, since it needs a `UserDetails` to set `uploadedBy` | raw JSON `Gallery` body — **not multipart**, no actual file upload | `Gallery` | Admin panel | Built but not real upload; any *authenticated* user (not just admin) can call it if they attach a token |
| DELETE `/{id}` | **public, no role check, no auth needed at all** | — | `void` | Admin panel | Built, wide open |

### Company — `/api/company` — **write endpoints added**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `Company` (about/mission/vision/contact/logo/banner) | Mobile app | Built |
| POST `/` | **public, no role check** | raw `Company` entity | `Company` | Admin panel | Built, insecure — and since `Company` has no singleton constraint, this can create duplicate rows |
| PUT `/{id}` | **public, no role check** | raw `Company` entity | `Company` | Admin panel | Built, insecure |

### Inquiry — `/api/inquiries` — **admin endpoints added, but public submit is now broken**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| POST `/` | **authenticated — regression, was public** | `InquiryRequest {name, email, phone, subject, message}` | `InquiryResponse` | Mobile app | **Broken for v1** — confirmed live: returns `403` with no token. The inquiry form is supposed to need no account. |
| GET `/` (admin list) | authenticated (any role, not admin-specific) | — | `List<InquiryResponse>` | Admin panel | Built, but not actually admin-gated |
| GET `/{id}` | authenticated (any role) | — | `InquiryResponse` | Admin panel | Built, not admin-gated |
| PUT `/{id}/resolve` | authenticated (any role) | — | `InquiryResponse` | Admin panel | Built, not admin-gated |

### Dashboard — `/api/admin/dashboard`
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| GET `/stats` | correctly `hasRole("ADMIN")` via the `/api/admin/**` matcher | — | `{totalUsers, totalProducts, totalOrders, totalInquiries, totalServices}` | Admin panel |

## Frontend integration flow (per module)

How each v1 module wires into the **Admin Panel (React + Vite)** and the
**Mobile App (React Native)**, once the missing endpoints above are built.

### Auth
- **Mobile app:** Splash/Login screen → `POST /auth/login` → store `token`
  + `role` in secure storage (Keychain/EncryptedSharedPreferences, not
  AsyncStorage in plaintext) → attach `Authorization: Bearer <token>` to
  every subsequent request via an Axios/fetch interceptor → on 401,
  clear token and redirect to login. Register screen → `POST
  /auth/register` → same token-store flow, auto-login after registration
  (response already includes the token, no separate login call needed).
- **Admin panel:** Login page → `POST /auth/login` → reject client-side if
  `role !== "ROLE_ADMIN"` even though it's the same endpoint as mobile —
  **but this is a UI-only check, not a security boundary**; the backend
  itself doesn't restrict which role can call `/api/auth/login`, so a
  regular user token would still work against the API if someone pointed
  a raw HTTP client at it. Store token, attach to all admin API calls,
  redirect to `/login` on 401/403.
- Both apps need a **token refresh interceptor**: on any 401 response, try
  `POST /auth/refresh-token` once with the existing token; if that also
  fails, force logout. `JWT_EXPIRATION` is currently 24h — fine for v1,
  but there's no refresh-token *rotation* (the endpoint just re-signs a
  new token from the still-valid old one), so a stolen token stays valid
  for its full lifetime with no way to revoke it server-side.

### Product (mobile: browse; admin: manage)
- **Mobile app:** Home screen → `GET /products/featured` on mount, cached
  in memory/query-cache (React Query/RTK Query recommended over raw
  `useEffect` + `fetch`, since this data barely changes). Product list
  screen → `GET /products?page=&size=&category=` with infinite-scroll or
  page-number pagination — **cannot be built against the current backend
  until pagination is added**, building against the unpaginated `List`
  response now means a rewrite later. Product detail screen → `GET
  /products/{id}`.
- **Admin panel:** Product list table → `GET /products` (also needs
  pagination for the admin table once catalog size grows). Create/Edit
  form → multipart `POST`/`PUT /products/{id}` with image files +
  `ProductRequest` JSON — **endpoint doesn't exist yet**, this is
  first-priority backend work. Delete → confirm dialog → `DELETE
  /products/{id}`.
- Both consumers depend on Cloudinary being wired in before image upload
  can work at all.

### Category
- **Mobile app:** filter chips/dropdown on the product list screen → `GET
  /categories` once on mount, cache client-side (categories change
  rarely). **Do not build this against the current `GET /categories`
  response** — it 500s the moment any category has a child category, see
  defects below. Fix must land before frontend work starts here.
- **Admin panel:** category picker in the Product create/edit form → same
  `GET /categories` call, same current-500 caveat. A dedicated "manage
  categories" screen isn't in the v1 checklist (categories were called
  "optional" in the original brief) — confirm with the client whether
  admin-side category CRUD UI is needed for v1 or products just get
  assigned to categories seeded some other way.

### Service
- **Mobile app:** Services list screen → `GET /services`. Service detail →
  `GET /services/{id}`. Home screen → `GET /services/featured`.
- **Admin panel:** list/create/edit/delete against
  `POST`/`PUT`/`DELETE /services`. The `PUT` (edit) endpoint must be
  built first — right now the admin panel could create and delete
  services but never edit one after creation.

### Gallery
- **Mobile app:** Gallery grid screen → `GET /gallery` → render Cloudinary
  URLs directly (no proxying through the backend) → tap image → full-
  screen zoom view (client-side only, no additional API call).
- **Admin panel:** Upload screen → multipart `POST /gallery` (needs
  building) → on success, refetch the grid. Delete → `DELETE
  /gallery/{id}` (needs building) — must delete both the DB row and the
  Cloudinary asset via its `public_id` (see the `cloudinaryPublicId`
  schema gap noted earlier).

### Company
- **Mobile app:** About screen + Contact screen both read from the same
  `GET /company` call — fetch once, cache, reuse across both screens
  rather than calling it twice.
- **Admin panel:** single "Company Info" settings form (not a list/CRUD
  screen — there's only ever one `Company` row) → prefill from `GET
  /company` → `PUT /company` on save (needs building). No delete —
  doesn't make sense for a singleton settings row.

### Inquiry
- **Mobile app:** Inquiry form screen → client-side validation matching
  `InquiryRequest` (`name`, `email`, `phone`, `subject`, `message` all
  required except phone) → `POST /inquiries` → success toast/screen. No
  auth needed, no token attached.
- **Admin panel:** Inquiries list screen → `GET /inquiries` (needs
  building) with status filter (`OPEN`/`IN_PROGRESS`/`RESOLVED`/`CLOSED`)
  → click a row → detail view → status dropdown → `PATCH
  /inquiries/{id}/status` (needs building, plus a `notes` field per the
  earlier decision).

### Dashboard (admin panel only)
- Admin panel home/landing page after login → `GET
  /admin/dashboard/stats` → render as stat tiles (products, services,
  inquiries, users, orders count). Low complexity, single call, no
  pagination needed.

## Approved per-module flows (2026-07-09)

Detailed request/response flows for Auth, Gallery, Services, Inquiry, and
Product were designed and approved. Summarized below, with each checked
against the entities that already exist in this repo.

### Auth
Client → `POST /auth/login` → `AuthController` → `UserDetailsService`
(load user by email) + BCrypt verify → `JwtTokenProvider` (sign token,
embed role) → token returned. Every subsequent request: `JwtFilter`
(extract/validate) → `SecurityContext` (auth + role) → controller
`@PreAuthorize`/matcher checks → 401/403 on invalid/expired token.
Matches what's already built (`JwtAuthenticationFilter`,
`JwtTokenProvider`, `CustomUserDetailsService`) — no entity changes
needed.

### Gallery
Images go through Cloudinary — Spring Boot never stores binary files.
Upload: admin panel → multipart → `GalleryController` (`POST
/api/gallery`) → `CloudinaryService` (upload via SDK) → CDN, returns
`public_id` + `secure_url` → `GalleryService` builds entity → Postgres.
Retrieval: mobile app → `GET /api/gallery` (public) → Postgres → JSON of
Cloudinary URLs, app loads images directly from the CDN.
**Schema gap — column added 2026-07-10, upload flow still not built.**
When Cloudinary stores an image it hands back two things: a `secure_url`
(the link to view/display it) and a `public_id` (its internal file
identifier). To delete that image later, Cloudinary's API asks for the
`public_id`, not the URL — without it, "delete" in the admin panel could
only ever remove our database row, leaving the actual file orphaned in
Cloudinary's storage forever. `Gallery.cloudinaryPublicId` now exists
(verified live: `alter table if exists gallery add column
cloudinary_public_id varchar(255)` ran via Hibernate's `ddl-auto: update`
on startup) so the column is ready to receive it. What's still missing:
there's no `CloudinaryService`, no SDK dependency in `pom.xml`, and
`GalleryController.create()` still takes a raw JSON body instead of a
multipart file upload — so nothing writes to the new column yet. The
schema is prepped; the actual integration is the next step.

### Services
Standard protected CRUD — admin writes, public reads. Admin path: admin
panel → JWT filter (validate Bearer, ROLE_ADMIN) → `ServiceController`
(POST/PUT/DELETE) → `ServiceService` → image upload to Cloudinary →
Postgres (`services` table). Public path: mobile app → `GET
/api/services` (no auth) → Postgres `WHERE is_active = true` → DTO list
(id, title, description, image_url).
Matches the existing `ServiceEntity` almost exactly (slug, image_url,
is_active, display_order already present). No entity changes needed —
just the missing `PUT` endpoint and the Cloudinary wiring.

### Inquiry form
Fully public to submit, admin-only (JWT + ROLE_ADMIN) to manage.
Submission: app → `POST /api/inquiries` (`@Valid`) → `InquiryService`
(status = NEW, map to entity) → Postgres; optional email notify to admin;
400 on validation failure. Admin: admin panel → `GET`/`PATCH status` →
Postgres, filter/sort/update.

**Decision (2026-07-09):** keep the existing `Inquiry` entity and its
`InquiryStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED }` enum as-is —
already wired through `InquiryController`/`InquiryService`, no schema
change. Add a `notes` column (the design calls for one, the entity
doesn't have it yet).

The originally-approved flow used a simpler `NEW → READ → DONE` status
model instead. Recording it here as a documented alternative in case it's
revisited later — not implemented:

| Approved design (not built) | Existing entity (kept) |
|---|---|
| `status: NEW \| READ \| DONE` | `status: OPEN \| IN_PROGRESS \| RESOLVED \| CLOSED` |
| — | `subject`, `priority`, `assignedTo`, `resolvedAt` (extra fields not in the design) |
| `notes` field | to be added |

### Product
Admin path: admin panel → `ProductController` (POST/PUT/DELETE, ROLE_ADMIN)
→ `ProductService` (validate, map entity) → Postgres (`products` table,
FK to `categories`); images → Cloudinary, URLs stored back on the
product. Public path: mobile app → `GET /api/products?category=&page=&size=`
→ paginated query, JOIN categories → `ProductDTO` list; `GET
/api/products/{id}` → full detail + specs.
**Schema gap:** current `Product` entity has no `specifications` column
(see decision below) and images are a separate `ProductImage`
`@OneToMany` table rather than a Cloudinary URL array — that's fine to
keep as-is (it's actually more flexible for per-image metadata), just
means the Cloudinary upload result gets written into `ProductImage` rows
instead of an array column.
`Category` already matches the approved design almost exactly
(slug, parent, display_order, is_active) — no changes needed.

## Decisions locked in (2026-07-09)
- **One controller + one service per module** (`ProductController` /
  `ProductService`, `ServiceController` / `ServiceService`, etc.) — this
  is already the pattern in the codebase; no restructuring needed.
- **`specifications` stored as a JSONB column** on `Product`, not a
  separate table — simpler to query/update than a normalized spec table.
  Requires adding `@Column(columnDefinition = "jsonb") private String
  specifications;` (or a `Map`/`JsonNode` with a Hibernate JSONB type) to
  the `Product` entity.
- **Pagination from day one** on catalog endpoints — `ProductController`
  currently returns the full list with no `Pageable`; this must change
  before the mobile app's product browse screen is built against it, not
  after.
- **Contact Us → WhatsApp**: start with Approach A, a `wa.me` deep link
  from the mobile app's Contact screen. Zero backend code — this is a
  mobile-app-only decision, noted here for completeness but doesn't
  affect this repo.

## Security gap — still open, re-confirmed 2026-07-10 after the merge

`SecurityConfig` still does this (unchanged by the `main` merge, plus two
new additions marked):

```java
.requestMatchers("/api/products/**").permitAll()
.requestMatchers("/api/categories/**").permitAll()
.requestMatchers("/api/services/**").permitAll()
.requestMatchers("/api/gallery/**").permitAll()
.requestMatchers("/api/company/**").permitAll()
.requestMatchers("/api/banners/**").permitAll()
.requestMatchers("/api/newsletter/**").permitAll()
.requestMatchers("/api/contact/**").permitAll()
.requestMatchers("/api/reviews/product/**").permitAll()
.requestMatchers("/api/orders/all").hasRole("ADMIN")   // new — correct pattern
.requestMatchers("/api/admin/**").hasRole("ADMIN")     // new — correct pattern
```

`permitAll()` on a path pattern applies to **every HTTP method**, not just
GET. Re-verified live, post-merge, with a fresh instance:

```
POST /api/categories  {"name":"Verify Cat","slug":"verify-cat"}
(no Authorization header)
→ 200 OK, category created
```

Still wide open. `Product`, `Company`, and `Gallery` all gained write
endpoints in this merge (see the inventory above) and **inherited the
same hole** — Product create/update/delete, Company create/update, and
Gallery create/delete are all currently reachable by anyone with no
token. `/api/orders/all` and `/api/admin/**` show the team already knows
the right pattern (`hasRole("ADMIN")`) — it just hasn't been applied to
Product/Category/Service/Gallery/Company/Banner's write methods yet.

Fix needed: split each of these into separate GET-permitAll /
write-hasRole("ADMIN") matchers, e.g.:

```java
.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
.requestMatchers("/api/products/**").hasRole("ADMIN")
```

(repeated per module).

## Confirmed defects (re-verified live, 2026-07-10, after the `main` merge)

Status of each defect found on 2026-07-09, re-tested against a fresh
instance after merging `main`'s DTO/entity fixes into `developer-2`, plus
one new issue the merge introduced.

### 1. Password hash leak — **half-fixed**
`ProfileController` (`GET`/`PUT /api/profile`) now returns a
`UserResponse` DTO — fixed, no more leak there.

`AuthController.getCurrentUser()` (`GET /api/auth/me`) was **not**
touched by the merge and still returns the raw `User` entity. Re-verified
live:

```
GET /api/auth/me →
{"data":{"id":2,"email":"...",
 "password":"$2a$10$tPnpUdsasRhs83GSYzDYt.TEBWCNvzNxne1eONcNvWGNnK4vHQiJa",
 ...}}
```

Same fix as `ProfileController` already demonstrates the pattern — return
`UserResponse` here too instead of `User`. Small, isolated change.

### 2. `LazyInitializationException` — **fixed**
`Product.images` and `Category.children` are now `fetch = FetchType.EAGER`
(`Category.children` also got `@JsonIgnore`, so it's not even serialized).
`ProductController` now returns `ProductResponse` DTOs rather than raw
entities. Re-verified live: created a parent/child category and called
`GET /api/categories` — no more 500, clean response. This class of bug
is closed for Category and Product; if any *other* entity gains a new
`@OneToMany` later, apply the same fix (EAGER fetch or a DTO) rather than
assuming lazy-by-default is safe with `open-in-view: false`.

### 3. Dashboard stats endpoint — fine, unchanged
Still correctly gated by `/api/admin/**` + `hasRole("ADMIN")`. No issue.

### 4. NEW — Inquiry public submission now requires login (v1-breaking)
The merge added real admin endpoints to `InquiryController` (list, get,
resolve) but `/api/inquiries` was never added to `SecurityConfig`'s
`permitAll` list, so the whole controller now falls under
`anyRequest().authenticated()`. Re-verified live:

```
POST /api/inquiries  {"name":"Test","email":"t@example.com",
                       "subject":"Hi","message":"Hello there"}
(no Authorization header)
→ 403 Forbidden
```

This breaks a core v1 requirement — the client's brief explicitly says
the inquiry form needs no account ("fully public to submit"). A mobile
app user who isn't logged in currently cannot submit an inquiry at all.
Separately, the three new admin endpoints (`GET /`, `GET /{id}`,
`PUT /{id}/resolve`) only require *some* authenticated user, not
specifically `ROLE_ADMIN` — so once the public-submit fix adds
`/api/inquiries` to `permitAll` for POST, the admin-only endpoints still
need their own `hasRole("ADMIN")` matcher, or any logged-in mobile app
user could view and resolve inquiries meant for admin eyes only.

### 5. NEW — Gallery POST can 500 for anonymous callers, and DELETE has zero auth
`GalleryController.create()` requires `@AuthenticationPrincipal
UserDetails` to set `uploadedBy`, but `/api/gallery/**` is `permitAll` —
an anonymous POST hits a `NullPointerException` (500) instead of a clean
401/403. `DELETE /api/gallery/{id}` needs no principal at all and no
role check — same open-write pattern as Category/Service/Banner.

## Modules with no use in v1

Exist in the codebase, fully working, but called by nothing in the v1
(or even v1.1) checklist:

- **Cart** (`/api/cart`) — v1.1 commerce, not v1.
- **Wishlist** (`/api/wishlist`) — not in v1 *or* v1.1 checklist at all.
- **Product Reviews** (`/api/reviews`) — not in v1 *or* v1.1 checklist at
  all.
- **Newsletter** (`/api/newsletter`) — not in v1 *or* v1.1 checklist at
  all.
- **Order** (`/api/orders`) — v1.1 commerce, not v1.
- **Notification** (`/api/notifications`) — v1.1 (FCM), not v1. Partial
  plumbing landed in the `main` merge: `firebase-admin` is now a `pom.xml`
  dependency, `FirebaseConfig` initializes the Firebase SDK from a
  `firebase-service-account.json` file (gitignored, not present in the
  repo — non-fatal if missing, `FirebaseConfig` just logs an error and
  continues), and `DeviceToken`/`DeviceTokenController`/
  `DeviceTokenRepository` exist for storing push tokens. `POST
  /{id}/send` on `NotificationController` still doesn't appear to call
  FCM directly — worth a closer look when v1.1 work actually starts, not
  now.
- **Contact** (`/api/contact`) — sends an email but never persists
  anything; functionally a near-duplicate of Inquiry submission
  (`ContactRequest` is the same shape as `InquiryRequest` minus
  `subject`). The v1 design has "Contact Us" as a static info screen
  (phone/email/address from `Company` + WhatsApp deep link), not a form —
  so this controller likely isn't needed for v1 at all. **Worth
  confirming with the client** rather than assuming.
- **Banner** (`/api/banners`) — a full carousel entity (title, subtitle,
  CTA, display order). The v1 admin panel checklist doesn't list "banner
  management" as a module, and the v1 mobile home screen says "company
  banner" (singular — likely `Company.bannerUrl`), not a carousel.
  **Ambiguous — confirm with the client** whether this is v1.1 or dead
  code; it's currently also part of the open `permitAll()` security gap.

**Decision (2026-07-10): keep all of these.** None get deleted — a
dormant, unused controller is genuinely harmless (nothing calls it, no
runtime cost) as long as it *compiles* and doesn't itself have bugs. That
distinction matters and just came up in practice: `WishlistService` had a
broken method (`Product.getImageUrl()`, which doesn't exist on `Product`)
that failed the build for the *entire* repo, even though nothing in v1 or
v1.1 calls Wishlist — Java compiles everything as one unit, so a bug in
dormant code still blocks everyone. That method was dead code (nothing
called it) and got removed; see the changelog below. The Wishlist API
surface itself (`WishlistController`, `/api/wishlist`) stays.

Two things to keep in mind about "harmless while dormant":
- **It only stays harmless if it keeps compiling and stays bug-free.**
  Since nobody's actively building against Cart/Wishlist/Reviews/
  Newsletter/Order/Notification/Contact/Banner right now, bugs in them
  can sit unnoticed until they break a build for an unrelated reason
  (like just happened) — worth a quick sanity check (`mvn compile`)
  after pulling changes that touch these files.
- **Dormant doesn't mean unreachable.** These are live REST endpoints the
  moment the backend is running, regardless of whether any frontend
  calls them — the `permitAll()` security gap applies to Category/
  Service/Banner today regardless of v1 status, so locking that down
  isn't optional just because a module is "not in scope yet."

Classification of Contact and Banner (v1 / v1.1 / genuinely unused) is
still worth confirming with the client for documentation accuracy, but
that's a scope question now, not a keep-or-delete one.

## Config / infra notes
- Secrets (DB password, mail credentials, JWT secret) now come from env
  vars via `.env` (gitignored) — see `.env.example`. Was previously
  hardcoded and committed; see [context.md](context.md) for the rotation
  note on the exposed Gmail app password.
- No Spring profiles (`application-dev.yaml` / `application-prod.yaml`)
  yet — one config for all environments.
- No Flyway/Liquibase — schema is `ddl-auto: update` (Hibernate
  auto-migration). Fine for two devs early on; worth revisiting before
  going to a shared/staging DB.

## Next steps (backend, to unblock v1)
Re-ordered 2026-07-10 after the `main` merge closed several items but
introduced one urgent regression.

1. **Fix the Inquiry public-submit regression** — add `/api/inquiries`
   (POST only) to `SecurityConfig`'s `permitAll`, and add
   `hasRole("ADMIN")` on the list/get/resolve endpoints so they're not
   left open to any logged-in user. This is actively broken for v1 right
   now, highest priority.
2. **Fix the remaining password leak** — `GET /api/auth/me` still returns
   the raw `User` entity; apply the same `UserResponse` DTO fix
   `ProfileController` already uses. Small, isolated.
3. **Lock down the `permitAll()` security gap** — still open for
   Category/Service/Banner (unchanged) and now also Product/Gallery/
   Company (new, inherited by their new write endpoints). Split each
   into GET-permitAll / write-hasRole("ADMIN"), following the pattern
   `/api/admin/**` and `/api/orders/all` already use correctly.
4. Fix `GalleryController.create()`'s `NullPointerException` risk for
   anonymous callers (needs `@AuthenticationPrincipal` but the path is
   `permitAll`) — folds into the security-gap fix above once POST
   requires a role.
5. Build the actual `CloudinaryService` (SDK dependency + upload/delete
   calls) — the `Gallery.cloudinaryPublicId` column is ready
   (2026-07-10), `GalleryController.create()` still needs to become a
   real multipart upload instead of a raw JSON body.
6. Product: wire Cloudinary image upload (schema mostly ready via
   `ProductImage`), add pagination (`Pageable`) to the catalog GET, add
   the `specifications` JSONB column per the earlier decision.
7. Service: add the missing update (PUT) endpoint.
8. Confirm with the client whether Contact and Banner are v1.1 scope or
   genuinely unused (not a deletion question — both stay in the codebase
   either way, per the 2026-07-10 keep-dormant decision).

## Changelog
- **2026-07-10** — Re-audited all confirmed defects against `main`'s
  merged code. Fixed by the merge: password leak in `ProfileController`
  (now `UserResponse` DTO), `LazyInitializationException` on
  `Category`/`Product` (now `FetchType.EAGER` + DTOs), Product CRUD,
  Company update endpoint, Inquiry admin list/resolve endpoints. Still
  open: `GET /api/auth/me` password leak, the `permitAll()` security gap
  (now also covering Product/Gallery/Company). Newly introduced by the
  merge: Inquiry's public submit endpoint now requires auth (breaks v1),
  Gallery's POST can 500 for anonymous callers. All re-verified live
  against a running instance, not just read from source.
- **2026-07-10** — Added `Gallery.cloudinaryPublicId` column (verified
  live via Hibernate's `ddl-auto: update` DDL log:
  `alter table if exists gallery add column cloudinary_public_id
  varchar(255)`). Prepares the schema for Cloudinary integration; the
  actual `CloudinaryService`/SDK/upload flow still needs building —
  nothing writes to this column yet.
- **2026-07-10** — Fixed a build-breaking bug on `developer-2`, later
  superseded by `main`'s proper fix during the merge:
  `WishlistService.toResponse()` called `Product.getImageUrl()`, which
  doesn't exist on the `Product` entity. Initially fixed by removing the
  dead method; the `main` merge then brought in a complete, correct
  `WishlistService`/`WishlistResponse` (proper DTO mapping via
  `product.getImages()`), which is what's in the codebase now.
