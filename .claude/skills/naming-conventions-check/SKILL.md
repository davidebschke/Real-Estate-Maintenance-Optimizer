---
name: naming-conventions-check
description: Check new or changed variable, function, component, and file names against the binding rules in .claude/rules/coding-conventions.md before considering a change done.
---

# Naming Conventions Check

Implements the naming rules required by [`.claude/rules/coding-conventions.md`](../../rules/coding-conventions.md) and `CLAUDE.md` Section 5: identifiers must be English, comments limited to one-sentence English JSDoc/TSDoc/JavaDoc, and functions/methods designed to be reusable rather than one-off.

## When to use this skill

After writing or modifying code in this repository (new variables, functions, methods, classes, components, or files), before considering the change done.

## Steps

1. **Language**: Confirm every new variable, function, method, class, and file name is in English. Flag any German or mixed-language identifier for renaming.
2. **Comments**: Confirm any added comment is a documentation comment (JSDoc/TSDoc in the frontend, JavaDoc in the backend), limited to one English sentence. Flag inline explanatory comments or multi-sentence documentation comments for removal or trimming.
3. **Reusability**: Check new functions/methods are not written as one-off special-case solutions. Flag logic that hardcodes a single caller's special case where a reusable parameterization would fit instead.
4. **File placement**: Cross-check new file names and locations against [`.claude/rules/folder-structure.md`](../../rules/folder-structure.md) so naming stays consistent with the established per-directory conventions (e.g. `*Service`, `*Controller`, `use*` composables, `*.spec.ts` for e2e tests).

## Non-goals

- Do not rename existing identifiers outside the current change's scope.
- Do not add documentation comments where none are required — only verify the ones already present or being added.
