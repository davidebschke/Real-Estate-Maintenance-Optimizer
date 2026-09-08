# Frontend

## Zweck
Enthaelt die komplette Vue.js 3 Anwendung des Real-Estate-Maintenance-Optimizers (Kalenderansicht, Terminerstellung, Routenoptimierung).

## Technologien
Vue.js 3, PrimeVue 4, Vue.cal (Kalenderansicht), Vue Router, Axios, Leaflet (Routenoptimierung)

## Ordnerstruktur

### `public/`
Statische Dateien, die vom Build-Prozess unveraendert uebernommen werden, z. B. `favicon.ico`, `index.html` oder statische Bilder. Diese Dateien durchlaufen keinen Bundling-Prozess. Assets, die im Code importiert oder wiederverwendet werden, gehoeren nicht hierher, sondern in einen Assets-Ordner unter `src/`.

### `src/`
Wurzelverzeichnis des eigentlichen Anwendungscodes, unterteilt in:

- **`components/`** – Wiederverwendbare UI-Bausteine (z. B. Buttons, Karten, Formularfelder, Kalender-Widgets), die in mehreren Views eingesetzt werden. Ein Component pro Datei, Dateiname in PascalCase (z. B. `AppointmentCard.vue`). Keine Business-Logik oder direkte API-Aufrufe in Components; dafuer `composables/` bzw. `services/` nutzen.
- **`views/`** – Seiten bzw. Routenziele der Anwendung, z. B. Terminuebersicht, Terminerstellung, Materialplanung. Eine View pro Route, Dateiname meist mit Suffix `View` (z. B. `AppointmentOverviewView.vue`). Views orchestrieren Components und enthalten selbst moeglichst wenig eigene Logik.
- **`router/`** – Zentrale Konfiguration von Vue Router: Routen-Definitionen, Zuordnung zu Views, Navigation Guards und Lazy-Loading. Eine zentrale `index.ts`/`index.js` als Einstiegspunkt, Routen mit sprechenden `name`-Attributen.
- **`services/`** – Axios-basierte API-Clients fuer die Kommunikation mit dem Spring-Boot-Backend. Ein Service pro fachlicher Domaene (z. B. `appointmentService`, `materialService`), zentrale Axios-Instanz mit Basis-URL und Interceptoren (z. B. fuer JWT).
- **`stores/`** – Globales State-Management, z. B. angemeldeter Benutzer, geladene Termine, aktuelle Filtereinstellungen. Ein Store pro fachlichem Bereich (z. B. `authStore`, `appointmentStore`); kein direkter API-Zugriff im Store, dafuer `services/` verwenden.
- **`composables/`** – Wiederverwendbare Vue-Composition-Functions zur Kapselung von reaktiver Logik, die in mehreren Components oder Views benoetigt wird. Namenskonvention `useXxx` (z. B. `useAppointmentDuration`), enthaelt reine Logik ohne UI-Darstellung.

### `package.json`
Abhaengigkeiten und Build-/Start-Skripte.

## Konventionen
Siehe `CLAUDE.md` im Repository-Root, Abschnitt "Coding Conventions": Variablennamen auf Englisch, Kommentare ausschliesslich als JSDoc/TSDoc (ein Satz, Englisch), Funktionen stets wiederverwendbar gestalten.
