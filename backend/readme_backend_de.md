# Backend

## Zweck
Enthaelt die komplette Spring-Boot-Anwendung des Real-Estate-Maintenance-Optimizers (REST-API, Terminlogik, Routenoptimierung, KI-gestuetzte Dauerabschaetzung).

## Technologien
Java 25 (LTS), Spring Boot 3, Spring Security, JJWT (JSON Web Token), Spring Data JPA, Spring Mail, PostgreSQL (User/Auth), MongoDB (Termine & Objekte), LangChain4j (KI-Integration)

## Ordnerstruktur

### `src/main/java/com/remo/realestatemaintainceoptimizer/`
Wurzelverzeichnis des eigentlichen Anwendungscodes, unterteilt in:

Bereits vorhanden:

- **`controller/`** – REST-Endpunkte der Anwendung, z. B. Termin-, Material- und Routenverwaltung. Ein Controller pro fachlicher Domaene, keine Business-Logik im Controller; dafuer `service/` nutzen. Aktuell enthalten:
  - `VersionController` – `GET /api/version`, liefert die aktuell gepackte Version aus `pom.xml` ueber Spring Boots `BuildProperties` (zur Build-Zeit erzeugt durch das `spring-boot-maven-plugin`-Ziel `build-info`, gebunden an die Phase `generate-resources`, sodass es vor Kompilierung/Tests laeuft).
  - `AppointmentController` – `GET/POST /api/appointments`, `GET /api/appointments/{id}`, `PATCH /api/appointments/{id}/schedule` (Verschieben), `PATCH /api/appointments/{id}/complete` (als erledigt markieren, setzt `actualEnd` auf die aktuelle Zeit), `PATCH /api/appointments/{id}/reopen` (Erledigt-Status zuruecksetzen) und `DELETE /api/appointments/{id}?scope=single|series` (Letzteres entfernt zusaetzlich alle noch nicht vergangenen Vorkommen einer wiederkehrenden Serie).
  - `PropertyController` – `GET /api/properties`, `GET /api/properties/{id}`; vorerst nur lesend (siehe "Objekte Auflisten" unten).
  - `GeocodingController` – `GET /api/geocode?address=...`, proxied Adressaufloesung fuer die Terminkarte (siehe "Adress-Geocoding" unten).
  - `GlobalExceptionHandler` – uebersetzt Domain-Exceptions (`exception/`) in lokalisierte `404`-/`409`-/`400`-Antworten.
- **`dto/`** – Data Transfer Objects fuer den Datenaustausch zwischen Backend und Frontend, entkoppelt von den internen Entities. Aktuell enthalten: `VersionResponse`, `ErrorResponse`, die Termin-DTOs (`CreateAppointmentRequest`, `MoveAppointmentRequest`, `AppointmentResponse` inkl. `actualEnd`/`completed`, `HistoryEntryResponse`), `PropertyResponse` (inkl. `latitude`/`longitude`) sowie `GeocodingResponse` (`latitude`/`longitude`, beide `null` wenn keine Aufloesung gefunden wurde).
- **`config/`** – Allgemeine Konfiguration der Anwendung. Aktuell enthalten: `CorsConfig` (CORS fuer den Frontend-Dev-Server, Origin ueber `remo.frontend.base-url` konfiguriert, erlaubt `GET`/`POST`/`PATCH`/`DELETE` auf `/api/**`), `LocalizationConfig` (loest die Locale der Anfrage anhand des `Accept-Language`-Headers auf, faellt auf Englisch zurueck, wenn keine unterstuetzte Locale angefragt wird), `StorageProperties` (Speicherordner fuer Termin-JSON-Dateien, `remo.storage.directory`), `PropertyStorageProperties` (Speicherordner fuer Objekt-JSON-Dateien, `remo.storage.properties.directory`) und `RestClientConfig` (stellt den `RestClient.Builder` fuer `GeocodingService` bereit).
- **`service/`** – Business-Logik. Aktuell enthalten: `AppointmentService` – Validierung, Materialisierung wiederkehrender Termine (ein wiederkehrender Termin legt sofort einen festen Horizont von 12 zukuenftigen Vorkommen an, die sich eine `seriesId` teilen und jeweils unabhaengig verschiebbar/sperrbar/loeschbar sind), Verschieben mit Sperre fuer unverschiebbare Termine, Markieren als erledigt (`complete`, setzt `actualEnd` auf `LocalDateTime.now()`) bzw. Zuruecksetzen (`reopen`, loescht `actualEnd` wieder), Loeschen einzeln oder als Serie sowie Lokalisierung des History-Logs jedes Termins beim Lesen; `PropertyService` – rein lesendes Auflisten (sortiert nach Name) und Suche per ID; sowie `GeocodingService` (siehe "Adress-Geocoding" unten).
- **`repository/`** – Datenzugriff. Aktuell enthalten: `AppointmentFileRepository` und `PropertyFileRepository`, **vorlaeufige**, JSON-Datei-basierte Repositories (eine Datei pro Datensatz) als Platzhalter fuer die in `CLAUDE.md` §2.3 beschriebenen MongoDB-Repositories, bis eine echte Datenbank angebunden ist.
- **`entity/`** – Domaenenmodell als Records. Aktuell enthalten: `Appointment` (inkl. `actualEnd`, `completed()` als abgeleitetem Zustand, Wither-Methoden `withSchedule`/`withActualEnd`), `HistoryEntry` und `HistoryEventType` (`CREATED`, `MOVED`, `COMPLETED`, `REOPENED`) sowie `Property` (`id`, `name`, `address`, `icon`, `latitude`, `longitude` – die Koordinaten sind `null`, solange eine Adresse nicht geokodiert werden konnte) – vorerst einfache Java-Records ohne JPA-/MongoDB-Mapping, da noch keine Datenbank existiert.
- **`exception/`** – Domain-Exceptions (`AppointmentNotFoundException`, `AppointmentLockedException`, `InvalidRecurrenceException`, `PropertyNotFoundException`), die vom `GlobalExceptionHandler` in HTTP-Antworten uebersetzt werden.

