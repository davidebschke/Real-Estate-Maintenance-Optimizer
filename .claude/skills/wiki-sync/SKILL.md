---
name: wiki-sync
description: Sync the project's bilingual GitHub Wiki with a content-relevant change to README.md, CLAUDE.md, the backend/frontend readmes, or a .claude/rules/*.md file, following the binding workflow in .claude/rules/wiki.md.
---

# Wiki Sync

Implements the workflow required by [`.claude/rules/wiki.md`](../../rules/wiki.md) whenever a content-relevant change was made to one of the source files it lists.

## When to use this skill

After any content-relevant (non-typo/non-formatting) change to one of:

- `README.md` → `EN-Features.md` / `DE-Features.md`
- `CLAUDE.md` Section 2 (tech stack) → `EN-Tech-Stack.md` / `DE-Tech-Stack.md`
- `backend/readme_backend_*.md` → `EN-Backend.md` / `DE-Backend.md`
- `frontend/readme_frontend_*.md` → `EN-Frontend.md` / `DE-Frontend.md`
- `.claude/rules/folder-structure.md` → `EN-Folder-Structure.md` / `DE-Ordnerstruktur.md`
- `.claude/rules/coding-conventions.md` → `EN-Coding-Conventions.md` / `DE-Coding-Konventionen.md`
- `.claude/rules/testing.md` → `EN-Testing.md` / `DE-Testing.md`
- `.claude/rules/git-workflow.md` → `EN-Git-Workflow.md` / `DE-Git-Workflow.md`

## Steps

1. Identify which source file(s) changed and map them to the corresponding Wiki page(s) using the table above. Always update the German **and** English page together — never only one language.
2. Clone the Wiki repository to a short local path outside this working tree (Windows path-length limits break a clone under a long project/scratch path):
   `git clone https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer.wiki.git`
3. Edit the affected `EN-*` / `DE-*` page(s), keeping their content equivalent to the updated source file. If a page was added or renamed, also update `Home.md` and `_Sidebar.md`.
4. Commit with a Conventional Commit message (see [coding-conventions.md](../../rules/coding-conventions.md)).
5. **Confirm with the user before pushing** — the Wiki has no PR review flow, and pushing publishes the change immediately and visibly to anyone with repo access.
6. Push to the Wiki's `master` branch only after explicit user confirmation.

## Non-goals

- Do not touch the Wiki for pure typo/formatting fixes in the source files.
- Do not push without confirmation, even if a prior push in the same session was approved.
