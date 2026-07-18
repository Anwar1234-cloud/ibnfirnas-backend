# IbnFirnas Backend — Changelog

Tracks notable changes, fixes, and decisions on `developer-2` in
reverse-chronological order. For the current architecture and defect
status see [architecture.md](architecture.md); for v1 scope see
[project_context.md](project_context.md).

## 2026-07-18

- **Merged `origin/main` into `developer-2`** (`8e295cf`), pulling in a
  real test suite that landed there since the last sync: `AuthControllerTest`,
  `AuthServiceTest`, `CartServiceTest`, `InquiryServiceTest`,
  `OrderServiceTest`, `ProductServiceTest` — 36 tests total, all
  passing. The only conflict was `AuthController.java`: `origin/main`
  had independently added its own fix for the `/me` anonymous-crash bug
  (an in-controller null check throwing `BadCredentialsException`,
  caught by `GlobalExceptionHandler` → `401`) as part of a "Fix
  controller and service tests with authentication fixes" commit.
  Reconciled by keeping **both** fixes: the `SecurityConfig`
  `.authenticated()` matchers from this branch (covers `/refresh-token`
  too, which `origin/main`'s fix didn't touch) *and* the controller-level
  null check from `origin/main`. Initially removed the null check as
  "redundant" since `SecurityConfig` already blocks anonymous access —
  but `AuthControllerTest` uses `@AutoConfigureMockMvc(addFilters =
  false)` specifically to bypass the security filter chain and test the
  controller in isolation, so the null check is the only thing that
  makes that test pass. Restored it. Committed as `6c53de7` (the
  `SecurityConfig` fix) + the merge commit `8e295cf`.
- **Rebuilt the forgot-password flow around phone OTP, replacing the
  email-token flow entirely** (a deliberate decision, not additive —
  the old `POST /api/auth/forgot-password` no longer exists). Fixes the
  email-enumeration issue that flow had (a distinguishable error for
  unregistered emails on a public endpoint) — the new flow never
  queries the `User` table until the final OTP-verified step, so there's
  nothing to enumerate at the request stage. New shape: `POST
  /api/otp/send {phone, purpose: "FORGOT_PASSWORD"}` to request a code,
  then `POST /api/auth/reset-password {phone, otp, newPassword}` to
  verify and reset in one call. Deleted the now-fully-dead
  `PasswordResetToken` entity, `PasswordResetTokenRepository`,
  `ForgotPasswordRequest` DTO, and `EmailService.sendPasswordResetEmail()`.
  Also fixed the OTP `purpose` case-sensitivity bug while in there —
  `/verify` now uppercases the input before parsing, same as `/send`
  already did, so a lowercase `purpose` gets a clean `400` instead of a
  raw `500`.
- **Removed the email-OTP path entirely — OTP is phone/SMS-only now.**
  `SendOtpRequest`/`VerifyOtpRequest` no longer have an `email` field.
  Deleted the infrastructure that existed solely to support it:
  `OtpVerification` entity, `OtpVerificationRepository`, `OtpType` enum,
  `EmailService.sendOtpEmail()`. SMS OTP was always stateless (delegated
  to Twilio Verify, no local DB tracking), so this doesn't change SMS
  behavior at all — it just removes the unreachable email branch and
  everything that only existed to support it.
- **Contact form notification now goes to the admin, not the
  submitter.** `ContactController` was calling
  `emailService.sendInquiryConfirmation(request.getEmail(), ...)` —
  confirmed this sent an auto-reply to whoever filled out the public
  form, not a notification to anyone on the team. Now fetches
  `Company.email` (the same field the public About/Contact screens
  already use) and sends the submitter's name/email/phone/message
  there via a new `EmailService.sendContactNotificationToAdmin()`.
- Verified after each change: `mvn compile` clean, all 36 tests passing.

## 2026-07-17

