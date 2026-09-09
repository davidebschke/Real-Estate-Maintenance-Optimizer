# Backend

## Purpose
Contains the complete Spring Boot application of the Real-Estate-Maintenance-Optimizer (REST API, appointment logic, route optimization, AI-supported duration estimation).

## Technologies
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (user/auth), MongoDB (appointments & objects), LangChain4j (AI integration)

## Folder Structure

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Root directory of the actual application source code, split into:

- **`controller/`** – REST endpoints of the application, e.g. appointment, material, and route management. One controller per business domain, no business logic inside the controller; use `service/` instead.
- **`service/`** – Business logic, e.g. appointment scheduling, route optimization, duration estimation. One service per business domain, orchestrates `repository/` and `ai/`.
- **`repository/`** – Spring Data JPA repositories for accessing PostgreSQL (user/auth) as well as corresponding repositories for MongoDB (appointments & objects).
- **`entity/`** – JPA entities and MongoDB documents representing the application's data models (e.g. appointment, object, user).
- **`dto/`** – Data Transfer Objects for exchanging data between backend and frontend, decoupled from the internal entities.
- **`security/`** – Spring Security configuration and JWT handling (authentication, authorization, token creation/validation).
- **`ai/`** – LangChain4j integration for AI-supported estimation of appointment duration based on similar previous appointments.
- **`config/`** – General application configuration (e.g. CORS, bean definitions, mail configuration).

### `src/main/resources/`
Application configuration files, e.g. `application.yml` as well as database migrations.

### `src/test/java/...`
JUnit tests, structure mirrors `src/main/java/...`.

### `pom.xml` / `build.gradle`
Dependencies and build scripts (not yet created).

## Conventions
See `CLAUDE.md` in the repository root, section "Coding Conventions": variable names in English, comments only as JavaDoc (one sentence, English), functions/methods always designed to be reusable.
