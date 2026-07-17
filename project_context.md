# IbnFirnas — Project Context

## Team
Two developers, both new to this client/stack, building the IbnFirnas
mobile app jointly. This repo (`ibnfirnas-backend`) is the Spring Boot API
serving the React Native customer app and the React + Vite admin panel.
One backend serves both clients — no separate backend per client; see
"How the admin panel actually talks to everything" in
[architecture.md](architecture.md) for the full communication model.

## Branches
- `main` — the shared baseline, tracks `origin/main`. Since the
  2026-07-10 sync, teammates added a full Product image-upload flow via
  Cloudinary (`97c758a`, `6383463`), a `ServiceRequest`-validated
  Service CRUD, an OTP verification module (email + Twilio SMS,
  `fb9aa43`), and a security-hardening pass on Order/Notification/
  Company controllers (`1889701`). `origin/main` is currently at
  `9b8b101`.
- `developer-2` — synced with `main` again on 2026-07-16 by merging
  `origin/main` in (`0e30e3c`), reconciling ~7 commits of divergence.
  Also carries this dev's own work since the 2026-07-10 sync: the
  Gallery password-hash leak fix, the `GET /api/auth/me` password leak
  fix, Cloudinary cleanup on gallery delete, the CORS-conflict
  consolidation (`CORS_ALLOWED_ORIGINS` env var), and a Service `PUT`
  endpoint (later superseded by `origin/main`'s validated version during
  the merge). A follow-up audit after the merge found 3 more open
  issues — see `architecture.md`'s "Confirmed defects" 1c/1d/1e and
  `changelog.md`'s 2026-07-16 entry. **`developer-2` is 9 commits ahead
  of `origin/developer-2` as of this writing — merged and committed
  locally, not yet pushed.** On top of that, further local work landed
  2026-07-17 and is verified live: centralized `401`/`403` JSON error
  handling and a `JwtAuthenticationFilter` fix for deleted-user tokens
  (see `changelog.md`'s 2026-07-17 entry) — still not staged or
  committed, but confirmed working against the running instance.
  Also: `*.md` was found to be gitignored repo-wide, meaning this whole
  doc set had never actually been tracked by git; fixed via explicit
  `.gitignore` exceptions, but the docs themselves are still untracked
  pending a `git add`.
- `origin` also has `develop`, `feature/authentication`,
  `feature/product-api` — `develop` and `feature/product-api` are where
  the OTP feature and controller security fixes originated before
  landing on `main`; not yet fully cross-referenced beyond what's
  documented here.
- Full dated history of what changed and why: [changelog.md](changelog.md).

## V1 scope (confirmed)
Source: client-approved feature checklist (screenshot, 2026-07-09) plus one
explicit change from that plan — **user accounts + profile management are
in v1**, not deferred to v1.1 as the screenshot originally showed.

v1 goal (client's own framing): *"a complete, professional, fully working
product. The client can browse the catalog, view the gallery, read about
the company, contact you, submit inquiries, register/manage their
account — all manageable from the admin panel."* v1.1 adds commerce
(cart/checkout/payments) and push notifications on top of this stable
foundation.

### Mobile app — v1
- Splash screen: logo + animation, initial data load
- Home screen: company banner, featured products, featured services
- Products: browse catalog, detail + specs, images, pricing
- Services: list view, detail + images
- Gallery: photo grid, full-screen zoom
- About: overview + mission, vision + history
- Contact: phone/email, maps link, social links
- Inquiry form: name/email/phone/message → backend, success feedback.
  **Now requires login as of 2026-07-10** (see "Working agreement"
  below) — a change from how this was originally scoped, needs client
  confirmation.
- **User accounts: register, login, profile management** (moved into v1)

### Mobile app — deferred to v1.1
- Cart + checkout (add to cart, checkout flow)
- Payments (PayTabs / Moyasar, order tracking)
- Push notifications (FCM setup, receive + navigate)

### Admin panel — v1
- Auth: secure login, JWT + roles
- Dashboard: product/service/inquiry counts
- Product CRUD: add/edit/delete, image upload
- Service CRUD: add/edit/delete
- Gallery CRUD: upload/delete images
- Company info: edit about/mission/vision, update contact details
- Inquiries: view all, mark seen/replied

### Admin panel — deferred to v1.1
- Order management (view orders, update status)
- Notifications (send push, broadcast campaigns)

### Backend — v1
- Auth module: JWT, BCrypt, roles
- **User module: register/login/profile (added to v1)**
- **OTP verification (email + SMS via Twilio) — built 2026-07-15, not
  yet in v1 scope or wired into the auth flows.** Given the Saudi
  market and the app's phone-number-first registration, this is a
  strong candidate to gate phone verification at registration (see
  `changelog.md`/`architecture.md` for the API contract) — but it needs
  a scope decision: does registration require OTP verification before
  v1 ships, or does it stay optional/deferred? Not yet confirmed with
  the client.
- Product API: full CRUD + Cloudinary image storage
- Service API: full CRUD + Cloudinary image storage
- Gallery API: upload/delete/list
- Company API: GET (public) / PUT (admin)
- Inquiry API: POST (**now authenticated, was public** — see below) /
  GET own (`/my`, authenticated) / GET list + status update (admin)

### Backend — deferred to v1.1
- Order API: CRUD + status flow
- Payment API: gateway integration (PayTabs/Moyasar)
- FCM API: token store, broadcast

## Working agreement
- This backend repo is the source of truth for what the API actually
  exposes — [architecture.md](architecture.md) documents the target
  architecture and cross-checks it against the current implementation so
  gaps are visible before mobile/admin-panel work depends on endpoints
  that don't exist yet.
- Client will share per-service architecture and flow details next; this
  file and `architecture.md` get updated as those land.
- **Dormant/out-of-v1 modules stay in the codebase, not deleted**
  (decided 2026-07-10). Cart, Wishlist, Product Reviews, Newsletter,
  Order, Notification, Contact, and Banner aren't called by anything in
  v1, but a working, uncalled controller is harmless — it just needs to
  keep compiling and stay bug-free, which is the actual risk (see
  `changelog.md` for a build break this already caused once). Full list
  and reasoning in architecture.md's "Modules with no use in v1" section.
- **All write endpoints now require authentication** (decided
  2026-07-10). Product/Category/Service/Gallery/Company/Banner writes
  require `hasRole("ADMIN")`; inquiry submission (`POST
  /api/inquiries`) now requires a logged-in user of any role. The
  inquiry-submission change is a genuine scope change — it removes
  guest inquiry submission, which the client's original feature
  checklist listed as a public action separate from account
  registration. **Needs client confirmation** that dropping guest
  submission is acceptable, since it means the mobile app's Inquiry form
  needs a login gate that wasn't part of the original design. See
  `architecture.md`'s "Security gap" section for the verified-live
  details.
