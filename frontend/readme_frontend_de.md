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

- **`components/`** – Wiederverwendbare UI-Bausteine (z. B. Buttons, Karten, Formularfelder, Kalender-Widgets), die in mehreren Views eingesetzt werden. Ein Component pro Datei, Dateiname in PascalCase (z. B. `AppointmentFormDialog.vue`). Keine Business-Logik oder direkte API-Aufrufe in Components; dafuer `composables/` bzw. `services/` nutzen. App-weite Layout-Bausteine liegen in `components/layout/` (z. B. `AppHeader.vue`, `AppFooter.vue`, `NavigationMenu.vue`), die Kalender-Bausteine in `components/calendar/` (`AppCalendar.vue` kapselt `vue-cal`, daneben `CalendarToolbar.vue`, `CalendarDayHeader.vue`, `CalendarEventCard.vue`) und die Termin-Erstellungs-/Detail-Bausteine in `components/appointments/` (`AppointmentFormDialog.vue`, `AppointmentDetailDrawer.vue`, `AppointmentPropertySelect.vue`, `AppointmentMaterialInput.vue`, `AppointmentAiSuggestionBanner.vue`).
- **`views/`** – Seiten bzw. Routenziele der Anwendung, z. B. Terminuebersicht, Terminerstellung, Materialplanung. Eine View pro Route, Dateiname meist mit Suffix `View` (z. B. `CalendarView.vue`). Views orchestrieren Components und enthalten selbst moeglichst wenig eigene Logik.
- **`router/`** – Zentrale Konfiguration von Vue Router: Routen-Definitionen, Zuordnung zu Views, Navigation Guards und Lazy-Loading. Eine zentrale `index.ts`/`index.js` als Einstiegspunkt, Routen mit sprechenden `name`-Attributen.
- **`services/`** – Axios-basierte API-Clients fuer die Kommunikation mit dem Spring-Boot-Backend. Ein Service pro fachlicher Domaene (z. B. `versionService.ts`, `appointmentService.ts`), einfache Module mit einer exportierten Async-Funktion pro Operation.
- **`stores/`** – Globales State-Management, z. B. die geladenen Termine und welches Erstellungs-/Detail-Overlay gerade offen ist (`stores/appointments.ts`, die erste echte Nutzung von Pinia in dieser Anwendung – noetig, weil der "+Termin"-Button im Header und das Kalender-Overlay auf denselben Zustand zugreifen muessen, was ein einfaches Composable nicht ueber mehrere Mount-Punkte hinweg teilen kann). Kein direkter API-Zugriff im Store, dafuer `services/` verwenden.
- **`composables/`** – Wiederverwendbare Vue-Composition-Functions zur Kapselung von reaktiver Logik, die in mehreren Components oder Views benoetigt wird. Namenskonvention `useXxx` (z. B. `useCalendarNavigation`, `useCalendarAppointments`, `useAppointmentDeleteConfirmation`), enthaelt reine Logik ohne UI-Darstellung.
- **`data/`** – Fest hinterlegte Beispiel-/Referenzdaten als Platzhalter fuer ein noch nicht existierendes Backend (z. B. `exampleProperties.ts`, genutzt vom "Objekt"-Dropdown im Terminformular, solange es keine echte Objekt-API gibt).
- **`types/`** – Handgeschriebene TypeScript-Typen (z. B. `appointment.ts`) sowie Ambient-Shims fuer ungetypte Pakete (z. B. `vue-cal-shims.d.ts` fuer `vue-cal`, das keine eigenen Typdefinitionen mitliefert).
- **`utils/`** – Kleine, wiederverwendbare, framework-unabhaengige Hilfsfunktionen (z. B. lokalisierte Datumsformatierung, `appointmentSchedulingOptions.ts`s Generatoren fuer Tag-/Uhrzeit-/Dauer-/Wiederholungsoptionen), die nicht an Vue's Reaktivitaet gebunden sind und daher nicht in `composables/` gehoeren.
- **`i18n/`** – Zentrales `vue-i18n`-Setup; erstellt und exportiert die i18n-Instanz der Anwendung und fuehrt alle Uebersetzungsdateien aus `locales/` zusammen.
- **`locales/`** – Uebersetzungsdateien fuer statische UI-Texte, aufgeteilt in `de/` (aktive Standardsprache) und `en/` (Fallback-Sprache), eine JSON-Datei pro Feature-Namespace (z. B. `header.json`, `footer.json`, `appointments.json`). Siehe `CLAUDE.md`, Abschnitt "Coding Conventions", zur Regel, dass alle uebrigen Bezeichner auf Englisch bleiben.
- **`styles/`** – Component-Styles, ausgelagert aus den `.vue`-Dateien und ueber `<style scoped src="...">` eingebunden, spiegelt die Ordnerstruktur von `components/` (z. B. `styles/layout/` fuer `components/layout/`, `styles/calendar/` fuer `components/calendar/`, `styles/appointments/` fuer `components/appointments/`).
- **`__tests__/`** – Vitest-Unit-Tests (Component-, Composable-, Service- und Store-Tests), eine Spec-Datei pro Quelldatei, sowie eine gemeinsame `setup.ts` (polyfillt `window.matchMedia`, das jsdom nicht mitbringt, aber von PrimeVues Overlay-Components beim Mounten aufgerufen wird).

### `e2e/`
Playwright-End-to-End-Tests (z. B. `navigation.spec.ts`, `calendar.spec.ts`, `appointments.spec.ts`), ausfuehrbar ueber `npm run test:e2e`. Die Termin-Abläufe benoetigen das lokal laufende Backend (`mvn spring-boot:run` in `backend/`), da Playwrights `webServer`-Konfiguration nur den Frontend-Dev-Server startet.

### `package.json`
Abhaengigkeiten und Build-/Start-Skripte.

## Konventionen
Siehe `CLAUDE.md` im Repository-Root, Abschnitt "Coding Conventions": Variablennamen auf Englisch, Kommentare ausschliesslich als JSDoc/TSDoc (ein Satz, Englisch), Funktionen stets wiederverwendbar gestalten.
