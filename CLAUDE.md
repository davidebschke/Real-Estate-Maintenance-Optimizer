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

Status of this file: project documentation exists (`README.md`, `LICENSE`, `SECURITY.md`, `.github/ISSUE_TEMPLATE`). Backend now has a first REST endpoint: `GET /api/version` (`VersionController`, `dto/VersionResponse`), which reports the application version currently packaged from `pom.xml` via Spring Boot's `BuildProperties` (generated at build time through the `spring-boot-maven-plugin` `build-info` goal, bound to the `generate-resources` phase so it runs before compilation/tests). Cross-origin access from the frontend dev server is allowed via `CorsConfig` (`config/`), whose allowed origin is configured through `remo.frontend.base-url` in `application.yml`. Backend also has a German/English translation setup for dynamic (server-generated) text: `backend/src/main/resources/locales/` (`messages.properties`, `messages_de.properties`, `messages_en.properties`, resolved via Spring's `MessageSource`) plus `LocalizationConfig` (`backend/src/main/java/.../config/`), which resolves the request locale from the `Accept-Language` header, falling back to English when no supported locale is requested (the base `messages.properties` bundle also holds the English texts, matching this fallback). Frontend now contains three real features: the application header, the application footer (`frontend/src/components/layout/`), and functional main navigation, built with Vue 3 and PrimeVue 4, including reusable composables (`useLocale`, `useAppVersion`), a first Axios-based service (`services/versionService.ts`, calling the backend version endpoint), a German/English translation setup for static UI text via `vue-i18n` (`frontend/src/locales/`, `frontend/src/i18n/`, German as the active default locale, English as `fallbackLocale` for missing keys), and its component styles extracted into `frontend/src/styles/` (referenced via `<style scoped src="...">`). The footer is a narrow, black, fixed bar pinned to the bottom of the viewport (`App.vue` wraps header/content/footer in a flex column with `min-height: 100vh`) showing a copyright notice and the backend-reported version, and is responsive like the header. Navigation is implemented via Vue Router (`frontend/src/router/index.ts`): the four main navigation entries (Overview/`Übersicht`, Calendar/`Kalender`, Statistics/`Statistik`, Properties/`Immobilien`) each route to a dedicated view under `frontend/src/views/` (`OverviewView.vue`, `CalendarView.vue`, `StatisticsView.vue`, `PropertiesView.vue`), which currently render only a localized placeholder sentence (e.g. "Hier ist die Übersichtsseite") via the reusable `components/ViewPlaceholder.vue`. `NavigationMenu.vue` renders its links as `RouterLink`s (active entry highlighted via `router-link-exact-active`) and the header logo/brand name (`AppHeader.vue`) link back to the overview page. Appointment creation and account management are not implemented yet — only non-functional placeholders (disabled button, example dropdown data).

Current branch status is not tracked in this file — run `git branch -a` for the up-to-date list of local and remote branches.

Once further code is added, Section 4 (Folder Structure) and Section 7 (Build & Run) must be reviewed and kept in sync with the actual state.

**Version check:** `pom.xml` holds the single source of truth for the application version (`<version>`, currently `0.1.0-SNAPSHOT`), surfaced to the frontend footer via `GET /api/version`. Whenever `pom.xml` is touched, check whether `<version>` changed; if it did, note the change here in Section 3 and verify the footer still displays the new version correctly (no other file duplicates this value, so nothing else needs updating).

## 4. Folder Structure (Monorepo)

