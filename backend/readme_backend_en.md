# Backend

## Purpose
Contains the complete Spring Boot application of the Real-Estate-Maintenance-Optimizer (REST API, appointment logic, route optimization, AI-supported duration estimation).

## Technologies
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (user/auth), MongoDB (appointments & objects), LangChain4j (AI integration)

## Folder Structure

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Root directory of the actual application source code, split into:

Already present:

- **`controller/`** – REST endpoints of the application, e.g. appointment, material, and route management. One controller per business domain, no business logic inside the controller; use `service/` instead. Currently contains:
  - `VersionController` — `GET /api/version`, reporting the version currently packaged from `pom.xml` via Spring Boot's `BuildProperties` (generated at build time through the `spring-boot-maven-plugin` `build-info` goal, bound to the `generate-resources` phase so it runs before compilation/tests).
  - `AppointmentController` — `GET/POST /api/appointments`, `GET /api/appointments/{id}`, `PATCH /api/appointments/{id}/schedule` (move), `PATCH /api/appointments/{id}/complete` (mark as completed, sets `actualEnd` to the current time), `PATCH /api/appointments/{id}/reopen` (revert the completed state), and `DELETE /api/appointments/{id}?scope=single|series` (the latter also removing every not-yet-past occurrence of a recurring series).
  - `PropertyController` — `GET /api/properties`, `GET /api/properties/{id}`; read-only for now (see "Objects Overview" below).
  - `GeocodingController` — `GET /api/geocode?address=...`, proxied address resolution for the appointment map (see "Address Geocoding" below).
  - `GlobalExceptionHandler` — maps domain exceptions (`exception/`) to localized `404`/`409`/`400` responses.
- **`dto/`** – Data Transfer Objects for exchanging data between backend and frontend, decoupled from the internal entities. Currently contains: `VersionResponse`, `ErrorResponse`, the appointment request/response DTOs (`CreateAppointmentRequest`, `MoveAppointmentRequest`, `AppointmentResponse` including `actualEnd`/`completed`, `HistoryEntryResponse`), `PropertyResponse` (including `latitude`/`longitude`), and `GeocodingResponse` (`latitude`/`longitude`, both `null` when no match was found).
- **`config/`** – General application configuration. Currently contains: `CorsConfig` (CORS for the frontend dev server, origin configured via `remo.frontend.base-url`, allowing `GET`/`POST`/`PATCH`/`DELETE` on `/api/**`), `LocalizationConfig` (resolves the request locale from the `Accept-Language` header, falling back to English when no supported locale is requested), `StorageProperties` (the appointment JSON-file storage directory, `remo.storage.directory`), `PropertyStorageProperties` (the property JSON-file storage directory, `remo.storage.properties.directory`), and `RestClientConfig` (provides the `RestClient.Builder` used by `GeocodingService`).
- **`service/`** – Business logic. Currently contains `AppointmentService`: validation, recurrence materialization (a recurring appointment eagerly creates a fixed horizon of 12 future occurrences sharing one `seriesId`, each independently movable/lockable/deletable), move with a locked-appointment guard, marking as completed (`complete`, sets `actualEnd` to `LocalDateTime.now()`) or reverting that (`reopen`, clears `actualEnd`), single-vs-series delete, and localizing each appointment's history log on read; `PropertyService`: read-only listing (sorted by name) and lookup by id; and `GeocodingService` (see "Address Geocoding" below).
- **`repository/`** – Data access. Currently contains `AppointmentFileRepository` and `PropertyFileRepository`, **temporary** JSON-file-backed repositories (one file per record) standing in for the MongoDB repositories described in `CLAUDE.md` §2.3 until a real database is wired up.
- **`entity/`** – Domain model records. Currently contains `Appointment` (including `actualEnd`, a derived `completed()` state, and wither methods `withSchedule`/`withActualEnd`), `HistoryEntry` and `HistoryEventType` (`CREATED`, `MOVED`, `COMPLETED`, `REOPENED`), and `Property` (`id`, `name`, `address`, `icon`, `latitude`, `longitude` — the coordinates are `null` while an address could not be geocoded) — plain Java records for now, not JPA/MongoDB-mapped, since there is no database yet.
- **`exception/`** – Domain exceptions (`AppointmentNotFoundException`, `AppointmentLockedException`, `InvalidRecurrenceException`, `PropertyNotFoundException`) translated into HTTP responses by `GlobalExceptionHandler`.

