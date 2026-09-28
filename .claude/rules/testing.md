# Testing

Binding testing conventions for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 8.

- Frontend: Vitest + Vue Test Utils configured (`vitest.config.ts`, `src/__tests__/`), Playwright configured for e2e (`playwright.config.ts`, `e2e/`); both have real coverage (e.g. the calendar and appointment create/read/move/delete flows).
- Backend: `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) via `pom.xml`; the appointment and property features are covered (`AppointmentRepositoryTest`, `PropertyRepositoryTest` via `@DataJpaTest`, `AppointmentServiceTest`, `PropertyServiceTest`, `AppointmentControllerTest`, `PropertyControllerTest`). Every Spring test context runs against a throwaway PostgreSQL 17 container (Testcontainers, imported via `TestcontainersConfiguration`), so `mvn test` requires a running Docker daemon; tests sharing a context empty the database in `@BeforeEach`.

## Component Test Coverage

- A test must be written for every component (frontend components as well as backend units such as services, controllers, and repositories).
- If edge cases exist for a component, ask the user whether these edge cases should also be covered before writing the tests.
