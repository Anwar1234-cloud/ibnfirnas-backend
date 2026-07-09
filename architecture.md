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

## Backend module → v1 requirement cross-check

This is what actually exists in `src/main/java/com/ibnfirnas` today,
checked against what v1 needs it to do.

| Module | v1 needs | Current state | Gap |
|---|---|---|---|
| Auth | Register, login, JWT, roles | `AuthController` has register/login/forgot-password/reset-password/refresh-token/me. `SecurityConfig` uses BCrypt + stateless JWT. `UserRole` = `ROLE_USER`/`ROLE_ADMIN`. | None — solid. |
| User profile | Get/update profile | `ProfileController` GET/PUT `/api/profile`, backed by `UserRepository`. | None. |
| Dashboard | Product/service/inquiry counts | `DashboardService.getStats()` returns totalUsers, totalProducts, totalOrders, totalInquiries, totalServices. | None (has a couple extra fields, harmless). |
| Product | Full CRUD + image upload (Cloudinary) | `ProductController` / `ProductService` are **read-only**: list, by-id, featured. No create/update/delete. `ProductRequest` DTO exists but nothing calls it. | **Missing:** create/update/delete endpoints, image upload wiring. |
| Service | Full CRUD | `ServiceController` has GET (all/featured/by-id) + POST create + DELETE. | **Missing:** update (PUT). |
| Gallery | Upload/delete/list | `GalleryController` is **read-only** — one GET endpoint, talks directly to `GalleryRepository` (no service layer). | **Missing:** upload/delete endpoints entirely. |
| Company | GET public / PUT admin | `CompanyController` is **read-only** — GET only. | **Missing:** update endpoint. |
| Inquiry | POST public / GET + status update admin | `InquiryController` has POST only (public submit). No admin list or status/notes update. | **Missing:** admin GET list + status update. |
| Image storage | Cloudinary | `FileStorageService` saves to a local `./uploads` folder via `Files.copy`, no Cloudinary SDK in `pom.xml`. | **Not started** — needs Cloudinary integration to match architecture. |
| Order/Payment/Notification | Deferred to v1.1 | Order and Notification already have partial CRUD scaffolding; Payment has none. | Fine to leave as-is — not v1-blocking. |

## Full v1 API inventory (verified against a running instance, 2026-07-09)

Every endpoint that exists in the codebase today, for the modules that are
actually in v1. "Auth (actual)" is what `SecurityConfig` really enforces
right now, not what it should enforce — see "Security gap" below for the
difference.

### Auth — `/api/auth`
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| POST `/register` | public | `{fullName, email, password, phone}` | `{token, email, fullName, role}` | Mobile app |
| POST `/login` | public | `{email, password}` | `{token, email, fullName, role}` | Mobile app, Admin panel |
| POST `/forgot-password` | public | `{email}` | `void` | Mobile app |
| POST `/reset-password` | public | `{token, newPassword}` | `void` | Mobile app |
| POST `/refresh-token` | authenticated | — (Bearer token) | `{accessToken}` | Both |
| GET `/me` | authenticated | — | full `User` entity **(leaks password hash — see defects)** | Both |

### Profile — `/api/profile`
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| GET `/` | authenticated | — | full `User` entity **(leaks password hash)** | Mobile app |
| PUT `/` | authenticated | full `User` JSON (only fullName/phone/avatarUrl are actually applied) | full `User` entity **(leaks password hash)** | Mobile app |

### Product — `/api/products`
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<Product>`, no pagination | Mobile app | Built, needs pagination |
| GET `/{id}` | public | — | `Product` | Mobile app | Built |
| GET `/featured` | public | — | `List<Product>` | Mobile app (home) | Built |
| POST `/` | **public — should be admin** | `ProductRequest` DTO exists, unused | — | Admin panel | **Missing** |
| PUT `/{id}` | **public — should be admin** | — | — | Admin panel | **Missing** |
| DELETE `/{id}` | **public — should be admin** | — | — | Admin panel | **Missing** |
| image upload | — | — | — | Admin panel | **Missing entirely** |

### Category — `/api/categories`
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| GET `/` | public | — | `List<Category>` **(500s once any category has children — see defects)** | Mobile app |
| GET `/{id}` | public | — | `Category` **(500s if it has children)** | Mobile app |
| POST `/` | **public, no role check** | raw `Category` entity | `Category` | Admin panel |
| DELETE `/{id}` | **public, no role check** | — | `void` | Admin panel |

### Service — `/api/services`
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<ServiceEntity>` | Mobile app | Built |
| GET `/featured` | public | — | `List<ServiceEntity>` | Mobile app (home) | Built |
| GET `/{id}` | public | — | `ServiceEntity` | Mobile app | Built |
| POST `/` | **public, no role check** | raw `ServiceEntity` | `ServiceEntity` | Admin panel | Built (insecure) |
| PUT `/{id}` | — | — | — | Admin panel | **Missing** |
| DELETE `/{id}` | **public, no role check** | — | `void` | Admin panel | Built (insecure) |

