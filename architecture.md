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
Inquiry admin endpoints, Company update endpoint), then fixing the
Inquiry auth regression and adding Company's missing social-link fields
the same day, then — later the same day — closing the `permitAll()`
write-endpoint gap across six modules, moving inquiry submission behind
auth, and adding a user-scoped inquiry list. See "Confirmed defects"
below for what's still open.

| Module | v1 needs | Current state | Gap |
|---|---|---|---|
| Auth | Register, login, JWT, roles | `AuthController` has register/login/reset-password(phone OTP)/refresh-token/me. `SecurityConfig` uses BCrypt + stateless JWT. `UserRole` = `ROLE_USER`/`ROLE_ADMIN`. | **Fixed** — no more password leak on `/me`, no more 500-vs-401 on `/me`/`refresh-token`, forgot-password rebuilt on phone OTP (2026-07-18, no more email enumeration). |
| User profile | Get/update profile | `ProfileController` GET/PUT `/api/profile`, now returns a `UserResponse` DTO, password change flow added. | **Fixed** — no more entity leak here. |
| Dashboard | Product/service/inquiry counts | `DashboardService.getStats()` returns totalUsers, totalProducts, totalOrders, totalInquiries, totalServices. | None. |
| Product | Full CRUD + image upload (Cloudinary) | `ProductController`/`ProductService` now have full GET/POST/PUT/DELETE via `ProductRequest`/`ProductResponse`. `Product.images` is `FetchType.EAGER` now (fixes the lazy-load crash). | **Still missing:** pagination on the catalog GET, actual Cloudinary upload (still no image upload wiring — `ProductRequest` doesn't carry a file). |
| Service | Full CRUD | `ServiceController` now has GET (all/featured/by-id) + POST create + `PUT /{id}` update (added 2026-07-15, superseded by `origin/main`'s `ServiceRequest`-validated version in the 2026-07-16 merge) + DELETE. | **Fixed** — full CRUD complete. |
| Gallery | Upload/delete/list | `GalleryController` has GET/POST/DELETE, returning a `GalleryResponse` DTO as of 2026-07-15 (no more raw-entity leak). POST still takes a raw JSON `Gallery` body (no multipart) — the admin panel is expected to call `POST /api/upload/image` first to get a Cloudinary URL, then POST that URL here, same pattern Product uses. DELETE now calls `CloudinaryService.deleteImage()` before removing the row (2026-07-15), so deletes no longer orphan Cloudinary assets. | Functionally complete for v1; still no direct multipart upload on `/api/gallery` itself (relies on the generic `/api/upload/image` two-step flow). |
| OTP | Not in original v1 checklist | `OtpController`/`OtpService`, **phone-only as of 2026-07-18** (email OTP path removed entirely, see below). `POST /api/otp/send` / `POST /api/otp/verify`, both public, delegate to Twilio Verify. | **Wired into forgot-password** (`POST /api/auth/reset-password` now verifies a phone OTP and sets the new password in one call) — registration/login OTP still not gating anything, remains optional/unused by those flows. See "OTP module" section below. |
| Company | GET public / PUT admin + social links | `CompanyController` has GET/POST/PUT (`PUT /api/company/{id}`). Added `websiteUrl`/`facebookUrl`/`instagramUrl`/`twitterUrl` 2026-07-10. | Functionally complete for v1 — still unauthenticated writes, see security gap. |
| Inquiry | POST public / GET + status update admin | `InquiryController` has POST/GET list/GET by id/PUT resolve/GET my. `POST` now requires authentication (any role) as of 2026-07-10 — a **scope change**, no longer "public" per the v1 brief; rest still `hasRole("ADMIN")`, plus a new `GET /my` (authenticated) so a user can see their own inquiries. | **Behavior changed by decision, not defect** — worth confirming with the client. |
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
| POST `/reset-password` | public | **Rebuilt 2026-07-18**: `{phone, otp, newPassword}` — verifies a phone OTP via `OtpService` and sets the password in one call. `POST /forgot-password` no longer exists; request the code via `POST /api/otp/send {phone, purpose: "FORGOT_PASSWORD"}` instead. | `void` | Mobile app |
| POST `/refresh-token` | `.authenticated()` — **fixed 2026-07-18**, carved out of the `permitAll` `/api/auth/**` | — (Bearer token) | `{accessToken}` | Both |
| GET `/me` | `.authenticated()` — **fixed 2026-07-18** | — | `UserResponse` DTO, no password | Both |

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
| POST `/` | `hasRole("ADMIN")` — **fixed 2026-07-10** | `ProductRequest` JSON, no file field | `ProductResponse` | Admin panel | Built, verified live, no image upload |
| PUT `/{id}` | `hasRole("ADMIN")` — **fixed 2026-07-10** | `ProductRequest` JSON | `ProductResponse` | Admin panel | Built, verified live |
| DELETE `/{id}` | `hasRole("ADMIN")` — **fixed 2026-07-10** | — | — | Admin panel | Built, verified live |
| image upload | — | — | — | Admin panel | **Still missing entirely** — no multipart handling, no Cloudinary |

### Category — `/api/categories` — **lazy-load crash fixed**
| Method & path | Auth (actual) | Request | Response | Consumer |
|---|---|---|---|---|
| GET `/` | public | — | `List<Category>` (`children` now `FetchType.EAGER` + `@JsonIgnore` — no more 500) | Mobile app |
| GET `/{id}` | public | — | `Category` | Mobile app |
| POST `/` | `hasRole("ADMIN")` — **fixed 2026-07-10** | raw `Category` entity | `Category` | Admin panel |
| DELETE `/{id}` | `hasRole("ADMIN")` — **fixed 2026-07-10** | — | `void` | Admin panel |

### Service — `/api/services` — **PUT added 2026-07-15/16**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<ServiceEntity>` | Mobile app | Built |
| GET `/featured` | public | — | `List<ServiceEntity>` | Mobile app (home) | Built |
| GET `/{id}` | public | — | `ServiceEntity` | Mobile app | Built |
| POST `/` | `hasRole("ADMIN")` | `@Valid ServiceRequest` JSON | `ServiceEntity` | Admin panel | Built, now validated (`@NotBlank name`) |
| PUT `/{id}` | `hasRole("ADMIN")` | `@Valid ServiceRequest` JSON | `ServiceEntity` | Admin panel | **Built 2026-07-15**, validated version from `origin/main` kept during the 2026-07-16 merge |
| DELETE `/{id}` | `hasRole("ADMIN")` | — | `void` | Admin panel | Built, verified live |

No category/grouping field exists on `ServiceEntity` — unlike Product,
services aren't linked to `Category` or anything similar, just a flat
`displayOrder`. Confirmed 2026-07-10 while answering a question about
whether services can be filtered/grouped like products. Not a defect,
just a gap worth flagging if the client wants service filtering in a
later version — would need a new FK (reusing `Category` or a simpler
flat `serviceType` field).

### Gallery — `/api/gallery` — **password leak + Cloudinary cleanup fixed 2026-07-15**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `List<GalleryResponse>` (DTO, no `uploadedBy`) — **fixed 2026-07-15**, ordered by `displayOrder` | Mobile app | Built, no more password-hash leak |
| POST `/` | `hasRole("ADMIN")` | raw JSON `Gallery` body — **not multipart**; admin panel calls `POST /api/upload/image` first to get a Cloudinary URL, then posts that URL here | `GalleryResponse` | Admin panel | Built, verified live with an admin token |
| DELETE `/{id}` | `hasRole("ADMIN")` | — | `void` | Admin panel | Built — **now also deletes the Cloudinary asset** via `CloudinaryService.deleteImage()` before removing the row (2026-07-15), no longer orphans files |

### OTP — `/api/otp` — **phone-only as of 2026-07-18, wired into forgot-password**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| POST `/send` | public | `SendOtpRequest {phone, purpose}` — `purpose` one of `REGISTRATION`/`LOGIN`/`FORGOT_PASSWORD`, case-insensitive | `void`, masked-phone message | Mobile app | Built. Delegates entirely to Twilio Verify SMS — no local DB tracking at all. |
| POST `/verify` | public | `VerifyOtpRequest {phone, otp, purpose}` — **`purpose` case-sensitivity bug fixed 2026-07-18**, now uppercases like `/send` before parsing | `boolean` | Mobile app | Built. Delegates to Twilio's `VerificationCheck`. `purpose` is validated for a well-formed value but not actually passed to Twilio (see caveat below). |

**Email OTP removed entirely 2026-07-18** — `SendOtpRequest`/`VerifyOtpRequest`
no longer have an `email` field, `phone` is the sole required
identifier. The email-OTP-only infrastructure it needed
(`OtpVerification` entity, `OtpVerificationRepository`, `OtpType` enum,
`EmailService.sendOtpEmail()`) was deleted rather than left dormant,
since nothing could reach it anymore.

**Now wired into `POST /api/auth/reset-password`** (forgot-password
flow) — see the Auth table above. Registration/login still don't call
`OtpService` at all; OTP verification isn't a gate on those flows, it's
only used for password reset right now.

**Known caveat, not fixed**: Twilio Verify doesn't track *why* a code
was requested — `purpose` is app-level bookkeeping only, not enforced
by Twilio. A valid SMS code obtained via a `LOGIN` or `REGISTRATION`
send could technically also be used to reset a password through
`/reset-password`, since `OtpService.verifySmsOtp()` doesn't take a
purpose parameter at all. Fixing this properly would mean tracking SMS
OTP sends locally too (mirroring how the old email path worked) — out
of scope for the forgot-password rework, worth revisiting if it
matters for the threat model.

`.env.example` still hasn't been updated with the
`TWILIO_ACCOUNT_SID`/`TWILIO_AUTH_TOKEN`/`TWILIO_VERIFY_SERVICE_SID`
vars `application.yaml` requires.

### Company — `/api/company` — **write endpoints added, social/website fields added**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| GET `/` | public | — | `Company` (about/mission/vision/contact/logo/banner + `websiteUrl`/`facebookUrl`/`instagramUrl`/`twitterUrl`, added 2026-07-10) | Mobile app | Built |
| POST `/` | `hasRole("ADMIN")` — **fixed 2026-07-10** | raw `Company` entity | `Company` | Admin panel | Built, verified live — still no singleton constraint, so an admin calling `POST` instead of `PUT` can still create a duplicate row |
| PUT `/{id}` | `hasRole("ADMIN")` — **fixed 2026-07-10** | raw `Company` entity | `Company` | Admin panel | Built, verified live |

No `linkedinUrl` field — was briefly added then removed before the
column made it into a commit; not present in the entity.

### Inquiry — `/api/inquiries` — **admin endpoints fixed 2026-07-10 AM, submit auth changed 2026-07-10 PM**
| Method & path | Auth (actual) | Request | Response | Consumer | Status |
|---|---|---|---|---|---|
| POST `/` | **authenticated (any role) — changed 2026-07-10** | `InquiryRequest {name, email, phone, subject, message}` | `InquiryResponse`, now linked to the caller via `Inquiry.user` | Mobile app | Working, but no longer public — guest submission removed by decision. Verified live: anonymous `403`, logged-in `200`. |
| GET `/my` | authenticated (any role) — **new 2026-07-10** | — | `List<InquiryResponse>`, only the caller's own inquiries | Mobile app | Working, verified live — correctly scoped, doesn't leak other users'/guest inquiries |
| GET `/` (admin list) | `hasRole("ADMIN")` — **fixed, verified live** (a plain `ROLE_USER` token gets `403`) | — | `List<InquiryResponse>` | Admin panel | Working, correctly gated |
| GET `/{id}` | `hasRole("ADMIN")` | — | `InquiryResponse` | Admin panel | Working, correctly gated |
| PUT `/{id}/resolve` | `hasRole("ADMIN")` | — | `InquiryResponse` | Admin panel | Working, correctly gated |

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

## Security gap — closed for Product/Category/Service/Gallery/Company/Banner and Inquiry (2026-07-10)

`SecurityConfig` now splits every content-management module into a
GET-permitAll / write-hasRole("ADMIN") pair — the pattern first proven
on Inquiry earlier the same day:

```java
.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
.requestMatchers("/api/products/**").hasRole("ADMIN")
```

(repeated for Category, Service, Gallery, Company, Banner.) Verified
live 2026-07-10 with three identities — anonymous, a `ROLE_USER` token,
a `ROLE_ADMIN` token — against all six modules: GET stayed `200` for
everyone, POST became `403` for anonymous and `ROLE_USER`, `200` for
`ROLE_ADMIN`. `Product`, `Company`, and `Gallery`'s write endpoints
(added in the earlier `main` merge, previously wide open) are covered by
this fix too.

`POST /api/inquiries` also changed the same day — no longer `permitAll`,
now `authenticated()` (any role, not just admin). See the Inquiry table
above and `changelog.md` for the scope-change discussion (this removes
guest inquiry submission, a deviation from the v1-confirmed brief that
needs client confirmation).

**Notification is the one module that never got this treatment.**
`/api/notifications/**` has no matcher in `SecurityConfig` at all, so it
falls through to `anyRequest().authenticated()` — any logged-in user,
not just admin, can hit `GET /api/notifications` (which also leaks a
password hash, see defect 1c above). The *write* endpoints on
`NotificationController` do have method-level
`@PreAuthorize("hasRole('ADMIN')")` as of 2026-07-15, but that's
inconsistent with every other module's pattern of a request-matcher
rule in `SecurityConfig`, and doesn't cover the `GET`.

**401/403 error responses — centralized 2026-07-16, uncommitted.**
Spring Security's default behavior for a rejected request (no token on
an endpoint that requires one → 401; wrong role on an admin-only
endpoint → 403) was an empty response body — confirmed live 2026-07-10,
documented in `frontend-integration-spec.md`. Two new classes,
`RestAuthenticationEntryPoint` (401) and `RestAccessDeniedHandler`
(403), are now wired into `SecurityConfig` via
`.exceptionHandling(...)` and return the same `{success, message,
data}` JSON envelope as every other response
(`{"success": false, "message": "Authentication required"/"Access
denied", "data": null}`). This only fires when Spring Security itself
rejects the request — it does **not** cover defect 1e's
`/refresh-token` case, where the matcher is `permitAll` and the crash
happens inside the controller instead (see 1e above). Currently local,
uncommitted work on `developer-2` as of this writing.

**`JwtAuthenticationFilter` hardened against deleted-user tokens —
2026-07-16, uncommitted.** A valid, unexpired JWT referencing a user
that's since been deleted used to throw an unhandled
`UsernameNotFoundException` mid-filter-chain (an unhandled 500 for any
request carrying such a token). Now caught explicitly: logs at debug
level, clears the security context, and lets the request continue as
anonymous — so a protected endpoint correctly falls through to the
401/403 handlers above instead of crashing.

**Auth error responses — standardized 2026-07-17.** Previously, Spring
Security's default behavior (no configured entry point/handler) meant
any auth failure — missing token, invalid token, wrong role — returned
a bare `403` with an **empty body**, indistinguishable from each other.
Added `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`
(`security/`), wired into `SecurityConfig` via `.exceptionHandling(...)`:
missing/invalid auth now returns `401
{"success":false,"message":"Authentication required","data":null}`;
valid auth but wrong role now returns `403
{"success":false,"message":"Access denied","data":null}` — same
envelope as every other response. Also fixed a related bug in
`JwtAuthenticationFilter`: a validly-signed token referencing a
deleted/nonexistent user used to throw an uncaught
`UsernameNotFoundException` that broke the filter chain before it
reached the `permitAll` check, so a stale token could even break
*public* endpoints. Now caught and treated as anonymous. See
`changelog.md`'s 2026-07-17 entry for verification detail —
`frontend-integration-spec.md` §1/§8 documented the old empty-body
behavior and has been corrected.

**CORS — consolidated 2026-07-15.** Previously two competing configs
existed: a standalone `CorsFilter` bean in `CorsConfig.java` and
`SecurityConfig`'s own `CorsConfigurationSource`, with contradicting
settings (one had drifted to `allowedOriginPatterns("*")` **with**
`allowCredentials(true)` — any origin, with credentials, defeating CORS
as a control). `CorsConfig.java` was deleted; `SecurityConfig` is now
the single source of truth, with allowed origins read from a new
`CORS_ALLOWED_ORIGINS` env var (comma-separated) instead of a hardcoded
value — set this to the real admin-panel/mobile-app origins before
deploying anywhere beyond `localhost:5173`.

**Still fully open, left unchanged by design:**
```java
.requestMatchers("/api/contact/**").permitAll()
.requestMatchers("/api/reviews/product/**").permitAll()
```
`permitAll()` on these path patterns still applies to every HTTP method,
so Contact/Reviews writes remain unauthenticated. Left as-is because
neither is in the v1 checklist — see "Modules with no use in v1" below
— but worth revisiting if either becomes load-bearing. (The Newsletter
module this used to also cover was deleted 2026-07-18 — see below.)

## Confirmed defects (re-verified live, 2026-07-10, after the `main` merge)

Status of each defect found on 2026-07-09, re-tested against a fresh
instance after merging `main`'s DTO/entity fixes into `developer-2`, plus
one new issue the merge introduced.

### 1. Password hash leak — **fixed 2026-07-15**
`ProfileController` (`GET`/`PUT /api/profile`) returns a `UserResponse`
DTO. `AuthController.getCurrentUser()` (`GET /api/auth/me`) was fixed
2026-07-15 — now also returns a `UserResponse` DTO instead of the raw
`User` entity. Both branches (`developer-2` and `origin/main`)
independently made this exact fix before the 2026-07-16 merge; no leak
remains on either endpoint.

### 1b. `GET /api/gallery` leaked the uploader's password hash, publicly — **fixed 2026-07-15**
`Gallery.uploadedBy` was a `@ManyToOne User` with no `@JsonIgnore`, and
`GalleryController` returned raw `Gallery` entities — reachable by
anyone, no authentication required at all, worse than the `/auth/me`
leak since that at least needs the victim's own token. Fixed by wiring
the previously-unused `GalleryResponse` DTO (it already existed in the
codebase, just was never used by the controller) into both `GET` and
`POST /api/gallery`.

### 1c. NEW — `GET /api/notifications` leaks the creating admin's password hash, and has no auth gate at all
Found in the 2026-07-16 audit, **still open**. Same bug class as 1b,
not yet caught: `Notification.createdBy` is a `@ManyToOne User` with no
`@JsonIgnore`, and `NotificationController.getAll()` returns raw
`Notification` entities. Unlike 1b, this one isn't even
`hasRole("ADMIN")`-gated — `SecurityConfig` has no matcher for
`/api/notifications/**` at all, so it falls through to
`anyRequest().authenticated()`. Net effect: **any logged-in user, of
any role**, can call `GET /api/notifications` and get the password hash
of every admin who's ever created a notification. (The *write*
endpoints — `create`/`send`/`delete` — do have
`@PreAuthorize("hasRole('ADMIN')")` as of the 2026-07-15 "Fix security
and update controllers" commit; only the `GET` and the leak itself
remain unfixed.) Fix: same pattern as Gallery — a `NotificationResponse`
DTO, plus add `.requestMatchers(HttpMethod.GET,
"/api/notifications/**").hasRole("ADMIN")` (or similar) to
`SecurityConfig`.

