# Backend

## Zweck
Enthaelt die komplette Spring-Boot-Anwendung des Real-Estate-Maintenance-Optimizers (REST-API, Terminlogik, Routenoptimierung, KI-gestuetzte Dauerabschaetzung).

## Technologien
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (User/Auth), MongoDB (Termine & Objekte), LangChain4j (KI-Integration)

## Ordnerstruktur

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Wurzelverzeichnis des eigentlichen Anwendungscodes, unterteilt in:

Bereits vorhanden:

- **`controller/`** – REST-Endpunkte der Anwendung, z. B. Termin-, Material- und Routenverwaltung. Ein Controller pro fachlicher Domaene, keine Business-Logik im Controller; dafuer `service/` nutzen. Aktuell enthalten: `VersionController` (`GET /api/version`), `AppointmentController` (`/api/appointments` Anlegen/Lesen/Verschieben/Loeschen) und `GlobalExceptionHandler` (uebersetzt Domain-Exceptions in lokalisierte HTTP-Antworten).
- **`dto/`** – Data Transfer Objects fuer den Datenaustausch zwischen Backend und Frontend, entkoppelt von den internen Entities. Aktuell enthalten: `VersionResponse`, `ErrorResponse` sowie die Termin-DTOs (`CreateAppointmentRequest`, `MoveAppointmentRequest`, `AppointmentResponse`, `HistoryEntryResponse`).
- **`config/`** – Allgemeine Konfiguration der Anwendung. Aktuell enthalten: `CorsConfig` (CORS fuer den Frontend-Dev-Server, Origin ueber `remo.frontend.base-url` konfiguriert, erlaubt `GET`/`POST`/`PATCH`/`DELETE`), `LocalizationConfig` (Locale-Aufloesung anhand des `Accept-Language`-Headers fuer dynamische Uebersetzungen) und `StorageProperties` (Speicherordner fuer Termin-JSON-Dateien, `remo.storage.directory`).
- **`service/`** – Business-Logik. Aktuell enthalten: `AppointmentService` (Anlegen inkl. Materialisierung wiederkehrender Termine, Verschieben mit Sperre fuer unverschiebbare Termine, Loeschen einzeln oder als Serie).
- **`repository/`** – Datenzugriff. Aktuell enthalten: `AppointmentFileRepository`, ein **vorlaeufiges**, JSON-Datei-basiertes Repository (eine Datei pro Termin) als Platzhalter fuer das in `CLAUDE.md` §2.3 beschriebene MongoDB-Repository, bis eine echte Datenbank angebunden ist.
- **`entity/`** – Domaenenmodell als Records. Aktuell enthalten: `Appointment`, `HistoryEntry` und `HistoryEventType` – vorerst einfache Java-Records ohne JPA-/MongoDB-Mapping, da noch keine Datenbank existiert.
- **`exception/`** – Domain-Exceptions (`AppointmentNotFoundException`, `AppointmentLockedException`, `InvalidRecurrenceException`), die vom `GlobalExceptionHandler` in HTTP-Antworten uebersetzt werden.

Geplant, noch nicht implementiert (siehe `CLAUDE.md`, Abschnitt "Folder Structure", Backend-Paketstruktur-Vorschlag):

- **`security/`** – Konfiguration von Spring Security und JWT-Verarbeitung (Authentifizierung, Autorisierung, Token-Erstellung/-Validierung).
- **`ai/`** – LangChain4j-Integration fuer die KI-gestuetzte Abschaetzung der Termindauer anhand aehnlicher vorangegangener Termine.
- Ablösung von `AppointmentFileRepository`s JSON-Dateien durch echte PostgreSQL- (User/Auth) und MongoDB-Persistenz (Termine & Objekte) gemaess `CLAUDE.md` §2.3.

### `src/main/resources/`
Konfigurationsdateien der Anwendung: `application.yml` (Spring-Boot-Konfiguration, u. a. `spring.messages.basename`, `remo.frontend.base-url` und `remo.storage.directory`) sowie `locales/` (Deutsch/Englisch-Uebersetzungen fuer dynamischen, serverseitig erzeugten Text via Springs `MessageSource`: `messages.properties`, `messages_de.properties`, `messages_en.properties`). Datenbank-Migrationen kommen hinzu, sobald die Persistenz implementiert ist.

### `ExampleTerms/`
Per .gitignore ausgeschlossen, wird zur Laufzeit im Arbeitsverzeichnis des Backends automatisch angelegt: eine JSON-Datei pro Termin, der unter `repository/` beschriebene, vorlaeufige Speicher.

### `src/test/java/...`
JUnit-Tests, Struktur spiegelt `src/main/java/...` wider. Deckt die Version-/Localization-/CORS-Konfiguration sowie die komplette Termin-Funktion ab (Repository, Service, Controller) – siehe `CLAUDE.md`, Abschnitt "Tests".

### `pom.xml`
Abhaengigkeiten und Build-Konfiguration (Maven). Haelt die einzige Quelle der Wahrheit fuer die Anwendungsversion (`<version>`), die ueber `GET /api/version` an das Frontend ausgeliefert wird.

## Konventionen
Siehe `CLAUDE.md` im Repository-Root, Abschnitt "Coding Conventions": Variablennamen auf Englisch, Kommentare ausschliesslich als JavaDoc (ein Satz, Englisch), Funktionen/Methoden stets wiederverwendbar gestalten.
