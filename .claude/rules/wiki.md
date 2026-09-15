# GitHub Wiki

Binding rule for keeping the project's GitHub Wiki in sync with the repository. Referenced from `CLAUDE.md`, Section 10.

## Location

- The Wiki is a separate Git repository: `https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer.wiki.git`. It is not part of this repository's working tree and is not cloned here by default.
- It is bilingual: a `Home.md` (English + German) plus one page per topic and language, prefixed `EN-` / `DE-` (e.g. `EN-Backend.md` / `DE-Backend.md`), and a `_Sidebar.md` for navigation.
- Current topic pages (both languages): Project Overview, Features, Tech Stack, Backend, Frontend, Folder Structure, Coding Conventions, Testing, Git Workflow, Build & Run.

## When to check the Wiki

After every change to this repository that is **content-relevant** (not a pure typo/formatting fix), check whether the corresponding Wiki page(s) need updating too — the Wiki is a second, reader-facing rendering of the same facts and must not silently drift out of sync. This applies in particular to changes to:

- `README.md` (features, epics, deployment) → `EN-Features.md` / `DE-Features.md`
- `CLAUDE.md` Section 2 (tech stack) → `EN-Tech-Stack.md` / `DE-Tech-Stack.md`
- `backend/readme_backend_*.md` → `EN-Backend.md` / `DE-Backend.md`
- `frontend/readme_frontend_*.md` → `EN-Frontend.md` / `DE-Frontend.md`
- `.claude/rules/folder-structure.md` → `EN-Folder-Structure.md` / `DE-Ordnerstruktur.md`
- `.claude/rules/coding-conventions.md` → `EN-Coding-Conventions.md` / `DE-Coding-Konventionen.md`
- `.claude/rules/testing.md` → `EN-Testing.md` / `DE-Testing.md`
- `.claude/rules/git-workflow.md` → `EN-Git-Workflow.md` / `DE-Git-Workflow.md`

If a change touches one of the source files above, update both the German and the English Wiki page together — never only one language.

## How to update it

1. Clone the Wiki repo to a short local path (Windows path-length limits break a clone under a long project/scratch path): `git clone https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer.wiki.git`.
2. Edit the affected `EN-*`/`DE-*` page(s) (and `Home.md`/`_Sidebar.md` if a page was added or renamed).
3. Commit with a Conventional Commit message (see [coding-conventions.md](coding-conventions.md)) and push directly to the Wiki's `master` branch — the Wiki has no PR review flow of its own, unlike the main repository (see [git-workflow.md](git-workflow.md)).
4. Confirm with the user before pushing, since it publishes immediately and is visible to anyone with repo access.
