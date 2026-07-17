# IbnFirnas Mobile App — Frontend Integration Spec (v1)

Consolidated build spec for the React Native customer app: API JSON
contracts, per-screen requirements, design system, and the WhatsApp
floating chat feature. Written to be usable directly as a build prompt.
Verified live against the running backend on `developer-2`, 2026-07-10
(updated later the same day for the Inquiry auth change and the new
`GET /api/inquiries/my` endpoint — see §1 and §9). Updated 2026-07-16
for the Gallery/`auth/me` leak fixes and the new OTP module. Updated
again 2026-07-17: the centralized 401/403 JSON error responses and the
`JwtAuthenticationFilter` stale-token fix are now verified live — see
§1 and §9.

Companion docs: [architecture.md](architecture.md) (backend detail),
[project_context.md](project_context.md) (v1 scope), [changelog.md](changelog.md).

## Phase 1 note — no backend wired yet

**Right now, build the UI against mock/static data, not live API calls.**
No backend integration in this phase. But structure the data layer so
wiring the real API later is a drop-in swap, not a rewrite:

- Define TypeScript interfaces matching the JSON contracts in §1 exactly
  (field names, types, nullability) and build mock fixtures against
  those interfaces.
- Put every "fetch" behind a hook (`useProducts()`, `useCompany()`, etc.)
  that today just returns the mock fixture, structured so the body can
  later become a real `fetch`/React Query call without touching any
  screen component.
- Don't hardcode mock data inline in screens — centralize it (e.g.
  `src/mocks/`) so swapping to live data is a one-file change per hook.

---

## 1. API JSON contracts

Every request/response shape below is copied from the actual DTOs/
entities in this backend repo, not guessed. All responses are wrapped in
the same envelope:

```json
{ "success": true, "message": "human-readable status", "data": { /* ... */ } }
```

