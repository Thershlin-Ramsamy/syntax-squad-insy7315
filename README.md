# MCC Construction Mobile Management System

**Syntax Squad | INSY7315 Work Integrated Learning Project**

A native Android application that helps MCC Construction manage its sites,
people and materials. Site workers, managers and administrators each get a
workspace tailored to their role, delivered through a single adaptive app that
works on phones, tablets and desktop-sized windows.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology](#technology)
- [Getting Started](#getting-started)
- [Demo Accounts](#demo-accounts)
- [Demonstration Walkthrough](#demonstration-walkthrough)
- [Adaptive Layout](#adaptive-layout)
- [Offline Support](#offline-support)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Contributing](#contributing)

---

## Overview

The system replaces paper-based site coordination with a connected mobile
workflow. Managers plan and assign work, workers receive and update it from
site, and administrators control access and system policy. Every role works
from the same shared data, so progress recorded on site is visible to the
office immediately.

This release is a front-end prototype. All data is held in memory through a
repository layer designed to be replaced by a production API without changes
to the user interface.

## Features

### Worker

- Daily dashboard with today's tasks, site and clock-in status
- Clock in and clock out with shift history and weekly hours
- Task list and task details with status updates and proof-of-work photos
- Daily report submission with weather, progress, problems and photos
- Safety checklist and incident reporting with severity levels
- Messages, notifications and profile

### Manager

- Dashboard with project progress, tasks by status, today's attendance,
  alerts and the latest updates from site
- Projects list and project details covering budget, dates, team, tasks,
  materials, documents and daily reports
- Task assignment to one or more workers, with automatic notifications
- Employee directory with search, attendance status and individual profiles
- Inventory with low-stock highlighting, supplier details and stock
  adjustments
- Reports history covering daily reports, safety inspections and incidents,
  with report approval
- Document library with categories, version history and uploads

### Administrator

- Dashboard with account totals, suspended accounts, recent activity and
  key settings
- User management with role changes, account suspension and restoration
- Audit log of administrative actions, searchable by person and action
- System settings for session timeout, background sync, offline mode, the
  daily safety checklist and low-stock alerts

## Technology

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| User interface | Jetpack Compose, Material 3 |
| Adaptive layout | Material 3 Adaptive, Navigation Suite |
| Navigation | Navigation Compose |
| Architecture | Repository pattern, one ViewModel per screen, `StateFlow` state |
| Images | Coil |
| Minimum Android version | Android 7.0 (API 24) |

## Getting Started

### Prerequisites

- Android Studio (latest stable release)
- An Android emulator or physical device running Android 7.0 or later

### Running the App

1. Clone the repository.
2. Open the project in Android Studio and allow Gradle to sync.
3. Select the `app` run configuration.
4. Run it on an emulator or a connected device.

To build from the command line:

```bash
./gradlew assembleDebug
```

## Demo Accounts

Any password is accepted. The sign-in screen also lists these accounts, and
selecting one fills in the details automatically.

| Role | Email |
| --- | --- |
| Worker | `worker@mcc.co.za` |
| Manager | `manager@mcc.co.za` |
| Administrator | `admin@mcc.co.za` |

## Demonstration Walkthrough

The following sequence shows the connected workflow between roles on a single
device.

1. **Assign the work.** Sign in as the manager, open a project and select
   **Assign a task**. Enter a title and deadline, select Sipho Dlamini and
   confirm.
2. **Receive and update.** Sign out and sign in as the worker. The new task
   appears in the task list alongside a notification from the manager. Open
   the task and select **Start task**.
3. **Track progress.** Sign in as the manager again. The dashboard's
   **Latest updates** panel shows that the task has started, and the task
   totals reflect the change.

Further interactions to explore:

- Approving a daily report in **Reports** notifies the worker who submitted it.
- Recording stock use that takes a material below its minimum level alerts the
  project manager.
- As the administrator, changing a role, suspending an account or saving a
  system setting adds an entry to the audit log. Suspended users cannot sign
  in.

## Adaptive Layout

The application uses a single codebase that responds to the available window
width.

| Window | Navigation | Layout |
| --- | --- | --- |
| Phone | Bottom navigation bar | Single column |
| Tablet and desktop | Navigation rail | Multi-column dashboards and list with detail panes |

To view the expanded layout, resize the emulator window or use a tablet or
resizable device profile. The interface updates immediately.

## Offline Support

A sync status indicator is visible on every main screen. Enable **Offline
mode** from Settings or the Offline and Sync screen to simulate working
without a connection. Changes such as clock-ins, reports and task updates are
queued on the device and uploaded when offline mode is switched off and a sync
is run.

## Testing

Run the unit test suite with:

```bash
./gradlew testDebugUnitTest
```

The suite covers the demo data, the repository layer and every screen
ViewModel, including an end-to-end test of the manager and worker workflow.

## Project Structure

```
app/src/main/java/com/mcc/construction
├── data/
│   ├── model/         Domain models aligned with the database design
│   ├── repository/    Repository interfaces used by the interface layer
│   └── fake/          In-memory implementations and demo data
├── di/                Application container for repository wiring
├── navigation/        Routes, navigation graph and role-based menus
└── ui/
    ├── components/    Shared interface components
    ├── theme/         Brand colours, typography and spacing
    └── screens/       Screens and ViewModels, grouped by feature
```

## Documentation

| Document | Contents |
| --- | --- |
| [Architecture guide](docs/ARCHITECTURE.md) | Package structure, repository pattern, adding a screen, shared components and fields beyond the database design |
| [Screenshots](docs/screenshots) | Captured screens at phone and desktop sizes |
| [Screenshot tools](tools/screenshots) | Scripts for recapturing the screenshots |

## Contributing

- Create a branch from `main` using `feature/<short-description>` or
  `fix/<short-description>`.
- Write commit messages in the Conventional Commits format, for example
  `feat: add login screen with form validation`.
- Keep each commit focused on one logical change.
- Open a pull request into `main` once the work is ready for review.

---

Developed by **Syntax Squad** for MCC Construction as part of INSY7315.