- **Fixed `JwtAuthenticationFilter` breaking `permitAll` endpoints when a
  token referenced a deleted/nonexistent user.** A JWT signed with the
  correct secret but whose `sub` (email) no longer exists in `users`
  caused `CustomUserDetailsService.loadUserByUsername()` to throw
  `UsernameNotFoundException` — uncaught inside the filter, so it
  propagated past the whole filter chain and never reached
  `AuthorizationFilter`'s `permitAll` check. Net effect: a stale/bad
  token broke *public* endpoints too — e.g. `POST /api/inquiries`, which
  needs no auth at all, would `403` if a garbage-but-validly-signed
  `Authorization` header was present. Fixed by catching
  `UsernameNotFoundException` around the lookup and clearing the
  security context, so the request continues as anonymous instead of
  aborting. Verified live: a token signed with the real `JWT_SECRET` but
  pointing at a nonexistent email now gets a normal `200` on `POST
  /api/inquiries` (was `403`).
- **Added `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler`,
  wired into `SecurityConfig` via `.exceptionHandling(...)`.** Spring
  Security's default behavior with no configured entry point/handler was
  a bare `403` with an **empty body** for both missing auth and
  insufficient role — indistinguishable from each other, and from
  `NoResourceFoundException`/firewall-rejection cases. Now:
  - No/invalid token on a protected route → `401
    {"success":false,"message":"Authentication required","data":null}`
  - Valid token, wrong role → `403
    {"success":false,"message":"Access denied","data":null}`

  **This changes previously-documented behavior** —
  `frontend-integration-spec.md` §1/§8 said 401/403 come back with an
  empty body; that claim is now corrected there. Verified live against
  `POST /api/inquiries`, `GET /api/inquiries/my`, and `GET
  /api/inquiries` with anonymous/`ROLE_USER`/`ROLE_ADMIN` callers.
- **Debugging note, no code change:** `POST /api/inquiries/` (trailing
  slash) or a path with a stray trailing space 401s/500s even though the
  bare `/api/inquiries` is `permitAll` — Spring Boot 3 doesn't treat a
  trailing slash as equivalent to the exact path, and Spring Security's
  `StrictHttpFirewall` rejects embedded control characters outright.
  Neither is a bug; both are easy to trip over when a URL gets
  duplicated/copy-pasted in Postman. No fix needed, just a rough edge to
  remember while testing.

## 2026-07-16

- **In progress, uncommitted**: centralized `401`/`403` error handling.
  Added `RestAuthenticationEntryPoint` and `RestAccessDeniedHandler`,
  wired into `SecurityConfig` via `.exceptionHandling(...)`, so a
  rejected request (no token, or wrong role) now returns the same
  `{success, message, data}` JSON envelope as every other response,
  instead of Spring Security's default empty body. Also hardened
  `JwtAuthenticationFilter` against tokens that reference a
  since-deleted user — previously an unhandled 500, now caught, logged,
  and treated as anonymous. `mvn compile` verified clean. Does **not**
  fix defect 1e (`/refresh-token`'s NPE-under-`permitAll` bug) — that
  matcher lets the request through before these handlers would ever
  run. See `architecture.md`'s "Security gap" section for full detail,
  and `frontend-integration-spec.md` §1 for the frontend-facing impact
  (401/403 responses are changing from empty-body to JSON-wrapped).
  Also discovered the same day: `*.md` was gitignored repo-wide (only
  `!README.md` was excepted, and this repo has no `README.md`) —
  every doc in this list had been local-only, invisible to git, since
  they were first created. Added explicit `.gitignore` exceptions for
  all 5 docs so they're trackable going forward.
- **Merged `origin/main` into `developer-2`** (`0e30e3c`), reconciling
  two branches that had diverged since `6383463`. Resolved conflicts in
  4 files:
  - `CorsConfig.java` — kept it deleted. `origin/main` had modified it,
    but its version had the same duplicate-CORS-filter bug already
    fixed on this branch (see 2026-07-15 below), so deletion still won.
  - `SecurityConfig.java` — kept this branch's env-driven
    `CORS_ALLOWED_ORIGINS` allowlist over `origin/main`'s hardcoded
    `http://localhost:5173`; every other request-matcher rule was
    already identical on both sides.
  - `AuthController.java` — both branches had independently fixed the
    `GET /api/auth/me` password leak with a `UserResponse` DTO; kept
    this branch's version (extracted to a private `toDTO` helper,
    matching `ProfileController`'s existing pattern).
  - `ServiceService.java` / `ServiceController.java` — kept
    `origin/main`'s `ServiceRequest`-based create/update (adds `@Valid`
    validation) over this branch's raw-`ServiceEntity` update. Also had
    to manually remove a duplicate `PUT /{id}` handler that git's
    auto-merge left behind in `ServiceController` — two methods mapped
    to the same route, which would have failed at Spring context
    startup with an ambiguous-mapping error even though git didn't flag
    it as a conflict.
  - Verified `mvn compile` clean after resolution. Not yet pushed as of
    this writing — `developer-2` is 9 commits ahead of
    `origin/developer-2`.
