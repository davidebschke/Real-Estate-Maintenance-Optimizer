# Backend

## Purpose
Contains the complete Spring Boot application of the Real-Estate-Maintenance-Optimizer (REST API, appointment logic, route optimization, AI-supported duration estimation).

## Technologies
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (user/auth), MongoDB (appointments & objects), LangChain4j (AI integration)

## Folder Structure

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Root directory of the actual application source code, split into:

Already present:

- **`controller/`** – REST endpoints of the application, e.g. appointment, material, and route management. One controller per business domain, no business logic inside the controller; use `service/` instead. Currently contains: `VersionController` (`GET /api/version`), `AppointmentController` (`/api/appointments` create/read/move/delete) and `GlobalExceptionHandler` (maps domain exceptions to localized HTTP responses).
- **`dto/`** – Data Transfer Objects for exchanging data between backend and frontend, decoupled from the internal entities. Currently contains: `VersionResponse`, `ErrorResponse`, and the appointment request/response DTOs (`CreateAppointmentRequest`, `MoveAppointmentRequest`, `AppointmentResponse`, `HistoryEntryResponse`).
- **`config/`** – General application configuration. Currently contains: `CorsConfig` (CORS for the frontend dev server, origin configured via `remo.frontend.base-url`, allowing `GET`/`POST`/`PATCH`/`DELETE`), `LocalizationConfig` (locale resolution from the `Accept-Language` header for dynamic translations), and `StorageProperties` (the appointment JSON-file storage directory, `remo.storage.directory`).
- **`service/`** – Business logic. Currently contains `AppointmentService` (create incl. recurring-series materialization, move with a locked-appointment guard, single-vs-series delete).
- **`repository/`** – Data access. Currently contains `AppointmentFileRepository`, a **temporary** JSON-file-backed repository (one file per appointment) standing in for the MongoDB repository described in `CLAUDE.md` §2.3 until a real database is wired up.
- **`entity/`** – Domain model records. Currently contains `Appointment`, `HistoryEntry` and `HistoryEventType` — plain Java records for now, not JPA/MongoDB-mapped, since there is no database yet.
- **`exception/`** – Domain exceptions (`AppointmentNotFoundException`, `AppointmentLockedException`, `InvalidRecurrenceException`) translated into HTTP responses by `GlobalExceptionHandler`.

Planned, not yet implemented (see `CLAUDE.md`, section "Folder Structure", backend package structure proposal):

- **`security/`** – Spring Security configuration and JWT handling (authentication, authorization, token creation/validation).
- **`ai/`** – LangChain4j integration for AI-supported estimation of appointment duration based on similar previous appointments.
- Replacing `AppointmentFileRepository`'s JSON files with real PostgreSQL (user/auth) and MongoDB (appointments & objects) persistence per `CLAUDE.md` §2.3.

### `src/main/resources/`
Application configuration files: `application.yml` (Spring Boot configuration, incl. `spring.messages.basename`, `remo.frontend.base-url` and `remo.storage.directory`) and `locales/` (German/English translations for dynamic, server-generated text via Spring's `MessageSource`: `messages.properties`, `messages_de.properties`, `messages_en.properties`). Database migrations will be added here once persistence is implemented.

### `ExampleTerms/`
Gitignored, created automatically at runtime under the backend working directory: one JSON file per appointment, the temporary storage described under `repository/` above.

### `src/test/java/...`
JUnit tests, structure mirrors `src/main/java/...`. Covers the version/localization/CORS config and the full appointment feature (repository, service, controller) — see `CLAUDE.md`, section "Tests".

### `pom.xml`
Dependencies and build configuration (Maven). Holds the single source of truth for the application version (`<version>`), surfaced to the frontend via `GET /api/version`.

## Conventions
See `CLAUDE.md` in the repository root, section "Coding Conventions": variable names in English, comments only as JavaDoc (one sentence, English), functions/methods always designed to be reusable.
