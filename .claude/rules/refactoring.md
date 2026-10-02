# Refactoring Workflow

Binding refactoring workflow for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 10.

This rule governs **refactoring** — changing the structure of existing code without changing its behaviour. It complements, and does not replace, the minimal-diff-editing rule in `.claude/rules/coding-conventions.md`: minimal-diff applies to point fixes and small feature changes; this workflow applies whenever the task is explicitly a restructuring of existing code (renaming across files, extracting functions/components, splitting a class/component, changing an internal data structure) with no intended behaviour change.

## The 6-Month Test

Before refactoring, name the smell: could someone (including future-you) understand this code in six months without asking the author? If not, that is the concrete justification for the refactor — state it in the commit/PR description, not just "cleanup". Typical smells: unclear names, a service/component doing too much to describe in one sentence, deep nesting, duplicated logic, magic values, a change that has to touch many unrelated files for one conceptual edit.

## Process

```
Assess → Characterize → Plan → Execute → Verify
```

1. **Assess**: name the specific problem and what could break.
2. **Characterize**: before touching code with no test covering the behaviour in question, add a test that captures what the code *does today*, even if a result looks questionable — fixing a bug and refactoring are two separate changes. Test through the public surface (a controller endpoint via `MockMvc`, a component's rendered output/emitted events via Vue Test Utils), not internals, so the test survives the refactor.
3. **Plan**: order steps from least to most risky — rename, then extract, then change data structures or APIs last. Each step must leave the code working.
4. **Execute**: one move at a time; run the affected tests after each move.
5. **Verify**: run the full relevant test suite (`mvn test` / `npm run test:unit`) and read the diff before committing.

If a characterization test fails mid-refactor, the change altered behaviour: either undo the step, or turn it into a deliberate, separately committed fix and update the test's expectation in that same commit — never silently edit a test to make a refactor "pass". Stop and ask the user before changing a test's expectation; don't assume the behaviour change was intended. A pure rename may still touch test code (an identifier); the test's assertions are the contract, not the identifier names.

## Core Moves

| Move | What it does |
|------|--------------|
| Rename | Give a variable, method, class, component, or prop a clearer name |
| Extract Method/Function | Pull a block into a named method (backend) or composable/function (frontend) |
| Extract Component | Pull markup and logic into a dedicated Vue component under the matching `components/<feature>/` folder |
| Extract Variable | Replace a complex expression with a named, intermediate variable |
| Inline | Replace a method/variable with its body (undo an over-extraction) |
| Move | Relocate a method/class to the correct package (backend) or a component/composable to the correct folder (frontend), per `.claude/rules/folder-structure.md` |

## Legacy Code

- **Boy Scout Rule**: improve only the area you're touching; don't refactor unrelated code in the same change.
- Add or extend a characterization test before changing anything that lacks coverage for the behaviour you're about to touch.
- For a large-scale restructuring (e.g. splitting a large service), prefer branch-by-abstraction (introduce an interface, build the replacement behind it, swap once ready) over a single big-bang rewrite, so the change stays reviewable in small commits per `.claude/rules/git-workflow.md`.

## AI-Assisted Refactoring

- Work one specific, named move at a time ("extract validation into `validateAddress`"), not "clean up this file" — a vague prompt produces an unreviewable diff.
- Stop and ask the user if a step makes an existing test fail; don't edit the test to make it pass unless the user explicitly confirms the behaviour change is intended.
- Independent, well-specified refactoring steps (e.g. writing characterization tests for several unrelated endpoints) may be delegated to parallel subagents; a single large "refactor the backend and frontend" prompt must not be, since the resulting diff would be too large to review properly.

## Post-Implementation Refactoring Review

Every non-trivial implementation gets a refactoring review of **the files it added or changed** before the code review (see `.claude/rules/git-workflow.md`, "Post-Implementation Refactoring Review"). It is a deliberate checkpoint, not an open-ended cleanup: the scope is `git diff main...HEAD`, and the `refactoring-review` skill (`.claude/skills/refactoring-review/SKILL.md`) runs it.

### What to check

| Principle | Question to ask of the changed code |
|-----------|--------------------------------------|
| DRY | Is the same logic, markup, literal or test helper written in three or more places (two only if they will clearly change together)? Is a value repeated as a literal although a constant for it already exists? |
| Single responsibility / cohesion | Can each class, component or function be described in one sentence? Does a component repeat a structural block that is really its own component? |
| Coupling | Does a class reach into another class's package-private constants or internals? Does a DTO, controller or template hard-code a rule that belongs to the domain? |
| Magic values | Are numbers and strings (`8`, `72`, `128`, `"demo-"`) named where they are defined and reused where they are needed? |
| KISS / YAGNI | Is there speculative generality — an abstraction with one caller, an option nobody uses, an export nobody imports? Remove it. |
| Naming and readability | Does each name still tell the truth after the change (the 6-month test above)? |
| Dead code | Unused exports, parameters, imports, branches, CSS rules, message keys. |
| Test code | Duplicated helpers/fixtures across specs, tests asserting several unrelated things, fixtures encoding a value that is no longer the default. |
| Cross-layer duplication | Rules that must exist in both frontend and backend (validation limits) cannot be shared; confirm each side names its values and the readmes state that they must stay in sync. |

### Rules for acting on a finding

- Each finding becomes **one named move** from the Core Moves table (e.g. "extract `PasswordPolicy`"), executed with the Process above, with the affected tests run after each move.
- Only behaviour-preserving moves are applied in the review. A finding that needs a behaviour change, a new test expectation or an architectural decision is recorded as a follow-up instead; stop and ask the user if a test fails.
- **Stay in scope**: touch only files the branch added or changed. A pre-existing file may be touched only for a purely mechanical, test-verified deduplication of something this branch introduced a copy of (e.g. replacing a duplicated test helper), and the commit message names it.
- **Apply the rule of three and YAGNI before extracting**: a helper with a single caller, or two short lines in two places, is recorded as "considered and rejected" with the reason, not extracted.
- Larger duplication in untouched code (e.g. three near-identical dialog stylesheets) is reported as a follow-up issue, not fixed in the same branch (Boy Scout Rule above).
- Commit every applied move group as its own `refactor:` commit, separate from the implementation commit and from later review-fix commits, and update the readmes and `folder-structure.md` when files or responsibilities moved.
- The outcome — applied moves, rejected candidates with reasons, follow-ups — is reported to the user.