**Errors don't all look the same** — worth handling correctly even in
mock form so the real integration doesn't need new error-handling code
later:
- `400`/`404`/`500` come back JSON-wrapped: `{"success": false, "message": "...", "data": null}`.
- **`401`/`403` are now also JSON-wrapped, as of 2026-07-17** —
  previously came back with an empty body (the behavior documented here
  until now). New `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`
  classes make these JSON-wrapped too, same envelope as everything else:
  `401 → {"success": false, "message": "Authentication required", "data": null}`,
  `403 → {"success": false, "message": "Access denied", "data": null}`.
  Verified live: `response.data.message` is now safe to read for every
  error status, no more special-casing 401/403. (Still local/uncommitted
  on `developer-2` as of this writing — confirmed working against the
  running instance, just not yet in a pushed commit — see
  `changelog.md`'s 2026-07-17 entry.)

### Auth

**`POST /api/auth/register`**
```json
// Request
{
  "fullName": "Ahmed Al-Farsi",
  "email": "ahmed@example.com",
  "password": "password123",
  "phone": "966501234567"
}
```
```json
// Response 200
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "ahmed@example.com",
    "fullName": "Ahmed Al-Farsi",
    "role": "ROLE_USER"
  }
}
```
`password` min 6 chars, `email` must be valid format, `fullName` required, `phone` optional.

**`POST /api/auth/login`**
```json
// Request
{ "email": "ahmed@example.com", "password": "password123" }
```
Response: identical shape to register's `data`.

**`POST /api/auth/forgot-password`**
```json
// Request
{ "email": "ahmed@example.com" }
```
```json
// Response
{ "success": true, "message": "Password reset email sent", "data": null }
```

**`POST /api/auth/reset-password`**
```json
// Request
{ "token": "<from-email-link>", "newPassword": "newPassword123" }
```

**`POST /api/auth/refresh-token`** — no body, `Authorization: Bearer <token>` header only.
```json
// Response
{ "success": true, "message": "Token refreshed",
  "data": { "accessToken": "eyJ...", "tokenType": "Bearer" } }
```

**`GET /api/auth/me`** — `Authorization: Bearer <token>` header only.
```json
// Response — fixed 2026-07-15, no more password field. Clean DTO now.
{
  "success": true,
  "message": "Current user",
  "data": {
    "id": 2, "email": "ahmed@example.com", "fullName": "Ahmed Al-Farsi",
    "phone": null, "avatarUrl": null, "role": "ROLE_USER",
    "isActive": true,
    "createdAt": "2026-07-10T01:23:31"
  }
}
```

### Profile (requires `Authorization: Bearer <token>`)

**`GET /api/profile`**
```json
{
  "success": true, "message": "Profile fetched",
  "data": {
    "id": 2, "email": "ahmed@example.com", "fullName": "Ahmed Al-Farsi",
    "phone": "966501234567", "avatarUrl": null, "role": "ROLE_USER",
    "isActive": true, "createdAt": "2026-07-10T01:23:31"
  }
}
```
Clean DTO, no password field — safe.

**`PUT /api/profile`**
```json
// Request — all fields optional; omit password fields if not changing it
{
  "fullName": "Ahmed Al-Farsi",
  "phone": "966501234567",
  "avatarUrl": "https://...",
  "currentPassword": "password123",
  "newPassword": "newPassword456"
}
```
Response: same shape as `GET /api/profile`. If `newPassword` is set
without a correct `currentPassword`, returns `400` with
`"Current password is required to set new password"` or
`"Current password is incorrect"`.

### Products (public, no auth)

**`GET /api/products`**, **`GET /api/products/featured`**
```json
{
  "success": true, "message": "Products fetched",
  "data": [
    {
      "id": 1, "name": "Steel Beam 200mm", "slug": "steel-beam-200mm",
      "description": "Full description text...",
      "shortDescription": "Short teaser text",
      "price": 450.00, "discountPrice": 399.00,
      "sku": "SB-200", "stockQuantity": 120, "stockStatus": "IN_STOCK",
      "isFeatured": true, "categoryId": 3, "categoryName": "Structural Steel",
      "isActive": true, "averageRating": 4.5, "totalReviews": 12,
      "primaryImageUrl": "https://.../steel-beam.jpg",
      "createdAt": "2026-06-01T10:00:00"
    }
  ]
}
```
**`GET /api/products/{id}`** — same object shape, single item, not an array.

⚠️ **Correcting an earlier assumption in this doc**: the API only
returns **one** image — `primaryImageUrl` (a single string). There is
**no** array of product images in the response, despite the `Product`
entity internally having a `ProductImage[]` relation. **Do not build a
multi-image carousel/gallery into the Product Detail screen** — design
for a single hero image only, until/unless the backend adds an
`images: string[]` field. No `specifications` field either (decided as a
future addition, never built) — don't design a specs table yet.

### Categories (public, no auth)

**`GET /api/categories`**, **`GET /api/categories/{id}`**
```json
{
  "success": true, "message": "Categories fetched",
  "data": [
    {
      "id": 3, "parent": null, "name": "Structural Steel",
      "slug": "structural-steel", "description": null,
      "iconUrl": null, "imageUrl": "https://.../cat.jpg",
      "displayOrder": 1, "isActive": true, "level": null,
      "createdAt": "2026-06-01T09:00:00"
    }
  ]
}
```
`parent` is a nested object of the same shape (one level up the tree) or
`null` for a root category. There's no `children` array in the response.

### Services (public, no auth)

**`GET /api/services`**, **`GET /api/services/featured`**, **`GET /api/services/{id}`**
```json
{
  "success": true, "message": "Services fetched",
  "data": [
    {
      "id": 1, "name": "Structural Design", "slug": "structural-design",
      "description": "Full description...", "shortDescription": "Short teaser",
      "iconUrl": "https://.../icon.png", "imageUrl": "https://.../service.jpg",
      "isFeatured": true, "isActive": true, "displayOrder": 1,
      "createdAt": "2026-06-01T09:00:00", "updatedAt": "2026-06-01T09:00:00"
    }
  ]
}
```

### Gallery (public, no auth) — fixed 2026-07-15

**`GET /api/gallery`**
```json
{
  "success": true, "message": "Gallery fetched",
  "data": [
    {
      "id": 1, "title": "Site Visit 2026", "description": null,
      "mediaUrl": "https://.../photo.jpg", "thumbnailUrl": null,
      "mediaType": "IMAGE", "altText": null,
      "displayOrder": 1,
      "createdAt": "2026-06-01T09:00:00"
    }
  ]
}
```

**Previously this leaked the uploader's password hash** via an
`uploadedBy` field — fixed 2026-07-15 by switching the endpoint to a
`GalleryResponse` DTO that never includes it. The field is now gone
from the response entirely, not just hidden — nothing to filter out
client-side anymore. Use `title`, `mediaUrl`, `thumbnailUrl`,
`mediaType`, `altText`, `displayOrder` as before.

### Company (public, no auth)

**`GET /api/company`**
```json
{
  "success": true, "message": "Company info fetched",
  "data": {
    "id": 1, "name": "IbnFirnas Trading & Contracting",
    "description": "Company overview text...",
    "vision": "Vision text...", "mission": "Mission text...",
    "logoUrl": "https://.../logo.png", "bannerUrl": "https://.../banner.jpg",
    "phone": "966501234567", "email": "info@ibnfirnas.com",
    "address": "Riyadh, Saudi Arabia",
    "googleMapsUrl": "https://maps.google.com/...",
    "websiteUrl": "https://ibnfirnas.com",
    "facebookUrl": "https://facebook.com/ibnfirnas",
    "instagramUrl": "https://instagram.com/ibnfirnas",
    "twitterUrl": "https://x.com/ibnfirnas",
    "updatedAt": "2026-07-01T12:00:00"
  }
}
```
Any of the social/website/contact fields may come back `null` — always
guard rendering (skip the icon/row entirely rather than showing an empty
link). No `linkedinUrl` field exists.

### Inquiry (⚠️ now requires login — changed 2026-07-10, was public)

**Correcting the earlier version of this doc**: `POST /api/inquiries`
was public with no auth required when this doc was first written. As of
2026-07-10 it requires `Authorization: Bearer <token>` (any logged-in
role, not just admin) — a deliberate scope change, not a bug. **This
means the Inquiry form and "Inquire about this product/service" CTAs
(§6) need a login gate that wasn't in the original design** — if the
user isn't logged in, route them to Login/Register first (with a
return-to-inquiry-form flow) rather than showing the form directly.
Worth flagging back to the client since the original v1 checklist listed
"submit inquiries" as a public action separate from account
registration.