### 1d. NEW — `GET /api/orders/{id}` has no ownership check (IDOR)
Found in the 2026-07-16 audit, **still open**.
`OrderController.getOrder(id)` fetches by ID with no comparison against
the calling user — any authenticated user can view any other user's
order (items, address, payment info) just by guessing/incrementing an
ID. Fix: check the order's owning user against
`@AuthenticationPrincipal` (or require `hasRole("ADMIN")` for
non-owners) before returning.

### 1e. `POST /api/auth/refresh-token` (and `/me`) threw 500 instead of 401 when called anonymously — **fixed 2026-07-18**
Both endpoints sat under the blanket `.requestMatchers("/api/auth/**").permitAll()`
matcher but relied on `@AuthenticationPrincipal` internally, so an
anonymous call NPE'd on a null principal instead of getting a clean
`401`. Fixed at two layers, defense in depth:
- `SecurityConfig` now carves `GET /api/auth/me` and `POST
  /api/auth/refresh-token` out of the blanket `permitAll`, requiring
  `.authenticated()` — Spring Security itself rejects an anonymous call
  via `RestAuthenticationEntryPoint` before the controller ever runs.
- `AuthController` also keeps an explicit `if (userDetails == null)
  throw new BadCredentialsException(...)` guard in both methods
  (originating from an independent fix on `origin/main`, kept
  deliberately rather than treated as redundant) — `GlobalExceptionHandler`
  already has a `BadCredentialsException` handler that maps to `401`.
  This layer matters in contexts where the full `SecurityConfig` filter
  chain doesn't run, e.g. `AuthControllerTest` uses
  `@AutoConfigureMockMvc(addFilters = false)` specifically to test the
  controller in isolation — without the controller-level guard, that
  test (`getMe_WithoutToken_Returns401`) fails with a `500`. Verified:
  all 36 tests pass with both layers in place.

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

