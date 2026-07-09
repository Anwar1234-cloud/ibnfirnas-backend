# IbnFirnas — Project Context

## Team
Two developers, both new to this client/stack, building the IbnFirnas
mobile app jointly. This repo (`ibnfirnas-backend`) is the Spring Boot API
serving the React Native customer app and the React + Vite admin panel.

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
