<!--
Sync Impact Report
Version change: 1.1.0 → 1.2.0 (MINOR: process scope reduced/clarified, no Core Principle redefined or removed)
Modified principles:
  - VI. Structured Git Workflow — clarified that the post-implementation code review's effort level now
    matches the change's own Low/Medium/High assessment instead of always running at High, with a
    security-relevant floor
Added sections: none
Removed sections:
  - Documentation Sync Obligations: the GitHub Wiki clause (the Wiki was removed as a documentation
    surface; .claude/rules/wiki.md and the wiki-sync skill no longer exist)
  - Governance: wiki.md dropped from the list of underlying rule files
Templates requiring updates: none (constitution is a derived summary; dependent templates read it at runtime, not modified here)
Follow-up TODOs: none
-->

# Real-Estate-Maintenance-Optimizer Constitution

## Core Principles

### I. Canonical Documentation Locations
Every fact about the project lives in exactly one canonical file; every other file only links to it, never duplicates it. `CLAUDE.md` stays a short entry point/router; implementation status lives in `backend/readme_backend_*.md` / `frontend/readme_frontend_*.md`; the folder tree lives in `.claude/rules/folder-structure.md`; full feature descriptions live in `README.md`. All of these, plus every file under `.claude/rules/`, are binding.

### II. English Identifiers, Minimal Documentation Comments
Variable, function, and identifier names are always in English. Comments are allowed only as one-sentence JSDoc/TSDoc (frontend) or JavaDoc (backend) documentation comments, in English, and only when the WHY is non-obvious (a hidden constraint, a workaround, a subtle invariant) — never to restate WHAT self-explanatory code already does.

### III. Reusable, Non-Redundant Code
Functions and methods are designed to be reusable; one-off solutions for special cases are not acceptable. Prefer extending an existing reusable unit over hard-coding a special case beside it.

### IV. Minimal-Diff Editing
When changing existing code or documentation, edit only the specific lines/sentences that actually need to change — not the surrounding section, function, or file — unless the task explicitly calls for a rewrite of that larger scope. This governs point fixes and small feature changes; a task that is explicitly a restructuring of existing code instead follows Principle VIII.

### V. Test Coverage as a Gate
A test is required for every new or changed component: frontend components (Vitest + Vue Test Utils, Playwright for e2e) and backend units — services, controllers, repositories (JUnit 5, Mockito, AssertJ, Testcontainers PostgreSQL). If edge cases exist for a component, they are raised with the user before tests are written, not silently assumed.

### VI. Structured Git Workflow
All features and fixes go through feature branches named `<issue-number>-<short-description>`, with issue numbers derived live from `git branch -a` and merged PR numbers before each new branch — never cached as a stale number in a rule file — and pull requests against `main` reviewed by the CODEOWNER (`@davidebschke`). Commit messages follow Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`). Every issue gets an effort estimate attached as one of `Low-Effort` / `Medium-Effort` / `High-Effort` at creation time, and whoever implements it matches that label to Sonnet 5's reasoning effort (low/medium/high, see `.claude/rules/git-workflow.md`); the mandatory post-implementation code review runs at that same effort level, except any Spring Security/JWT/authentication change, which is always reviewed at `High` regardless.

### VII. Fail Fast, Handle Errors Explicitly
Invalid state and required-dependency failures stop the request or the startup, not surface later as a silent bug: missing configuration fails at startup, invalid input is rejected at the boundary with a clear 4xx. Every outgoing external HTTP call (e.g. to Nominatim) carries an explicit timeout, set once on the shared client, never left at the framework default of none. A caught exception or promise rejection is either rethrown with added context or handled as a deliberate, logged, documented degradation of an optional feature — never silently swallowed. See `.claude/rules/error-handling.md`.

### VIII. Refactor Only Through the Characterization Workflow
Restructuring existing code without changing its behaviour (rename, extract, move, inline, split) follows Assess → Characterize → Plan → Execute → Verify: a test capturing today's actual behaviour is added or confirmed before legacy code without coverage is touched, steps run least-risky-first, and each step leaves the code working. A characterization test failing mid-refactor means behaviour changed; that change is either undone or made deliberate and committed separately with an updated test expectation — an existing test is never silently edited to make a refactor "pass". See `.claude/rules/refactoring.md`.

### IX. Linting as a CI Gate
Static analysis is enforced automatically, not left to reviewer attention alone: PMD runs against the backend and ESLint against the frontend as their own CI steps, both failing the build on a violation. Pre-existing debt at the time a linter is introduced may be grandfathered in a documented baseline (e.g. `backend/pmd-exclusions.properties`) so adoption doesn't block on unrelated history, but any new violation — on any file — still fails the build, and the baseline shrinks as flagged code is next touched (Boy Scout Rule, Principle VIII). See `.claude/rules/linting.md`.

## Documentation Sync Obligations

Every content-relevant change (not a pure typo/formatting fix) triggers a review of: `CLAUDE.md`, the affected `backend/readme_*.md` / `frontend/readme_*.md`, `.claude/rules/folder-structure.md`, and `README.md`. Security-relevant changes (Spring Security, JWT) are reviewed with extra care and never merged without tests.

## Technology Constraints

Frontend: Vue.js 3, PrimeVue 4, vue-cal, Vue Router, Axios. Backend: Java 25 (LTS), Spring Boot 3, Spring Security/JJWT, Spring Data JPA, Spring Mail. Database: PostgreSQL (appointments/objects and user/auth) on Supabase, via Spring Data JPA with Flyway-managed schema migrations. AI/Intelligence: LangChain4j. See `CLAUDE.md` Section 2 for the authoritative, up-to-date table.

## Governance

This constitution distills the binding rules already defined under `.claude/rules/` (`coding-conventions.md`, `documentation.md`, `error-handling.md`, `folder-structure.md`, `git-workflow.md`, `linting.md`, `refactoring.md`, `testing.md`) and in `CLAUDE.md`; those files remain the authoritative source. An amendment here is only valid once the corresponding underlying rule file is updated first — this file is a derived summary, not an independent source of truth. Every spec, plan, and PR produced through the spec-kit workflow must comply with these principles; deviations must be justified in the plan's Complexity Tracking section.

**Version**: 1.2.0 | **Ratified**: 2026-09-17 | **Last Amended**: 2026-09-29
