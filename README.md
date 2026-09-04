# [Real-Estate-Maintenance-Optimizer](https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer)

- [Real-Estate-Maintenance-Optimizer](#real-estate-maintenance-optimizer)
  - [English](#english)
    - [📌 Project Overview](#-project-overview)
    - [📌 Prototype](#-prototype)
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
  - [German](#german)
    - [📌 Projektübersicht](#-projektübersicht)
    - [📌 Prototyp](#-prototyp)
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

#### 2. ➕ Appointment Creation (`Term creation`)
- **Standard Input Form:** Fast manual entry interface.
- **Voice/Audio Feature:** Hands-free appointment logging designed for busy work environments.

#### 3. 🗺️ Location-Based Planning (`Automatic planning Place`)
- Smart location grouping to **minimize driving time** between service calls.
- Dynamically slots smaller tasks near the previous job site into the daily schedule.

#### 4. ⏱️ Time-Based Planning (`Automatic planning Time`)
- Automated scheduling and rescheduling maintaining safe buffer times.
- **Predefined Templates:** Instant creation of standardized services (e.g., basement cleaning, grounds maintenance).

#### 5. 📦 Material Planning (`Material planning`)
- Attach required tools and materials directly to jobs to prevent extra supply runs.

#### 6. 🚗 Route Optimization (`Fast route planning`)
- Direct **Google Maps / Apple Maps integration** within the appointment summary.

#### 7. 🔒 Fixed Appointments (`Unrescheduled terms`)
- Lock specific appointments so automated AI scheduling leaves them untouched.

#### 8. 🔄 Recurring Appointments (`Recurring appointments`)
- Set up automatic recurring schedules (e.g., maintenance every 3 months) without manual oversight.

---

## 📋 Backlog & Future Features (`Backlog/Issues`)

- Native mobile porting to **Android** & **iOS**

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

#### 2. ➕ Terminerstellung
- **Eingabemaske:** Schnelle manuelle Erstellung von Aufträgen.
- **Spracheingabe / Audio-Funktion:** Freihändige Terminerstellung per Sprachbefehl – ideal, wenn man gerade die Hände voll hat.

#### 3. 🗺️ Ortsabhängige Planung
- Intelligent abgestimmte Routen zur **Reduzierung von Fahrzeiten** zwischen Einsatzorten.
- Effizientes Einschieben kleinerer Aufträge in der Nähe des vorherigen Einsatzorts.

#### 4. ⏱️ Zeitbasierte Planung
- Automatische Terminvergabe und Umplanung mit konfigurierbarem zeitlichem Abstand.
- **Vordefinierte Vorlagen:** Schnelles Anlegen wiederkehrender Aufgaben (z. B. Kellerreinigung, Pflege der Außenanlage).

#### 5. 📦 Materialplanung
- Zuordnung von benötigtem Material direkt zum Termin, um unnötige Fahrten zum Lager oder Baumarkt zu vermeiden.

#### 6. 🚗 Routenoptimierung 
- Direkt integrierter **Maps-Link** in der Kurzübersicht für den schnellen Start der Navigation.

#### 7. 🔒 Unverschiebbare Termine 
- Fixierte Termine werden durch den KI-Optimierungsalgorithmus nicht verändert.

#### 8. 🔄 Wiederkehrende Termine
- Automatische Daueraufträge (z. B. Inspektion alle 3 Monate), ohne manuell daran denken zu müssen.
---

### 📋 Backlog & Zukünftige Features (`Backlog/Issues`)

- Native App-Portierung für **Android** und **iOS**

------------

## 🧰 Tech Stack

### 🖥️ Frontend
* **Framework:** Vue.js 3
* **UI Components:** PrimeVue 4
* **Calendar:** Vue.cal
* **State & Routing:** Vue Router, Axios

### ☁️ Backend
* **Language & Framework:** Java, Spring Boot 3
* **Authentication & Security:** Spring Security, JJWT (JSON Web Token)
* **Data Access & Mail:** Spring Data JPA, Spring Mail

### 🗄️ Datenbank
* **Database:** PostgreSQL

### 🤖 AI / Intelligence
* **Framework:** LangChain4j
