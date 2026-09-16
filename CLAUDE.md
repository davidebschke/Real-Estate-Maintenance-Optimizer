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

### 2.2 Backend
| Area                     | Technology                  |
|--------------------------|-----------------------------|
| Language / Framework     | Java 25 (LTS), Spring Boot 3 |
| Authentication           | Spring Security, JJWT (JSON Web Token) |
| Data Access              | Spring Data JPA             |
| Mail Sending             | Spring Mail                 |

### 2.3 Databases

The architecture uses a polyglot persistence approach with two separate databases:

| Area                             | Technology  | Hosting               |
|-----------------------------------|-------------|------------------------|
| Relational (User / Auth)         | PostgreSQL  | Supabase / Neon.tech   |
| NoSQL (Appointments & Objects)    | MongoDB     | MongoDB Atlas          |

- **PostgreSQL** stores user and authentication data, accessed via Spring Data JPA.
- **MongoDB** stores appointments and their associated objects.

### 2.4 AI / Intelligence
| Area       | Technology   |
|------------|--------------|
| Framework  | LangChain4j  |

## 3. Repository Status

Project documentation exists (`README.md`, `LICENSE`, `SECURITY.md`, `.github/ISSUE_TEMPLATE`). Current branch status is not tracked in this file — run `git branch -a` for the up-to-date list of local and remote branches. Once further code is added, Section 4 (Folder Structure) and Section 7 (Build & Run) must be reviewed and kept in sync with the actual state.

The detailed, current implementation status (which controllers/services/components exist and what they do) is documented in `backend/readme_backend_en.md` / `readme_backend_de.md` and `frontend/readme_frontend_en.md` / `readme_frontend_de.md`, not here — see [`.claude/rules/documentation.md`](.claude/rules/documentation.md) for why those files, not this one, are the canonical location. What follows is a short orientation summary only.

### 3.1 Backend

- `GET /api/version` (`VersionController`) reports the application version from `pom.xml`.
- Cross-origin access from the frontend dev server is allowed via `CorsConfig`, origin configured through `remo.frontend.base-url`.
- German/English translation for dynamic (server-generated) text via Spring's `MessageSource` (`backend/src/main/resources/locales/`, `LocalizationConfig`).
- **Appointments (create/read/move/delete/complete):** fully implemented via `AppointmentController`/`AppointmentService`, including recurrence materialization, locking, and a completed status (`PATCH .../complete` sets `actualEnd` to now, `PATCH .../reopen` reverts it).
  - **Storage (temporary, deviates from §2.3):** `AppointmentFileRepository` persists each appointment as its own JSON file under `remo.storage.directory` (gitignored) instead of MongoDB — a temporary prototype mechanism; a real database is expected to replace it later without changing the REST contract.
- **Properties (read-only):** `PropertyController`/`PropertyService` expose `GET /api/properties` and `GET /api/properties/{id}` for the same objects selectable when creating an appointment; create/update/delete are not yet implemented. Storage mirrors the appointment mechanism: `PropertyFileRepository` persists each property as its own JSON file under `remo.storage.properties.directory` (`ExampleObjects/`) — checked into the repository, unlike the gitignored `ExampleTerms/`, since there is no create endpoint yet to repopulate it after a fresh checkout.

### 3.2 Frontend

- **Layout & navigation:** header/footer and the four main routed views implemented; `OverviewView` and `StatisticsView` currently render only a placeholder, `CalendarView` renders the calendar, `PropertiesView` renders the property list.
- **Properties overview (read-only):** `PropertiesView` lists every property as a responsive card (icon, name, address, open/completed appointment counts, next appointment as a link into `AppointmentDetailDrawer`), backed by `stores/properties.ts` and derived from `stores/appointments.ts`; the same store now backs the appointment form's "Objekt" dropdown (previously hardcoded example data).
- **Calendar:** `vue-cal`-based day/week/month/year calendar with custom toolbar, per-day headers and category-colored events.
- **Appointments (create/read/move/delete/complete):** implemented via the shared `stores/appointments.ts` Pinia store and two overlays (`AppointmentFormDialog`, `AppointmentDetailDrawer`). A completed appointment gets a muted calendar color and is drawn up to its actual (not planned) end time.
- **i18n (static UI text):** German/English via `vue-i18n`, German as the active default locale, English as `fallbackLocale`.
- **Not yet implemented:** account management (non-functional placeholder only). Route/distance calculation (Feature 6) and location-based automatic scheduling (Features 3–4) are also not yet implemented.

### 3.3 Version Tracking

`pom.xml` holds the single source of truth for the application version (`<version>`, currently `0.1.0-SNAPSHOT`), surfaced to the frontend footer via `GET /api/version`. Whenever `pom.xml` is touched, check whether `<version>` changed; if it did, note the change here in Section 3 and verify the footer still displays the new version correctly (no other file duplicates this value, so nothing else needs updating).

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

Backend: `mvn spring-boot:run` (requires Java 25 and Maven; Spring Boot 4.1.1 via `pom.xml`).
Frontend: `npm install` then `npm run dev` (Vite dev server). `npm run build` for a production build, `npm run test:unit` (Vitest) and `npm run test:e2e` (Playwright) for tests.

## 8. Tests

Binding testing conventions are defined in [`.claude/rules/testing.md`](.claude/rules/testing.md).

## 9. Git Workflow

Binding Git workflow (remote, main branch, code owner, branch/PR process) is defined in [`.claude/rules/git-workflow.md`](.claude/rules/git-workflow.md).

## 10. Notes for Claude Code

- Binding documentation conventions are defined in [`.claude/rules/documentation.md`](.claude/rules/documentation.md). `CLAUDE.md` must be reviewed and updated every time something changes in the repository.
- All files under `.claude/rules/` are binding in addition to this document, including the coding conventions (Section 5), the folder structure (Section 4), testing conventions (Section 8), Git workflow (Section 9), and the GitHub Wiki sync rule ([`.claude/rules/wiki.md`](.claude/rules/wiki.md)) referenced above.
- After every content-relevant change (not a pure typo/formatting fix), check whether the corresponding page(s) in the project's GitHub Wiki also need updating — see [`.claude/rules/wiki.md`](.claude/rules/wiki.md) for which source files map to which Wiki pages and how to push the update.
- `CLAUDE.md` itself must stay a short entry point/router: detailed, fast-changing implementation status belongs in `backend/readme_backend_*.md` / `frontend/readme_frontend_*.md`, the full folder tree belongs in `.claude/rules/folder-structure.md`, and full feature descriptions belong in `README.md` — Sections 3, 4 and 6 of this file only hold short summaries pointing to those files. When implementation status changes, update the readmes (or `folder-structure.md`), not a long prose block here.
- `.claude/skills/` holds project-specific Claude skills: `wiki-sync` (Wiki update workflow), `docs-consistency-check` (post-change documentation review), `naming-conventions-check`, `branch-naming-check`, `commit-message-lint` (coding-convention checks), `folder-structure-sync` (folder tree sync), `test-coverage-check` (test coverage review) and `i18n-parity-check` (frontend/backend translation parity).
- Review security-relevant changes (Spring Security, JWT) especially carefully and never merge without tests.
- Whenever `pom.xml` is changed, check whether `<version>` changed; if it did, verify that `GET /api/version` and the frontend footer (`FooterVersion.vue`) still report the new version correctly, and update Section 3 accordingly.