### 4. Inquiry public submission required login — **fixed 2026-07-10**
The merge added real admin endpoints to `InquiryController` (list, get,
resolve) but `/api/inquiries` was never added to `SecurityConfig`'s
`permitAll` list, so the whole controller fell under
`anyRequest().authenticated()` — breaking the v1 requirement that the
inquiry form needs no account. Fixed same day: `POST /api/inquiries` is
now `permitAll` (method-specific), and `GET /`, `GET /{id}`,
`PUT /{id}/resolve` now require `hasRole("ADMIN")` specifically, not
just any logged-in user. Re-verified live, fresh instance:

```
POST /api/inquiries (no Authorization header) → 200 OK, inquiry created
GET  /api/inquiries (no Authorization header) → 403
GET  /api/inquiries (ROLE_USER token, not admin) → 403
```

### 5. Gallery POST 500-for-anonymous and open DELETE — fixed 2026-07-10
`GalleryController.create()` requires `@AuthenticationPrincipal
UserDetails` to set `uploadedBy`. Now that `/api/gallery/**` writes
require `hasRole("ADMIN")` (see "Security gap" above), an anonymous
caller is rejected by Spring Security before it ever reaches the
controller — the `NullPointerException` path is no longer reachable.
`DELETE /api/gallery/{id}` now requires `hasRole("ADMIN")` too. Verified
live.

