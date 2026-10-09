# Testing

Binding testing conventions for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 8.

- Frontend: Vitest + Vue Test Utils configured (`vitest.config.ts`, `src/__tests__/`), Playwright configured for e2e (`playwright.config.ts`, `e2e/`); both have real coverage (e.g. the calendar and appointment create/read/move/delete flows).
- Backend: `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) via `pom.xml`; the appointment and property features are covered (`AppointmentRepositoryTest`, `PropertyRepositoryTest`, `ApartmentRepositoryTest` via `@DataJpaTest`, `AppointmentServiceTest`, `PropertyServiceTest`, `ApartmentServiceTest`, `AppointmentControllerTest`, `PropertyControllerTest`, `ApartmentControllerTest`). Every Spring test context runs against a throwaway PostgreSQL 17 container (Testcontainers, imported via `TestcontainersConfiguration`), so `mvn test` requires a running Docker daemon; tests sharing a context empty the database in `@BeforeEach` (deleting all `users` cascades to their properties and appointments). Authentication is covered by `security/` unit tests, `SecurityConfigTest` (whole filter chain incl. a real CSRF double submit), `AuthControllerTest`, `AuthServiceTest` and the demo account tests; data endpoints are tested authenticated via `TestAccounts.mockMvcAs(...)` plus account-isolation cases. Never use spring-security-test's `csrf()` post-processor: it permanently replaces the CSRF token repository of the cached, shared context — use `TestAccounts.withCsrfToken()` instead, and `TestAccounts.uniqueClientAddress()` for anything hitting a rate limit. `src/test/resources/application.properties` holds the test-only JWT secret.
- E2E: the Playwright `setup` project logs in once as `E2E_USERNAME` (default `account_default`) with the required `E2E_PASSWORD` and stores the session for every browser project; e2e tests must never log out that shared account (it would revoke every stored session).

## Component Test Coverage

- A test must be written for every component (frontend components as well as backend units such as services, controllers, and repositories).
- If edge cases exist for a component, ask the user whether these edge cases should also be covered before writing the tests.
