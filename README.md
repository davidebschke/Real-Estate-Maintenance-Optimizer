# [Real-Estate-Maintenance-Optimizer](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer)

[![Backend Tests](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/actions/workflows/backend-tests.yml/badge.svg)](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/actions/workflows/backend-tests.yml)
[![Frontend Tests](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/actions/workflows/frontend-tests.yml/badge.svg)](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/actions/workflows/frontend-tests.yml)
[![Deploy to Oracle Cloud](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/actions/workflows/deploy-oracle-cloud.yml/badge.svg)](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/actions/workflows/deploy-oracle-cloud.yml)

### 📊 Repository Stats

[![Lines of Code](https://sloc.xyz/github/davidebschke/Real-Estate-Maintenance-Optimizer)](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer)
[![Commits](https://badgen.net/github/commits/davidebschke/Real-Estate-Maintenance-Optimizer)](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/commits/main)
[![Pull Requests](https://img.shields.io/github/issues-pr-closed/davidebschke/Real-Estate-Maintenance-Optimizer?label=pull%20requests)](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer/pulls?q=is%3Apr)

- [Real-Estate-Maintenance-Optimizer](#real-estate-maintenance-optimizer)
  - [📊 Repository Stats](#-repository-stats)
  - [English](#english)
    - [📌 Project Overview](#-project-overview)
    - [Prototype](#prototype)
    - [🚀 Features \& Epics (MVP)](#-features--epics-mvp)
      - [1. 📅 Appointment Overview (`Term Overview`)](#1--appointment-overview-term-overview)
      - [2. ➕ Appointment Creation (`Term creation`)](#2--appointment-creation-term-creation)
      - [3. 🗺️ Location-Based Planning (`Automatic planning Place`)](#3-️-location-based-planning-automatic-planning-place)
      - [4. ⏱️ Time-Based Planning (`Automatic planning Time`)](#4-️-time-based-planning-automatic-planning-time)
      - [5. 📦 Material Planning (`Material planning`)](#5--material-planning-material-planning)
      - [6. 🚗 Route Optimization (`Fast route planning`)](#6--route-optimization-fast-route-planning)
      - [7. 🔒 Fixed Appointments (`Unrescheduled terms`)](#7--fixed-appointments-unrescheduled-terms)
      - [8. 🔄 Recurring Appointments (`Recurring appointments`)](#8--recurring-appointments-recurring-appointments)
    - [📋 Backlog \& Future Features (`Backlog/Issues`)](#-backlog--future-features-backlogissues)
    - [☁️ Deployment](#️-deployment)
  - [German](#german)
    - [📌 Projektübersicht](#-projektübersicht)
    - [Prototyp](#prototyp)
    - [🚀 Features \& Epics (MVP)](#-features--epics-mvp-1)
      - [1. 📅 Terminübersicht](#1--terminübersicht)
      - [2. ➕ Terminerstellung](#2--terminerstellung)
      - [3. 🗺️ Ortsabhängige Planung](#3-️-ortsabhängige-planung)
      - [4. ⏱️ Zeitbasierte Planung](#4-️-zeitbasierte-planung)
      - [5. 📦 Materialplanung](#5--materialplanung)
      - [6. 🚗 Routenoptimierung](#6--routenoptimierung)
      - [7. 🔒 Unverschiebbare Termine](#7--unverschiebbare-termine)
      - [8. 🔄 Wiederkehrende Termine](#8--wiederkehrende-termine)
    - [📋 Backlog \& Zukünftige Features (`Backlog/Issues`)](#-backlog--zukünftige-features-backlogissues)
    - [☁️ Deployment](#️-deployment-1)
  - [🧰 Tech Stack](#-tech-stack)
    - [🖥️ Frontend](#️-frontend)
    - [☁️ Backend](#️-backend)
    - [🗄️ Datenbank](#️-datenbank)
    - [🤖 AI / Intelligence](#-ai--intelligence)




## English
The core objective of the project is to help a property management company optimize maintenance. This will be achieved by reducing travel distances, better coordinating appointments based on location and duration, and estimating the potential duration of appointments based on previous appointments with similar contexts. This will save the company not only time but also money. In addition, operating costs could be reduced as a result. This would also benefit the tenants. More come Later

### 📌 Project Overview

This project is a **smart appointment and route scheduling application** tailored for field service technicians and tradespeople. It automates schedule optimization, minimizes travel times between jobs, and simplifies material tracking on the go.

### Prototype
You can find the prototype here: https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer-Prototype

<img width="3420" height="1304" alt="image" src="https://github.com/user-attachments/assets/5910b825-23d0-41d3-9ad9-86d68b14ccaf" />


### 🚀 Features & Epics (MVP)

#### 1. 📅 Appointment Overview (`Term Overview`)
- **Short View:** Clean calendar overview displaying all scheduled tasks.
- **Detailed View:** Full job context and addresses available upon selection.
- **Completion Tracking:** Mark a job as done directly from the detail view; it is recolored and its calendar block is drawn to the actual end time.
- **Daily Overview:** Today's appointments as a sorted list next to a map with numbered, connected markers.
- **Properties & Tenants:** List of all properties; each one opens a detail view with its apartments (floor, living area, rent, base rent, service charges) and tenants. Several tenants can share one apartment.
- **Property Statistics:** A statistics page lists every property with its open and completed appointments and its next appointment.

#### 2. ➕ Appointment Creation (`Term creation`)
- **Standard Input Form:** Fast manual entry interface.
- **Conflict Detection:** Appointments that overlap with, or come closer than a configurable buffer time to, another appointment are rejected when creating or moving them, and the next free slot of the same duration is suggested.

#### 3. 🗺️ Location-Based Planning (`Automatic planning Place`)
- Smart location grouping to **minimize driving time** between service calls.
- Dynamically slots smaller tasks near the previous job site into the daily schedule.
- **AI appointment optimization:** from four weeks ahead, Claude (via LangChain4j) proposes moves that save kilometres and driving time and still leave enough driving time to the next appointment; every move is only applied after the user confirms it, and a demo account can use it once.

#### 4. ⏱️ Time-Based Planning (`Automatic planning Time`)
- Automated scheduling and rescheduling maintaining safe buffer times.
- **Predefined Templates:** Instant creation of standardized services (e.g., basement cleaning, grounds maintenance).

#### 5. 📦 Material Planning (`Material planning`)
- Attach required tools and materials directly to jobs to prevent extra supply runs.

#### 6. 🚗 Route Optimization (`Fast route planning`)
- Route optimization and map display run via **Leaflet** (road routes calculated via **openrouteservice**), embedded directly in the appointment summary.
- The kilometres and driving time saved by accepted AI proposals are charted per week or month in the statistics.

#### 7. 🔒 Fixed Appointments (`Unrescheduled terms`)
- Lock specific appointments so automated AI scheduling leaves them untouched.

#### 8. 🔄 Recurring Appointments (`Recurring appointments`)
- Set up automatic recurring schedules (e.g., maintenance every 3 months) without manual oversight.
- The AI optimization moves a recurring appointment by at most two weeks.

---

### 📋 Backlog & Future Features (`Backlog/Issues`)

- Native mobile porting to **Android** & **iOS**
- **Voice/Audio** appointment capture: hands-free appointment logging designed for busy work environments

### ☁️ Deployment

| Service  | Platform          | Notes                                                                                           |
|----------|-------------------|---------------------------------------------------------------------------------------------------|
| Frontend | **Oracle Cloud**  | Built with Vite (`npm run build`); the resulting `dist` folder is deployed directly |
| Backend  | **Oracle Cloud**  | Built with Maven into an executable jar with a fixed name (`remo-backend.jar`, via `finalName` in `pom.xml`, independent of the version), run directly on Oracle Cloud Infrastructure (OCI) (`java -jar remo-backend.jar`) |

Both services deploy their build output directly (the frontend's `dist` folder, the backend's executable jar) — no Docker image or container registry is used for this deployment. The PostgreSQL database stays hosted on Supabase (project "RemoDB"), reached from the Oracle Cloud instance via Supabase's session pooler; the connection is passed in through the GitHub secrets `REMO_DB_URL`, `REMO_DB_USERNAME` and `REMO_DB_PASSWORD`. The login additionally needs the GitHub secrets `REMO_AUTH_JWT_SECRET` (signing key of the session token, at least 32 bytes, the deploy fails without it) and `REMO_AUTH_INITIAL_USER_PASSWORD` (initial password of the account `account_default`); the site must be served over HTTPS, otherwise the repository variable `REMO_AUTH_COOKIE_SECURE` has to be set to `false`. The optional GitHub secret `REMO_ROUTING_API_KEY` (openrouteservice key) enables road routing on the map; without it the deploy only warns and the map keeps drawing straight lines.

### 🛠️ Running the Project Locally

This guide starts the whole project on your own machine: PostgreSQL in Docker, the Spring Boot backend and the Vue frontend. All commands work in Windows PowerShell, in the Visual Studio Code terminal and in Bash (macOS/Linux); differences are noted. The flow database → backend → frontend was tested on Windows 11 with Docker Desktop, JDK 25, Maven 3.9 and Node.js.

#### 1. Install the prerequisites

| Tool | Version | Check command |
|------|---------|---------------|
| Git | current | `git --version` |
| Java JDK | **25** (LTS) | `java -version` |
| Apache Maven | 3.9 or newer | `mvn -version` |
| Node.js (incl. npm) | `^22.18` or `>=24.12` | `node -v` and `npm -v` |
| Docker Desktop | current, **must be running** | `docker --version` and `docker info` |

`docker info` must not report an error such as "cannot connect to the Docker daemon" — if it does, start Docker Desktop and wait until it shows "Engine running". Maven is used through the global installation (`mvn`); the repository has no Maven wrapper.

#### 2. Clone the repository

```sh
git clone https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer.git
cd Real-Estate-Maintenance-Optimizer
```

#### 3. Start the database with Docker

```sh
cd backend
docker compose up -d
docker compose ps
docker compose exec postgres pg_isready -U remo -d remo
```

- `docker compose up -d` pulls the image `postgres:17-alpine` on the first run and starts the database in the background (database `remo`, user `remo`, password `remo`, port `5432`). This matches the defaults in `application.yml` exactly, so no database configuration is needed.
- `docker compose ps` must show the `postgres` service with the status `running`.
- `pg_isready` must print `accepting connections`. Only then start the backend.
- Flyway creates the database schema automatically when the backend starts; no SQL has to be run by hand.

#### 4. Configure the backend (`backend/.env`)

The backend only starts with a secret signing key for the login. Locally it reads its values from the file `backend/.env` (listed in `.gitignore`, never committed). **Create the file anew** with exactly these three lines (do not copy `.env.example`: it additionally contains placeholders for the production database connection, which would override the local Docker database):

```properties
REMO_AUTH_JWT_SECRET=<random-value-of-at-least-32-characters>
REMO_AUTH_INITIAL_USER_PASSWORD=<your-password-8-to-72-characters>
REMO_AUTH_COOKIE_SECURE=false
```

- `REMO_AUTH_JWT_SECRET`: key for the session token, at least 32 characters. Without it startup aborts with an error message. A random value can be generated, for example, with (PowerShell) `$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)` — or (Bash) `openssl rand -base64 48`.
- `REMO_AUTH_INITIAL_USER_PASSWORD`: password of the account `account_default`, applied on the first start (afterwards stored as a BCrypt hash; changing the value later has no effect).
- `REMO_AUTH_COOKIE_SECURE=false`: locally you work over `http://localhost` instead of HTTPS, so the "Secure" flag of the login cookie has to be switched off (in production it stays on).
- Write the values without quotation marks, and save the file without a BOM (UTF-8 without signature). Visual Studio Code does this by default; in Windows PowerShell 5.1 therefore do not use `Out-File`/`Set-Content` without `-Encoding ascii`.
- Optional: `REMO_ROUTING_ENABLED=true` with `REMO_ROUTING_API_KEY=<openrouteservice-key>` enables road routes, and additionally `REMO_AI_ENABLED=true` with `REMO_AI_API_KEY=<anthropic-key>` the AI appointment optimization (it needs road routing for the travel matrix; see `backend/.env.example`).

#### 5. Start the backend

```sh
mvn spring-boot:run
```

Run the command in the `backend` folder (that is where the `.env` lives). The first start downloads the Maven dependencies and takes longer. The backend is ready as soon as the log shows `Started RealEstateMaintenanceOptimizerApplication in … seconds`. Check it in a **second** terminal:

```sh
curl http://localhost:8080/api/version
```

If the server answers with a JSON containing the version number, the backend is running. (Alternatively open `http://localhost:8080/api/version` in the browser.) The terminal stays occupied by the running backend.

#### 6. Configure and start the frontend

In a **second** terminal, starting from the repository root:

```sh
cd frontend
cp .env.example .env
npm install
npm run dev
```

- `cp .env.example .env` (PowerShell: `Copy-Item .env.example .env`) creates `frontend/.env` with `VITE_API_BASE_URL=http://localhost:8080`. Without this file the API calls go nowhere (404 from the Vite server itself). `http://localhost:8080` is only for local use; for the Oracle deployment the value stays empty.
- `npm install` downloads the dependencies (only needed the first time and after changes to `package.json`).
- `npm run dev` starts the Vite development server and prints its address, by default **http://localhost:5173**.

#### 7. Log in with the browser

Open `http://localhost:5173`. The login page offers two ways in:

- **Regular login:** username `account_default` and the password from `REMO_AUTH_INITIAL_USER_PASSWORD`.
- **Demo account:** the demo account button — no password, with example data, valid for a limited time only.

The project is now fully running locally.

#### Example with Visual Studio Code

1. Open Visual Studio Code and choose **File → Open Folder…**, then select the repository root `Real-Estate-Maintenance-Optimizer`.
2. Install the recommended extensions (**Extensions** view, `Ctrl+Shift+X`): **Extension Pack for Java**, **Vue (Official)**; optionally **Docker**. For the `frontend` folder VS Code suggests further extensions from `frontend/.vscode/extensions.json` on its own.
3. Open a terminal: **Terminal → New Terminal** (`` Ctrl+` ``).
4. Create two terminals side by side in the terminal panel (the **+** icon, or `Ctrl+Shift+5` to split):
   - **Terminal 1 (database + backend):** `cd backend`, then `docker compose up -d`, `docker compose exec postgres pg_isready -U remo -d remo`, then create the file `backend/.env` in the Explorer via **New File** and fill it with the three lines from step 4 (save with `Ctrl+S`), then `mvn spring-boot:run`.
   - **Terminal 2 (frontend):** `cd frontend`, `Copy-Item .env.example .env` (PowerShell), `npm install`, `npm run dev`. `Ctrl` + click on the printed address `http://localhost:5173` opens the application in the browser.
5. Do not start the backend with the "Run" button of the Java extension; use the terminal in the `backend` folder as above: only there does Spring find the `.env` file.

#### Stopping and cleaning up

| Action | Command |
|--------|---------|
| Stop backend or frontend | `Ctrl+C` in the respective terminal |
| Stop the database (data is kept) | `docker compose stop` (in the `backend` folder) |
| Remove the database container (data stays in the volume) | `docker compose down` |
| Remove the container **and all local data** | `docker compose down -v` |

#### Common problems

| Problem | Cause and fix |
|---------|---------------|
| `cannot connect to the Docker daemon` | Docker Desktop is not started; start it and re-check `docker info`. |
| `port is already allocated` for port 5432 | Another PostgreSQL instance occupies the port; stop it. |
| Backend aborts with `remo.auth.jwt-secret … must be set to at least 32 bytes` | `backend/.env` is missing, in the wrong folder or the value is too short; run `mvn spring-boot:run` in the `backend` folder. |
| Backend reports `Connection refused` to the database | The container is not ready yet; check `docker compose ps` and `pg_isready` and start the backend again. |
| `release version 25 not supported` or similar | An older JDK is in use; point `java -version` and `JAVA_HOME` to JDK 25. |
| Login fails with wrong credentials | The password differs from `REMO_AUTH_INITIAL_USER_PASSWORD`. It is stored as a hash on the first backend start and never overwritten; to reset it run `docker compose down -v` (deletes the local database) and start again. |
| Login seems to work but you end up on the login page again | Check that `REMO_AUTH_COOKIE_SECURE=false` is in `backend/.env`; restart the backend after changing it. |
| Vite prints "Port 5173 is in use, trying another one" and the page loads no data | The backend only allows the origin `http://localhost:5173` (CORS). Stop the program on port 5173 and restart `npm run dev`. |

## German
Das Projekt soll im Kern einer Immobilienverwaltung helfen Wartungen zu optimieren. Dies soll geschehen indem Anfahrtswege reduziert werden, Termine anhand des Ortes und der dauer besser abgestimmt werden und die potenzielle Dauer von Terminen eingeschätzt werden aufgrund von vorran gegangenen Terminen ähnlichen Kontextes. Das Unternehmen spart damit nicht nur Zeit sondern auch bares Geld. Zusätzlich könnten Betriebskosten dadurch gesenkt werden . Dies würde auch den Mieter freuen. Mehr wird später kommen.

### 📌 Projektübersicht

Diese Anwendung ist ein intelligentes System zur **Termin- und Tourenplanung**, das speziell auf die Bedürfnisse von Handwerkern und Außendienstmitarbeitern zugeschnitten ist. Das Ziel ist es, Fahrzeiten zu minimieren, die Tagesnutzung zu maximieren und administrative Aufwände unterwegs zu reduzieren.

### Prototyp
Hier finden Sie den Prototypen: https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer-Prototype

<img width="3420" height="1304" alt="image" src="https://github.com/user-attachments/assets/5910b825-23d0-41d3-9ad9-86d68b14ccaf" />

### 🚀 Features & Epics (MVP)

#### 1. 📅 Terminübersicht
- **Kurzansicht:** Übersichtliche Kalenderdarstellung aller anstehenden Termine.
- **Detailansicht:** Ein Klick öffnet sämtliche Kunden- und Auftragsdetails.
- **Erledigt-Status:** Ein Auftrag kann direkt in der Detailansicht als erledigt markiert werden; er wird daraufhin farblich hervorgehoben und im Kalender bis zur tatsächlichen Endzeit dargestellt.
- **Tagesübersicht:** Die heutigen Termine als sortierte Liste neben einer Karte mit nummerierten, verbundenen Markern.
- **Objekte & Mieter:** Liste aller Objekte; jedes öffnet eine Detailansicht mit seinen Wohnungen (Stockwerk, Wohnfläche, Mietpreis, Kaltmiete, Nebenkosten) und Mietern. Mehrere Mieter können sich eine Wohnung teilen.
- **Objektstatistik:** Eine Statistikseite listet jedes Objekt mit seinen offenen und erledigten Terminen sowie dem nächsten Termin.

#### 2. ➕ Terminerstellung
- **Eingabemaske:** Schnelle manuelle Erstellung von Aufträgen.
- **Konflikterkennung:** Termine, die sich mit einem anderen Termin überschneiden oder eine konfigurierbare Pufferzeit dazu unterschreiten, werden beim Anlegen und Verschieben abgelehnt und der nächste freie Zeitraum gleicher Dauer wird vorgeschlagen.

#### 3. 🗺️ Ortsabhängige Planung
- Intelligent abgestimmte Routen zur **Reduzierung von Fahrzeiten** zwischen Einsatzorten.
- Effizientes Einschieben kleinerer Aufträge in der Nähe des vorherigen Einsatzorts.
- **KI-Terminoptimierung:** Ab vier Wochen im Voraus schlägt Claude (über LangChain4j) Verschiebungen vor, die Kilometer und Fahrzeit sparen und trotzdem genug Fahrzeit zum nächsten Termin lassen; jede Verschiebung wird erst nach Bestätigung übernommen, ein Demo-Account kann sie einmal nutzen.

#### 4. ⏱️ Zeitbasierte Planung
- Automatische Terminvergabe und Umplanung mit konfigurierbarem zeitlichem Abstand.
- **Vordefinierte Vorlagen:** Schnelles Anlegen wiederkehrender Aufgaben (z. B. Kellerreinigung, Pflege der Außenanlage).

#### 5. 📦 Materialplanung
- Zuordnung von benötigtem Material direkt zum Termin, um unnötige Fahrten zum Lager oder Baumarkt zu vermeiden.

#### 6. 🚗 Routenoptimierung 
- Routenoptimierung und Kartenanzeige laufen über **Leaflet** (Straßenrouten berechnet über **openrouteservice**), direkt in der Terminübersicht eingebettet.
- Die durch übernommene KI-Vorschläge gesparten Kilometer und Fahrzeiten zeigt die Statistik als Liniendiagramm je Woche oder Monat.

#### 7. 🔒 Unverschiebbare Termine 
- Fixierte Termine werden durch den KI-Optimierungsalgorithmus nicht verändert.

#### 8. 🔄 Wiederkehrende Termine
- Automatische Daueraufträge (z. B. Inspektion alle 3 Monate), ohne manuell daran denken zu müssen.
- Die KI-Optimierung verschiebt einen wiederkehrenden Termin höchstens um zwei Wochen.
---

### 📋 Backlog & Zukünftige Features (`Backlog/Issues`)

- Native App-Portierung für **Android** und **iOS**
- **Spracheingabe / Audio-Funktion**: freihändige Terminerstellung per Sprachbefehl – ideal, wenn man gerade die Hände voll hat

### ☁️ Deployment

| Service   | Plattform          | Hinweise                                                                                          |
|-----------|--------------------|------------------------------------------------------------------------------------------------------|
| Frontend  | **Oracle Cloud**   | Gebaut mit Vite (`npm run build`); der resultierende `dist`-Ordner wird direkt deployed |
| Backend   | **Oracle Cloud**   | Gebaut mit Maven zu einem ausführbaren Jar mit festem Namen (`remo-backend.jar`, über `finalName` in `pom.xml`, unabhängig von der Version), direkt auf Oracle Cloud Infrastructure (OCI) ausgeführt (`java -jar remo-backend.jar`) |

Beide Services deployen ihr Build-Ergebnis direkt (der `dist`-Ordner des Frontends, das ausführbare Jar des Backends) — für dieses Deployment wird kein Docker-Image und keine Container-Registry genutzt. Die PostgreSQL-Datenbank bleibt bei Supabase gehostet (Projekt "RemoDB") und wird von der Oracle-Cloud-Instanz über Supabases Session-Pooler erreicht; die Verbindung wird über die GitHub-Secrets `REMO_DB_URL`, `REMO_DB_USERNAME` und `REMO_DB_PASSWORD` übergeben. Die Anmeldung braucht zusätzlich die GitHub-Secrets `REMO_AUTH_JWT_SECRET` (Signaturschlüssel des Sitzungs-Tokens, mindestens 32 Byte, ohne ihn schlägt das Deployment fehl) und `REMO_AUTH_INITIAL_USER_PASSWORD` (initiales Passwort des Accounts `account_default`); die Seite muss über HTTPS ausgeliefert werden, andernfalls ist die Repository-Variable `REMO_AUTH_COOKIE_SECURE` auf `false` zu setzen. Das optionale GitHub-Secret `REMO_ROUTING_API_KEY` (openrouteservice-Schlüssel) aktiviert das Straßenrouting auf der Karte; ohne es warnt das Deployment nur und die Karte zeichnet weiter Luftlinien.

### 🛠️ Projekt lokal starten

Diese Anleitung startet das komplette Projekt auf dem eigenen Rechner: PostgreSQL in Docker, das Spring-Boot-Backend und das Vue-Frontend. Alle Befehle funktionieren in Windows PowerShell, im Terminal von Visual Studio Code sowie in Bash (macOS/Linux); Abweichungen sind vermerkt. Der Ablauf Datenbank → Backend → Frontend wurde auf Windows 11 mit Docker Desktop, JDK 25, Maven 3.9 und Node.js getestet.

#### 1. Voraussetzungen installieren

| Programm | Version | Prüfbefehl |
|----------|---------|------------|
| Git | aktuell | `git --version` |
| Java JDK | **25** (LTS) | `java -version` |
| Apache Maven | 3.9 oder neuer | `mvn -version` |
| Node.js (inkl. npm) | `^22.18` oder `>=24.12` | `node -v` und `npm -v` |
| Docker Desktop | aktuell, **muss laufen** | `docker --version` und `docker info` |

`docker info` darf keinen Fehler wie „cannot connect to the Docker daemon" melden — sonst Docker Desktop starten und warten, bis es „Engine running" anzeigt. Maven läuft über die globale Installation (`mvn`); im Repository gibt es keinen Maven-Wrapper.

#### 2. Repository klonen

```sh
git clone https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer.git
cd Real-Estate-Maintenance-Optimizer
```

#### 3. Datenbank mit Docker starten

```sh
cd backend
docker compose up -d
docker compose ps
docker compose exec postgres pg_isready -U remo -d remo
```

- `docker compose up -d` lädt beim ersten Mal das Image `postgres:17-alpine` und startet die Datenbank im Hintergrund (Datenbank `remo`, Benutzer `remo`, Passwort `remo`, Port `5432`). Das passt genau zu den Standardwerten in `application.yml`, es ist keine Datenbank-Konfiguration nötig.
- `docker compose ps` muss den Dienst `postgres` mit dem Status `running` zeigen.
- `pg_isready` muss `accepting connections` ausgeben. Erst dann das Backend starten.
- Das Datenbankschema legt Flyway beim Start des Backends automatisch an; es muss kein SQL von Hand ausgeführt werden.

#### 4. Backend konfigurieren (`backend/.env`)

Das Backend startet nur mit einem geheimen Signaturschlüssel für die Anmeldung. Lokal liest es die Werte aus der Datei `backend/.env` (liegt in `.gitignore` und wird nie committet). Die Datei **neu anlegen** mit genau diesen drei Zeilen (nicht `.env.example` kopieren: dort stehen zusätzlich Platzhalter für die Produktions-Datenbankverbindung, die die lokale Docker-Datenbank überschreiben würden):

```properties
REMO_AUTH_JWT_SECRET=<mindestens-32-zeichen-langer-zufallswert>
REMO_AUTH_INITIAL_USER_PASSWORD=<dein-passwort-8-bis-72-zeichen>
REMO_AUTH_COOKIE_SECURE=false
```

- `REMO_AUTH_JWT_SECRET`: Schlüssel für das Sitzungs-Token, mindestens 32 Zeichen. Ohne ihn bricht der Start mit einer Fehlermeldung ab. Einen Zufallswert erzeugt zum Beispiel (PowerShell): `$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)` — oder (Bash) `openssl rand -base64 48`.
- `REMO_AUTH_INITIAL_USER_PASSWORD`: Passwort des Accounts `account_default`, das beim ersten Start gesetzt wird (danach als BCrypt-Hash gespeichert; eine spätere Änderung des Werts hat keine Wirkung mehr).
- `REMO_AUTH_COOKIE_SECURE=false`: lokal wird über `http://localhost` statt HTTPS gearbeitet, daher muss das Sicherheitsmerkmal „Secure" des Anmelde-Cookies ausgeschaltet sein (in Produktion bleibt es an).
- Die Werte stehen ohne Anführungszeichen in der Datei, und sie muss ohne BOM (UTF-8 ohne Signatur) gespeichert sein. In Visual Studio Code ist das der Standard; in Windows PowerShell 5.1 daher nicht `Out-File`/`Set-Content` ohne `-Encoding ascii` verwenden.
- Optional: `REMO_ROUTING_ENABLED=true` mit `REMO_ROUTING_API_KEY=<openrouteservice-key>` schaltet Straßenrouten ein, zusätzlich `REMO_AI_ENABLED=true` mit `REMO_AI_API_KEY=<anthropic-key>` die KI-Terminoptimierung (sie braucht das Straßenrouting für die Reisematrix; siehe `backend/.env.example`).

#### 5. Backend starten

```sh
mvn spring-boot:run
```

Der Befehl muss im Ordner `backend` ausgeführt werden (dort liegt die `.env`). Der erste Start lädt die Maven-Abhängigkeiten und dauert entsprechend länger. Das Backend ist bereit, sobald im Log `Started RealEstateMaintenanceOptimizerApplication in … seconds` steht. Kontrolle in einem **zweiten** Terminal:

```sh
curl http://localhost:8080/api/version
```

Antwortet der Server mit einem JSON mit der Versionsnummer, läuft das Backend. (Alternativ `http://localhost:8080/api/version` im Browser öffnen.) Das Terminal bleibt für das laufende Backend belegt.

#### 6. Frontend konfigurieren und starten

In einem **zweiten** Terminal, ausgehend vom Repository-Hauptordner:

```sh
cd frontend
cp .env.example .env
npm install
npm run dev
```

- `cp .env.example .env` (PowerShell: `Copy-Item .env.example .env`) legt `frontend/.env` mit `VITE_API_BASE_URL=http://localhost:8080` an. Ohne diese Datei laufen die API-Aufrufe ins Leere (Fehler 404 vom Vite-Server selbst). `http://localhost:8080` gilt nur lokal; für das Oracle-Deployment bleibt der Wert leer.
- `npm install` lädt die Abhängigkeiten (nur beim ersten Mal und nach Änderungen an `package.json` nötig).
- `npm run dev` startet den Vite-Entwicklungsserver und zeigt die Adresse an, standardmäßig **http://localhost:5173**.

#### 7. Im Browser anmelden

`http://localhost:5173` öffnen. Auf der Anmeldeseite gibt es zwei Wege:

- **Reguläre Anmeldung:** Benutzername `account_default` und das Passwort aus `REMO_AUTH_INITIAL_USER_PASSWORD`.
- **Demo-Account:** Schaltfläche für den Demo-Account — ohne Passwort, mit Beispieldaten, nur begrenzte Zeit gültig.

Damit läuft das Projekt vollständig lokal.

#### Beispiel mit Visual Studio Code

1. Visual Studio Code öffnen, über **Datei → Ordner öffnen…** den Repository-Hauptordner `Real-Estate-Maintenance-Optimizer` wählen.
2. Empfohlene Erweiterungen installieren (Ansicht **Erweiterungen**, `Strg+Shift+X`): **Extension Pack for Java**, **Vue (Official)**; optional **Docker**. Für den Ordner `frontend` schlägt VS Code weitere Erweiterungen aus `frontend/.vscode/extensions.json` selbst vor.
3. Ein Terminal öffnen: **Terminal → Neues Terminal** (`Strg+Ö` auf deutscher Tastatur, sonst `` Strg+` ``).
4. Im Terminal-Bereich über das **+**-Symbol (oder `Strg+Shift+5` zum Teilen) insgesamt zwei Terminals nebeneinander anlegen:
   - **Terminal 1 (Datenbank + Backend):** `cd backend`, dann `docker compose up -d`, `docker compose exec postgres pg_isready -U remo -d remo`, anschließend die Datei `backend/.env` im Explorer über **Neue Datei** anlegen und mit den drei Zeilen aus Schritt 4 füllen (speichern mit `Strg+S`), danach `mvn spring-boot:run`.
   - **Terminal 2 (Frontend):** `cd frontend`, `Copy-Item .env.example .env` (PowerShell), `npm install`, `npm run dev`. Mit `Strg` + Klick auf die ausgegebene Adresse `http://localhost:5173` öffnet sich die Anwendung im Browser.
5. Das Backend nicht über die Schaltfläche „Run" der Java-Erweiterung starten, sondern wie oben im Terminal im Ordner `backend`: Nur dort findet Spring die Datei `.env`.

#### Beenden und aufräumen

| Aktion | Befehl |
|--------|--------|
| Backend bzw. Frontend stoppen | `Strg+C` im jeweiligen Terminal |
| Datenbank stoppen (Daten bleiben erhalten) | `docker compose stop` (im Ordner `backend`) |
| Datenbank-Container entfernen (Daten bleiben im Volume) | `docker compose down` |
| Container **und alle lokalen Daten** löschen | `docker compose down -v` |

#### Häufige Probleme

| Problem | Ursache und Lösung |
|---------|--------------------|
| `cannot connect to the Docker daemon` | Docker Desktop ist nicht gestartet; starten und `docker info` erneut prüfen. |
| `port is already allocated` bei Port 5432 | Eine andere PostgreSQL-Instanz belegt den Port; diese beenden oder stoppen. |
| Backend bricht mit `remo.auth.jwt-secret … must be set to at least 32 bytes` ab | `backend/.env` fehlt, liegt im falschen Ordner oder der Wert ist zu kurz; `mvn spring-boot:run` im Ordner `backend` starten. |
| Backend meldet `Connection refused` zur Datenbank | Container läuft noch nicht bereit; `docker compose ps` und `pg_isready` prüfen und das Backend erneut starten. |
| `release version 25 not supported` o. ä. | Es läuft ein älteres JDK; `java -version` und `JAVA_HOME` auf JDK 25 stellen. |
| Anmeldung schlägt mit „falsche Zugangsdaten" fehl | Das Passwort entspricht nicht `REMO_AUTH_INITIAL_USER_PASSWORD`. Es wird beim ersten Backend-Start als Hash gespeichert und nie überschrieben; zum Zurücksetzen `docker compose down -v` ausführen (löscht die lokale Datenbank) und neu starten. |
| Anmeldung scheint zu klappen, aber man landet wieder auf der Anmeldeseite | Prüfen, ob `REMO_AUTH_COOKIE_SECURE=false` in `backend/.env` steht; Backend nach einer Änderung neu starten. |
| Vite meldet „Port 5173 is in use, trying another one" und die Seite lädt keine Daten | Das Backend erlaubt als Herkunft nur `http://localhost:5173` (CORS). Das Programm auf Port 5173 beenden und `npm run dev` neu starten. |

------------

## 🧰 Tech Stack

### 🖥️ Frontend
* **Framework:** Vue.js 3 mit TypeScript, Vite
* **UI Components:** PrimeVue 4
* **Calendar:** Vue.cal
* **Map:** Leaflet
* **Charts:** Chart.js (via PrimeVue `Chart`)
* **State, Routing & i18n:** Pinia, Vue Router, vue-i18n, Axios
* **Tests:** Vitest, Playwright

### ☁️ Backend
* **Language & Framework:** Java 25 (LTS), Spring Boot 4
* **Authentication & Security:** Spring Security, JJWT (JSON Web Token)
* **Data Access:** Spring Data JPA
* **External HTTP Client:** Spring `RestClient` (Adress-Geocoding via OpenStreetMap Nominatim, Straßenrouting und Reisematrix via openrouteservice)
* **Tests & Linting:** JUnit 5, Testcontainers, PMD

### 🗄️ Datenbank
* **Database:** PostgreSQL on Supabase (Termine, Objekte & User/Auth / Appointments, Objects & User/Auth), Flyway migrations

### 🤖 AI / Intelligence
* **Framework:** LangChain4j (`langchain4j-anthropic`) mit Claude Haiku 4.5 (KI-Terminoptimierung / AI appointment optimization)