**`POST /api/inquiries`** — requires `Authorization: Bearer <token>`
```json
// Request
{
  "name": "Ahmed Al-Farsi", "email": "ahmed@example.com",
  "phone": "966501234567", "subject": "Product inquiry: Steel Beam 200mm",
  "message": "I'd like a quote for 50 units."
}
```
```json
// Response
{
  "success": true, "message": "Inquiry submitted",
  "data": {
    "id": 5, "name": "Ahmed Al-Farsi", "email": "ahmed@example.com",
    "phone": "966501234567", "subject": "Product inquiry: Steel Beam 200mm",
    "message": "I'd like a quote for 50 units.",
    "status": "OPEN", "priority": "NORMAL",
    "createdAt": "2026-07-10T01:23:31"
  }
}
```
`name`, `email`, `subject`, `message` required; `phone` optional. The
submitted inquiry is now linked server-side to the logged-in account
(not present in this JSON response, but drives `GET /my` below).
Calling this with no token now returns `403`, JSON-wrapped as of
2026-07-17 (was an empty body from 2026-07-10 until then — see the
401/403 handling note at the top of this section).

**`GET /api/inquiries/my`** — requires `Authorization: Bearer <token>`,
new 2026-07-10
```json
// Response — same shape as the POST response's data, as an array,
// scoped to the caller only (not other users', not guest submissions
// made before this endpoint existed)
{
  "success": true, "message": "Inquiries fetched",
  "data": [
    {
      "id": 5, "name": "Ahmed Al-Farsi", "email": "ahmed@example.com",
      "phone": "966501234567", "subject": "Product inquiry: Steel Beam 200mm",
      "message": "I'd like a quote for 50 units.",
      "status": "OPEN", "priority": "NORMAL",
      "createdAt": "2026-07-10T01:23:31"
    }
  ]
}
```
Good fit for a "My Inquiries" list in the Profile tab, showing status
and past submissions.

### OTP verification (new 2026-07-15, no auth required)

