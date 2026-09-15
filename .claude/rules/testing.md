# Testing

Binding testing conventions for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 8.

- Frontend: Vitest + Vue Test Utils configured (`vitest.config.ts`, `src/__tests__/`), Playwright configured for e2e (`playwright.config.ts`, `e2e/`); both have real coverage (e.g. the calendar and appointment create/read/move/delete flows).
- Backend: `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) via `pom.xml`; the appointment feature is covered (`AppointmentFileRepositoryTest`, `AppointmentServiceTest`, `AppointmentControllerTest`, `StoragePropertiesTest`), isolated from the real `backend/ExampleTerms/` storage via JUnit `@TempDir`. Recommended: Testcontainers for PostgreSQL/MongoDB integration tests once real database persistence replaces the JSON-file storage.

## Component Test Coverage

- A test must be written for every component (frontend components as well as backend units such as services, controllers, and repositories).
- If edge cases exist for a component, ask the user whether these edge cases should also be covered before writing the tests.
