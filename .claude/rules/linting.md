# Linting

Binding linting conventions for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 8.

Linting catches a defined class of mechanical problems (bug patterns, unchecked concurrency issues, some security patterns, unused code); it does not replace the tests in `.claude/rules/testing.md` or human review, in particular a code review still has to judge whether names are clear or a fallback silently hides a real failure.

## Backend — PMD

- `mvn pmd:check` (bound to the `verify` phase, and run as its own CI step in `.github/workflows/backend-tests.yml`) runs `maven-pmd-plugin` against `backend/pom.xml`'s configured rulesets: `bestpractices`, `errorprone`, `multithreading`, `security`.
- SpotBugs was evaluated first and rejected: as of `spotbugs-maven-plugin` 4.9.3.0 it cannot parse Java 25 class files (`Unsupported class file major version 69`), which this project's `<java.version>25</java.version>` produces. PMD analyzes source, not bytecode, so it is unaffected. Re-evaluate SpotBugs once a release supports Java 25 bytecode, since its bug-pattern detectors are more thorough than PMD's.
- `backend/pmd-exclusions.properties` grandfathers the violations that existed when PMD was introduced (one line per fully-qualified class name, listing its excluded rules), so CI stayed green on adoption instead of blocking on unrelated pre-existing debt. It is not a permanent waiver: shrink an entry (or drop the line) when you touch that class again for an unrelated reason, per the Boy Scout Rule in `.claude/rules/refactoring.md`. A genuinely new violation on any class — listed or not — still fails the build.

## Frontend — ESLint

- `npx eslint .` (flat config, `frontend/eslint.config.ts`, combining `eslint-plugin-vue`, `@vue/eslint-config-typescript`, `eslint-plugin-playwright` for `e2e/`, and `@vitest/eslint-plugin` for `src/**/__tests__/`) runs as its own step in `.github/workflows/frontend-tests.yml`, without `--fix` so a violation fails the build instead of being silently rewritten. `npm run lint` (with `--fix`, plus `oxlint`) remains the local, auto-fixing entry point for day-to-day development.
