# CLAUDE.md

This file provides Claude Code (and other AI assistants) with the necessary context and binding guidelines for working in this repository.

## 1. Project Overview

**Real-Estate-Maintenance-Optimizer** is an intelligent system for appointment and route scheduling for tradespeople and field service technicians in property management.

### 1.1 Core Goal
Optimize property management maintenance by:
- reducing travel distances,
- better coordinating appointments based on location and duration,
- accounting for material trips and required material,
- estimating the potential duration of new appointments based on similar previous appointments with AI support.

Result: time and cost savings for the company, lower operating costs, happier tenants.

### 1.2 Prototype
https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer-Prototype

## 2. Tech Stack

### 2.1 Frontend
| Area              | Technology        |
|--------------------|-------------------|
| Framework          | Vue.js 3          |
| UI Components      | PrimeVue 4        |
| Calendar View      | Vue.cal           |
| Routing            | Vue Router        |
| HTTP Client        | Axios             |
| Map View           | Leaflet           |
| Charts             | Chart.js (via PrimeVue `Chart`) |

### 2.2 Backend
| Area                     | Technology                  |
|--------------------------|-----------------------------|
| Language / Framework     | Java 25 (LTS), Spring Boot 4 |
| Authentication           | Spring Security, JJWT (JSON Web Token) |
| Data Access              | Spring Data JPA             |
| External HTTP Client     | Spring `RestClient` (address geocoding, road routing and travel matrix via openrouteservice) |

### 2.3 Databases

All persistent data lives in a single relational database:

| Area                                          | Technology  | Hosting                                   |
|-----------------------------------------------|-------------|-------------------------------------------|
| Appointments, Objects & User/Auth             | PostgreSQL  | Supabase (project "RemoDB"); locally via `backend/docker-compose.yml` |

- **PostgreSQL** is accessed via Spring Data JPA, with the schema (`remo`) managed by Flyway migrations.

### 2.4 AI / Intelligence
| Area       | Technology   |
|------------|--------------|
| Framework  | LangChain4j (`langchain4j-anthropic`), Claude Haiku 4.5 (`claude-haiku-4-5`) for the AI appointment optimization |

## 3. Repository Status

Project documentation exists (`README.md`, `LICENSE`, `SECURITY.md`, `.github/ISSUE_TEMPLATE`). Current branch status is not tracked in this file — run `git branch -a` for the up-to-date list of local and remote branches. Once further code is added, Section 4 (Folder Structure) and Section 7 (Build & Run) must be reviewed and kept in sync with the actual state.

The detailed, current implementation status (which controllers/services/components exist and what they do) is documented in `backend/readme_backend_en.md` / `readme_backend_de.md` and `frontend/readme_frontend_en.md` / `readme_frontend_de.md`, not here — see [`.claude/rules/documentation.md`](.claude/rules/documentation.md) for why those files, not this one, are the canonical location. What follows is a short orientation summary only.

### 3.1 Backend

- `GET /api/version` (`VersionController`) reports the application version from `pom.xml`.
- Cross-origin access from the frontend dev server is allowed via `CorsConfig`, origin configured through `remo.frontend.base-url`.
- German/English translation for dynamic (server-generated) text via Spring's `MessageSource` (`backend/src/main/resources/locales/`, `LocalizationConfig`).
- **Authentication & account binding:** login and a session-only demo account via `AuthController` (Spring Security, JWT in an HttpOnly cookie, CSRF protection); every property and appointment belongs to exactly one account — see `backend/readme_backend_en.md`, section "Authentication & Account Binding".
- **Profile settings:** `AccountController`/`AccountService` (`PUT /api/account/username|password|appointment-buffer`, regular accounts only, demo accounts get `403`) change the username (needs the current password), the password (needs the current password; revokes all other sessions and re-issues the current one) and the account's own appointment buffer (`users.appointment_buffer_minutes`, overriding `remo.appointments.buffer-minutes`) — see `backend/readme_backend_en.md`, section "Authentication & Account Binding".
- **Appointments (create/read/move/update/delete/complete):** fully implemented via `AppointmentController`/`AppointmentService`, including recurrence materialization, locking, and a completed status (`PATCH .../complete` accepts an optional explicit `actualEnd`, falling back to now when omitted; `PATCH .../reopen` reverts it; `PUT /api/appointments/{id}` updates title, property, schedule, locked/recurring state, description and materials, rejecting a schedule change on a locked appointment). Creating (`POST /api/appointments`) or moving (`PATCH .../schedule`) an appointment rejects a schedule overlapping, or lying within the buffer (default `remo.appointments.buffer-minutes` = 5, overridable per account in the profile settings) of, another not-yet-deleted appointment of the same account with `409` and the next free slot of the same duration — see `backend/readme_backend_en.md`, section "Appointment Conflict Detection".
  - **Storage:** PostgreSQL via Spring Data JPA (`AppointmentRepository`/`PropertyRepository`), schema managed by Flyway; see `backend/readme_backend_en.md`, section "Persistence".
