# Backend

## Zweck
Enthaelt die komplette Spring-Boot-Anwendung des Real-Estate-Maintenance-Optimizers (REST-API, Terminlogik, Routenoptimierung, KI-gestuetzte Dauerabschaetzung).

## Technologien
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (User/Auth), MongoDB (Termine & Objekte), LangChain4j (KI-Integration)

## Ordnerstruktur

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Wurzelverzeichnis des eigentlichen Anwendungscodes, unterteilt in:

- **`controller/`** – REST-Endpunkte der Anwendung, z. B. Termin-, Material- und Routenverwaltung. Ein Controller pro fachlicher Domaene, keine Business-Logik im Controller; dafuer `service/` nutzen.
- **`service/`** – Business-Logik, z. B. Terminplanung, Routenoptimierung, Dauerabschaetzung. Ein Service pro fachlicher Domaene, orchestriert `repository/` und `ai/`.
- **`repository/`** – Spring-Data-JPA-Repositories fuer den Zugriff auf PostgreSQL (User/Auth) sowie entsprechende Repositories fuer MongoDB (Termine & Objekte).
- **`entity/`** – JPA-Entities bzw. MongoDB-Dokumente, welche die Datenmodelle der Anwendung abbilden (z. B. Termin, Objekt, Benutzer).
- **`dto/`** – Data Transfer Objects fuer den Datenaustausch zwischen Backend und Frontend, entkoppelt von den internen Entities.
- **`security/`** – Konfiguration von Spring Security und JWT-Verarbeitung (Authentifizierung, Autorisierung, Token-Erstellung/-Validierung).
- **`ai/`** – LangChain4j-Integration fuer die KI-gestuetzte Abschaetzung der Termindauer anhand aehnlicher vorangegangener Termine.
- **`config/`** – Allgemeine Konfiguration der Anwendung (z. B. CORS, Bean-Definitionen, Mail-Konfiguration).

### `src/main/resources/`
Konfigurationsdateien der Anwendung, z. B. `application.yml` sowie Datenbank-Migrationen.

### `src/test/java/...`
JUnit-Tests, Struktur spiegelt `src/main/java/...` wider.

### `pom.xml` / `build.gradle`
Abhaengigkeiten und Build-Skripte (noch nicht angelegt).

## Konventionen
Siehe `CLAUDE.md` im Repository-Root, Abschnitt "Coding Conventions": Variablennamen auf Englisch, Kommentare ausschliesslich als JavaDoc (ein Satz, Englisch), Funktionen/Methoden stets wiederverwendbar gestalten.
