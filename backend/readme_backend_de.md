# Backend

## Zweck
Enthaelt die komplette Spring-Boot-Anwendung des Real-Estate-Maintenance-Optimizers (REST-API, Terminlogik, Routenoptimierung, KI-gestuetzte Dauerabschaetzung).

## Technologien
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (User/Auth), MongoDB (Termine & Objekte), LangChain4j (KI-Integration)

## Ordnerstruktur

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Wurzelverzeichnis des eigentlichen Anwendungscodes, unterteilt in:

Bereits vorhanden:

- **`controller/`** – REST-Endpunkte der Anwendung, z. B. Termin-, Material- und Routenverwaltung. Ein Controller pro fachlicher Domaene, keine Business-Logik im Controller; dafuer `service/` nutzen. Aktuell enthalten: `VersionController` (`GET /api/version`).
- **`dto/`** – Data Transfer Objects fuer den Datenaustausch zwischen Backend und Frontend, entkoppelt von den internen Entities. Aktuell enthalten: `VersionResponse`.
- **`config/`** – Allgemeine Konfiguration der Anwendung. Aktuell enthalten: `CorsConfig` (CORS fuer den Frontend-Dev-Server, Origin ueber `remo.frontend.base-url` konfiguriert) und `LocalizationConfig` (Locale-Aufloesung anhand des `Accept-Language`-Headers fuer dynamische Uebersetzungen).

Geplant, noch nicht implementiert (siehe `CLAUDE.md`, Abschnitt "Folder Structure", Backend-Paketstruktur-Vorschlag):

- **`service/`** – Business-Logik, z. B. Terminplanung, Routenoptimierung, Dauerabschaetzung. Ein Service pro fachlicher Domaene, orchestriert `repository/` und `ai/`.
- **`repository/`** – Spring-Data-JPA-Repositories fuer den Zugriff auf PostgreSQL (User/Auth) sowie entsprechende Repositories fuer MongoDB (Termine & Objekte).
- **`entity/`** – JPA-Entities bzw. MongoDB-Dokumente, welche die Datenmodelle der Anwendung abbilden (z. B. Termin, Objekt, Benutzer).
- **`security/`** – Konfiguration von Spring Security und JWT-Verarbeitung (Authentifizierung, Autorisierung, Token-Erstellung/-Validierung).
- **`ai/`** – LangChain4j-Integration fuer die KI-gestuetzte Abschaetzung der Termindauer anhand aehnlicher vorangegangener Termine.

### `src/main/resources/`
Konfigurationsdateien der Anwendung: `application.yml` (Spring-Boot-Konfiguration, u. a. `spring.messages.basename` und `remo.frontend.base-url`) sowie `locales/` (Deutsch/Englisch-Uebersetzungen fuer dynamischen, serverseitig erzeugten Text via Springs `MessageSource`: `messages.properties`, `messages_de.properties`, `messages_en.properties`). Datenbank-Migrationen kommen hinzu, sobald die Persistenz implementiert ist.

### `src/test/java/...`
JUnit-Tests, Struktur spiegelt `src/main/java/...` wider. Aktuell noch keine Tests vorhanden (siehe `CLAUDE.md`, Abschnitt "Tests").

### `pom.xml`
Abhaengigkeiten und Build-Konfiguration (Maven). Haelt die einzige Quelle der Wahrheit fuer die Anwendungsversion (`<version>`), die ueber `GET /api/version` an das Frontend ausgeliefert wird.

## Konventionen
Siehe `CLAUDE.md` im Repository-Root, Abschnitt "Coding Conventions": Variablennamen auf Englisch, Kommentare ausschliesslich als JavaDoc (ein Satz, Englisch), Funktionen/Methoden stets wiederverwendbar gestalten.
