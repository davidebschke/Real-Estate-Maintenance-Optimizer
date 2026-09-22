# Git Workflow

Binding Git workflow for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 9.

- Remote `origin`: https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer
- Main branch: `main`
- CODEOWNER: `@davidebschke` (see `.github/CODEOWNERS.md`)
- New features/fixes via feature branches and pull requests against `main`.
- Branch numbers (the `<issue-number>` prefix in `.claude/rules/coding-conventions.md`) are assigned sequentially by the user across the whole repository, independent of actual GitHub issue numbers. Before creating a new branch, check `git branch -a` (local and remote) for the highest existing prefix and use the next integer — merged branches are deleted, so also check merged PR numbers in the commit history, not just `git branch -a`. Last number used: **42** (`42-ci-add-backend-and-frontend-test-workflows`); 41 (`41-chore-fixed-backend-jar-name`), 40 (`40-fix-leaflet-marker-icon-paths`) and 39 (`39-chore-set-html-title-to-remo`) preceded it.
- This counter is unrelated to GitHub spec-kit's own `specs/NNN-short-name/` numbering (see `.claude/rules/folder-structure.md`); a spec-kit feature directory number and a branch's `<issue-number>` are not expected to match.

## Issue Effort Labels and Model Selection

- Every GitHub issue created in this repository — manually, via an `.github/ISSUE_TEMPLATE/` template, or automatically via the `speckit-taskstoissues` skill — must get exactly one of the repository's existing effort labels attached at creation time (not as a later follow-up edit): `Low-Effort`, `Medium-Effort`, or `High-Effort`.
- Effort guideline for the estimate:
  - **Low-Effort**: change confined to a single file or a small, self-contained unit (e.g. a copy/typo fix, a config tweak, a single new endpoint or component with no new architecture).
  - **Medium-Effort**: one feature slice end-to-end across a handful of files (e.g. a new frontend component plus its store/composable/tests, or a backend endpoint plus service/repository/tests), without a cross-cutting architectural change.
  - **High-Effort**: cross-cutting or architecture-level change, anything touching Spring Security/JWT, or work spanning both frontend and backend plus their readmes/Wiki pages.
- Whoever implements the issue must match the model's reasoning effort to its label: `Low-Effort` → Sonnet 5 at low reasoning effort, `Medium-Effort` → Sonnet 5 at medium reasoning effort, `High-Effort` → Sonnet 5 at high reasoning effort.
- When Claude Code creates the issue itself, it must perform this effort assessment before calling the issue-creation tool, so the label is set in the same call that creates the issue.