## Modules with no use in v1

Exist in the codebase, fully working, but called by nothing in the v1
(or even v1.1) checklist:

- **Cart** (`/api/cart`) — v1.1 commerce, not v1.
- **Wishlist** (`/api/wishlist`) — not in v1 *or* v1.1 checklist at all.
- **Product Reviews** (`/api/reviews`) — not in v1 *or* v1.1 checklist at
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
  **Fixed 2026-07-18**: the notification email used to go to the
  *submitter* (`sendInquiryConfirmation(request.getEmail(), ...)` — an
  auto-reply to whoever filled out the form, not a notification to
  anyone on the team). Now sends to `Company.email` (the admin/company
  inbox, same field the public "About/Contact" screens already read)
  with the submitter's name/email/phone/message in the body, via a new
  `EmailService.sendContactNotificationToAdmin()`.
- **Banner** (`/api/banners`) — a full carousel entity (title, subtitle,
  CTA, display order). The v1 admin panel checklist doesn't list "banner
  management" as a module, and the v1 mobile home screen says "company
  banner" (singular — likely `Company.bannerUrl`), not a carousel.
  **Ambiguous — confirm with the client** whether this is v1.1 or dead
  code; it's currently also part of the open `permitAll()` security gap.

**Decision (2026-07-10): keep all of these.** None get deleted — a
dormant, unused controller is genuinely harmless (nothing calls it, no
runtime cost) as long as it *compiles* and doesn't itself have bugs.