- **Properties (create/read/update/delete):** `PropertyController`/`PropertyService` expose `GET /api/properties`, `GET /api/properties/{id}`, `POST /api/properties`, `PUT /api/properties/{id}` (updates name/address/coordinates, keeping id and icon) and `DELETE /api/properties/{id}` (cascades to every appointment, apartment and tenant of that property) for the same objects selectable when creating an appointment, including optional `latitude`/`longitude` and the derived `tenantCount` (number of tenants in the property's apartments); a created property always gets the default icon `pi-building` (no icon field in the create form). Appointments reference their property by foreign key (no copied name/address), so edits show up immediately and deleting a property cascades in the database.
- **Apartments & tenants:** `ApartmentController`/`ApartmentService` (`GET/POST /api/properties/{id}/apartments`, `POST /api/apartments/{id}/tenants`, `PUT/DELETE /api/tenants/{id}`) store the apartments of a property (floor, area, rent, base rent, service charges) and their tenants; co-tenants share one apartment, the last tenant takes the apartment with them when deleted — see `backend/readme_backend_en.md`, section "Apartments & Tenants".
- **Address geocoding:** `GET /api/geocode` (`GeocodingController`/`GeocodingService`) proxies address lookups to the OpenStreetMap Nominatim API with a server-side identifying `User-Agent` (a browser cannot set this header itself, and Nominatim blocks requests without one); `GET /api/geocode/validate` additionally checks whether a structured street/house-number/postal-code/city combination really exists, returning `MATCH`/`SUGGESTION` (with a correction)/`NOT_FOUND` — see `backend/readme_backend_en.md`, section "Address Geocoding".
- **AI appointment optimization:** `OptimizationController` (`POST /api/optimizations`, `GET /api/optimizations/proposals`, `POST .../proposals/{id}/accept|reject`, `GET /api/optimizations/savings`): the I/O-free `OptimizationPlanner` calculates feasible moves inside the window of 28 to 365 days ahead (locked/completed appointments stay, recurring ones move at most 14 days, buffer plus driving time from the openrouteservice matrix to the neighbours, savings in km and driving time), Claude Haiku 4.5 via LangChain4j (`ai/`) selects which to propose, the planner re-validates them, and they are stored as pending proposals that only move an appointment once the user accepts them; a demo account may run it once; switchable via `remo.ai.enabled` (`501` while off) — see `backend/readme_backend_en.md`, section "AI Appointment Optimization".
- **Road routing:** `POST /api/routes` (`RoutingController`/`RoutingService`) proxies road route calculation (geometry, distance, travel time per leg, by car or on foot) to openrouteservice server-side so its API key never reaches the browser; cached, rate-limited, switchable via `remo.routing.enabled`, disabled routing answers `501` and any other failure `503` (the frontend then keeps the straight line, with a hint only for `503`) — see `backend/readme_backend_en.md`, section "Road Routing".

### 3.2 Frontend

- **Login screen:** a global router guard shows `LoginView` (login or demo account) before anything else; the header shows the logged-in account and logs out — see `frontend/readme_frontend_en.md`, "Authentication".
- **Profile & Settings:** the account menu's "Profile & Settings" entry opens `ProfileSettingsDialog` (username, password with the current password as proof, appointment buffer between appointments); it is disabled and not mounted for demo accounts — see `frontend/readme_frontend_en.md`.
- **Layout & navigation:** header/footer and the five main routed views implemented; `OverviewView` renders the daily appointment overview, `OptimizationView` the AI optimization, `StatisticsView` renders the statistics menu with the sub page `PropertyStatisticsView` (property statistics as a list), `CalendarView` renders the calendar, `PropertiesView` renders the property list.
- **Daily appointment overview (read-only, today only):** `OverviewView` lists only today's appointments (via `stores/appointments.ts` and the `useTodaysAppointments` composable), sorted from the next upcoming to the last one of the day, each as a numbered `DailyAppointmentCard` (number = position in the whole day's plan, matching its map marker; time, title, property, materials, drive to it/duration, plus a "Route via Google Maps" link from the current location to the property address); clicking one opens the shared `AppointmentDetailDrawer`. Ends with `OptimizationBanner`, which counts the pending AI optimization proposals and links to them.
- **Daily appointment map (read-only, today only):** `AppointmentMapCard` renders a Leaflet map to the left of the daily appointment list, showing today's appointments as numbered markers (same numbers as the cards, appointments at one location share a marker) connected by their road route (`useAppointmentRoute`, `services/routingService.ts`; switchable between car and walking via `RouteModeSwitch`; a dashed straight line while the route is unavailable); each marker prefers its property's stored coordinates, falling back to the backend's `GET /api/geocode` proxy (`services/geocodingService.ts`) only when those are missing. Auto-fits its zoom to all markers while still allowing manual zoom.
- **Properties overview (create/read/update/delete):** `PropertiesView` lists every property as a responsive card (icon, name, address, tenant count and a `PropertyCardControls` panel with manage tenants, edit and delete buttons), pinned first by a `PropertyCreateCard` that opens `PropertyFormDialog` (also used, pre-filled, for editing); backed by `stores/properties.ts` — details in `frontend/readme_frontend_en.md`.
- **Tenants (property detail view):** clicking a property card's name or its "Manage tenants" control panel button opens `PropertyDetailDrawer` listing the property's apartments with their tenants (`ApartmentCard`); `TenantFormDialog` (tenant data and apartment data as separate sections) creates a tenant with a new apartment, adds a further tenant to an existing apartment and edits a tenant; backed by `stores/tenants.ts` — see `frontend/readme_frontend_en.md`.
- **AI optimization & savings statistics:** `OptimizationView` (launch card with the planning rules and the demo account's single run, proposal cards with apply/decline, backed by `stores/optimization.ts`) and the statistics sub page `SavingsStatisticsView` (total tiles and a Chart.js line chart of the cumulative saved kilometres and driving hours per week or month) — see `frontend/readme_frontend_en.md`.
- **Calendar:** `vue-cal`-based day/week/month/year calendar with custom toolbar, per-day headers and category-colored events, plus a fifth, day-grouped list view (`CalendarListView`) that vue-cal itself does not provide.
- **Appointments (create/read/move/update/delete/complete):** implemented via the shared `stores/appointments.ts` Pinia store and two overlays (`AppointmentFormDialog`, `AppointmentDetailDrawer`). A completed appointment gets a muted calendar color and is drawn up to its actual (not planned) end time. Rescheduling also works by dragging an event onto a new day/time cell in the calendar's day/week view (`useAppointmentDragAndDrop`, Pointer Events based so mouse and touch both work), in addition to the manual form. An "Edit appointment" action in `AppointmentDetailDrawer` opens the shared `AppointmentFormDialog` in edit mode, pre-filled with the appointment's current title, property, schedule, locked/recurring state, description and materials.
- **i18n (static UI text):** German/English via `vue-i18n`, German as the active default locale, English as `fallbackLocale`.
- **Not yet implemented:** account management beyond login/logout and the profile settings (e.g. display name, deleting an account). The AI optimization proposes moves that the user confirms; a fully automatic rescheduling without confirmation and the AI-supported duration estimation are not implemented.

### 3.3 Version Tracking

`pom.xml` holds the single source of truth for the application version (`<version>`, currently `0.7.0-SNAPSHOT`), surfaced to the frontend footer via `GET /api/version`. Whenever `pom.xml` is touched, check whether `<version>` changed; if it did, note the change here in Section 3 and verify the footer still displays the new version correctly (no other file duplicates this value, so nothing else needs updating).

## 4. Folder Structure (Monorepo)

The full monorepo layout (frontend, backend, `.claude/`), the proposed backend package structure, and the `.claude/` directory contents are documented in [`.claude/rules/folder-structure.md`](.claude/rules/folder-structure.md). Review and keep that file in sync whenever files or directories are added, renamed, or removed — before generating new code, check whether it still matches the actual structure.

## 5. Coding Conventions

Binding coding conventions (variable names, comments, function/method design, commit messages, branch naming) are defined in [`.claude/rules/coding-conventions.md`](.claude/rules/coding-conventions.md). This file is binding for every code change in this repository.

## 6. Features & Epics (MVP)

Full descriptions (English & German) live in the root [`README.md`](README.md). Short reference list, in implementation-relevant order:

1. Appointment Overview (calendar + detail view)
2. Appointment Creation (manual form only in the MVP)
3. Location-Based Planning (automatic grouping by location)
4. Time-Based Planning (buffer times, standard-task templates)
5. Material Planning (material assigned per appointment)
6. Route Optimization (Leaflet only)
7. Fixed Appointments (locked against automatic AI rescheduling)
8. Recurring Appointments (automatic standing orders)

Backlog (native Android/iOS app, voice/audio appointment capture) is tracked in `README.md` as well.

## 7. Build & Run

Backend: `docker compose up -d` (local PostgreSQL) then `mvn spring-boot:run` (requires Java 25 and Maven; Spring Boot 4.1.1 via `pom.xml`) with `REMO_AUTH_JWT_SECRET` set (plus `REMO_AUTH_INITIAL_USER_PASSWORD` and `REMO_AUTH_COOKIE_SECURE=false` locally, optionally `REMO_ROUTING_ENABLED=true` with `REMO_ROUTING_API_KEY` for road routing and additionally `REMO_AI_ENABLED=true` with `REMO_AI_API_KEY` for the AI optimization, e.g. in `backend/.env`, see `backend/.env.example`); `mvn test` needs a running Docker daemon (Testcontainers).
A step-by-step guide (English & German, incl. a Visual Studio Code example and troubleshooting) is in the root `README.md`, section "Running the Project Locally" / "Projekt lokal starten".
Frontend: `npm install` then `npm run dev` (Vite dev server). `npm run build` for a production build, `npm run test:unit` (Vitest) and `npm run test:e2e` (Playwright, needs the backend and `E2E_PASSWORD`) for tests.

## 8. Tests

Binding testing conventions are defined in [`.claude/rules/testing.md`](.claude/rules/testing.md); binding linting conventions (PMD for the backend, ESLint for the frontend, both enforced in CI) are defined in [`.claude/rules/linting.md`](.claude/rules/linting.md).

## 9. Git Workflow

Binding Git workflow (remote, main branch, code owner, branch/PR process, issue effort labeling and matching model selection, and the mandatory post-implementation refactoring review and code review) is defined in [`.claude/rules/git-workflow.md`](.claude/rules/git-workflow.md).

## 10. Notes for Claude Code

- Binding documentation conventions are defined in [`.claude/rules/documentation.md`](.claude/rules/documentation.md). `CLAUDE.md` must be reviewed and updated every time something changes in the repository.
- All files under `.claude/rules/` are binding in addition to this document, including the coding conventions (Section 5), the folder structure (Section 4), testing and linting conventions (Section 8), Git workflow (Section 9), error-handling and robustness conventions ([`.claude/rules/error-handling.md`](.claude/rules/error-handling.md)), and the refactoring workflow ([`.claude/rules/refactoring.md`](.claude/rules/refactoring.md)).
- `CLAUDE.md` itself must stay a short entry point/router: detailed, fast-changing implementation status belongs in `backend/readme_backend_*.md` / `frontend/readme_frontend_*.md`, the full folder tree belongs in `.claude/rules/folder-structure.md`, and full feature descriptions belong in `README.md` — Sections 3, 4 and 6 of this file only hold short summaries pointing to those files. When implementation status changes, update the readmes (or `folder-structure.md`), not a long prose block here.
- `.claude/skills/` holds project-specific Claude skills: `docs-consistency-check` (post-change documentation review), `naming-conventions-check`, `branch-naming-check`, `commit-message-lint` (coding-convention checks), `folder-structure-sync` (folder tree sync), `test-coverage-check` (test coverage review), `i18n-parity-check` (frontend/backend translation parity) and `refactoring-review` (post-implementation refactoring review of the files a branch changed).
- `.claude/agents/` holds project-specific Claude subagents: `documentation-dave` (runs `docs-consistency-check`, read-only) and `paritycheck-paul` (runs `i18n-parity-check`, read-only); they can run in parallel after a non-trivial implementation.
- GitHub [spec-kit](https://github.com/github/spec-kit) is installed (`.specify/`, `.claude/skills/speckit-*`, see [`.claude/rules/folder-structure.md`](.claude/rules/folder-structure.md)) for optional Spec-Driven Development of new features: `/speckit-constitution` → `/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`, with `/speckit-clarify`, `/speckit-checklist`, `/speckit-analyze` and `/speckit-converge` as optional steps in between.
- Review security-relevant changes (Spring Security, JWT) especially carefully and never merge without tests.
- Whenever `pom.xml` is changed, check whether `<version>` changed; if it did, verify that `GET /api/version` and the frontend footer (`FooterVersion.vue`) still report the new version correctly, and update Section 3 accordingly.
