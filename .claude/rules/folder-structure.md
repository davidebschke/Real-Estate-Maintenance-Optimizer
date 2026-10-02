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
│   │   │   ├── calendar/          # calendar building blocks (AppCalendar wrapping vue-cal, CalendarToolbar, CalendarDayHeader, CalendarEventCard, CalendarListView for the day-grouped list view vue-cal itself does not provide)
│   │   │   ├── appointments/      # appointment create/detail building blocks (AppointmentFormDialog, AppointmentDetailDrawer, AppointmentConflictNotice shared by both for a blocked creation/move, AppointmentPropertySelect, AppointmentMaterialInput, AppointmentAiSuggestionBanner)
│   │   │   ├── properties/        # property-list building blocks (PropertyList, PropertyCard with its edit/delete icon buttons, PropertyCreateCard for the pinned "add property" card, PropertyFormDialog shared by creation and, pre-filled, editing, PropertyLocationPreviewMap for the dialog's Leaflet location preview)
│   │   │   ├── overview/          # daily appointment overview building blocks (DailyAppointmentList, DailyAppointmentCard)
│   │   │   ├── map/               # appointment map building blocks (AppointmentMapCard for the card frame, AppointmentMap wrapping Leaflet)
│   │   │   ├── auth/              # login/demo account building blocks (AuthCard switching between LoginForm and DemoAccountPanel, DemoAccountBanner below the header, DemoQuotaHint in the create dialogs)
│   │   │   ├── profile/           # profile and settings dialog building blocks (ProfileSettingsDialog, ProfileSection as the shared frame of the three sections, ProfileUsernameSection, ProfilePasswordConfirmDialog, ProfilePasswordSection, ProfileAppointmentBufferSection)
│   │   │   └── forms/             # form building blocks shared by several dialogs (RequiredFieldLabel for a label with its required-field asterisk)
│   │   ├── views/                 # pages / route targets (e.g. OverviewView, CalendarView, StatisticsView, PropertiesView, LoginView)
│   │   ├── router/                # Vue Router configuration (route definitions incl. the public /login route, NavigationKey type, authGuard.ts login guard)
│   │   ├── services/              # Axios API clients (e.g. versionService.ts, appointmentService.ts, propertyService.ts, authService.ts, accountService.ts), httpClient.ts for the shared axios defaults (session cookie, CSRF header, expired-session handling), and other external-service clients (geocodingService.ts, OpenStreetMap Nominatim)
│   │   ├── stores/                # state management (Pinia; e.g. stores/appointments.ts, stores/properties.ts, stores/auth.ts)
│   │   ├── composables/           # reusable Vue Composition functions (e.g. useLocale, useAppVersion, useCalendarNavigation, useCalendarAppointments, useAppointmentDragAndDrop, useAppointmentDeleteConfirmation, usePropertyAppointmentSummaries, usePropertyDeleteConfirmation, useTodaysAppointments, useAppointmentMapMarkers, useSuggestedSlotLabel, useDemoQuota, useAccountPresentation, useMediaQuery, useDurationOptions, useTouchedFields, useSettingsSubmission)
│   │   ├── types/                 # hand-written TypeScript types (e.g. Appointment, Property, auth.ts, vue-cal.ts) plus ambient shims for untyped packages (vue-cal-shims.d.ts)
│   │   ├── utils/                 # small reusable, framework-agnostic helpers (e.g. locale-aware date formatting, appointmentSchedulingOptions.ts, propertyAddressParsing.ts for splitting a stored property address back into its form fields, addressFieldValidation.ts for the street/house number/postal code/city format checks, safeRedirect.ts for the post-login redirect, initials.ts, accountFieldValidation.ts for the username/password/appointment buffer checks of the profile dialog)
│   │   ├── i18n/                  # vue-i18n setup, merges all locale message files
│   │   ├── locales/
│   │   │   ├── de/                # German UI texts, one file per feature namespace (e.g. header.json, footer.json, overview.json, calendar.json, statistics.json, properties.json, appointments.json, auth.json, profile.json)
│   │   │   └── en/                # English UI texts, mirrors the de/ structure
│   │   ├── styles/
│   │   │   ├── layout/            # one CSS file per components/layout building block (e.g. app-header.css, app-footer.css, navigation-menu.css)
│   │   │   ├── calendar/          # one CSS file per components/calendar building block (e.g. app-calendar.css, calendar-toolbar.css, calendar-list-view.css)
│   │   │   ├── appointments/      # one CSS file per components/appointments building block (e.g. appointment-form-dialog.css, appointment-detail-drawer.css, appointment-conflict-notice.css)
│   │   │   ├── properties/        # one CSS file per components/properties building block plus PropertiesView (property-card.css, property-list.css, properties-view.css, property-create-card.css, property-form-dialog.css, property-location-preview-map.css)
│   │   │   ├── overview/          # one CSS file per components/overview building block (daily-appointment-list.css, daily-appointment-card.css)
│   │   │   ├── map/               # one CSS file per components/map building block (appointment-map-card.css, appointment-map.css)
│   │   │   ├── auth/              # one CSS file per components/auth building block plus LoginView (login-view.css, auth-card.css, login-form.css, demo-account-panel.css, demo-account-banner.css, demo-quota-hint.css)
│   │   │   ├── profile/           # one CSS file per components/profile building block set (profile-settings-dialog.css)
│   │   │   ├── forms/             # one CSS file per components/forms building block (required-field-label.css)
│   │   │   ├── view-placeholder.css # styling for the shared ViewPlaceholder component
│   │   │   └── confirm-dialog.css # styling for the globally-mounted PrimeVue ConfirmDialog (appointment and property delete confirmations)
│   │   └── __tests__/             # Vitest unit tests (httpError.ts is the shared helper building the axios error a rejected request carries)
│   ├── e2e/                       # Playwright end-to-end tests (e.g. navigation.spec.ts, calendar.spec.ts, appointments.spec.ts, overview-map.spec.ts, properties.spec.ts, auth.spec.ts, profile.spec.ts), auth.setup.ts logs in once and stores the session in the git-ignored .auth/, support/apiSession.ts provides e2e credentials and CSRF headers, support/testProperty.ts creates/deletes a per-test property via the API
│   ├── public/
│   ├── .env.example                # documents frontend runtime env vars (e.g. VITE_API_BASE_URL)
│   └── package.json
├── backend/                       # Spring Boot application
│   ├── src/main/java/com/remo/realestatemaintainceoptimizer/
│   │   ├── config/                # general configuration (e.g. LocalizationConfig for Accept-Language resolution, CorsConfig, RestClientConfig, AuthProperties, AppointmentSchedulingProperties for the default appointment buffer time, SchedulingConfig)
│   │   ├── controller/             # REST endpoints (e.g. VersionController, AuthController, AppointmentController, PropertyController, AccountController for the profile settings, GeocodingController, GlobalExceptionHandler)
│   │   ├── service/                # business logic (e.g. AppointmentService, PropertyService, GeocodingService — proxies address geocoding to OpenStreetMap Nominatim, see backend readme —, AuthService, AccountService for username/password/appointment buffer changes, DemoAccountService, DemoDataSeeder, DemoAccountCleanupScheduler, InitialAccountPasswordService)
│   │   ├── repository/             # persistence via Spring Data JPA (e.g. AppointmentRepository, PropertyRepository, UserRepository, see backend readme)
│   │   ├── entity/                  # JPA entities (e.g. Appointment, HistoryEntry as @Embeddable record, Property with ownerId and latitude/longitude, User)
│   │   ├── exception/               # domain exceptions mapped to HTTP responses by GlobalExceptionHandler
│   │   ├── security/                # Spring Security + session JWT (SecurityConfig, JwtService, SessionCookieManager, JwtAuthenticationFilter, CsrfCookieFilter, AuthenticatedUser, SlidingWindowRateLimiter, PasswordPolicy and UsernamePolicy for the shared account field limits)
│   │   ├── dto/                    # data transfer objects (e.g. VersionResponse, AppointmentResponse, CreateAppointmentRequest, AppointmentConflictResponse, PropertyResponse, CreatePropertyRequest, GeocodingResponse, AddressValidationResponse, AddressValidationStatus, LoginRequest, CurrentUserResponse)
│   │   └── ...                    # further Java source code
│   ├── src/main/resources/
│   │   ├── application.yml        # Spring Boot configuration (incl. spring.messages.basename, remo.frontend.base-url, remo.auth.*, optional import of a local .env, local spring.datasource defaults, JPA/Flyway settings)
│   │   ├── db/migration/           # Flyway migrations (V<n>__*.sql) owning the PostgreSQL schema "remo", applied on startup
│   │   └── locales/                # German/English translations for dynamic (server-generated) text, Spring MessageSource convention: messages.properties (fallback bundle, English), messages_de.properties, messages_en.properties
│   ├── docker-compose.yml           # local PostgreSQL 17 for development, matching the application.yml datasource defaults
│   ├── .env.example                 # documents the production (Supabase) SPRING_DATASOURCE_* and the REMO_AUTH_* environment variables (a local copy as backend/.env is git-ignored)
│   ├── src/test/java/...          # JUnit tests (TestcontainersConfiguration provides the PostgreSQL test database, TestAccounts creates accounts, session cookies and CSRF tokens)
│   ├── src/test/resources/          # application.properties with the test-only JWT secret
│   ├── pmd-exclusions.properties    # PMD baseline (see .claude/rules/linting.md), grandfathers the violations that existed when the linter was introduced
│   └── pom.xml                     # holds the single source of truth for the app version (<version>), exposed via GET /api/version; also configures the maven-pmd-plugin (bound to the verify phase)
├── .claude/
│   ├── rules/                      # binding rules extracted from CLAUDE.md (coding conventions, git workflow, ...)
│   └── skills/                     # project-specific Claude skills (incl. refactoring-review), plus the GitHub spec-kit skills (speckit-constitution, speckit-specify, speckit-plan, speckit-tasks, speckit-clarify, speckit-checklist, speckit-analyze, speckit-implement, speckit-converge, speckit-taskstoissues)
├── .specify/                       # GitHub spec-kit (Spec-Driven Development) project infrastructure, installed via `specify init --here --integration claude`
│   ├── memory/constitution.md      # project constitution, derived from .claude/rules/*.md, filled/updated via /speckit-constitution
│   ├── templates/                  # spec/plan/tasks/checklist templates used by the speckit-* skills
│   ├── scripts/bash/               # shell scripts the speckit-* skills call (feature directory creation, prerequisite checks, ...)
│   └── workflows/speckit/          # bundled spec-kit workflow definition (not currently invoked by any speckit-* skill)
├── specs/                          # not yet created — appears on the first `/speckit-specify` run, one `NNN-short-name/` directory per feature (spec.md, plan.md, tasks.md, ...); `NNN` is spec-kit's own sequential counter (`.specify/init-options.json` → `feature_numbering`), independent of the git branch issue-number counter in `.claude/rules/git-workflow.md` — the two numbers are not expected to match
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── workflows/                  # GitHub Actions CI (backend-tests.yml, frontend-tests.yml) and CD (deploy-oracle-cloud.yml — builds backend jar and frontend dist and deploys them to the Oracle Cloud instance via SSH/rsync/scp on every push to main)
├── README.md
├── SECURITY.md
└── CLAUDE.md
```

## Backend Package Structure (Proposal, Classic Layered Approach)

```
com.remo.realestatemaintainceoptimizer
├── controller/     # REST endpoints
├── service/        # business logic
├── repository/     # persistence (Spring Data JPA repositories backed by PostgreSQL)
├── entity/         # domain model (JPA entities)
├── dto/            # data transfer objects
├── exception/      # domain exceptions mapped to HTTP responses
├── security/       # JWT / Spring Security configuration
├── ai/             # LangChain4j integration
└── config/         # general configuration
```

## `.claude` Directory

- `.claude/rules/` — binding project rules extracted from `CLAUDE.md`, one topic per file (e.g. `coding-conventions.md`, `git-workflow.md`, `testing.md`, `linting.md`, `error-handling.md`, `refactoring.md`, `documentation.md`, `folder-structure.md`). Each corresponding section in `CLAUDE.md` references its rule file.
- `.claude/skills/` — project-specific Claude skills: `docs-consistency-check/SKILL.md` (post-change documentation review per [`documentation.md`](documentation.md)), `naming-conventions-check/SKILL.md` and `branch-naming-check/SKILL.md` and `commit-message-lint/SKILL.md` (naming/branch/commit conventions per [`coding-conventions.md`](coding-conventions.md)), `folder-structure-sync/SKILL.md` (keeps this file in sync with the actual tree), `test-coverage-check/SKILL.md` (coverage review per [`testing.md`](testing.md)), `i18n-parity-check/SKILL.md` (frontend/backend translation parity) and `refactoring-review/SKILL.md` (post-implementation refactoring review per [`refactoring.md`](refactoring.md)) — plus the GitHub spec-kit skills (`speckit-constitution`, `speckit-specify`, `speckit-plan`, `speckit-tasks`, `speckit-clarify`, `speckit-checklist`, `speckit-analyze`, `speckit-implement`, `speckit-converge`, `speckit-taskstoissues`) that drive the Spec-Driven Development workflow described in `.specify/`.

For the current, detailed implementation status of the backend and frontend (which controllers/services/components exist, what they do), see `backend/readme_backend_en.md` / `readme_backend_de.md` and `frontend/readme_frontend_en.md` / `readme_frontend_de.md` instead of this file — this file only tracks the physical folder layout.
