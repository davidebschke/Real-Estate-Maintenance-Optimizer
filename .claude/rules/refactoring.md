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
