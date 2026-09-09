# Testing

Binding testing conventions for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 8.

Tooling is set up, actual test coverage is not yet present:
- Frontend: Vitest + Vue Test Utils configured (`vitest.config.ts`, `src/__tests__/`), Playwright configured for e2e (`playwright.config.ts`, `e2e/`).
- Backend: `spring-boot-starter-test` (JUnit 5, Mockito) available via `pom.xml`, no tests written yet. Recommended: Testcontainers for PostgreSQL/MongoDB integration tests once persistence is implemented.
