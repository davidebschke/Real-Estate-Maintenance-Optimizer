---
name: i18n-parity-check
description: Check that frontend/src/locales/de and en, and the backend messages*.properties bundles, stay key-for-key in sync after any UI text or server message change.
---

# i18n Parity Check

Keeps the two translation surfaces of this repository in sync: the frontend's `vue-i18n` locale files (German default, English fallback, per `CLAUDE.md` Section 3.2) and the backend's Spring `MessageSource` bundles (per `CLAUDE.md` Section 3.1).

## When to use this skill

After adding, renaming, or removing any user-facing UI text or server-generated (dynamic) message, before considering the change done.

## Steps

1. **Frontend namespaces**: For every file under `frontend/src/locales/de/<namespace>.json` touched by the change, confirm the matching `frontend/src/locales/en/<namespace>.json` has the exact same set of keys (no missing, no extra, no renamed-but-not-mirrored keys).
2. **New namespace**: If a new namespace file was added in `de/`, confirm the corresponding `en/` file was also added, and that `frontend/src/i18n/` still merges it correctly.
3. **Backend bundles**: For every key touched in `backend/src/main/resources/locales/messages.properties` (fallback, English), confirm the same key exists with an appropriate translation in `messages_de.properties` and `messages_en.properties`.
4. **No orphaned keys**: Confirm no locale file retains a key that is no longer referenced anywhere in the corresponding frontend components/composables or backend `MessageSource` lookups.

## Non-goals

- Do not judge translation quality/wording beyond flagging obviously missing or placeholder text — defer phrasing decisions to the user.
- Do not touch the GitHub Wiki for pure translation-content changes; the Wiki mapping in [`.claude/rules/wiki.md`](../../rules/wiki.md) does not cover locale files.
