---
name: folder-structure-sync
description: Detect newly added, renamed, or removed files and directories in frontend/ and backend/ and keep .claude/rules/folder-structure.md in sync with the actual tree.
---

# Folder Structure Sync

Implements the review step required by [`.claude/rules/folder-structure.md`](../../rules/folder-structure.md): the physical folder tree there must match reality before new code is generated and after any file/directory is added, renamed, or removed.

## When to use this skill

Immediately after any file or directory under `frontend/` or `backend/` is added, renamed, or removed, and before generating new code that depends on the existing structure.

## Steps

1. **Compare**: Diff the actual `frontend/` and `backend/` trees (in particular `src/`, `src/main/java/...`, `src/main/resources/`) against the tree documented in [`.claude/rules/folder-structure.md`](../../rules/folder-structure.md).
2. **Update the tree**: If a directory or file was added, renamed, or removed, edit the tree in `folder-structure.md` to match, keeping the existing inline comments' style (short purpose description per entry).
3. **Update the backend package proposal**: If a new top-level Java package was introduced (e.g. `security/`, `ai/`), confirm it is reflected in the "Backend Package Structure" section of the same file.
4. **Cross-check readmes**: If the change also affects what's described as implemented in `backend/readme_backend_*.md` / `frontend/readme_frontend_*.md`, hand off to the `docs-consistency-check` skill rather than duplicating that update here.
5. **Wiki**: If `folder-structure.md` was edited, hand off to the `wiki-sync` skill (`EN-Folder-Structure.md` / `DE-Ordnerstruktur.md`).

## Non-goals

- Do not describe implementation status (what a file does) here — that belongs in the backend/frontend readmes, not the folder tree.
- Do not restructure the tree's ordering or formatting beyond what the actual change requires.