**⚠️ Not yet wired into register/login/forgot-password on the backend**
— these endpoints exist and work standalone, but calling `/verify`
doesn't itself create an account or log anyone in. If the client wants
phone/email verification as part of registration, that flow (send →
user enters code → verify → then call the actual `/auth/register` or
`/auth/login`) needs to be built on both sides; nothing connects them
automatically today.

**`POST /api/otp/send`**
```json
// Request — send EITHER email or phone, not both (email wins if both given)
{ "email": "ahmed@example.com", "purpose": "REGISTRATION" }
```
`purpose` is one of `REGISTRATION` / `LOGIN` / `FORGOT_PASSWORD`,
case-insensitive on this endpoint.
```json
// Response 200
{ "success": true, "message": "OTP sent to a***@example.com", "data": null }
```
Email OTPs are a 6-digit code generated and tracked server-side (10-min
TTL). SMS OTPs are generated and delivered entirely by Twilio Verify —
the backend never sees the actual code for the SMS path.

**`POST /api/otp/verify`**
```json
// Request
{ "email": "ahmed@example.com", "otp": "123456", "purpose": "REGISTRATION" }
```
**⚠️ `purpose` must be sent in exact uppercase here** — unlike `/send`,
this endpoint does not normalize case, and a lowercase value currently
throws an unhandled server error (`500`) instead of a clean validation
error. Always send `REGISTRATION`/`LOGIN`/`FORGOT_PASSWORD` exactly as
shown.
```json
// Response 200
{ "success": true, "message": "Email OTP verified", "data": true }
```
On failure, `success: false` with a message like `"Invalid OTP"`,
`"OTP expired. Request a new one"`, or `"Too many attempts. Please
request a new OTP"` (email path only — SMS attempt/expiry limits are
enforced by Twilio, not documented here).

---

## 2. App-wide architecture

```
┌─────────────────────────────────────────────┐
│               React Native App               │
│                                               │
│  Navigation (React Navigation)                │
│   ├─ Splash                                   │
│   ├─ Auth stack (Login / Register / Forgot /  │
│   │   Reset)                                  │
│   └─ Main tabs (Home / Products / Services /  │
│       Gallery / Profile)                       │
│                                               │
│  Data layer (Phase 1: mock, later: live)      │
│   ├─ axios instance, baseURL from env config  │
│   ├─ request interceptor → attach Bearer token│
│   └─ response interceptor → 401/403 → logout  │
│                                               │
│  Server state: React Query (per-endpoint      │
│  hooks, cache, retry, pull-to-refresh)         │
│                                               │
│  Session state: token + role in secure storage│
│  (Keychain / EncryptedSharedPreferences —      │
│  NOT plain AsyncStorage)                       │
│                                               │
│  i18n: react-i18next, EN default, AR toggle,  │
│  RTL layout via I18nManager                    │
└───────────────────┬───────────────────────────┘
                     │  HTTPS + JWT (Phase 2+)
                     ▼
            Spring Boot backend (this repo)
```

- **One API client, one interceptor pair** — every screen's data hook
  goes through the same client so token attachment and 401/403 handling
  are never duplicated per-screen. As of 2026-07-17, 401/403 responses
  are JSON-wrapped (see §1) — the interceptor can read
  `response.data.message` uniformly, no empty-body special case needed.
- **No pagination anywhere yet** — build list screens with a
  `FlatList`/virtualized list but expect the *whole* dataset in one
  response. Pull-to-refresh, not infinite-scroll, for now.
- Contact/About are folded into the Profile or Home flow rather than a
  dedicated tab (see §4 bottom nav) to keep the tab bar to 5 items.

---

## 3. Design system

### Colors

Anchored on the brand blue from the logo. Since I can't see the actual
`splash-icon.png` pixel values from here, these are strong starting hex
values in the "navy → sky blue" family you asked for — sample the real
logo file and nudge `primary` to match exactly if it's off.

