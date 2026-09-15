---
name: commit-message-lint
description: Check a drafted commit message against the Conventional Commits style required by .claude/rules/coding-conventions.md before committing.
---

# Commit Message Lint

Implements the commit message rule required by [`.claude/rules/coding-conventions.md`](../../rules/coding-conventions.md) and `CLAUDE.md` Section 5: commit messages follow Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`), as already practiced in the existing history.

## When to use this skill

Before running `git commit`, whenever a commit message has been drafted for this repository.

## Steps

1. **Type prefix**: Confirm the message starts with one of `feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:` (or a scoped variant like `feat(calendar):`), matching what the change actually does — a bug fix is `fix:`, not `chore:`; a documentation-only change is `docs:`, not `feat:`.
2. **Subject line**: Confirm the subject after the prefix is a concise, meaningful summary in imperative mood, not a vague label like "update files".
3. **Consistency with history**: Compare against `git log --oneline -20` to confirm the message matches the style already used in this repository's history.
4. **Body**: If the change is non-trivial, confirm the body (if present) explains the "why", not a restatement of the diff.

## Non-goals

- Do not invent a commit type when the change mixes concerns — ask the user to split the commit instead of picking an inaccurate type.
- Do not rewrite already-pushed commit messages without explicit user confirmation.