### Objekte Auflisten
`PropertyController`/`PropertyService`/`PropertyFileRepository` stellen dieselben Objekte, die auch bei der Terminerstellung auswaehlbar sind (`AppointmentPropertySelect.vue` im Frontend), als rein lesende Liste fuer die "Immobilien"-Seite bereit. Es existieren bislang nur `GET`-Operationen – Anlegen/Bearbeiten/Loeschen von Objekten ist noch nicht implementiert. Offene/erledigte Termin-Anzahlen und der naechste Termin je Objekt werden nicht auf `Property` gespeichert; das Frontend leitet sie aus den bereits geladenen Terminen ab (gefiltert nach `propertyId`), sodass kein Aggregations-Endpunkt noetig war.

### Adress-Geocoding
`GeocodingService` loest Adressen ueber die OpenStreetMap-Nominatim-API zu Koordinaten auf, fuer die Terminkarte auf der Frontend-Uebersichtsseite (siehe `frontend/readme_frontend_de.md`). Der Aufruf laeuft **serverseitig** statt direkt aus dem Browser, weil Nominatims Missbrauchserkennung Anfragen ohne eine echte, identifizierende `User-Agent`-Kennung blockiert – ein Header, den ein Browser aus Sicherheitsgruenden nie selbst setzen kann. `RestClientConfig` stellt dafuer den `RestClient.Builder` bereit, `GeocodingService` setzt einen projektbezogenen `User-Agent` (Verweis auf das GitHub-Repository, keine persoenlichen Daten), cached Ergebnisse pro Adresse im Arbeitsspeicher und drosselt neue Anfragen auf eine pro Sekunde (Nominatims Nutzungsrichtlinie). Findet die vollstaendige Adresse keinen Treffer, versucht der Service es erneut ohne einen angehaengten Stadtteil-Suffix (z. B. "Koeln-Porz"), da Nominatims Freitextsuche bei solchen Adressen sonst scheitert, obwohl die Strasse selbst real existiert, und faellt erst danach auf eine strukturierte Suche nach der enthaltenen deutschen Postleitzahl zurueck. Alle 23 Beispiel-Objekte in `ExampleObjects/` sind mit so ermittelten Koordinaten vorbefuellt; `GET /api/geocode` wird vom Frontend nur noch als Fallback fuer Objekte ohne gespeicherte Koordinaten aufgerufen.

Geplant, noch nicht implementiert (siehe `.claude/rules/folder-structure.md`, Backend-Paketstruktur-Vorschlag):

- **`security/`** – Konfiguration von Spring Security und JWT-Verarbeitung (Authentifizierung, Autorisierung, Token-Erstellung/-Validierung).
- **`ai/`** – LangChain4j-Integration fuer die KI-gestuetzte Abschaetzung der Termindauer anhand aehnlicher vorangegangener Termine.
- Ablösung von `AppointmentFileRepository`s JSON-Dateien durch echte PostgreSQL- (User/Auth) und MongoDB-Persistenz (Termine & Objekte) gemaess `CLAUDE.md` §2.3.

### `src/main/resources/`
Konfigurationsdateien der Anwendung: `application.yml` (Spring-Boot-Konfiguration, u. a. `spring.messages.basename`, `remo.frontend.base-url` und `remo.storage.directory`) sowie `locales/` (Deutsch/Englisch-Uebersetzungen fuer dynamischen, serverseitig erzeugten Text via Springs `MessageSource`: `messages.properties`, `messages_de.properties`, `messages_en.properties`, aufgeloest ueber `LocalizationConfig`; das Basis-Bundle `messages.properties` enthaelt die englischen Texte, passend zum englischen Fallback). Datenbank-Migrationen kommen hinzu, sobald die Persistenz implementiert ist.

### `ExampleTerms/`
Per .gitignore ausgeschlossen, wird zur Laufzeit im Arbeitsverzeichnis des Backends automatisch angelegt: eine JSON-Datei pro Termin, der unter `repository/` beschriebene, vorlaeufige Speicher.

### `ExampleObjects/`
Im Repository eingecheckt (anders als das per .gitignore ausgeschlossene `ExampleTerms/`), mit einer JSON-Datei pro Objekt vorbefuellt (entspricht den bei der Terminerstellung auswaehlbaren Objekten, z. B. "Wohnanlage Sonnenhof"); der vorlaeufige Speicher hinter `PropertyFileRepository`, konfiguriert ueber `remo.storage.properties.directory`. Eingecheckt statt gitignored, da es noch keinen Create-Endpunkt gibt, um den Ordner nach einem frischen Checkout erneut zu befuellen.

### `src/test/java/...`
JUnit-Tests, Struktur spiegelt `src/main/java/...` wider. Deckt die Version-/Localization-/CORS-Konfiguration sowie die komplette Termin-, Objekt- und Geocoding-Funktion ab (Repository, Service, Controller) – siehe `CLAUDE.md`, Abschnitt "Tests".

### `pom.xml`
Abhaengigkeiten und Build-Konfiguration (Maven). Haelt die einzige Quelle der Wahrheit fuer die Anwendungsversion (`<version>`), die ueber `GET /api/version` an das Frontend ausgeliefert wird.

## Konventionen
Siehe `CLAUDE.md` im Repository-Root, Abschnitt "Coding Conventions": Variablennamen auf Englisch, Kommentare ausschliesslich als JavaDoc (ein Satz, Englisch), Funktionen/Methoden stets wiederverwendbar gestalten.
