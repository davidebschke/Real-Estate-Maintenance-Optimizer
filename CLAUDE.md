# CLAUDE.md

This file provides Claude Code (and other AI assistants) with the necessary context and binding guidelines for working in this repository.

## 1. Project Overview

**Real-Estate-Maintenance-Optimizer** is an intelligent system for appointment and route scheduling for tradespeople and field service technicians in property management.

### 1.1 Core Goal
Optimize property management maintenance by:
- reducing travel distances,
- better coordinating appointments based on location and duration,
- accounting for material trips and required material,
- estimating the potential duration of new appointments based on similar previous appointments with AI support.

Result: time and cost savings for the company, lower operating costs, happier tenants.

### 1.2 Prototype
https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer-Prototype

## 2. Tech Stack

### 2.1 Frontend
| Area              | Technology        |
|--------------------|-------------------|
| Framework          | Vue.js 3          |
| UI Components      | PrimeVue 4        |
| Calendar View      | Vue.cal           |
| Routing            | Vue Router        |
| HTTP Client        | Axios             |

### 2.2 Backend
| Area                     | Technology                  |
|--------------------------|-----------------------------|
| Language / Framework     | Java 25 (LTS), Spring Boot 3 |
| Authentication           | Spring Security, JJWT (JSON Web Token) |
| Data Access              | Spring Data JPA             |
| Mail Sending             | Spring Mail                 |

### 2.3 Databases

The architecture uses a polyglot persistence approach with two separate databases:

| Area                             | Technology  | Hosting               |
|-----------------------------------|-------------|------------------------|
| Relational (User / Auth)         | PostgreSQL  | Supabase / Neon.tech   |
| NoSQL (Appointments & Objects)    | MongoDB     | MongoDB Atlas          |

- **PostgreSQL** stores user and authentication data, accessed via Spring Data JPA.
- **MongoDB** stores appointments and their associated objects.

### 2.4 AI / Intelligence
| Area       | Technology   |
|------------|--------------|
| Framework  | LangChain4j  |

## 3. Repository Status

Status of this file: project documentation exists (`README.md`, `LICENSE`, `SECURITY.md`, `.github/ISSUE_TEMPLATE`). The backend has a minimal Maven/Spring Boot skeleton (`pom.xml`, application entry point); there is **no source code** yet for the frontend, and no business logic yet in the backend.

Current branch status is not tracked in this file — run `git branch -a` for the up-to-date list of local and remote branches.

Once code is added, Section 4 (Folder Structure) and Section 7 (Build & Run) must be replaced with the actual state.

## 4. Recommended Folder Structure (Monorepo)

```
Real-Estate-Maintenance-Optimizer/
├── frontend/                      # Vue.js 3 application
│   ├── src/
│   │   ├── components/            # reusable UI building blocks
│   │   ├── views/                 # pages / route targets
│   │   ├── router/                # Vue Router configuration
│   │   ├── services/              # Axios API clients
│   │   ├── stores/                # state management
│   │   └── composables/           # reusable Vue Composition functions
│   ├── public/
│   └── package.json
├── backend/                       # Spring Boot application
│   ├── src/main/java/...          # Java source code (controller, service, repository, entity)
│   ├── src/main/resources/        # application.yml, migrations
│   ├── src/test/java/...          # JUnit tests
│   └── pom.xml (or build.gradle)
├── .claude/
│   ├── rules/                      # binding rules extracted from this file (coding conventions, git workflow, ...)
│   └── skills/                     # project-specific Claude skills
├── .github/
│   └── ISSUE_TEMPLATE/
├── README.md
├── SECURITY.md
└── CLAUDE.md
```

Backend package structure (proposal, classic layered approach):
```
com.<organization>.realestatemaintainceoptimizer
├── controller/     # REST endpoints
├── service/        # business logic
├── repository/     # Spring Data JPA repositories
├── entity/         # JPA entities
├── dto/            # data transfer objects
├── security/       # JWT / Spring Security configuration
├── ai/             # LangChain4j integration
└── config/         # general configuration
```

### 4.1 `.claude` Directory

- `.claude/rules/` — binding project rules extracted from this file, one topic per file (e.g. `coding-conventions.md`, `git-workflow.md`, `testing.md`, `documentation.md`). Each section below references its corresponding rule file.
- `.claude/skills/` — project-specific Claude skills. Currently empty (`.gitkeep`).

## 5. Coding Conventions

Binding coding conventions (variable names, comments, function/method design, commit messages, branch naming) are defined in [`.claude/rules/coding-conventions.md`](.claude/rules/coding-conventions.md). This file is binding for every code change in this repository.

## 6. Features & Epics (MVP)

1. **Appointment Overview** – short view (calendar) and detailed view (full job context).
2. **Appointment Creation** – manual input via form only in the MVP.
3. **Location-Based Planning** – automatic grouping by location to reduce travel time.
4. **Time-Based Planning** – automatic scheduling with buffer times, templates for standard tasks (e.g. basement cleaning).
5. **Material Planning** – assigning required material to an appointment to avoid extra trips.
6. **Route Optimization** – runs exclusively via Leaflet.
7. **Fixed Appointments** – locking individual appointments against automatic AI rescheduling.
8. **Recurring Appointments** – automatic standing orders (e.g. inspection every 3 months).

### 6.1 Backlog
- Native app port for Android & iOS.
- Voice/audio capture for appointment creation (hands-free use).

## 7. Build & Run

Backend: `mvn spring-boot:run` (requires Java 25 and Maven; not yet verified on all developer machines — check locally before relying on it).
Frontend: not yet defined.

## 8. Tests

Binding testing conventions are defined in [`.claude/rules/testing.md`](.claude/rules/testing.md).

## 9. Git Workflow

Binding Git workflow (remote, main branch, code owner, branch/PR process) is defined in [`.claude/rules/git-workflow.md`](.claude/rules/git-workflow.md).

## 10. Notes for Claude Code

- Binding documentation conventions are defined in [`.claude/rules/documentation.md`](.claude/rules/documentation.md). `CLAUDE.md` must be reviewed and updated every time something changes in the repository.
- All files under `.claude/rules/` are binding in addition to this document, including the coding conventions (Section 5), testing conventions (Section 8), and Git workflow (Section 9) referenced above.
- `.claude/skills/` holds project-specific Claude skills (currently empty).
- Review security-relevant changes (Spring Security, JWT) especially carefully and never merge without tests.