**Exception, 2026-07-18: Newsletter was deleted entirely** (entity,
repository, service, controller, DTO — `/api/newsletter/**` no longer
exists). It was never in the v1 or v1.1 checklist and, unlike the
others, was pure bookkeeping (subscribe/unsubscribe an email address)
with no actual send/campaign mechanism ever built — nothing else in the
codebase referenced it, so removing it didn't touch anything else.
Also cleaned up the now-orphaned `.requestMatchers("/api/newsletter/**")
.permitAll()` rule in `SecurityConfig`. This is a deliberate departure
from the "keep dormant modules" decision above, made because Newsletter
had no path to ever becoming load-bearing without new work anyway.

That distinction matters and just came up in practice: `WishlistService` had a
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
Re-ordered 2026-07-18. Everything about the auth-leak bugs, CORS, the
401/403 handlers, and the forgot-password rework is now done — see
`changelog.md`'s 2026-07-15 through 2026-07-18 entries. A real test
suite also landed via the `origin/main` merge (`AuthControllerTest`,
`AuthServiceTest`, `CartServiceTest`, `InquiryServiceTest`,
`OrderServiceTest`, `ProductServiceTest` — 36 tests, all passing).
Current open list:

1. **Fix `GET /api/notifications`'s password-hash leak and missing auth
   gate** (defect 1c) — highest priority open item now, same bug class
   as the already-fixed Gallery leak. Add a `NotificationResponse` DTO
   and a `SecurityConfig` matcher restricting the `GET` to admin (or at
   least authenticated + role-appropriate).