- **Brought in via the merge**: a full OTP verification feature
  (`OtpController`, `OtpService`, Twilio SDK integration via
  `TwilioConfig`) — see `frontend-integration-spec.md` §1 for the API
  contract. `POST /api/otp/send` / `POST /api/otp/verify`, both public.
  Email OTPs are generated and tracked in our own DB (10-min TTL,
  3-attempt cap); SMS OTPs are delegated entirely to Twilio Verify, so
  the same limits don't apply there. **Not yet wired into
  `/api/auth/register`, `/login`, or `/forgot-password`** — it's a
  standalone utility API right now, nothing in `AuthController` calls
  it. `.env.example` was never updated with the new `TWILIO_*`
  variables the app now reads from `application.yaml` — still open.
- **Also brought in**: a teammate's "Fix security and update
  controllers" commit (`1889701`), which independently closed two
  issues found in this session's audit (see below) — `PUT
  /api/orders/{id}/status` and `POST`/`POST .../send`/`DELETE
  /api/notifications/**` now carry `@PreAuthorize("hasRole('ADMIN')")`.
  `CompanyController.update()` was also confirmed to already null-check
  each field before overwriting (partial updates are safe).
- **Full backend audit performed** (raw-entity leaks, missing
  endpoints, `SecurityConfig` gaps, N+1 risk, dead code, validation
  gaps). Found 2 new critical/high issues beyond the two the merge
  fixed — both **still open**, see `architecture.md`'s "Confirmed
  defects" for full detail:
  - `GET /api/notifications` still returns raw `Notification` entities;
    `createdBy` (a `@ManyToOne User`, no `@JsonIgnore`) leaks the
    creating admin's password hash. The `GET` itself also has no role
    restriction — any logged-in user, not just admin, can call it. Same
    bug class as the Gallery leak, not yet caught by anyone.
  - `GET /api/orders/{id}` has no ownership check (IDOR) — any
    logged-in user can view any other user's order by ID.
  - `POST /api/auth/refresh-token` has the same NPE-under-`permitAll`
    bug already fixed for `/me`: an anonymous call throws an unguarded
    `NullPointerException` that `GlobalExceptionHandler`'s catch-all
    turns into a `500` (and leaks the exception message) instead of a
    clean `401`.

## 2026-07-15

- **Fixed `GET /api/gallery`'s public password-hash leak** (open since
  2026-07-10, defect 1b). `GalleryController` now maps to the
  previously-unused `GalleryResponse` DTO on both `GET` and `POST`
  instead of returning raw `Gallery` entities, so `uploadedBy` (and its
  password hash) is never serialized. Verified via `mvn compile`; a
  proper `GalleryResponse` DTO already existed in the codebase but had
  never been wired into the controller — dead code until now.
- **Fixed Gallery deletes orphaning Cloudinary assets.** `DELETE
  /api/gallery/{id}` now calls `CloudinaryService.deleteImage()` on the
  item's `mediaUrl` before removing the DB row, using the
  `CloudinaryService`/`UploadController` plumbing that already existed
  for Product uploads but was never reused here.
- **Fixed `GET /api/auth/me`'s password-hash leak** (open since
  2026-07-09/07-10, defect 1). Returns a `UserResponse` DTO now,
  same pattern `ProfileController` already used for its own fix.
- **Added the missing `PUT /api/services/{id}` endpoint.** Confirmed it
  was genuinely absent — no `@PutMapping`, no update method in
  `ServiceService` — closing item 6 from architecture.md's "Next
  steps". (Superseded later the same day by `origin/main`'s
  `ServiceRequest`-validated version during the 2026-07-16 merge.)
- **Consolidated CORS to a single source of truth.** Found two
  competing CORS configs: a standalone `CorsFilter` bean in
  `CorsConfig.java` (wildcard origins, no credentials) and
  `SecurityConfig`'s own `CorsConfigurationSource` (had drifted to
  `allowedOriginPatterns("*")` **with** `allowCredentials(true)` — worse
  than either config alone, since it let any origin call the API with
  credentials). Deleted `CorsConfig.java` entirely and made
  `SecurityConfig`'s allowlist configurable via a new
  `CORS_ALLOWED_ORIGINS` env var (comma-separated), defaulting to
  `http://localhost:5173` so local dev keeps working unchanged.

## 2026-07-10

- **Closed the `permitAll()` security gap on Product/Category/Service/
  Gallery/Company/Banner.** `SecurityConfig` now splits each into
  `GET → permitAll` (public reads) and everything else (POST/PUT/DELETE)
  `→ hasRole("ADMIN")` — the same pattern already used for Inquiry.
  Verified live with three identities (anonymous, a `ROLE_USER` token, a
  `ROLE_ADMIN` token) against all six modules: reads stayed `200` for
  everyone, writes became `403` for anonymous and `ROLE_USER`, `200` for
  `ROLE_ADMIN`. This also makes the Gallery anonymous-
  `NullPointerException` bug (defect 5, below) moot — an anonymous
  caller can no longer reach `GalleryController.create()` at all, it's
  rejected by Spring Security before the controller runs.

- **`POST /api/inquiries` now requires authentication — reverses the
  earlier "public, no account needed" requirement.** Decision made
  2026-07-10: inquiry submission now requires a valid token (any role,
  not just admin), matching the "every write behind auth" direction
  taken for the rest of the API. **This is a scope change from the
  v1-confirmed feature checklist**, which listed "submit inquiries" as
  a public browsing action separate from "register/manage account" —
  worth confirming with the client that guest inquiry submission is
  intentionally being dropped, since the mobile app's Inquiry form and
  "Inquire about this product/service" CTAs will need a login gate that
  wasn't in the original design. Verified live: anonymous
  `POST /api/inquiries` now `403` (was `200`); a logged-in user still
  gets `200`.

- **Added a user-scoped inquiry list.** `Inquiry.user` (already present
  on the entity, previously never set) is now populated on submission;
  new `GET /api/inquiries/my` (authenticated, any role) returns only the
  caller's own inquiries, kept separate from the admin-only
  `GET /api/inquiries` full list. Verified live: a user who submits an
  inquiry while logged in sees only that inquiry via `/my` — not other
  users' inquiries, and not guest submissions made before this change
  (those have `user = null` and aren't retroactively linkable).
  Committed as `e4a6521` on `developer-2` (not yet pushed as of this
  writing).

- **Found (not yet fixed): `GET /api/gallery` publicly leaks password
  hashes.** While writing
  [frontend-integration-spec.md](frontend-integration-spec.md) (the
  mobile app JSON contracts needed exact response shapes, which is how
  this surfaced), discovered `Gallery.uploadedBy` is a raw `User`
  relation with no `@JsonIgnore`, and `GalleryController` returns raw
  entities. Verified live: an anonymous `GET /api/gallery` call — no
  token, no login — returns the full uploader account including the
  BCrypt password hash for every gallery item that has one. This is the
  most severe open issue in the backend: unlike the `/auth/me` and
  `/profile` leaks, it needs no authentication at all to trigger.

- **Fixed the Inquiry public-submit regression, and added Company's
  missing social-link fields**, driven by writing
  [frontend-integration-spec.md](frontend-integration-spec.md) (a
  mobile-app build spec) and finding two real blockers while at it.
  `SecurityConfig` now has `POST /api/inquiries` as a method-specific
  `permitAll` and `GET`/`GET {id}`/`PUT {id}/resolve` as `hasRole
  ("ADMIN")` — verified live (public submit works with no token, a
  plain `ROLE_USER` token gets `403` on the admin endpoints). `Company`
  gained `websiteUrl`, `facebookUrl`, `instagramUrl`, `twitterUrl`
  columns (verified live via Hibernate's DDL log); `linkedinUrl` was
  briefly added then removed before being committed, so it's not in the
  entity.

- **Re-audited all confirmed defects against `main`'s merged code, live.**
  Fixed by the merge: password leak in `ProfileController` (now
  `UserResponse` DTO), `LazyInitializationException` on
  `Category`/`Product` (now `FetchType.EAGER` + response DTOs), Product
  CRUD, Company update endpoint, Inquiry admin list/resolve endpoints.
  Still open: `GET /api/auth/me` password leak, the `permitAll()`
  security gap (now also covering Product/Gallery/Company's new write
  endpoints). **New regression introduced by the merge:** Inquiry's
  public submit endpoint (`POST /api/inquiries`) now requires auth,
  breaking the v1 requirement that the inquiry form needs no account
  (confirmed live: returns `403` with no token).

- **Added `Gallery.cloudinaryPublicId` column.** Prepares the schema for
  Cloudinary integration. Verified live via Hibernate's `ddl-auto:
  update` DDL log: `alter table if exists gallery add column
  cloudinary_public_id varchar(255)`. No `CloudinaryService`/SDK/upload
  flow exists yet — nothing writes to this column yet.

- **Synced `developer-2` with `main`.** `developer-2` was 22 commits
  behind `origin/main`. Fast-forwarded local `main`, then merged it into
  `developer-2` — one conflict in `application.yaml` (env-var refactor
  vs. two new springdoc config lines), resolved by keeping both. Caught
  and fixed a subtle bug the merge/stash interaction silently
  introduced: an earlier stashed edit had deleted an import line that
  `main`'s (better) `WishlistService` implementation still needed.

- **Fixed a build-breaking bug in `WishlistService`** (pre-`main`-merge):
  `toResponse()` called `Product.getImageUrl()`, which doesn't exist on
  `Product` (images live in a separate `ProductImage` table). The method
  was dead code — nothing called it — so it was removed along with the
  unused `WishlistResponse` DTO. Later superseded during the `main`
  merge, which brought in a complete, correct `WishlistService`/
  `WishlistResponse` (proper DTO mapping via `product.getImages()`) —
  that's what's in the codebase now.

- **Decision: keep all dormant/out-of-v1 modules.** Cart, Wishlist,
  Product Reviews, Newsletter, Order, Notification, Contact, and Banner
  aren't called by anything in v1, but stay in the codebase rather than
  being deleted — a dormant controller is harmless as long as it compiles
  and stays bug-free (the Wishlist bug above is exactly the failure mode
  to watch for). Full reasoning in `architecture.md`.

- **Documented the admin panel ↔ backend ↔ DB communication model** — one
  Spring Boot backend serves both the admin panel and mobile app; neither
  client touches Postgres directly or talks to the other client directly.

- **Moved secrets out of `application.yaml`.** DB password, Gmail SMTP
  credentials, and the JWT signing secret were hardcoded and committed in
  git history. Now read from environment variables via `.env`
  (gitignored), with `.env.example` as the template. The exposed Gmail
  app password should still be rotated in the Google account, since git
  history wasn't rewritten.

## 2026-07-09

- **Full v1 API inventory audit, verified live.** Ran the backend and hit
  endpoints directly rather than only reading source. Found: password
  hash leaking in `GET /api/auth/me` and `GET`/`PUT /api/profile`
  (raw `User` entity returned, no DTO); `LazyInitializationException`
  (500) on `GET /api/categories` once any category has a child; a
  `permitAll()` bug in `SecurityConfig` that applies to all HTTP methods,
  not just GET, letting anyone create/delete Category/Service/Banner
  records with no login at all.

- **Established v1 scope** from the client's approved feature checklist,
  with one explicit change: user accounts + profile management moved
  into v1 (originally shown as v1.1 in the source screenshot).

- **Documented approved per-module flows** (Auth, Gallery, Services,
  Inquiry, Product) and locked in several decisions: `specifications` as
  a JSONB column on `Product` (not a separate table), pagination on
  catalog endpoints from day one, one controller + one service per
  module (already the existing pattern), WhatsApp deep link (not a form)
  for the mobile app's Contact Us screen.

- **Initial project documentation created**: `context.md` (codebase
  overview), `project_context.md` (team, v1 scope), `architecture.md`
  (target architecture, module cross-check, API inventory).