### Objects Overview ("Objekte Auflisten")
`PropertyController`/`PropertyService`/`PropertyFileRepository` expose the same properties selectable when creating an appointment (`AppointmentPropertySelect.vue` on the frontend) as a read-only list for the "Immobilien" page. Only `GET` operations exist so far — creating/editing/deleting properties is not yet implemented. Open/completed appointment counts and each property's next appointment are not stored on `Property` itself; the frontend derives them from the already-loaded appointments (filtered by `propertyId`), so no aggregation endpoint was needed.

### Address Geocoding
`GeocodingService` resolves addresses to coordinates via the OpenStreetMap Nominatim API, for the appointment map on the frontend overview page (see `frontend/readme_frontend_en.md`). The call runs **server-side** instead of directly from the browser because Nominatim's abuse detection blocks requests lacking a real, application-identifying `User-Agent` header — one a browser can never set itself for security reasons. `RestClientConfig` provides the `RestClient.Builder` for this; `GeocodingService` sets a project-identifying `User-Agent` (pointing at the GitHub repository, no personal data), caches results per address in memory, and throttles new requests to one per second (Nominatim's usage policy). If the full address has no match, the service automatically falls back to a structured search by the German postal code it contains. The 23 example properties in `ExampleObjects/` are already seeded with coordinates resolved this way; the frontend now only calls `GET /api/geocode` as a fallback for properties without stored coordinates.

Planned, not yet implemented (see `.claude/rules/folder-structure.md`, backend package structure proposal):

- **`security/`** – Spring Security configuration and JWT handling (authentication, authorization, token creation/validation).
- **`ai/`** – LangChain4j integration for AI-supported estimation of appointment duration based on similar previous appointments.
- Replacing `AppointmentFileRepository`'s JSON files with real PostgreSQL (user/auth) and MongoDB (appointments & objects) persistence per `CLAUDE.md` §2.3.

### `src/main/resources/`
Application configuration files: `application.yml` (Spring Boot configuration, incl. `spring.messages.basename`, `remo.frontend.base-url` and `remo.storage.directory`) and `locales/` (German/English translations for dynamic, server-generated text via Spring's `MessageSource`: `messages.properties`, `messages_de.properties`, `messages_en.properties`, resolved via `LocalizationConfig`; the base `messages.properties` bundle holds the English texts, matching the English fallback locale). Database migrations will be added here once persistence is implemented.

### `ExampleTerms/`
Gitignored, created automatically at runtime under the backend working directory: one JSON file per appointment, the temporary storage described under `repository/` above.

### `ExampleObjects/`
Checked into the repository (unlike the gitignored `ExampleTerms/`), seeded with one JSON file per property (matching the objects selectable when creating an appointment, e.g. "Wohnanlage Sonnenhof"); the temporary storage backing `PropertyFileRepository`, configured via `remo.storage.properties.directory`. Committed rather than gitignored because there is no create endpoint yet to repopulate it after a fresh checkout.

### `src/test/java/...`
JUnit tests, structure mirrors `src/main/java/...`. Covers the version/localization/CORS config and the full appointment, property, and geocoding features (repository, service, controller) — see `CLAUDE.md`, section "Tests".

### `pom.xml`
Dependencies and build configuration (Maven). Holds the single source of truth for the application version (`<version>`), surfaced to the frontend via `GET /api/version`.

## Conventions
See `CLAUDE.md` in the repository root, section "Coding Conventions": variable names in English, comments only as JavaDoc (one sentence, English), functions/methods always designed to be reusable.
