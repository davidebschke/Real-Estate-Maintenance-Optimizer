---
name: docs-consistency-check
description: Check whether CLAUDE.md, the backend/frontend readmes, .claude/rules/folder-structure.md, and README.md still match the actual repository state after a change, per the binding rules in .claude/rules/documentation.md.
---

# Docs Consistency Check

Implements the review step required by [`.claude/rules/documentation.md`](../../rules/documentation.md) and `CLAUDE.md` Section 10: after any change to code, folder structure, branches, or dependencies, verify the canonical documentation still reflects reality — and keep `CLAUDE.md` itself limited to short pointers rather than accumulating detail.

## When to use this skill

After implementing a change in this repository (new/removed files or directories, new controllers/services/components, dependency changes, `pom.xml`/`package.json` edits), before considering the task done.

## Steps

1. **Folder structure**: Compare the actual `frontend/` and `backend/` trees against [`.claude/rules/folder-structure.md`](../../rules/folder-structure.md). Update that file if directories or files were added, renamed, or removed.
2. **Implementation status**: Compare actual controllers/services/components/views/stores against `backend/readme_backend_en.md` / `readme_backend_de.md` and `frontend/readme_frontend_en.md` / `readme_frontend_de.md`. Update whichever pair is out of date, and keep both language versions of each pair in sync with each other.
3. **CLAUDE.md**: Confirm Sections 3, 4, and 6 still only hold short summaries pointing to the canonical files above — move any accumulated detail out into the appropriate readme/rule file rather than leaving it in `CLAUDE.md`.
4. **Version tracking**: If `pom.xml` was touched, check whether `<version>` changed. If it did, verify `GET /api/version` and `frontend/.../FooterVersion.vue` report the new version correctly, and update `CLAUDE.md` Section 3.3 accordingly.
5. **README.md**: If a feature/epic's scope or status changed, confirm the root `README.md` (English & German) still describes it accurately.
6. **Wiki**: If any file touched in steps 1–5 is one of the source files tracked by [`.claude/rules/wiki.md`](../../rules/wiki.md), hand off to the `wiki-sync` skill.

## Non-goals

- Do not add implementation-status prose or the full folder tree into `CLAUDE.md` itself — it must stay a short entry point.
- Do not update a readme pair in only one language.
