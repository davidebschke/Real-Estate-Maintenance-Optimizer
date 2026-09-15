# Frontend

## Purpose
Contains the complete Vue.js 3 application of the Real-Estate-Maintenance-Optimizer (calendar view, appointment creation, route optimization).

## Technologies
Vue.js 3, PrimeVue 4, Vue.cal (calendar view), Vue Router, Axios, Leaflet (route optimization)

## Folder Structure

### `public/`
Static files copied as-is by the build process, e.g. `favicon.ico`, `index.html`, or static images. These files are not bundled. Assets that are imported or reused in code do not belong here; they belong in an assets folder under `src/`.

### `src/`
Root directory of the actual application source code, split into:

- **`components/`** – Reusable UI building blocks (e.g. buttons, cards, form fields, calendar widgets) used across multiple views. One component per file, filename in PascalCase (e.g. `AppointmentFormDialog.vue`). No business logic or direct API calls inside components; use `composables/` or `services/` instead. App-wide layout building blocks live in `components/layout/` (e.g. `AppHeader.vue`, `AppFooter.vue`, `NavigationMenu.vue`), calendar building blocks live in `components/calendar/` (`AppCalendar.vue` wraps `vue-cal`, alongside `CalendarToolbar.vue`, `CalendarDayHeader.vue`, `CalendarEventCard.vue`), and appointment create/detail building blocks live in `components/appointments/` (`AppointmentFormDialog.vue`, `AppointmentDetailDrawer.vue`, `AppointmentPropertySelect.vue`, `AppointmentMaterialInput.vue`, `AppointmentAiSuggestionBanner.vue`).
- **`views/`** – Pages / route targets of the application, e.g. appointment overview, appointment creation, material planning. One view per route, filename usually suffixed with `View` (e.g. `CalendarView.vue`). Views orchestrate components and should contain as little own logic as possible.
- **`router/`** – Central configuration of Vue Router: route definitions, mapping to views, navigation guards, and lazy-loading. One central `index.ts`/`index.js` as the entry point, routes with descriptive `name` attributes.
- **`services/`** – Axios-based API clients for communicating with the Spring Boot backend. One service per business domain (e.g. `versionService.ts`, `appointmentService.ts`), plain modules with one exported async function per operation.
- **`stores/`** – Global state management, e.g. the loaded appointments and which create/detail overlay is open (`stores/appointments.ts`, the first real use of Pinia in this app — needed because the header's "+Termin" button and the calendar overlay must act on the same state, which a plain composable can't share across mount points). No direct API access inside a store, use `services/` instead.
- **`composables/`** – Reusable Vue Composition functions encapsulating reactive logic needed across multiple components or views. Naming convention `useXxx` (e.g. `useCalendarNavigation`, `useCalendarAppointments`, `useAppointmentDeleteConfirmation`), contains pure logic without UI rendering.
- **`data/`** – Hardcoded example/reference data standing in for a not-yet-built backend (e.g. `exampleProperties.ts`, used by the appointment form's "Objekt" dropdown until a real properties API exists).
- **`types/`** – Hand-written TypeScript types (e.g. `appointment.ts`) plus ambient shims for untyped packages (e.g. `vue-cal-shims.d.ts` for `vue-cal`, which ships no type declarations of its own).
- **`utils/`** – Small, reusable, framework-agnostic helper functions (e.g. locale-aware date formatting, `appointmentSchedulingOptions.ts`'s day/time/duration/recurrence option generators) that are not tied to Vue's reactivity system and therefore don't belong in `composables/`.
- **`i18n/`** – Central `vue-i18n` setup; creates and exports the app's i18n instance, merging all locale message bundles from `locales/`.
- **`locales/`** – Translation message files for static UI text, split into `de/` (active default locale) and `en/` (fallback locale), one JSON file per feature namespace (e.g. `header.json`, `footer.json`, `appointments.json`). See `CLAUDE.md`, Coding Conventions, for the rule that all other identifiers stay in English.
- **`styles/`** – Component styles extracted from `.vue` files, referenced via `<style scoped src="...">`, mirroring the `components/` folder structure (e.g. `styles/layout/` for `components/layout/`, `styles/calendar/` for `components/calendar/`, `styles/appointments/` for `components/appointments/`).
- **`__tests__/`** – Vitest unit tests (component, composable, service and store tests), one spec file per source file, plus a shared `setup.ts` (polyfills `window.matchMedia`, which jsdom lacks but PrimeVue's overlay components call on mount).

### `e2e/`
Playwright end-to-end tests (e.g. `navigation.spec.ts`, `calendar.spec.ts`, `appointments.spec.ts`), run via `npm run test:e2e`. The appointment flows need the backend running locally (`mvn spring-boot:run` in `backend/`), since Playwright's `webServer` config only starts the frontend dev server.

### `package.json`
Dependencies and build/start scripts.

## Conventions
See `CLAUDE.md` in the repository root, section "Coding Conventions": variable names in English, comments only as JSDoc/TSDoc (one sentence, English), functions always designed to be reusable.
