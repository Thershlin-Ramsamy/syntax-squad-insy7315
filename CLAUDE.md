# MCC Construction Mobile Management System — Task 1 Prototype

## What this is
INSY7315 WIL Task 1 prototype for a client called MCC Construction. Native Android
app, built in Android Studio. No real backend, no database — mock/hardcoded data
only. Must demonstrate both a "mobile" (compact) and a "desktop" (expanded) layout
of the interface — this is graded, and it's handled here as ONE adaptive app, not
two separate builds.

## Stack
- Kotlin
- Jetpack Compose + Material 3
- Material 3 Adaptive (window size classes: compact / medium / expanded) — this is
  how the mobile-vs-desktop requirement is satisfied: one codebase that responds to
  window size, not a second project
- Navigation Compose for routing between screens
- All data lives in a hardcoded mock data layer — no network calls, no Room, no
  ViewModel wired to a real API

This is likely to become the real base for Task 2 as well now that the project is
confirmed Android-only — so build it like it's meant to last, not like throwaway
prototype code.

## User roles
- **Administrator** — user management, system settings, audit logs
- **Manager** — projects, task assignment, inventory, reports, employees, attendance
- **Worker** — today's tasks, clock in/out, daily reports, safety checklists, messages

## Brand tokens
- Primary: Navy Blue `#0B3D91`
- Accent: Construction Orange `#F9A826`
- Background: White `#FFFFFF`
- Card background: Light Grey `#F4F4F4`
- Success: Green `#28A745`
- Error/Danger: Red `#DC3545`
- Headings: bold, ~24sp / Subheading ~20sp / Body ~16sp
- Rounded corners, Material icons

## Navigation (adaptive, not two separate menus)
- **Compact** (phone width): bottom navigation bar — Home, Projects, Tasks,
  Messages, Profile
- **Expanded** (desktop/tablet width): NavigationRail or permanent side drawer with
  the fuller Manager/Admin menu — Dashboard, Employees, Inventory, Reports,
  Documents, Safety, Settings
- Drive this with `currentWindowAdaptiveInfo().windowSizeClass` (or
  `NavigationSuiteScaffold`) — don't hand-roll separate compact/expanded screens

## Full screen list (build in small batches, not all at once)
Splash, Login, Forgot Password, Worker Dashboard, Manager Dashboard, Projects List,
Project Details, Task List, Task Details, Attendance (Clock In/Out), Employees,
Employee Profile, Inventory, Material Details, Daily Report (submit), Reports
History, Messages (chat list), Chat, Documents, Safety Checklist, Incident Report,
Notifications, Profile, Settings, Offline Sync status.

## Design notes specific to this app
- Workers may be wearing gloves on a construction site — big touch targets, minimal
  typing, icon-forward UI.
- High contrast, needs to be readable outdoors.
- Offline-first: always show a visible online/offline + sync status indicator.
- Keep worker flows to 3 taps or fewer where possible.

## Git workflow
- **Never commit directly to `main`.** All work happens on a feature branch,
  merged into `main` via Pull Request.
- **Branch naming**: `feature/<short-description>` for new functionality,
  `fix/<short-description>` for bug fixes — e.g. `feature/initial-screens`,
  `fix/attendance-clockout-bug`.
- **Commit messages** follow Conventional Commits: `type: summary`, imperative
  mood, no trailing period. Types: `feat`, `fix`, `refactor`, `docs`, `style`,
  `test`, `chore`.
  - Good: `feat: add login screen with form validation`
  - Bad: `stuff`, `updates`, `wip`
- Keep commits scoped to one logical change. A single prompt's worth of related
  screens/features landing in one commit is fine — don't bundle unrelated work
  together, and don't split one cohesive feature into artificially tiny commits.
- Open a Pull Request from the feature branch into `main` once work is ready,
  even when self-merging for now — this keeps history clean and reviewable as
  the team grows.

## How to test both sizes
- Phone: BlueStacks at its default window size.
- Desktop/expanded: resize the BlueStacks window wider, or use Android Studio's
  resizable emulator profile if it becomes available — the same app should visibly
  respond, live.
