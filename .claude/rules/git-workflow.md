# Git Workflow

Binding Git workflow for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 9.

- Remote `origin`: https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer
- Main branch: `main`
- CODEOWNER: `@davidebschke` (see `.github/CODEOWNERS.md`)
- New features/fixes via feature branches and pull requests against `main`.
- Pull requests must be integrated via an ordinary merge commit (`git merge` / GitHub's "Merge pull request" button). Squash merges are forbidden, since they collapse the feature branch's individual commits and lose that history on `main`.
- Branch numbers (the `<issue-number>` prefix in `.claude/rules/coding-conventions.md`) are assigned sequentially by the user across the whole repository, independent of actual GitHub issue numbers. Before creating a new branch, check `git branch -a` (local and remote) for the highest existing prefix and use the next integer — merged branches are deleted, so also check merged PR numbers in the commit history, not just `git branch -a`. Last number used: **46** (`46-feat-address-validation-on-property-create`); 45 (`45-docs-simplify-deployment-section`), 44 (`44-ci-add-playwright-e2e-to-frontend-workflow`), 43 (`43-docs-post-implementation-code-review`) and 42 (`42-ci-add-backend-and-frontend-test-workflows`) preceded it.
- This counter is unrelated to GitHub spec-kit's own `specs/NNN-short-name/` numbering (see `.claude/rules/folder-structure.md`); a spec-kit feature directory number and a branch's `<issue-number>` are not expected to match.

## Issue Effort Labels and Model Selection

- Every GitHub issue created in this repository — manually, via an `.github/ISSUE_TEMPLATE/` template, or automatically via the `speckit-taskstoissues` skill — must get exactly one of the repository's existing effort labels attached at creation time (not as a later follow-up edit): `Low-Effort`, `Medium-Effort`, or `High-Effort`.
- Effort guideline for the estimate:
  - **Low-Effort**: change confined to a single file or a small, self-contained unit (e.g. a copy/typo fix, a config tweak, a single new endpoint or component with no new architecture).
  - **Medium-Effort**: one feature slice end-to-end across a handful of files (e.g. a new frontend component plus its store/composable/tests, or a backend endpoint plus service/repository/tests), without a cross-cutting architectural change.
  - **High-Effort**: cross-cutting or architecture-level change, anything touching Spring Security/JWT, or work spanning both frontend and backend plus their readmes/Wiki pages.
- Whoever implements the issue must match the model's reasoning effort to its label: `Low-Effort` → Sonnet 5 at low reasoning effort, `Medium-Effort` → Sonnet 5 at medium reasoning effort, `High-Effort` → Sonnet 5 at high reasoning effort.
- When Claude Code creates the issue itself, it must perform this effort assessment before calling the issue-creation tool, so the label is set in the same call that creates the issue.

## Post-Implementation Code Review

- After finishing any non-trivial implementation task, Claude Code must first commit the result as-is, following the commit-message conventions in `.claude/rules/coding-conventions.md`.
- Only after that commit exists, Claude Code must run the `code-review` skill against it at effort level `high` (Sonnet 5, high reasoning effort) — this review effort level is fixed and independent of the issue's own effort label from the section above, and the review must be extremely critical, surfacing every plausible correctness, security, and robustness issue rather than only the obvious ones.
- Confirmed findings must be fixed and committed separately, as a follow-up commit on top of the original implementation commit, not folded into it or squashed together — so the implementation and the review-driven fixes stay distinguishable in history.
- This applies regardless of whether the change will later be squash-free-merged via PR (squash merges are already forbidden above); the separate fix commit is required even for direct pushes.