| Token | Hex | Use |
|---|---|---|
| `navy` | `#0B1F3A` | Deepest shade — header/nav background base, dark text on light surfaces, status bar area |
| `primary` (**app name / title color**) | `#2E6FE0` | The "light shade between navy and sky blue" — app name, active tab icon, primary buttons, links |
| `primaryLight` | `#5B9BF0` | Hover/pressed states, secondary icons, gradient end-stop |
| `sky` | `#8FD3FF` | Gradient accents, glass tints, subtle highlights |
| `accent` | `#F5A623` | Sparingly — CTA badges ("Featured", "New"), the one warm color against all the blue for contrast |
| `surface` | `#FFFFFF` | Cards, sheets |
| `surfaceMuted` | `#F4F7FC` | App background |
| `textPrimary` | `#0B1F3A` | Body text on light surfaces |
| `textMuted` | `#5C6B7A` | Secondary text, subtitles, placeholders |
| `success` | `#2AA876` | Confirmation states (inquiry submitted) |
| `error` | `#E5484D` | Form validation, error states |
| `whatsapp` | `#25D366` | WhatsApp FAB only — don't reuse this green elsewhere, keep it exclusive to that one CTA |

**Richness/depth technique**: don't use flat `primary` fills everywhere.
Use a subtle linear gradient (`navy → primary` or `primary → sky`) on the
header background, primary buttons, and the splash background — this is
what reads as "rich" rather than "flat corporate blue." Pair every
elevated surface (cards, header, bottom nav, FAB) with a soft shadow
(see Glass effect below) rather than a hard 1px border.

### Fonts

Bilingual (EN/AR) app, so pick a pairing that shares visual weight:

- **English / Latin**: **Poppins** (geometric, rounded, professional —
  reads as modern-corporate without being cold). Weights: 400 (body),
  500 (subtitles/labels), 600–700 (headings, app name).
- **Arabic**: **Cairo** — pairs well with Poppins (similarly geometric
  and rounded), widely used in professional Arabic-market apps, good
  weight range (400/600/700), free on Google Fonts.
- Fallback stack: system default (`San Francisco` iOS / `Roboto`
  Android) if the custom fonts fail to load, so the app never shows a
  broken/system-ugly font as a hard failure.
- Load both via `expo-font` (or `react-native-vector-icons`-style asset
  linking if bare RN), switch the active family based on the current
  i18n locale, not per-screen.

---

## 4. Global UI components

### Logo usage & the 3D effect

Use `splash-icon.png` (your existing asset) as the single source of
truth for the logo everywhere — splash screen, login page, app header —
rather than separate cropped/resized copies, so any future logo update
is a one-file swap.

**3D effect, achievable in React Native without a new asset:**
- Wrap the logo `Image` in a `View` with a layered shadow — a soft, larger,
  low-opacity shadow offset down-right (simulates depth/light source from
  top-left): `shadowColor: navy`, `shadowOffset: {width: 0, height: 6}`,
  `shadowOpacity: 0.25`, `shadowRadius: 12` (iOS), `elevation: 8`
  (Android).
- Add a subtle glossy highlight: an `expo-linear-gradient` overlay,
  `rgba(255,255,255,0.25)` at the top fading to transparent by the
  vertical midpoint, positioned absolutely over the top half of the logo
  — reads as a light reflection, a cheap but effective "3D glass icon"
  trick.
- On the Splash screen specifically: animate the logo in with a
  scale-up-from-90%-to-100% + fade-in (200–300ms, ease-out) using
  `react-native-reanimated` — motion sells depth more than a static
  shadow alone.
- Keep the shadow/gradient treatment consistent everywhere the logo
  appears (splash, header, login) — a logo with 3D treatment on one
  screen and flat on another reads as inconsistent, not intentional.

### Glass effect (glassmorphism)

Use `expo-blur`'s `BlurView` for a real backdrop blur (works on both
iOS and Android via Expo, unlike CSS-only blur tricks):
- App header and bottom nav: `BlurView` background,
  `intensity={40–60}`, `tint="light"`, layered under a
  `rgba(255,255,255,0.55)` tint overlay, `1px` border
  `rgba(255,255,255,0.4)`, and the soft shadow described above.
- Cards/sheets that float over imagery (e.g. a stat overlay on the
  featured product carousel): same `BlurView` treatment at a lower
  intensity (~20–30) so underlying content stays legible.
- Don't apply glass to every surface — reserve it for things that
  visually "float" over other content (header, bottom nav, FAB,
  modals/sheets). Flat cards on a plain background should stay
  opaque `surface`/`surfaceMuted` — glass on everything stops reading as
  a deliberate effect and starts looking like a rendering bug.