2. **Fix the `GET /api/orders/{id}` IDOR** (defect 1d) — add an
   ownership check against the caller before returning an order.
3. **SMS OTP `purpose` isn't actually enforced** — Twilio Verify has no
   concept of it, so a code sent for `LOGIN` could technically be reused
   to reset a password via `/api/auth/reset-password`. See the "OTP
   module" section above. Would need local tracking of SMS OTP sends to
   close, out of scope for now.
4. Update `.env.example` with the `TWILIO_ACCOUNT_SID`/
   `TWILIO_AUTH_TOKEN`/`TWILIO_VERIFY_SERVICE_SID` vars
   `application.yaml` requires — still missing.
5. **Confirm the inquiry-submission auth change with the client** —
   `POST /api/inquiries` now requires login (2026-07-10 decision),
   removing guest inquiry submission from the v1-confirmed feature
   list. The mobile app needs a login gate added to the Inquiry form and
   the "Inquire about this product/service" CTAs before this can ship
   as-is — not yet built, see `frontend-integration-spec.md`.
6. Product: wire Cloudinary image upload (schema mostly ready via
   `ProductImage`), add pagination (`Pageable`) to the catalog GET, add
   the `specifications` JSONB column per the earlier decision.
7. Service has no category/grouping field (unlike Product) — confirm
   with the client whether services need filtering/grouping in a later
   version.
8. Confirm with the client whether Contact and Banner are v1.1 scope or
   genuinely unused (not a deletion question — both stay in the codebase
   either way, per the 2026-07-10 keep-dormant decision).
9. `GlobalExceptionHandler`'s catch-all returns
   `"Something went wrong: " + ex.getMessage()` with a `500` for any
   unhandled exception — leaks internal exception detail to any client.
   Worth tightening to a generic message before production.
10. `application.yaml` has `org.springframework.security`/
    `com.ibnfirnas` at `DEBUG` and `show-sql: true` with no
    dev/prod profile split — would ship to production as-is.

## Changelog
Moved to [changelog.md](changelog.md) — full dated history of fixes,
decisions, and defect re-audits lives there now.
