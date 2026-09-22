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
│   │   │   ├── appointments/      # appointment create/detail building blocks (AppointmentFormDialog, AppointmentDetailDrawer, AppointmentPropertySelect, AppointmentMaterialInput, AppointmentAiSuggestionBanner)
│   │   │   ├── properties/        # property-list building blocks (PropertyList, PropertyCard, PropertyCreateCard for the pinned "add property" card, PropertyFormDialog for its creation dialog, PropertyLocationPreviewMap for the dialog's Leaflet location preview)
│   │   │   ├── overview/          # daily appointment overview building blocks (DailyAppointmentList, DailyAppointmentCard)
│   │   │   └── map/               # appointment map building blocks (AppointmentMapCard for the card frame, AppointmentMap wrapping Leaflet)
│   │   ├── views/                 # pages / route targets (e.g. OverviewView, CalendarView, StatisticsView, PropertiesView)
│   │   ├── router/                # Vue Router configuration (route definitions, NavigationKey type)
│   │   ├── services/              # Axios API clients (e.g. versionService.ts, appointmentService.ts, propertyService.ts) and other external-service clients (geocodingService.ts, OpenStreetMap Nominatim)
│   │   ├── stores/                # state management (Pinia; e.g. stores/appointments.ts, stores/properties.ts)
│   │   ├── composables/           # reusable Vue Composition functions (e.g. useLocale, useAppVersion, useCalendarNavigation, useCalendarAppointments, useAppointmentDeleteConfirmation, usePropertyAppointmentSummaries, useTodaysAppointments, useAppointmentMapMarkers)
│   │   ├── types/                 # hand-written TypeScript types (e.g. Appointment, Property, vue-cal.ts) plus ambient shims for untyped packages (vue-cal-shims.d.ts)
│   │   ├── utils/                 # small reusable, framework-agnostic helpers (e.g. locale-aware date formatting, appointmentSchedulingOptions.ts)
│   │   ├── i18n/                  # vue-i18n setup, merges all locale message files
│   │   ├── locales/
│   │   │   ├── de/                # German UI texts, one file per feature namespace (e.g. header.json, footer.json, overview.json, calendar.json, statistics.json, properties.json, appointments.json)
│   │   │   └── en/                # English UI texts, mirrors the de/ structure
│   │   ├── styles/
│   │   │   ├── layout/            # one CSS file per components/layout building block (e.g. app-header.css, app-footer.css, navigation-menu.css)
│   │   │   ├── calendar/          # one CSS file per components/calendar building block (e.g. app-calendar.css, calendar-toolbar.css)
│   │   │   ├── appointments/      # one CSS file per components/appointments building block (e.g. appointment-form-dialog.css, appointment-detail-drawer.css)
│   │   │   ├── properties/        # one CSS file per components/properties building block plus PropertiesView (property-card.css, property-list.css, properties-view.css, property-create-card.css, property-form-dialog.css, property-location-preview-map.css)
│   │   │   ├── overview/          # one CSS file per components/overview building block (daily-appointment-list.css, daily-appointment-card.css)
│   │   │   ├── map/               # one CSS file per components/map building block (appointment-map-card.css, appointment-map.css)
│   │   │   └── view-placeholder.css # styling for the shared ViewPlaceholder component
│   │   └── __tests__/             # Vitest unit tests
│   ├── e2e/                       # Playwright end-to-end tests (e.g. navigation.spec.ts, calendar.spec.ts, appointments.spec.ts, overview-map.spec.ts, properties.spec.ts)
│   ├── public/
│   ├── .env.example                # documents frontend runtime env vars (e.g. VITE_API_BASE_URL)
│   └── package.json
├── backend/                       # Spring Boot application
│   ├── src/main/java/com/remo/realestatemaintainceoptimizer/
│   │   ├── config/                # general configuration (e.g. LocalizationConfig for Accept-Language resolution, CorsConfig, StorageProperties, PropertyStorageProperties, RestClientConfig)
│   │   ├── controller/             # REST endpoints (e.g. VersionController, AppointmentController, PropertyController, GeocodingController, GlobalExceptionHandler)
│   │   ├── service/                # business logic (e.g. AppointmentService, PropertyService, GeocodingService — proxies address geocoding to OpenStreetMap Nominatim, see backend readme)
│   │   ├── repository/             # persistence (e.g. AppointmentFileRepository, PropertyFileRepository — JSON-file-backed, see backend readme)
│   │   ├── entity/                  # domain records (e.g. Appointment, HistoryEntry, Property with latitude/longitude)
│   │   ├── exception/               # domain exceptions mapped to HTTP responses by GlobalExceptionHandler
│   │   ├── dto/                    # data transfer objects (e.g. VersionResponse, AppointmentResponse, CreateAppointmentRequest, PropertyResponse, CreatePropertyRequest, GeocodingResponse)
│   │   └── ...                    # further Java source code
│   ├── src/main/resources/
│   │   ├── application.yml        # Spring Boot configuration (incl. spring.messages.basename, remo.frontend.base-url, remo.storage.directory, remo.storage.properties.directory)
│   │   └── locales/                # German/English translations for dynamic (server-generated) text, Spring MessageSource convention: messages.properties (fallback bundle, English), messages_de.properties, messages_en.properties
│   ├── ExampleTerms/                # gitignored, created at runtime: appointments persisted as one JSON file each (see backend readme)
│   ├── ExampleObjects/              # checked in (unlike ExampleTerms/), seeded with one JSON file per property since there is no create endpoint yet (see backend readme)
│   ├── src/test/java/...          # JUnit tests
│   └── pom.xml                     # holds the single source of truth for the app version (<version>), exposed via GET /api/version
├── .claude/
│   ├── rules/                      # binding rules extracted from CLAUDE.md (coding conventions, git workflow, ...)
│   └── skills/                     # project-specific Claude skills, plus the GitHub spec-kit skills (speckit-constitution, speckit-specify, speckit-plan, speckit-tasks, speckit-clarify, speckit-checklist, speckit-analyze, speckit-implement, speckit-converge, speckit-taskstoissues)
├── .specify/                       # GitHub spec-kit (Spec-Driven Development) project infrastructure, installed via `specify init --here --integration claude`
│   ├── memory/constitution.md      # project constitution, derived from .claude/rules/*.md, filled/updated via /speckit-constitution
│   ├── templates/                  # spec/plan/tasks/checklist templates used by the speckit-* skills
│   ├── scripts/bash/               # shell scripts the speckit-* skills call (feature directory creation, prerequisite checks, ...)
│   └── workflows/speckit/          # bundled spec-kit workflow definition (not currently invoked by any speckit-* skill)
├── specs/                          # not yet created — appears on the first `/speckit-specify` run, one `NNN-short-name/` directory per feature (spec.md, plan.md, tasks.md, ...); `NNN` is spec-kit's own sequential counter (`.specify/init-options.json` → `feature_numbering`), independent of the git branch issue-number counter in `.claude/rules/git-workflow.md` — the two numbers are not expected to match
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── workflows/                  # GitHub Actions CI (backend-tests.yml, frontend-tests.yml)
├── README.md
├── SECURITY.md
└── CLAUDE.md
```

## Backend Package Structure (Proposal, Classic Layered Approach)

```
com.remo.realestatemaintainceoptimizer
├── controller/     # REST endpoints
├── service/        # business logic
├── repository/     # persistence (Spring Data JPA once a database backs it; currently JSON-file repositories for appointments and properties)
├── entity/         # domain model (JPA entities once a database backs them; currently plain records for appointments and properties)
├── dto/            # data transfer objects
├── exception/      # domain exceptions mapped to HTTP responses
├── security/       # JWT / Spring Security configuration
├── ai/             # LangChain4j integration
└── config/         # general configuration
```

## `.claude` Directory

- `.claude/rules/` — binding project rules extracted from `CLAUDE.md`, one topic per file (e.g. `coding-conventions.md`, `git-workflow.md`, `testing.md`, `documentation.md`, `folder-structure.md`). Each corresponding section in `CLAUDE.md` references its rule file.
- `.claude/skills/` — project-specific Claude skills: `wiki-sync/SKILL.md` (Wiki update workflow per [`wiki.md`](wiki.md)), `docs-consistency-check/SKILL.md` (post-change documentation review per [`documentation.md`](documentation.md)), `naming-conventions-check/SKILL.md` and `branch-naming-check/SKILL.md` and `commit-message-lint/SKILL.md` (naming/branch/commit conventions per [`coding-conventions.md`](coding-conventions.md)), `folder-structure-sync/SKILL.md` (keeps this file in sync with the actual tree), `test-coverage-check/SKILL.md` (coverage review per [`testing.md`](testing.md)), and `i18n-parity-check/SKILL.md` (frontend/backend translation parity) — plus the GitHub spec-kit skills (`speckit-constitution`, `speckit-specify`, `speckit-plan`, `speckit-tasks`, `speckit-clarify`, `speckit-checklist`, `speckit-analyze`, `speckit-implement`, `speckit-converge`, `speckit-taskstoissues`) that drive the Spec-Driven Development workflow described in `.specify/`.

For the current, detailed implementation status of the backend and frontend (which controllers/services/components exist, what they do), see `backend/readme_backend_en.md` / `readme_backend_de.md` and `frontend/readme_frontend_en.md` / `readme_frontend_de.md` instead of this file — this file only tracks the physical folder layout.
