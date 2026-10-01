# Anleitung: AI-native Workflow Classifier (n8n)

Ziel der Aufgabe: `Webhook → AI Classification → Switch → Action`, ausgelöst von einem Chat im eigenen Projekt.

## 1. Was schon umgesetzt ist (Claude Code)

| Miniboard-Schritt | Ergebnis im Repo |
|---|---|
| **Create Entry Point** | Neuer Navigationsbereich **Chat** neben „Immobilien“ (`/chat`, `ChatView`, `ChatMessageList`, `ChatMessageInput`, `useChat`, `chatService`) mit Chat-Icon, Eingabefeld, Senden-Button und POST an den n8n-Webhook. |
| **Design Workflow** | Plan siehe Abschnitt 3. |
| **Workflow-Datei** | [remo-chat-classifier.workflow.json](remo-chat-classifier.workflow.json) zum Import in n8n. |
| Tests | Vitest-Tests für Service, Composable, beide Komponenten und View; Router-Test um `chat` erweitert. |

## 2. Request Body (für das Board, Feld „Create Entry Point“)

```json
{
  "message": "user message"
}
```

Antwort des Workflows: `{ "reply": "…", "category": "damage_report | appointment_request | material_question | other" }`

## 3. Workflow-Design (für das Board, Feld „Design Workflow“)

- **1 Trigger:** Webhook (POST `/webhook/remo-chat`, Antwort über „Respond to Webhook“)
- **1 AI-Reasoning-Schritt:** Text Classifier + Anthropic Chat Model; Entscheidung: zu welcher Kategorie gehört die Nachricht?
- **Ausgabeformat:** genau eine Kategorie aus `damage_report`, `appointment_request`, `material_question`, `other`
- **Routing:** Der Text Classifier hat pro Kategorie einen eigenen Ausgang (übernimmt die Switch-Rolle).
- **Action:** je Kategorie ein Set-Node mit passender Antwort, danach „Respond to Webhook“ → Antwort erscheint im Chat.

```
Webhook → AI classification (Text Classifier) → Switch/Routing (4 Ausgänge) → Action (Antwort) → Respond to Webhook
```

## 4. Manuelle Schritte (du)

1. **n8n bereitstellen:** n8n Cloud-Account oder lokal `npx n8n` (läuft dann auf `http://localhost:5678`).
2. **Workflow importieren:** n8n → *Workflows → Import from File* → `n8n/remo-chat-classifier.workflow.json`.
3. **Anthropic-Credential anlegen:** im Node „Anthropic Chat Model“ deinen API-Key hinterlegen (alternativ OpenAI-Modell-Node verwenden).
4. **Testen und aktivieren:** Workflow mit „Execute workflow“ testen, danach *Active* schalten – nur dann gilt die Production-URL `/webhook/remo-chat` (Test-URL: `/webhook-test/remo-chat`).
5. **Frontend konfigurieren:** `frontend/.env.example` nach `frontend/.env` kopieren und `VITE_N8N_WEBHOOK_URL` auf deine Webhook-URL setzen (bei n8n Cloud die dort angezeigte Production-URL). Danach `npm run dev` neu starten.
6. **Ende-zu-Ende testen:** Chat öffnen, z. B. „Im Keller ist ein Rohr geplatzt“ senden → Kategorie „Schadensmeldung“ muss erscheinen.
7. **Board ausfüllen:** Body-Beispiel (Abschnitt 2), Plan (Abschnitt 3) und Screenshot des n8n-Workflows einfügen; alle Import-Fehler samt Fix im Feld „Import to n8n“ dokumentieren (Fehler → Fix). Typische Stellen: Modellname im Anthropic-Node, fehlende Credentials, Webhook nicht aktiv, CORS.
8. **Finaler Link:** n8n-Workflow teilen bzw. Link zum Repo-Branch einfügen.
9. **Branch pushen / PR:** auf Wunsch lasse ich committen und pushen (nicht automatisch geschehen).

## 5. Was ich übernehmen kann / was nicht

| Ich (Claude Code) | Du (manuell) |
|---|---|
| Chat-UI, Route, Navigation, i18n, Tests | n8n-Instanz starten / Account anlegen |
| Workflow-JSON erzeugen und bei Import-Fehlern anpassen (Fehlermeldung einfach schicken) | Workflow in n8n importieren und aktivieren |
| Doku, Anleitung, Commit | API-Key / Credentials eintragen (Secret, gehört nicht in den Chat) |
| Nach Zusendung Fehler fixen | Screenshots machen, Board ausfüllen, Link einreichen |
| Branch `Main-n8nTaskBootcamp-<Timestamp>` angelegt | Push/PR nach Rückfrage bei mir oder selbst |
