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

- **`components/`** – Reusable UI building blocks (e.g. buttons, cards, form fields, calendar widgets) used across multiple views. One component per file, filename in PascalCase (e.g. `AppointmentCard.vue`). No business logic or direct API calls inside components; use `composables/` or `services/` instead.
- **`views/`** – Pages / route targets of the application, e.g. appointment overview, appointment creation, material planning. One view per route, filename usually suffixed with `View` (e.g. `AppointmentOverviewView.vue`). Views orchestrate components and should contain as little own logic as possible.
- **`router/`** – Central configuration of Vue Router: route definitions, mapping to views, navigation guards, and lazy-loading. One central `index.ts`/`index.js` as the entry point, routes with descriptive `name` attributes.
- **`services/`** – Axios-based API clients for communicating with the Spring Boot backend. One service per business domain (e.g. `appointmentService`, `materialService`), central Axios instance with base URL and interceptors (e.g. for JWT).
- **`stores/`** – Global state management, e.g. the logged-in user, loaded appointments, current filter settings. One store per business area (e.g. `authStore`, `appointmentStore`); no direct API access inside a store, use `services/` instead.
- **`composables/`** – Reusable Vue Composition functions encapsulating reactive logic needed across multiple components or views. Naming convention `useXxx` (e.g. `useAppointmentDuration`), contains pure logic without UI rendering.

### `package.json`
Dependencies and build/start scripts.

## Conventions
See `CLAUDE.md` in the repository root, section "Coding Conventions": variable names in English, comments only as JSDoc/TSDoc (one sentence, English), functions always designed to be reusable.
