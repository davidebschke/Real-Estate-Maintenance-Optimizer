# Folder Structure

Binding folder structure reference for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 4.

This file must be reviewed and kept in sync with the actual folder structure every time files or directories are added, renamed, or removed — before generating new code, check whether the tree below still matches reality.

## Monorepo Layout

```
Real-Estate-Maintenance-Optimizer/
├── frontend/                      # Vue.js 3 application (Vite)
│   ├── src/
│   │   ├── assets/icons/          # SVG icons (e.g. brand logo)
│   │   ├── components/
│   │   │   ├── ViewPlaceholder.vue # reusable placeholder content block used by the not-yet-implemented views
│   │   │   ├── layout/            # app-wide layout building blocks (e.g. AppHeader, AppFooter, NavigationMenu and their parts)
│   │   │   ├── calendar/          # calendar building blocks (AppCalendar wrapping vue-cal, CalendarToolbar, CalendarDayHeader, CalendarEventCard)
│   │   │   └── appointments/      # appointment create/detail building blocks (AppointmentFormDialog, AppointmentDetailDrawer, AppointmentPropertySelect, AppointmentMaterialInput, AppointmentAiSuggestionBanner)
│   │   ├── views/                 # pages / route targets (e.g. OverviewView, CalendarView, StatisticsView, PropertiesView)
│   │   ├── router/                # Vue Router configuration (route definitions, NavigationKey type)
│   │   ├── services/              # Axios API clients (e.g. versionService.ts, appointmentService.ts)
│   │   ├── stores/                # state management (Pinia; e.g. stores/appointments.ts)
│   │   ├── composables/           # reusable Vue Composition functions (e.g. useLocale, useAppVersion, useCalendarNavigation, useCalendarAppointments, useAppointmentDeleteConfirmation)
│   │   ├── data/                  # hardcoded example/reference data standing in for a not-yet-built backend (e.g. exampleProperties.ts)
│   │   ├── types/                 # hand-written TypeScript types (e.g. Appointment, vue-cal.ts) plus ambient shims for untyped packages (vue-cal-shims.d.ts)
│   │   ├── utils/                 # small reusable, framework-agnostic helpers (e.g. locale-aware date formatting, appointmentSchedulingOptions.ts)
│   │   ├── i18n/                  # vue-i18n setup, merges all locale message files
│   │   ├── locales/
│   │   │   ├── de/                # German UI texts, one file per feature namespace (e.g. header.json, footer.json, overview.json, calendar.json, statistics.json, properties.json, appointments.json)
│   │   │   └── en/                # English UI texts, mirrors the de/ structure
│   │   ├── styles/
│   │   │   ├── layout/            # one CSS file per components/layout building block (e.g. app-header.css, app-footer.css, navigation-menu.css)
│   │   │   ├── calendar/          # one CSS file per components/calendar building block (e.g. app-calendar.css, calendar-toolbar.css)
│   │   │   ├── appointments/      # one CSS file per components/appointments building block (e.g. appointment-form-dialog.css, appointment-detail-drawer.css)
│   │   │   └── view-placeholder.css # styling for the shared ViewPlaceholder component
│   │   └── __tests__/             # Vitest unit tests
│   ├── e2e/                       # Playwright end-to-end tests (e.g. navigation.spec.ts, calendar.spec.ts, appointments.spec.ts)
│   ├── public/
│   ├── .env.example                # documents frontend runtime env vars (e.g. VITE_API_BASE_URL)
│   └── package.json
├── backend/                       # Spring Boot application
│   ├── src/main/java/com/remo/realestatemaintainceoptimizer/
│   │   ├── config/                # general configuration (e.g. LocalizationConfig for Accept-Language resolution, CorsConfig, StorageProperties)
│   │   ├── controller/             # REST endpoints (e.g. VersionController, AppointmentController, GlobalExceptionHandler)
│   │   ├── service/                # business logic (e.g. AppointmentService)
│   │   ├── repository/             # persistence (e.g. AppointmentFileRepository — JSON-file-backed, see backend readme)
│   │   ├── entity/                  # domain records (e.g. Appointment, HistoryEntry)
│   │   ├── exception/               # domain exceptions mapped to HTTP responses by GlobalExceptionHandler
│   │   ├── dto/                    # data transfer objects (e.g. VersionResponse, AppointmentResponse, CreateAppointmentRequest)
│   │   └── ...                    # further Java source code
│   ├── src/main/resources/
│   │   ├── application.yml        # Spring Boot configuration (incl. spring.messages.basename, remo.frontend.base-url, remo.storage.directory)
│   │   └── locales/                # German/English translations for dynamic (server-generated) text, Spring MessageSource convention: messages.properties (fallback bundle, English), messages_de.properties, messages_en.properties
│   ├── ExampleTerms/                # gitignored, created at runtime: appointments persisted as one JSON file each (see backend readme)
│   ├── src/test/java/...          # JUnit tests
│   └── pom.xml                     # holds the single source of truth for the app version (<version>), exposed via GET /api/version
├── .claude/
│   ├── rules/                      # binding rules extracted from CLAUDE.md (coding conventions, git workflow, ...)
│   └── skills/                     # project-specific Claude skills
├── .github/
│   └── ISSUE_TEMPLATE/
├── README.md
├── SECURITY.md
└── CLAUDE.md
```

## Backend Package Structure (Proposal, Classic Layered Approach)

```
com.remo.realestatemaintainceoptimizer
├── controller/     # REST endpoints
├── service/        # business logic
├── repository/     # persistence (Spring Data JPA once a database backs it; currently a JSON-file repository for appointments)
├── entity/         # domain model (JPA entities once a database backs them; currently plain records for appointments)
├── dto/            # data transfer objects
├── exception/      # domain exceptions mapped to HTTP responses
├── security/       # JWT / Spring Security configuration
├── ai/             # LangChain4j integration
└── config/         # general configuration
```

## `.claude` Directory

- `.claude/rules/` — binding project rules extracted from `CLAUDE.md`, one topic per file (e.g. `coding-conventions.md`, `git-workflow.md`, `testing.md`, `documentation.md`, `folder-structure.md`). Each corresponding section in `CLAUDE.md` references its rule file.
- `.claude/skills/` — project-specific Claude skills: `wiki-sync/SKILL.md` (Wiki update workflow per [`wiki.md`](wiki.md)) and `docs-consistency-check/SKILL.md` (post-change documentation review per [`documentation.md`](documentation.md)).

For the current, detailed implementation status of the backend and frontend (which controllers/services/components exist, what they do), see `backend/readme_backend_en.md` / `readme_backend_de.md` and `frontend/readme_frontend_en.md` / `readme_frontend_de.md` instead of this file — this file only tracks the physical folder layout.