### App header — rounded, floating, glass

- A rounded rectangle (border radius ~24px), **not** stuck to the top
  edge — floats with margin below the safe-area inset (respecting the
  notch/status bar, see §5).
- Background: glass effect per above.
- **Left**: small `splash-icon.png` logo (3D treatment) + a text block:
  - Line 1: app name, **all caps**, `primary` color (`#2E6FE0`),
    Poppins/Cairo 600 weight, e.g. `IBN FIRNAS`.
  - Line 2, directly below, smaller: `TRADING AND CONTRACTING`, also
    all caps, same color family but lighter/muted (use `primaryLight`
    or `textMuted` at ~70% opacity), smaller font size (roughly 55–60%
    of the app name's size) — a subtitle, not competing with the name.
- **Right**: two tappable icons — a user/profile avatar (circular, shows
  the user's `avatarUrl` if logged in, else a generic person icon; tap
  → Profile tab or Login if logged out) and a sections/menu icon (grid
  or hamburger; tap → quick-nav sheet to About/Contact/Gallery if those
  aren't all in the bottom tab bar).
- This exact **all-caps, `primary`-blue** treatment applies to the app
  name/title wherever it appears (header, splash, login) — keep it
  consistent, don't reintroduce mixed-case or a different blue elsewhere.

### Bottom navigation

- Same floating-rounded-glass treatment as the header, for visual
  consistency (both read as one design system, not two different nav
  patterns).
- 5 tabs: **Home, Products, Services, Gallery, Profile** — Contact/About
  content lives inside Home or a Profile-adjacent screen rather than
  eating a 6th tab slot.
- Active tab: icon + label in `primary` blue; inactive: `textMuted`
  grey, icon-only or icon+small label per your icon set's readability.
- Positioned above the bottom safe-area inset (home indicator on iOS,
  gesture bar on Android) — see §5.

### Splash screen

- Full-bleed `navy → primary` gradient background (the "richness"
  gradient from §3).
- `splash-icon.png` centered, 3D treatment (shadow + gloss highlight)
  and the scale+fade entrance animation from above.
- App name below the icon in the same all-caps `primary`-on-gradient
  treatment — on a dark gradient background you may need a lighter tint
  of `primary` (closer to `sky`) so it stays legible against `navy`.
- This screen also does the mock-data warm-up in Phase 1 (or real
  session check once wired) before routing to Main tabs — see §2.

### Login page

- `splash-icon.png` at the top, **small but clearly visible** — roughly
  15–20% of screen width (not icon-toolbar-sized, not hero-sized), same
  3D shadow/gloss treatment as everywhere else.
- **Language toggle**: a small pill/button near the top (e.g.
  top-right, near or below the logo) showing `EN` / `AR` (or a globe
  icon + current language label) — tapping switches the app's active
  locale via `react-i18next` and triggers the RTL layout flip for
  Arabic (see below). Make this accessible from Login specifically
  since it's the first screen most first-time users see before
  registering.
- Standard email/password fields + "Log in" button using the `primary`
  gradient treatment, "Forgot password?" link, "Don't have an account?
  Register" link.

### RTL (Arabic) — technical note

Since Arabic is right-to-left, switching languages isn't just swapping
strings:
- Use `I18nManager.forceRTL(true/false)` + `I18nManager.allowRTL(true)`
  when the user toggles to Arabic — note this typically requires an app
  reload (`Updates.reloadAsync()` in Expo, or a manual restart prompt) to
  fully apply on some RN versions, so design the language-switch flow to
  expect a brief reload rather than an instant in-place flip.
- Build layouts with RTL-aware properties where possible (`flexDirection:
  'row'` mirrors automatically under RTL in RN; avoid hardcoding
  `marginLeft`/`marginRight` where `marginStart`/`marginEnd` would flip
  correctly instead).
- The app header's left/right content (logo+name vs. profile+menu) should
  swap sides under RTL — don't hardcode which icon is "left" vs "right"
  in a way that survives the RTL flip incorrectly.

---

## 5. Platform compatibility & safe areas

- Use `react-native-safe-area-context`: wrap the app root in
  `SafeAreaProvider`, and use `useSafeAreaInsets()` (not the older
  `SafeAreaView` alone) to position the floating header's top margin and
  the bottom nav's bottom margin — this is what actually keeps both
  clear of the notch/Dynamic Island/status bar on iOS and the gesture
  navigation bar on Android, across different device shapes.
- Test on at least one notched iOS device (or simulator) and one
  gesture-nav Android device (or emulator) — the floating rounded header/
  nav pattern is exactly the kind of UI that looks fine on a "normal"
  screen and collides with system UI on an edge-to-edge one if safe-area
  insets aren't actually wired in (as opposed to just hardcoded padding
  that happens to work on your primary test device).
- Blur (`expo-blur`) and shadow rendering differ slightly between iOS and
  Android — verify the glass header/nav on both platforms rather than
  just one, since Android's `elevation`-based shadow and iOS's
  `shadowOffset/shadowRadius` can need different tuning to look
  equivalent.

---

## 6. Per-module screens, features, and integration flow

### Splash
See §4 for visual spec. Functionally: warm up mock data (Phase 1) or
check secure storage for a token and validate it (Phase 2+, once wired)
→ route to Main tabs regardless of auth state (browsing is public).

### Auth stack
- **Login** — see §4 for the logo/language-toggle spec. Email + password
  → on submit, store token + role → navigate to Main tabs.
- **Register** — fullName, email, password (≥6 chars), phone (optional)
  → response includes a token, so auto-login, no separate login call.
- **Forgot Password** — email → generic "check your email" message
  regardless of whether the account exists.
- **Reset Password** — token (from email link) + new password.

### Home (tab)
- Sections: company banner (`Company.bannerUrl`), featured products
  carousel, featured services carousel, "Contact us" CTA row (using
  `Company.phone`/`email`/`address`/social links).
- WhatsApp floating button visible here (§7).

### Products (tab)
- **List screen**: category filter chips from Categories data. Filter
  client-side by `categoryId` (no `?category=` query param exists).
- **Detail screen**: single hero image (`primaryImageUrl` — see the §1
  correction, no multi-image gallery), name, price + `discountPrice`
  (strikethrough on original if a discount exists), `stockStatus` badge,
  description. "Inquire about this product" CTA → Inquiry form
  pre-filled with `subject: "Product inquiry: {product.name}"`. **As of
  2026-07-10 this CTA needs a login check first** (see the Inquiry
  section in §1) — if logged out, prompt login/register before showing
  the form, don't let the tap silently fail on a `403`.

### Services (tab)
- **List screen** and **Detail screen** — icon, image, description, same
  "Inquire about this service" CTA pattern as Product (including the
  2026-07-10 login-gate requirement noted above).

### Gallery (tab)
- Grid using `thumbnailUrl` (fall back to `mediaUrl`), tap → full-screen
  pinch-zoom/swipe viewer using `mediaUrl`. **Only render `title`,
  `mediaUrl`, `thumbnailUrl`, `mediaType`, `altText`, `displayOrder`** —
  see the §1 warning about `uploadedBy`.

### Inquiry form
- **Requires login as of 2026-07-10** — this is a change from the
  original design (was meant to be public, no account needed). Gate
  entry to this screen (and the Product/Service "Inquire" CTAs) behind
  an auth check; if logged out, route to Login/Register first with a
  way back to the pre-filled form afterward.
- Fields: `name`, `email`, `phone` (optional), `subject`, `message`.
  Client-side validation mirroring the backend constraints (§1).
  Success state: a deliberate confirmation screen, not just a toast.
- Consider prefilling `name`/`email` from the logged-in profile
  (`GET /profile`) since the user is authenticated anyway.

### Profile (tab)
- Logged out: "Log in / Register" prompt, not forced login on app open.
- Logged in: name/phone/avatar/email(read-only)/role, edit screen, and
  a password-change sub-flow requiring the current password.
- **New 2026-07-10**: a "My Inquiries" section/screen →
  `GET /api/inquiries/my` (see §1) — list of the user's own past
  inquiry submissions with `status`/`subject`/`createdAt`. Doesn't need
  pagination (same no-pagination pattern as the rest of the app for
  now).

---

## 7. WhatsApp floating chat icon

No backend work required — pure frontend feature.

**Design**
- Circular FAB, WhatsApp brand green (`#25D366`, kept exclusive to this
  one element — see §3), white glyph, ~56×56dp, drop shadow.
- Bottom-right, ~16–24dp margin, positioned above the bottom nav.
- Visible on: Home, Product Detail, Service Detail. Hidden on: Auth
  screens, the Inquiry form itself, and full-screen overlays (Gallery
  zoom viewer).
- Optional: one-time pulse animation on first appearance per session;
  hide-on-scroll-down / reveal-on-scroll-up on content-heavy screens.

**Behavior**
```js
Linking.openURL(
  `https://wa.me/${phoneDigitsOnly}?text=${encodeURIComponent(message)}`
)
```
- `phoneDigitsOnly`: from `Company.phone`, digits only, full
  international format with no leading `+` — **verify this format with
  whoever populates the field**, wa.me fails silently on the wrong
  format.
- `message` is context-aware (Home: general intro; Product/Service
  Detail: mentions the item by name).
- `wa.me` falls back to WhatsApp Web automatically if the app isn't
  installed — no separate fallback code needed.

---

## 8. Cross-cutting UI patterns
- **Loading**: skeleton placeholders, not spinners, for list/detail
  screens.
- **Error**: retry button + short human message — never surface raw
  backend error strings. As of 2026-07-17, 401/403 are JSON-wrapped like
  every other error status (see §1) — safe to read
  `error.response.data.message` uniformly now.
- **Empty states**: distinct copy for "nothing here yet" vs. "failed to
  load."
- **Images**: `expo-image` or `react-native-fast-image` for caching —
  backend URLs are plain, no CDN transform params.

---

## 9. Known backend issues relevant to this build

**Fixed since this doc was first written:**
- `GET /api/gallery` no longer leaks the uploader's password hash — see
  §1, fixed 2026-07-15.
- `GET /api/auth/me` no longer leaks the password hash — see §1, fixed
  2026-07-15.
- A token referencing a deleted user account used to crash the request
  with an unhandled exception; `JwtAuthenticationFilter` now catches
  this, clears the security context, and lets the request continue as
  anonymous (so a protected endpoint correctly falls through to a clean
  401/403 instead of a 500). Verified live 2026-07-17. Uncommitted as of
  this writing (local work on `developer-2`, not yet pushed).
- New centralized `401`/`403` JSON error responses (see §1) —
  `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` replace
  Spring Security's default empty-body responses with the same
  `{success, message, data}` envelope used everywhere else. Verified
  live 2026-07-17 against `/api/inquiries`, `/api/inquiries/my`, and
  `/api/inquiries` with anonymous/`ROLE_USER`/`ROLE_ADMIN` callers — safe
  to build against now. Uncommitted as of this writing (local work on
  `developer-2`, not yet pushed).

**Still open:**
- **Inquiry submission still requires login (changed 2026-07-10)** —
  see the Inquiry section in §1 and the "Inquiry form" notes in §6.
  Still needs a login gate on the Inquiry form and both "Inquire about
  this product/service" CTAs.
- **`GET /api/notifications` has the same password-hash-leak bug the
  Gallery endpoint used to have** — not a mobile-app v1 module
  (Notification is v1.1/admin-only), but flagging in case the mobile
  team ever touches it: don't build against this response shape until
  it's fixed backend-side.
- **New OTP endpoints (§1) exist but aren't wired into
  register/login/forgot-password** — don't assume calling
  `/api/otp/verify` does anything beyond confirming the code matched.
- **No pagination** on Product/Service/Gallery lists — structure hooks
  so adding `page`/`size` later is a data-layer change, not a UI
  rewrite.
- **`Product.specifications` and multi-image support don't exist** —
  don't design UI for either until the backend adds them.
- **Services have no category/grouping field** — unlike Product (which
  has a full `Category` tree), `ServiceEntity` has nothing to filter or
  group by. Build the Services tab as a flat list; don't design filter
  chips for it the way Products has them, unless the backend adds this
  later. (Service *does* now have a full CRUD `PUT` endpoint as of
  2026-07-15/16, if the admin panel needs it.)
