# Screenshots

Captured from the running debug build (`:app:assembleDebug`) on 14 Aug 2026,
at commit `05a1623`.

Filenames are prefixed with the window size class the app resolved to, because
the same build produces all three layouts — that is the point of the adaptive
navigation, and the prefix is what makes the difference visible at a glance.

## Breakpoints

| Prefix | Width | Navigation chrome | Captured on |
|---|---|---|---|
| `compact-` | 360 dp | Bottom navigation bar, 5 destinations | BlueStacks, 540×960 @ 240 dpi |
| `medium-` | 720 dp | NavigationRail, 10 destinations | BlueStacks, 1080×1080 override @ 240 dpi |
| `expanded-` | 864 dp | Permanent drawer, labelled rows | Android Studio emulator, 1080×2400 @ 200 dpi |

`expanded-01-splash.png` is the exception: it was taken at 2560×1600 @ 240 dpi
(1707 dp) before that geometry was abandoned — see "Reproducing" below.

## Index

### Compact — 360 dp

| File | Shows |
|---|---|
| `compact-01-login.png` | Login, top of form — brand mark, role selector, Worker preselected |
| `compact-02-login-full-form.png` | Login scrolled to reveal Sign In and Forgot password |
| `compact-03-login-validation-error.png` | Validation on empty submit. "s.dlamini" is placeholder text, not a value |
| `compact-04-login-username-entered.png` | Username filled, focus ring on the field |
| `compact-05-worker-dashboard.png` | Worker dashboard — bottom bar, sync chip, metric tiles, Clock In, safety banner |
| `compact-06-task-list.png` | Task list — segmented status filter, status pills, high-priority flags |
| `compact-07-task-detail.png` | Task detail — nav arg resolved, back arrow, sticky action, **no bottom bar** (pushed screen) |
| `compact-08-app-icon-on-launcher.png` | Adaptive launcher icon on the BlueStacks home screen |

### Medium — 720 dp

| File | Shows |
|---|---|
| `medium-01-login.png` | Login — form fits without scrolling, content capped rather than stretched |
| `medium-02-login-manager-selected.png` | Login with Manager role selected |
| `medium-03-manager-dashboard-navigation-rail.png` | Manager dashboard — **NavigationRail** replaces the bottom bar, fuller 10-item menu |

### Expanded — 864 dp

| File | Shows |
|---|---|
| `expanded-01-splash.png` | Splash — centred, not stretched, at 1707 dp |
| `expanded-02-login.png` | Login — content centred at max width |
| `expanded-03-login-manager-selected.png` | Login filled, Manager selected, tablet split keyboard |
| `expanded-04-manager-dashboard-permanent-drawer.png` | Manager dashboard — **permanent drawer** with header and labelled rows |
| `expanded-05-notifications-single-top-bar.png` | Notifications inside the drawer shell with a **single** top bar |

## What these do *not* cover

Roughly 20 of the 28 screens were never rendered. Not captured:

- **Worker** — Attendance, Daily Report, Safety Checklist, Incident Report, Messages, Chat
- **Manager** — Projects List, Project Detail, Employees, Employee Profile, Inventory, Material Detail, Reports
- **Shared** — Profile, Settings, Documents, Sync Status, Forgot Password
- **Administrator** — Admin Dashboard, User Management, Audit Log (the entire role is unverified)

No interaction was exercised either: clock in/out, ticking the safety checklist,
marking a task complete, sending a chat message, changing a user's role or
suspending them, "Sync now", and sign-out are all untested.

No crashes appeared in logcat across everything that *was* run.

## Reproducing

BlueStacks exposes adb on `127.0.0.1:5555`. The Android Studio emulator needs
`wm density` rather than `wm size` to reach the expanded breakpoint: at the
native 1080 px width, density 200 gives 1080 ÷ 200 × 160 = **864 dp**, just over
the 840 dp threshold, with no scaling cost. Forcing `wm size 2560x1600` also
reaches expanded but makes SystemUI ANR repeatedly on a software-rendered
emulator, which blanks the screen.

Two further gotchas: `screencap` cannot write to `/sdcard` on Android 17 — use
`/data/local/tmp` — and launching via `monkey` can trip a recents gesture that
sends the app straight to the background, so prefer `am start`.

The built APK lands in `app/build/intermediates/apk/debug/`, not
`app/build/outputs/`.
