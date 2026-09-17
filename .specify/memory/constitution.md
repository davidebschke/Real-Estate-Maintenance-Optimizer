# Real-Estate-Maintenance-Optimizer Constitution

## Core Principles

### I. Canonical Documentation Locations
Every fact about the project lives in exactly one canonical file; every other file only links to it, never duplicates it. `CLAUDE.md` stays a short entry point/router; implementation status lives in `backend/readme_backend_*.md` / `frontend/readme_frontend_*.md`; the folder tree lives in `.claude/rules/folder-structure.md`; full feature descriptions live in `README.md`. All of these, plus every file under `.claude/rules/`, are binding.

### II. English Identifiers, Minimal Documentation Comments
Variable, function, and identifier names are always in English. Comments are allowed only as one-sentence JSDoc/TSDoc (frontend) or JavaDoc (backend) documentation comments, in English, and only when the WHY is non-obvious (a hidden constraint, a workaround, a subtle invariant) — never to restate WHAT self-explanatory code already does.

### III. Reusable, Non-Redundant Code
Functions and methods are designed to be reusable; one-off solutions for special cases are not acceptable. Prefer extending an existing reusable unit over hard-coding a special case beside it.

### IV. Minimal-Diff Editing
When changing existing code or documentation, edit only the specific lines/sentences that actually need to change — not the surrounding section, function, or file — unless the task explicitly calls for a rewrite of that larger scope.

### V. Test Coverage as a Gate
A test is required for every new or changed component: frontend components (Vitest + Vue Test Utils, Playwright for e2e) and backend units — services, controllers, repositories (JUnit 5, Mockito, AssertJ). If edge cases exist for a component, they are raised with the user before tests are written, not silently assumed.

### VI. Structured Git Workflow
All features and fixes go through feature branches named `<issue-number>-<short-description>`, with issue numbers assigned sequentially across the repository (tracked in `.claude/rules/git-workflow.md`), and pull requests against `main` reviewed by the CODEOWNER (`@davidebschke`). Commit messages follow Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`).

## Documentation Sync Obligations

Every content-relevant change (not a pure typo/formatting fix) triggers a review of: `CLAUDE.md`, the affected `backend/readme_*.md` / `frontend/readme_*.md`, `.claude/rules/folder-structure.md`, `README.md`, and the corresponding bilingual GitHub Wiki page(s) per `.claude/rules/wiki.md`. Security-relevant changes (Spring Security, JWT) are reviewed with extra care and never merged without tests.

## Technology Constraints

Frontend: Vue.js 3, PrimeVue 4, vue-cal, Vue Router, Axios. Backend: Java 25 (LTS), Spring Boot 3, Spring Security/JJWT, Spring Data JPA, Spring Mail. Databases: PostgreSQL (user/auth, via Spring Data JPA) and MongoDB (appointments/objects) — currently a temporary JSON-file storage prototype stands in for persistence where noted in the backend readme, expected to be replaced by real database persistence without changing the REST contract. AI/Intelligence: LangChain4j. See `CLAUDE.md` Section 2 for the authoritative, up-to-date table.

## Governance

This constitution distills the binding rules already defined under `.claude/rules/` (`coding-conventions.md`, `documentation.md`, `folder-structure.md`, `git-workflow.md`, `testing.md`, `wiki.md`) and in `CLAUDE.md`; those files remain the authoritative source. An amendment here is only valid once the corresponding underlying rule file is updated first — this file is a derived summary, not an independent source of truth. Every spec, plan, and PR produced through the spec-kit workflow must comply with these principles; deviations must be justified in the plan's Complexity Tracking section.

**Version**: 1.0.0 | **Ratified**: 2026-09-17 | **Last Amended**: 2026-09-17