### Gallery — `/api/gallery`
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<Gallery>`, ordered by `displayOrder` | Mobile app | Built, controller calls repository directly (no service layer) |
| POST `/` (upload) | — | — | — | Admin panel | **Missing entirely** |
| DELETE `/{id}` | — | — | — | Admin panel | **Missing entirely** |

### Company — `/api/company`
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `Company` (about/mission/vision/contact/logo/banner) | Mobile app | Built |
| PUT `/` | — | — | — | Admin panel | **Missing** |

### Inquiry — `/api/inquiries`
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| POST `/` | public | `InquiryRequest {name, email, phone, subject, message}` | `Inquiry` | Mobile app | Built |
| GET `/` (admin list) | — | — | — | Admin panel | **Missing** |
| PATCH/PUT `/{id}/status` | — | — | — | Admin panel | **Missing** |

### Dashboard — `/api/admin/dashboard`
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| GET `/stats` | **public — should be admin** (not in any `permitAll` matcher, but not `hasRole` either — falls to `anyRequest().authenticated()`, so actually just needs *any* logged-in user, not specifically an admin) | — | `{totalUsers, totalProducts, totalOrders, totalInquiries, totalServices}` | Admin panel |

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
**Schema gap:** current `Gallery` entity (`mediaUrl`, `thumbnailUrl`,
`mediaType`) has no `public_id` field. When Cloudinary stores an image it
hands back two things: a `secure_url` (the link to view/display it) and a
`public_id` (its internal file identifier). To delete that image later,
Cloudinary's API asks for the `public_id`, not the URL. If we only save
the URL and never save the `public_id`, "delete" in the admin panel can
only remove our database row — the actual file stays sitting in
Cloudinary's storage forever, orphaned and still counting against the
account's storage quota. Fix: add a `cloudinaryPublicId` column and save
it at upload time (Cloudinary already returns it for free — no extra
API call needed).

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

## Security gap — confirmed by running the server (2026-07-09)

`SecurityConfig` currently does this:

```java
.requestMatchers("/api/products/**").permitAll()
.requestMatchers("/api/services/**").permitAll()
.requestMatchers("/api/gallery/**").permitAll()
.requestMatchers("/api/company/**").permitAll()
.requestMatchers("/api/banners/**").permitAll()
```

`permitAll()` on a path pattern applies to **every HTTP method**, not just
GET. This isn't theoretical — verified by starting the backend and
calling `POST /api/categories` with **no `Authorization` header at all**:

```
POST /api/categories  {"name":"Parent Cat","slug":"parent-cat"}
→ 200 OK, category created
```

Since `ServiceController`, `BannerController`, and `CategoryController`
already expose `POST`/`PUT`/`DELETE` under permitAll paths, anyone —
unauthenticated, no token, nothing — can create/delete services, banners,
and categories right now. Once Product, Gallery, and Company get their
write endpoints (per the table above), the same hole opens there too
unless the fix lands first.

Fix needed: split each of these into separate GET-permitAll /
write-hasRole("ADMIN") matchers, e.g.:

```java
.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
.requestMatchers("/api/products/**").hasRole("ADMIN")
```

(repeated per module), instead of relying on the catch-all
`/api/admin/**` prefix that only `DashboardController` currently uses —
and even that one isn't actually role-gated (see defects below).

## Confirmed defects (verified by running the server, 2026-07-09)

These were reproduced against a live instance, not inferred from reading
code — both will affect v1 directly.

### 1. Password hash leaks in API responses
`AuthController.getCurrentUser()` (`GET /api/auth/me`) and both
`ProfileController` endpoints (`GET`/`PUT /api/profile`) return the raw
`User` JPA entity instead of a DTO. The entity has no `@JsonIgnore` on
`password`, so the BCrypt hash goes out over the wire on every profile
fetch:

```json
GET /api/auth/me →
{"data":{"id":1,"email":"...","password":"$2a$10$9n.QCOtilYVm59r6lQs2SeIGP.ev2T9yfvn5tKd2xq.j1Bue1y91.", ...}}
```

Not exploitable on its own (BCrypt is slow to crack), but it's a hash
disclosure that should never leave the server, and it means every mobile
app response, every network log, every crash reporter that captures
response bodies now potentially contains a password hash. Fix: return a
`UserResponse` DTO (id, email, fullName, phone, avatarUrl, role) instead
of the entity, on both endpoints.

### 2. `LazyInitializationException` — GET endpoints 500 once relations are populated
Reproduced directly: created a parent category, then a child category
pointing at it (via the open `POST /api/categories`, see security gap
above), then called `GET /api/categories`:

```json
{"success":false,"message":"Something went wrong: Could not write JSON:
failed to lazily initialize a collection of role:
com.ibnfirnas.entity.Category.children: could not initialize proxy -
no Session","data":null}
```
→ HTTP 500, and this breaks **every** row in the list, not just the one
with a child.

Root cause: `application.yaml` sets `open-in-view: false` (good practice
on its own), but no entity DTO layer sits between JPA and Jackson for
these modules — controllers return entities directly. Spring Data JPA
repository calls are transactional only for the duration of the query
itself; by the time Jackson serializes the response, the Hibernate
session is closed. Any `@OneToMany`/lazy collection Jackson tries to
touch during serialization throws.

This is confirmed on `Category.children` right now, and **will hit the
same wall on `Product.images` (`@OneToMany`, lazy) the moment Product
CRUD is built and a product has more than the default fetch**, and
potentially `Cart.items` once cart has data. It doesn't show up in
today's read-only endpoints only because there's no data yet to trigger
it (empty tables serialize fine).

Fix: introduce response DTOs (`ProductResponse` already exists as a
class but isn't used by the controller — same fix needed for
`CategoryResponse`), or annotate collections `@JsonIgnore` where the
frontend doesn't need them nested, rather than relying on returning raw
entities anywhere a `@OneToMany`/`@ManyToMany` exists.

### 3. Dashboard stats endpoint isn't actually admin-only
`GET /api/admin/dashboard/stats` lives under `/api/admin/**`, which
*is* covered by `.requestMatchers("/api/admin/**").hasRole("ADMIN")` in
`SecurityConfig` — so this one's fine. Listed here only to confirm it
was checked, not to report a bug: this is the one write-sensitive
endpoint currently protected correctly, and the pattern it uses
(`/api/admin/**` + `hasRole`) is the one to replicate for the other
modules' write endpoints once they're built.

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
- **Notification** (`/api/notifications`) — v1.1 (FCM), not v1; also has
  no actual Firebase integration yet (`POST /{id}/send` just flips a flag
  in the DB, doesn't call FCM — no `firebase-admin` dependency in
  `pom.xml`).
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

None of these need to be deleted — they're harmless sitting dormant — but
worth an explicit decision with the client rather than silently carrying
unused surface area (and unused *attack* surface, given the security gap
applies to some of them too).

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
Ordered by what blocks the most other work, given the confirmed defects
above.

1. **Fix the password leak** (`GET /api/auth/me`, `GET`/`PUT
   /api/profile`) — return a `UserResponse` DTO instead of the raw
   entity. Small, isolated, no dependency on anything else.
2. **Fix `LazyInitializationException`** on `Category` now (already
   reproducing in prod-shaped data), and preemptively on `Product`/`Cart`
   before their write endpoints go live — introduce response DTOs rather
   than returning entities directly wherever a `@OneToMany` exists.
3. **Lock down the `permitAll()` security gap** — currently
   unauthenticated write access to Category/Service/Banner, and it'll
   extend to Product/Gallery/Company the moment those get write
   endpoints if not fixed first.
4. Replace `FileStorageService` (local disk) with a `CloudinaryService`
   (Cloudinary Java SDK) — every module below depends on this.
5. Product: add `specifications` JSONB column, add create/update/delete
   endpoints with Cloudinary image upload wired to `ProductImage`, add
   pagination (`Pageable`) to the catalog GET.
6. Gallery: add a `GalleryService` layer (currently the controller talks
   to the repository directly), add a `cloudinaryPublicId` column, add
   upload/delete endpoints.
7. Service: add the missing update (PUT) endpoint + Cloudinary image
   upload.
8. Company: add the update (PUT, admin-only) endpoint.
9. Inquiry: add admin list (GET) + status update (PATCH) endpoints, add a
   `notes` field.
10. Get client decisions on: Contact controller (keep or drop), Banner
    module (v1, v1.1, or drop), and Cart/Wishlist/Review/Newsletter (keep
    dormant, remove, or fold into v1.1 scope).
