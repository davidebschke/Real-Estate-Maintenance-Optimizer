---
name: branch-naming-check
description: Validate that a branch about to be pushed or opened as a PR follows the <issue-number>-<short-description> scheme from .claude/rules/coding-conventions.md.
---

# Branch Naming Check

Implements the branch naming rule required by [`.claude/rules/coding-conventions.md`](../../rules/coding-conventions.md) and `CLAUDE.md` Section 5: branch names follow the scheme `<issue-number>-<short-description>` (e.g. `4-chore-refaktor-folder-structure-from-frontend`).

## When to use this skill

Before creating a new branch, and before pushing a branch or opening a pull request against `main` (see [`.claude/rules/git-workflow.md`](../../rules/git-workflow.md)).

## Steps

1. **Check the scheme**: Confirm the branch name starts with a numeric issue number followed by a hyphen and a short, lowercase, hyphen-separated description (e.g. `21-chore-wiki-sync-reminder-in-claude-md`).
2. **Missing issue number**: If no GitHub issue exists yet for the change, ask the user for the issue number or whether one should be created first, rather than guessing one.
3. **Mismatch**: If an existing branch does not follow the scheme, propose the corrected name and rename it with `git branch -m <old> <new>` (and update any already-pushed remote tracking branch only after user confirmation, since renaming a pushed branch affects shared state).
4. **PR readiness**: Before opening a PR, confirm the branch name and target (`main`) match [`.claude/rules/git-workflow.md`](../../rules/git-workflow.md).

## Non-goals

- Do not invent an issue number when none was given — ask the user instead.
- Do not rename a branch that has already been merged.
