# Screenshots

61 screenshots of the running debug build, captured 15 Aug 2026 from commit
`cfe5ad1` (`:app:assembleDebug`) on the Android Studio emulator, API 37.

The whole set was re-shot in one pass after the real client logo and brand
colours landed, so every image shows the sampled navy `#173061` and gold
`#DEAC2D` and there is no mix of runtimes or builds.

Every screen in the app is covered, across all three user roles and all three
window size classes.

## Breakpoints

The same build produces all three layouts — that is the point of the adaptive
navigation, so the prefix names the size class rather than the device.

| Prefix | Resolution | Density | Width | Navigation chrome |
|---|---|---|---|---|
| `phone-` | 1080×2337 | 420 dpi | 411 dp — compact | Bottom bar, 5 destinations |
| `tablet-` | 1536×2000 | 320 dpi | 768 dp — medium | NavigationRail, 10 destinations |
| `desktop-` | 1920×1152 | 320 dpi | 960 dp — expanded | Permanent drawer, labelled rows |

Images are uniform within each breakpoint. The status bar is cropped from every
image (63 px phone, 48 px tablet and desktop, measured from
`dumpsys window displays`) so the set is visually consistent — see "Known
imperfections" for why.

## Phone — 411 dp, all screens

### Entry
| File | Screen |
|---|---|
| `phone-01-splash` | Splash |
| `phone-02-login` | Login |
| `phone-03-login-validation-error` | Login, validation on empty submit |
| `phone-04-forgot-password` | Forgot Password |

### Worker
| File | Screen |
|---|---|
| `phone-05-worker-dashboard` | Worker Dashboard |
| `phone-06-worker-dashboard-quick-actions` | Worker Dashboard, scrolled to quick actions |
| `phone-07-task-list` | Task List |
| `phone-08-task-detail` | Task Detail |
| `phone-09-attendance` | Attendance, not clocked in |
| `phone-10-attendance-clocked-in` | Attendance, after clocking in |
| `phone-11-messages` | Messages |
| `phone-12-chat` | Chat |
| `phone-13-profile` | Profile |
| `phone-14-profile-more` | Profile, secondary destinations |
| `phone-15-documents` | Documents |
| `phone-16-sync-status` | Offline & Sync Status |
| `phone-17-safety-checklist` | Safety Checklist |
| `phone-18-daily-report` | Daily Report |
| `phone-19-settings` | Settings |

### Manager
| File | Screen |
|---|---|
| `phone-20-manager-dashboard` | Manager Dashboard |
| `phone-21-manager-dashboard-projects` | Manager Dashboard, projects section |
| `phone-22-projects-list` | Projects List |
| `phone-23-project-detail` | Project Detail |
| `phone-24-project-detail-tasks` | Project Detail, tasks section |
| `phone-25-employees` | Employees |
| `phone-26-employee-profile` | Employee Profile |
| `phone-27-manager-task-list` | Task List, manager role |
| `phone-28-inventory` | Inventory |
| `phone-29-material-detail` | Material Detail |
| `phone-30-reports` | Reports History |
| `phone-31-reports-incidents` | Reports, incidents section |

### Administrator
| File | Screen |
|---|---|
| `phone-32-admin-dashboard` | Admin Dashboard |
| `phone-33-admin-dashboard-activity` | Admin Dashboard, recent activity |
| `phone-34-user-management` | User Management |
| `phone-35-user-management-roles` | User Management, role controls |
| `phone-36-audit-log` | Audit Log |
| `phone-37-notifications` | Notifications |

## Tablet — 768 dp, navigation rail

`tablet-01-login`, `tablet-02-worker-dashboard`, `tablet-03-task-list`,
`tablet-04-task-detail`, `tablet-05-attendance`, `tablet-06-manager-dashboard`,
`tablet-07-projects-list`, `tablet-08-project-detail`, `tablet-09-employees`,
`tablet-10-inventory`, `tablet-11-reports`, `tablet-12-notifications`

## Desktop — 960 dp, permanent drawer

`desktop-01-login`, `desktop-02-worker-dashboard`, `desktop-03-task-list`,
`desktop-04-task-detail`, `desktop-05-attendance`,
`desktop-06-manager-dashboard`, `desktop-07-projects-list`,
`desktop-08-project-detail`, `desktop-09-employees`, `desktop-10-inventory`,
`desktop-11-reports`, `desktop-12-notifications`

## Known imperfections

- **The status bar is cropped, not styled.** SystemUI demo mode (which pins the
  clock to 09:30 with full battery and signal) stopped being honoured partway
  through capture — the broadcast started returning `result=0` after an emulator
  reboot and never recovered. Rather than ship a set with disagreeing clocks,
  the status bar is cropped from all 61 images.
- **The gesture pill is still present** at the bottom of each image.
- **The status bar now shows a pinned 09:30 clock**, because demo mode held for
  this run. It is still cropped from every image, so the set matches the earlier
  ones and does not depend on demo mode having behaved.
- Tablet and desktop cover a representative subset of screens, not all 37 — they
  exist to evidence the navigation chrome and how content reflows.

## Findings from this capture

- **Fixed: the login CTA fell below the fold on a short landscape window.** At
  960×600 dp the Sign In button was off-screen and needed a scroll to reach.
  The login screen now measures its own height and, below 720 dp, lays the
  brand block out horizontally and puts the three role options in a single row
  — about 250 dp reclaimed, enough to bring the button back into view. The
  phone layout is unchanged. Compare `desktop-01-login` (after the fix) with
  the taller stacked form still used on `phone-02-login`.

## Reproducing

Capture is scripted and drives the app by reading the accessibility tree
(`uiautomator dump`) rather than hard-coded coordinates, so the same script runs
unchanged at every window size. Notes for anyone repeating it:

- Set the size class with `wm size` + `wm density`. Density is the useful lever:
  at a fixed 1080 px width, density 200 yields 864 dp (expanded) with no
  scaling cost, where forcing a large `wm size` makes SystemUI ANR on a
  software-rendered emulator.
- Reboot the emulator after each geometry change and wait for
  `sys.boot_completed=1` plus a further ~20 s; the first launch after a reboot
  is otherwise too cold and the app never reaches the foreground.
- A failed `uiautomator dump` leaves the *previous* screen's XML on the device.
  Delete `/data/local/tmp/ui.xml` before each dump or you will silently query
  the wrong screen.
- Dismiss the IME between form fields. On a short window the keyboard covers the
  next field, so the tap lands on the keyboard, focus never moves, and the text
  is appended to the previous field.
- `screencap` cannot write to `/sdcard` on Android 17 — use `/data/local/tmp`.
- Launch with `am start`, not `monkey`; `monkey` can trip a recents gesture that
  sends the app straight to the background.
- Install from `app/build/outputs/apk/debug/app-debug.apk`. A stale copy also
  sits under `app/build/intermediates/apk/debug/` and is easy to install by
  mistake, which silently tests the previous build.
