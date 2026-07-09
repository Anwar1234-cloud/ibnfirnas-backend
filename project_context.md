# IbnFirnas — Project Context

## Team
Two developers, both new to this client/stack, building the IbnFirnas
mobile app jointly. This repo (`ibnfirnas-backend`) is the Spring Boot API
serving the React Native customer app and the React + Vite admin panel.
One backend serves both clients — no separate backend per client; see
"How the admin panel actually talks to everything" in
[architecture.md](architecture.md) for the full communication model.

## Branches
- `main` — the shared baseline, tracks `origin/main`.
- `developer-2` — a second developer's parallel work (Wishlist DTOs,
  password change, pom.xml tweaks), diverged from `main` by several
  commits. This dev's env-var/secrets refactor and the doc set
  (`context.md`, `project_context.md`, `architecture.md`) were committed
  here on 2026-07-10, not on `main`.
- `origin` also has `develop`, `feature/authentication`,
  `feature/product-api` — not yet cross-referenced against `main` or
  `developer-2`; worth checking before assuming `main` has the latest of
  everything.

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
- Inquiry form: name/email/phone/message → backend, success feedback
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
- Product API: full CRUD + Cloudinary image storage
- Service API: full CRUD + Cloudinary image storage
- Gallery API: upload/delete/list
- Company API: GET (public) / PUT (admin)
- Inquiry API: POST (public) / GET + status update (admin)

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
  keep compiling and stay bug-free, which is the actual risk (see the
  Changelog in `architecture.md` for a build break this already caused
  once). Full list and reasoning in architecture.md's "Modules with no
  use in v1" section.
