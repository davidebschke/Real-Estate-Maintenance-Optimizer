# Git Workflow

Binding Git workflow for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 9.

- Remote `origin`: https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer
- Main branch: `main`
- CODEOWNER: `@davidebschke` (see `.github/CODEOWNERS.md`)
- New features/fixes via feature branches and pull requests against `main`.
- Pull requests must be integrated via an ordinary merge commit (`git merge` / GitHub's "Merge pull request" button). Squash merges are forbidden, since they collapse the feature branch's individual commits and lose that history on `main`.
- Branch numbers (the `<issue-number>` prefix in `.claude/rules/coding-conventions.md`) are assigned sequentially by the user across the whole repository, independent of actual GitHub issue numbers. Always derive the next number live before creating a branch — never cache a "last number used" value in this file, since it silently goes stale: run `git branch -a` (local and remote) for the highest existing prefix, and also check merged PR numbers (e.g. `gh pr list --state merged --limit 30 --json number,headRefName`), since merged branches are deleted and won't show up in `git branch -a` alone. Use the next integer above the highest one found either way.
- This counter is unrelated to GitHub spec-kit's own `specs/NNN-short-name/` numbering (see `.claude/rules/folder-structure.md`); a spec-kit feature directory number and a branch's `<issue-number>` are not expected to match.

## Issue Effort Labels and Model Selection

- Every GitHub issue created in this repository — manually, via an `.github/ISSUE_TEMPLATE/` template, or automatically via the `speckit-taskstoissues` skill — must get exactly one of the repository's existing effort labels attached at creation time (not as a later follow-up edit): `Low-Effort`, `Medium-Effort`, or `High-Effort`.
- Effort guideline for the estimate:
  - **Low-Effort**: change confined to a single file or a small, self-contained unit (e.g. a copy/typo fix, a config tweak, a single new endpoint or component with no new architecture).
  - **Medium-Effort**: one feature slice end-to-end across a handful of files (e.g. a new frontend component plus its store/composable/tests, or a backend endpoint plus service/repository/tests), without a cross-cutting architectural change.
  - **High-Effort**: cross-cutting or architecture-level change, anything touching Spring Security/JWT, or work spanning both frontend and backend plus their readmes.
- Whoever implements the issue must match the model's reasoning effort to its label: `Low-Effort` → Sonnet 5 at low reasoning effort, `Medium-Effort` → Sonnet 5 at medium reasoning effort, `High-Effort` → Sonnet 5 at high reasoning effort.
- When Claude Code creates the issue itself, it must perform this effort assessment before calling the issue-creation tool, so the label is set in the same call that creates the issue.

## Pull Request Before the Reviews

- After the implementation commit of a non-trivial change, Claude Code pushes the branch and opens the pull request against `main` **before** any review starts, so that every review step is visible on the PR. The description states the problem, the changes, how it was verified and the effort label.
- The order after the implementation commit is fixed: refactoring review → PR comment with its outcome → `refactor:` fix commits, pushed → code review (`ralf-reviewer`) → PR comment with its outcome → review-fix commit, pushed.
- Each review leaves exactly one PR comment (`gh pr comment`), written by the main session because both reviewers are read-only. It is posted **before** that review's fixes are applied, lists every finding (file/line, one-sentence summary, concrete scenario) and for each whether it will be applied, rejected (with the reason) or left as a follow-up, and states explicitly when nothing was found.
- A review skipped as trivial is stated in a PR comment as well, with the reason, so the PR shows that the step was considered.

## Post-Implementation Refactoring Review

- After the implementation commit and the opened pull request (see "Pull Request Before the Reviews" above) and **before** the code review, Claude Code must run the `refactoring-review` skill over the files the branch added or changed, following the checklist and scope rules in `.claude/rules/refactoring.md` ("Post-Implementation Refactoring Review"), at the same effort level as the code review.
- The review's outcome is posted as a PR comment first; applied moves are then committed as separate `refactor:` commits on top of the implementation commit, never folded into it, and pushed; findings that need a behaviour change or an architectural decision are reported as follow-ups instead of being applied.
- Skipping the review is only acceptable for a trivial change (a typo, a copy fix, a config value); say so explicitly in that case.

## Post-Implementation Code Review

- After finishing any non-trivial implementation task, Claude Code must first commit the result as-is, following the commit-message conventions in `.claude/rules/coding-conventions.md`, and open the pull request (see "Pull Request Before the Reviews" above).
- Only after that commit exists (and, for a non-trivial change, after the refactoring review above has finished), Claude Code must run the `code-review` skill against it together with any `refactor:` commits, at a reasoning effort level matching the change's own effort assessment (the same `Low-Effort`/`Medium-Effort`/`High-Effort` scale from the section above — reuse the issue's label if there is one, otherwise assess it by the same criteria). Any change touching Spring Security/JWT/authentication is always reviewed at `high` regardless of that assessment, as a floor. Whatever the level, the review must be genuinely critical, surfacing every plausible correctness, security, and robustness issue for the scope of that level, not just the obvious ones. The review's outcome is posted as a PR comment before any fix is applied.
- Confirmed findings must be fixed and committed separately, as a follow-up commit on top of the original implementation commit, not folded into it or squashed together — so the implementation and the review-driven fixes stay distinguishable in history; every fix commit is pushed to the PR branch.
- This applies regardless of whether the change will later be squash-free-merged via PR (squash merges are already forbidden above); the separate fix commit is required even for direct pushes.
