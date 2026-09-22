# Git Workflow

Binding Git workflow for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 9.

- Remote `origin`: https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer
- Main branch: `main`
- CODEOWNER: `@davidebschke` (see `.github/CODEOWNERS.md`)
- New features/fixes via feature branches and pull requests against `main`.
- Branch numbers (the `<issue-number>` prefix in `.claude/rules/coding-conventions.md`) are assigned sequentially by the user across the whole repository, independent of actual GitHub issue numbers. Before creating a new branch, check `git branch -a` (local and remote) for the highest existing prefix and use the next integer — merged branches are deleted, so also check merged PR numbers in the commit history, not just `git branch -a`. Last number used: **34** (`34-feat-property-creation`); 33 (`33-docs-ai-commit-attribution-rule`) preceded it, and 32 (`32-fix-appointment-map-coordinates`) was used for the coordinate-fix issue, fixing [issue #31](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/issues/31).
- This counter is unrelated to GitHub spec-kit's own `specs/NNN-short-name/` numbering (see `.claude/rules/folder-structure.md`); a spec-kit feature directory number and a branch's `<issue-number>` are not expected to match.
