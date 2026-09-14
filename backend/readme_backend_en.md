# Backend

## Purpose
Contains the complete Spring Boot application of the Real-Estate-Maintenance-Optimizer (REST API, appointment logic, route optimization, AI-supported duration estimation).

## Technologies
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (user/auth), MongoDB (appointments & objects), LangChain4j (AI integration)

## Folder Structure

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Root directory of the actual application source code, split into:

Already present:

- **`controller/`** – REST endpoints of the application, e.g. appointment, material, and route management. One controller per business domain, no business logic inside the controller; use `service/` instead. Currently contains: `VersionController` (`GET /api/version`).
- **`dto/`** – Data Transfer Objects for exchanging data between backend and frontend, decoupled from the internal entities. Currently contains: `VersionResponse`.
- **`config/`** – General application configuration. Currently contains: `CorsConfig` (CORS for the frontend dev server, origin configured via `remo.frontend.base-url`) and `LocalizationConfig` (locale resolution from the `Accept-Language` header for dynamic translations).

Planned, not yet implemented (see `CLAUDE.md`, section "Folder Structure", backend package structure proposal):

- **`service/`** – Business logic, e.g. appointment scheduling, route optimization, duration estimation. One service per business domain, orchestrates `repository/` and `ai/`.
- **`repository/`** – Spring Data JPA repositories for accessing PostgreSQL (user/auth) as well as corresponding repositories for MongoDB (appointments & objects).
- **`entity/`** – JPA entities and MongoDB documents representing the application's data models (e.g. appointment, object, user).
- **`security/`** – Spring Security configuration and JWT handling (authentication, authorization, token creation/validation).
- **`ai/`** – LangChain4j integration for AI-supported estimation of appointment duration based on similar previous appointments.

### `src/main/resources/`
Application configuration files: `application.yml` (Spring Boot configuration, incl. `spring.messages.basename` and `remo.frontend.base-url`) and `locales/` (German/English translations for dynamic, server-generated text via Spring's `MessageSource`: `messages.properties`, `messages_de.properties`, `messages_en.properties`). Database migrations will be added here once persistence is implemented.

### `src/test/java/...`
JUnit tests, structure mirrors `src/main/java/...`. No tests written yet (see `CLAUDE.md`, section "Tests").

### `pom.xml`
Dependencies and build configuration (Maven). Holds the single source of truth for the application version (`<version>`), surfaced to the frontend via `GET /api/version`.

## Conventions
See `CLAUDE.md` in the repository root, section "Coding Conventions": variable names in English, comments only as JavaDoc (one sentence, English), functions/methods always designed to be reusable.
