---
name: refactoring-review
description: Review the files a branch added or changed for refactoring opportunities (DRY, cohesion, coupling, magic values, YAGNI, dead code, test-code smells), apply the safe behaviour-preserving moves one at a time, and report rejected candidates and follow-ups, per .claude/rules/refactoring.md.
---

# Refactoring Review

Implements the "Post-Implementation Refactoring Review" required by [`.claude/rules/refactoring.md`](../../rules/refactoring.md) and [`.claude/rules/git-workflow.md`](../../rules/git-workflow.md). It runs after the implementation commit and before the `code-review` skill.

## When to use this skill

After finishing any non-trivial implementation task and committing it, before the code review. Skip it only for a trivial change (typo, copy fix, config value) and say so explicitly.

## Steps

1. **Scope**: list the files the branch added or changed with `git diff --stat main...HEAD` (and `git status` for uncommitted work). Read them in their final state; this review covers nothing else.
2. **Assess**: walk the checklist table in `refactoring.md` ("What to check") against those files. Look for concrete evidence, not impressions — grep for repeated literals, duplicated helper functions, near-identical markup blocks, unused exports and cross-class access to package-private constants. For each finding name the smell and the specific move from the Core Moves table (rename, extract method/function/component, extract variable, inline, move).
3. **Filter** each finding through the rules in `refactoring.md`: rule of three and YAGNI (reject single-caller abstractions), stay in scope (no unrelated cleanup), behaviour-preserving only. Sort the findings into:
   - **Apply** — behaviour-preserving, covered by tests (or coverable by a characterization test first).
   - **Reject** — not worth it, with the reason.
   - **Follow-up** — needs a behaviour change, an architectural decision, or touches code outside the branch's scope; report it so the user can open an issue.
4. **Characterize**: for every "Apply" finding without test coverage of the behaviour at stake, add a characterization test first (through the public surface: `MockMvc` endpoint, rendered component output), in its own step.
5. **Execute one move at a time**, least risky first (rename → extract → change data structures or APIs last). After each move run the affected tests; backend `mvn test` (needs Docker) and `mvn pmd:check`, frontend `npx vue-tsc --build`, `npx eslint .` and `npm run test:unit`. If an existing test fails, undo the move or stop and ask the user — never edit a test's expectation to make a refactor pass.
6. **Independent moves may run in parallel** via subagents only when they touch disjoint files and are each well specified (see "AI-Assisted Refactoring" in `refactoring.md`); never one vague "refactor everything" prompt.
7. **Commit** each applied move group as its own `refactor:` commit (Conventional Commits, with the AI-assistance footer from `coding-conventions.md`), separate from the implementation commit. Keep the message to the smell and the move, and state "No behaviour change".
8. **Sync documentation**: if files, components or responsibilities moved or were added, update the backend/frontend readmes (DE and EN) and `.claude/rules/folder-structure.md` (use the `folder-structure-sync` and `docs-consistency-check` skills), and commit that as `docs:`.
9. **Report** to the user: applied moves (one line each), rejected candidates with reasons, and follow-ups. Do not claim a clean result for code you did not read.

## Effort level

Match the review's depth to the change's effort label (`Low-Effort`/`Medium-Effort`/`High-Effort`, see `git-workflow.md`): Low checks the changed files for duplicated literals, dead code and naming; Medium adds cohesion, coupling and test-code smells; High also checks cross-layer duplication and whether new abstractions are justified.

## Non-goals

- Do not refactor files the branch did not touch (report them as follow-ups), and do not mix a behaviour change into a refactor commit.
- Do not extract an abstraction for its own sake: a helper with one caller or two short repeated lines is "considered and rejected".
- This is not the correctness/security review — that is the `code-review` skill, which runs afterwards on the final structure.
