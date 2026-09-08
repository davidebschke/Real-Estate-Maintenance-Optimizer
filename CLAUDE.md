# CLAUDE.md

Diese Datei liefert Claude Code (und anderen KI-Assistenten) den notwendigen Kontext und die verbindlichen Richtlinien für die Arbeit in diesem Repository.

## 1. Projektübersicht

**Real-Estate-Maintenance-Optimizer** ist ein intelligentes System zur Termin- und Tourenplanung für Handwerker und Außendienstmitarbeiter in der Immobilienverwaltung.

### 1.1 Kernziel
Wartungen einer Immobilienverwaltung optimieren, indem:
- Anfahrtswege reduziert werden,
- Termine anhand von Ort und Dauer besser abgestimmt werden,
- Materialfahrten und benötigtes Material berücksichtigt werden,
- die potenzielle Dauer neuer Termine anhand ähnlicher vorheriger Termine geschätzt wird mithilfe von KI unterstützung.

Ergebnis: Zeit- und Kostenersparnis für das Unternehmen, geringere Betriebskosten, zufriedenere Mieter.

### 1.2 Prototyp
https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer-Prototype

## 2. Tech Stack

### 2.1 Frontend
| Bereich            | Technologie       |
|--------------------|-------------------|
| Framework          | Vue.js 3          |
| UI-Komponenten     | PrimeVue 4        |
| Kalender-Ansicht   | Vue.cal           |
| Routing            | Vue Router        |
| HTTP-Client        | Axios             |

### 2.2 Backend
| Bereich                  | Technologie                |
|--------------------------|-----------------------------|
| Sprache / Framework      | Java, Spring Boot 3        |
| Authentifizierung        | Spring Security, JJWT (JSON Web Token) |
| Datenzugriff             | Spring Data JPA             |
| E-Mail-Versand           | Spring Mail                 |

### 2.3 Datenbank
| Bereich    | Technologie |
|------------|-------------|
| Datenbank  | PostgreSQL  |

### 2.4 KI / Intelligence
| Bereich    | Technologie  |
|------------|--------------|
| Framework  | LangChain4j  |

## 3. Repository-Status

Stand dieser Datei: nur Projekt-Dokumentation vorhanden (`README.md`, `LICENSE`, `SECURITY.md`, `.github/ISSUE_TEMPLATE`). Es existiert noch **kein Quellcode** für Frontend oder Backend.

Vorhandene Branches:
- `main` (Hauptbranch)
- `4-chore-refaktor-folder-structure-from-frontend` (geplante Ordnerstruktur-Überarbeitung Frontend)
- `5-chore-add-claudemd-file-in-claude` (Erstellung/Pflege der `CLAUDE.md`)

Sobald Code hinzugefügt wird, muss Abschnitt 4 (Ordnerstruktur) und Abschnitt 7 (Build & Run) durch die tatsächlichen Gegebenheiten ersetzt werden.

## 4. Empfohlene Ordnerstruktur (Monorepo)

```
Real-Estate-Maintenance-Optimizer/
├── frontend/                      # Vue.js 3 Anwendung
│   ├── src/
│   │   ├── components/            # wiederverwendbare UI-Bausteine
│   │   ├── views/                 # Seiten / Routen-Ziele
│   │   ├── router/                # Vue Router Konfiguration
│   │   ├── services/              # Axios API-Clients
│   │   ├── stores/                # State Management
│   │   └── composables/           # wiederverwendbare Vue Composition-Functions
│   ├── public/
│   └── package.json
├── backend/                       # Spring Boot Anwendung
│   ├── src/main/java/...          # Java-Quellcode (Controller, Service, Repository, Entity)
│   ├── src/main/resources/        # application.yml, Migrations
│   ├── src/test/java/...          # JUnit-Tests
│   └── pom.xml (oder build.gradle)
├── .github/
│   └── ISSUE_TEMPLATE/
├── README.md
├── SECURITY.md
└── CLAUDE.md
```

Backend-Paketstruktur (Vorschlag, klassisch nach Schichten):
```
com.<organisation>.realestatemaintainceoptimizer
├── controller/     # REST-Endpunkte
├── service/        # Geschäftslogik
├── repository/     # Spring Data JPA Repositories
├── entity/         # JPA-Entitäten
├── dto/            # Data Transfer Objects
├── security/       # JWT / Spring Security Konfiguration
├── ai/             # LangChain4j Integration
└── config/         # allgemeine Konfiguration
```

## 5. Coding-Konventionen

- **Variablennamen:** ausnahmslos auf Englisch.
- **Kommentare:** kurz und knapp, ausschließlich auf Englisch — keine weiteren Sprachen, auch nicht in Methodennamen.
- **Dokumentations-Kommentare:** im Frontend JSDoc (TSDoc bei TypeScript), im Backend JavaDoc — Beschreibung jeweils maximal ein Satz.
- **Funktionen/Methoden:** immer wiederverwertbar (reusable) konzipieren, keine Einmal-Lösungen für Spezialfälle.
- **Commit-Messages:** aussagekräftig, Conventional-Commits-Stil (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`), wie in der bisherigen Historie bereits praktiziert.
- **Branch-Namen:** Schema `<issue-nr>-<kurzbeschreibung>` (z. B. `4-chore-refaktor-folder-structure-from-frontend`).

## 6. Features & Epics (MVP)

1. **Terminübersicht** – Kurzansicht (Kalender) und Detailansicht (voller Auftragskontext).
2. **Terminerstellung** – Eingabemaske sowie Sprach-/Audio-Erfassung für freihändige Nutzung.
3. **Ortsabhängige Planung** – automatische Gruppierung nach Standort zur Fahrzeitreduzierung.
4. **Zeitbasierte Planung** – automatische Terminvergabe mit Pufferzeiten, Vorlagen für Standardaufgaben (z. B. Kellerreinigung).
5. **Materialplanung** – Zuordnung von benötigtem Material zum Termin, um Extra-Fahrten zu vermeiden.
6. **Routenoptimierung** – Google-Maps- / Apple-Maps-Integration direkt in der Terminübersicht.
7. **Unverschiebbare Termine** – Fixierung einzelner Termine gegen automatische KI-Umplanung.
8. **Wiederkehrende Termine** – automatische Daueraufträge (z. B. Inspektion alle 3 Monate).

### 6.1 Backlog
- Native App-Portierung für Android & iOS.

## 7. Build & Run

*Noch nicht definiert – wird ergänzt, sobald das Frontend- und Backend-Grundgerüst existiert (erwartet: `npm install && npm run dev` für das Frontend, `mvn spring-boot:run` bzw. `./gradlew bootRun` für das Backend).*

## 8. Tests

*Noch nicht vorhanden.* Empfohlen:
- Frontend: Vitest + Vue Test Utils
- Backend: JUnit 5 + Mockito, Testcontainers für PostgreSQL-Integrationstests

## 9. Git-Workflow

- Remote `origin`: https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer
- Hauptbranch: `main`
- CODEOWNER: `@davidebschke` (siehe `.github/CODEOWNERS.md`)
- Neue Features/Fixes über Feature-Branches und Pull Requests gegen `main`.

## 10. Hinweise für Claude Code

- Vor jeder Code-Generierung prüfen, ob sich die tatsächliche Ordnerstruktur bereits geändert hat (dieses Dokument entsprechend aktuell halten).
- Abschnitt 5 (Coding-Konventionen) ist verbindlich für jede Code-Änderung in diesem Repository.
- Sicherheitsrelevante Änderungen (Spring Security, JWT) besonders sorgfältig prüfen und nicht ungetestet mergen.
