---
name: test-coverage-check
description: Confirm a test exists for every new or changed frontend component and backend service/controller/repository, per .claude/rules/testing.md, and ask about edge cases before writing tests.
---

# Test Coverage Check

Implements the coverage rule required by [`.claude/rules/testing.md`](../../rules/testing.md) and `CLAUDE.md` Section 8: a test must be written for every component (frontend components as well as backend services, controllers, and repositories).

## When to use this skill

After adding or materially changing a frontend component/composable/store or a backend service/controller/repository, before considering the change done.

## Steps

1. **Locate the unit**: Identify the new or changed component/composable/store (frontend) or service/controller/repository (backend).
2. **Check for a test**: Confirm a corresponding test exists — Vitest under `frontend/src/__tests__/` for frontend units, JUnit 5 under `backend/src/test/java/...` for backend units. If e2e-relevant (a full user flow), also check `frontend/e2e/`.
3. **Edge cases**: If the unit has non-trivial edge cases (error branches, empty/boundary inputs, concurrent access, locking), ask the user whether these edge cases should also be covered before writing the tests — do not silently decide scope, per the binding rule.
4. **Write or extend the test**: Add the missing test (or extend an existing one), following the existing test file's structure and naming in the same directory.
5. **Isolation**: For backend repository tests touching file storage, confirm isolation via JUnit `@TempDir` rather than the real `backend/ExampleTerms/` directory.

## Non-goals

- Do not skip asking about edge cases just because the happy path is already covered.
- Do not write tests for third-party library internals (e.g. `vue-cal`, PrimeVue) — only for this repository's own code.