```
Real-Estate-Maintenance-Optimizer/
├── frontend/                      # Vue.js 3 application (Vite)
│   ├── src/
│   │   ├── assets/icons/          # SVG icons (e.g. brand logo)
│   │   ├── components/
│   │   │   ├── ViewPlaceholder.vue # reusable placeholder content block used by the not-yet-implemented views
│   │   │   └── layout/            # app-wide layout building blocks (e.g. AppHeader, AppFooter, NavigationMenu and their parts)
│   │   ├── views/                 # pages / route targets (e.g. OverviewView, CalendarView, StatisticsView, PropertiesView)
│   │   ├── router/                # Vue Router configuration (route definitions, NavigationKey type)
│   │   ├── services/              # Axios API clients (e.g. versionService.ts)
│   │   ├── stores/                # state management (Pinia)
│   │   ├── composables/           # reusable Vue Composition functions (e.g. useLocale, useAppVersion)
│   │   ├── i18n/                  # vue-i18n setup, merges all locale message files
│   │   ├── locales/
│   │   │   ├── de/                # German UI texts, one file per feature namespace (e.g. header.json, footer.json, overview.json, calendar.json, statistics.json, properties.json)
│   │   │   └── en/                # English UI texts, mirrors the de/ structure
│   │   ├── styles/
│   │   │   ├── layout/            # one CSS file per components/layout building block (e.g. app-header.css, app-footer.css, navigation-menu.css)
│   │   │   └── view-placeholder.css # styling for the shared ViewPlaceholder component
│   │   └── __tests__/             # Vitest unit tests
│   ├── e2e/                       # Playwright end-to-end tests (e.g. navigation.spec.ts)
│   ├── public/
│   ├── .env.example                # documents frontend runtime env vars (e.g. VITE_API_BASE_URL)
│   └── package.json
├── backend/                       # Spring Boot application
│   ├── src/main/java/com/remo/realestatemaintainceoptimizer/
│   │   ├── config/                # general configuration (e.g. LocalizationConfig for Accept-Language resolution, CorsConfig)
│   │   ├── controller/             # REST endpoints (e.g. VersionController)
│   │   ├── dto/                    # data transfer objects (e.g. VersionResponse)
│   │   └── ...                    # further Java source code (service, repository, entity, ...)
│   ├── src/main/resources/
│   │   ├── application.yml        # Spring Boot configuration (incl. spring.messages.basename, remo.frontend.base-url)
│   │   └── locales/                # German/English translations for dynamic (server-generated) text, Spring MessageSource convention: messages.properties (fallback bundle, English), messages_de.properties, messages_en.properties
│   ├── src/test/java/...          # JUnit tests
│   └── pom.xml                     # holds the single source of truth for the app version (<version>), exposed via GET /api/version
├── .claude/
│   ├── rules/                      # binding rules extracted from this file (coding conventions, git workflow, ...)
│   └── skills/                     # project-specific Claude skills
├── .github/
│   └── ISSUE_TEMPLATE/
├── README.md
├── SECURITY.md
└── CLAUDE.md
```

Backend package structure (proposal, classic layered approach):
```
com.remo.realestatemaintainceoptimizer
├── controller/     # REST endpoints
├── service/        # business logic
├── repository/     # Spring Data JPA repositories
├── entity/         # JPA entities
├── dto/            # data transfer objects
├── security/       # JWT / Spring Security configuration
├── ai/             # LangChain4j integration
└── config/         # general configuration
```

### 4.1 `.claude` Directory

- `.claude/rules/` — binding project rules extracted from this file, one topic per file (e.g. `coding-conventions.md`, `git-workflow.md`, `testing.md`, `documentation.md`). Each section below references its corresponding rule file.
- `.claude/skills/` — project-specific Claude skills. Currently empty (`.gitkeep`).

## 5. Coding Conventions

Binding coding conventions (variable names, comments, function/method design, commit messages, branch naming) are defined in [`.claude/rules/coding-conventions.md`](.claude/rules/coding-conventions.md). This file is binding for every code change in this repository.

## 6. Features & Epics (MVP)

1. **Appointment Overview** – short view (calendar) and detailed view (full job context).
2. **Appointment Creation** – manual input via form only in the MVP.
3. **Location-Based Planning** – automatic grouping by location to reduce travel time.
4. **Time-Based Planning** – automatic scheduling with buffer times, templates for standard tasks (e.g. basement cleaning).
5. **Material Planning** – assigning required material to an appointment to avoid extra trips.
6. **Route Optimization** – runs exclusively via Leaflet.
7. **Fixed Appointments** – locking individual appointments against automatic AI rescheduling.
8. **Recurring Appointments** – automatic standing orders (e.g. inspection every 3 months).

### 6.1 Backlog
- Native app port for Android & iOS.
- Voice/audio capture for appointment creation (hands-free use).

## 7. Build & Run

Backend: `mvn spring-boot:run` (requires Java 25 and Maven; Spring Boot 4.1.1 via `pom.xml`).
Frontend: `npm install` then `npm run dev` (Vite dev server). `npm run build` for a production build, `npm run test:unit` (Vitest) and `npm run test:e2e` (Playwright) for tests.

## 8. Tests

Binding testing conventions are defined in [`.claude/rules/testing.md`](.claude/rules/testing.md).

## 9. Git Workflow

Binding Git workflow (remote, main branch, code owner, branch/PR process) is defined in [`.claude/rules/git-workflow.md`](.claude/rules/git-workflow.md).

## 10. Notes for Claude Code

- Binding documentation conventions are defined in [`.claude/rules/documentation.md`](.claude/rules/documentation.md). `CLAUDE.md` must be reviewed and updated every time something changes in the repository.
- All files under `.claude/rules/` are binding in addition to this document, including the coding conventions (Section 5), testing conventions (Section 8), and Git workflow (Section 9) referenced above.
- `.claude/skills/` holds project-specific Claude skills (currently empty).
- Review security-relevant changes (Spring Security, JWT) especially carefully and never merge without tests.
- Whenever `pom.xml` is changed, check whether `<version>` changed; if it did, verify that `GET /api/version` and the frontend footer (`FooterVersion.vue`) still report the new version correctly, and update Section 3 accordingly.
