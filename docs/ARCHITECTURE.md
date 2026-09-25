# Architecture

How the MCC Construction app is put together, and how to add to it without
fighting it. Read this before building a screen.

## The short version

- **One app, every window size.** A phone gets a bottom navigation bar; a
  tablet or desktop window gets a navigation rail. Screens are written once
  and adapt — there are no separate phone and desktop copies.
- **Screens are stateless.** A screen takes a `UiState` and callbacks. A
  ViewModel owns the state and exposes it as a `StateFlow`.
- **ViewModels talk to repository interfaces, never to data directly.** Today
  every interface is backed by a `Fake…Repository` holding realistic demo data
  in memory. The backend team will add real implementations; no screen or
  ViewModel changes when they do.

```
 ┌────────────┐   UiState    ┌──────────────┐  Flow / suspend  ┌──────────────────────┐
 │  …Screen   │ ◀─────────── │  …ViewModel  │ ◀─────────────── │  …Repository         │
 │ (stateless)│ ───────────▶ │  StateFlow   │ ───────────────▶ │  (interface)         │
 └────────────┘  callbacks   └──────────────┘                  └──────────┬───────────┘
        ▲                                                                 │ implemented by
        │ …Route creates the ViewModel and                    ┌───────────┴───────────┐
        │ turns callbacks into navigation                     │ Fake…Repository (now) │
        │                                                     │ API-backed (Task 2)   │
                                                              └───────────────────────┘
```

## Package structure

```
com.mcc.construction
├── MainActivity.kt            Sets the theme and hosts AppRoot
├── MccApplication.kt          Holds the AppContainer — the one line to change for a real backend
├── di/
│   └── AppContainer.kt        Interface listing every repository, plus DemoAccount
├── data/
│   ├── model/                 Domain models, one file per area, matching the ERD
│   ├── repository/            Repository interfaces — what the UI may ask for
│   └── fake/                  In-memory implementations + FakeSeedData + FakeAppContainer
├── navigation/
│   ├── Routes.kt              A route constant for every screen
│   ├── NavItem.kt             NavMenus: which items each role sees, app bar titles
│   ├── AppRoot.kt             NavigationSuiteScaffold, app bar, signed-out guard
│   ├── AppNavHost.kt          The navigation graph — one entry per screen
│   └── AppViewModel.kt        Session, sync status and unread count for the shell
└── ui/
    ├── theme/                 Color, Type, Theme, Spacing, Tone
    ├── components/            Shared building blocks — reuse these (table below)
    ├── adaptive/              WindowLayout / LocalWindowLayout
    ├── preview/               @DevicePreviews and PreviewSurface
    ├── format/                Rand, dates, "12 min ago"
    ├── common/                appContainer() for ViewModel factories
    ├── ModelStyle.kt          Label, colour (Tone) and icon for every model enum
    └── screens/<area>/        One package per area: …Screen.kt + …ViewModel.kt
```

## The repository / fake pattern

### Models (`data/model`)

Plain data classes named after the ERD tables: `User`, `Role`, `Project`,
`Task`, `TaskAssignment`, `Attendance`, `DailyReport`, `Supplier`,
`Inventory`, `Document`, `SafetyInspection`, `IncidentReport`,
`Notification`, `Message`, `AuditLog` and `OfflineSync`.

- Field names follow the ERD columns (`projectName`, `quantityAvailable`,
  `dateReported`, …).
- Money is `BigDecimal`, dates are `java.time` (`LocalDate`, `LocalDateTime`).
  Core library desugaring makes `java.time` safe on the app's minSdk of 24.
- Status columns stored as text in the ERD are enums (`TaskStatus`,
  `ProjectStatus`, …) so a typo can't slip through.
- Models hold **no display text**. The words, colours and icons for an enum
  live in `ui/ModelStyle.kt`.

### Repositories (`data/repository`)

One interface per area. Reads return `Flow`, so screens update by themselves
when data changes; writes are `suspend` functions.

| Repository | Covers |
|---|---|
| `AuthRepository` | Sign in / out, password reset, the current `UserSession` (user + role) |
| `UserRepository` | Users and roles, role assignment, suspending and restoring accounts |
| `ProjectRepository` | Projects |
| `TaskRepository` | Creating tasks, assigning them, status updates |
| `AttendanceRepository` | Shifts, clock in / clock out, everyone's shifts on a day |
| `DailyReportRepository` | Submitting and approving daily reports |
| `InventoryRepository` | Materials, stock adjustments, suppliers |
| `DocumentRepository` | Project documents, uploading a new document or version |
| `SafetyRepository` | Checklist items, inspections, incident reports |
| `NotificationRepository` | Notifications, unread count, mark as read |
| `MessageRepository` | Conversations and direct messages |
| `AuditLogRepository` | Audit trail of administrative changes |
| `SyncRepository` | Online / offline, offline mode, the upload queue, sync now, history |
| `SettingsRepository` | On-device preferences |
| `SystemSettingsRepository` | Organisation-wide settings changed by administrators |

### Fakes (`data/fake`)

`FakeSeedData` holds the whole demo dataset — South African sites (Centurion,
Cape Town, Pietermaritzburg, Sandton, Umhlanga), suppliers, materials such as
cement and rebar, rand budgets — with ids that line up across tables.
`FakeSeedDataTest` checks every foreign key. Dates are worked out from the
current day, so "today's tasks" are always today's.

Each `Fake…Repository` keeps its data in a `MutableStateFlow`. Writes made
while the app is offline also land in the sync queue, so the offline path can
be demonstrated end to end: switch on **Offline mode** (Settings or the Offline
& sync screen), clock in or update a task, watch the indicator count go up,
switch offline mode off and tap **Sync now**.

The fakes also send the notifications the server will send, through
`FakeNotifier`, so the manager–worker loop works on one device:

1. A manager assigns a task (Project Details → **Assign a task**).
   `TaskRepository.createTask` and `assignTask` add it, and each assigned
   worker gets a notification.
2. The worker signs in, sees the notification and the task, and starts it.
3. `updateStatus` notifies the project's manager ("Sipho Dlamini started …"),
   and the manager dashboard's **Latest updates** and task counts change.

The same goes for approving a daily report (the author is told), a failed
checklist or incident (the project manager is told) and stock falling below
its minimum. Nobody is notified about their own actions. Role changes,
suspensions and system settings are written to the audit log.

### Wiring (`di/AppContainer`)

`AppContainer` lists every repository. `FakeAppContainer` provides the fakes,
and `MccApplication` creates it:

```kotlin
class MccApplication : Application() {
    val container: AppContainer by lazy { FakeAppContainer() }
}
```

**For the backend team:** write a class that implements `AppContainer` with
API-backed repositories, and change that one line. Return an empty
`demoAccounts` list and the demo sign-in shortcuts disappear on their own.

### Demo accounts

Any password works with the fake data.

| Email | Role | Person |
|---|---|---|
| `worker@mcc.co.za` | Worker | Sipho Dlamini, Site Operative |
| `manager@mcc.co.za` | Manager | Naledi Khumalo, Site Manager |
| `admin@mcc.co.za` | Administrator | Riaan van Wyk, Systems Administrator |

The sign-in screen lists them, so a role can be picked in one tap.

## How to add a new screen

Every screen in the screen list is built. For a new one, add its route first
(a `PlaceholderScreen` is fine while you work), then follow these steps.

### 1. State and ViewModel

`ui/screens/<area>/<Name>ViewModel.kt`:

```kotlin
data class TaskListUiState(
    val tasks: List<Task>? = null   // null = still loading
)

class TaskListViewModel(
    authRepository: AuthRepository,
    taskRepository: TaskRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TaskListUiState> = authRepository.session
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else taskRepository.observeTasksAssignedTo(session.user.userId)
        }
        .map { TaskListUiState(tasks = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                TaskListViewModel(container.authRepository, container.taskRepository)
            }
        }
    }
}
```

- Expose one `StateFlow<…UiState>`. User actions are plain functions
  (`onTaskClick`, `onSubmit`).
- Never pass a `NavController`, `Context` or string resource into a
  ViewModel. For "go somewhere" results, set a field in the state (see
  `LoginUiState.signedInAs`) and let the Route react.
- Errors are enums (see `LoginError`); the screen turns them into text.

### 2. Route and Screen

`ui/screens/<area>/<Name>Screen.kt`:

```kotlin
@Composable
fun TaskListRoute(
    onTaskClick: (Int) -> Unit,
    viewModel: TaskListViewModel = viewModel(factory = TaskListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TaskListScreen(state = state, onTaskClick = onTaskClick)
}

@Composable
fun TaskListScreen(
    state: TaskListUiState,
    onTaskClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(modifier = modifier) {
        // …
    }
}

@DevicePreviews
@Composable
private fun TaskListScreenPreview() {
    PreviewSurface { TaskListScreen(state = TaskListUiState(tasks = sampleTasks), onTaskClick = {}) }
}
```

- The **Route** is the only part that knows about the ViewModel and
  navigation. The **Screen** gets everything as parameters, which is what makes
  it previewable and testable.
- `@DevicePreviews` renders the phone and wide sizes side by side. Every screen
  needs it — add a second preview for any important state (empty, error,
  offline).

### 3. Navigation

- Add the route to `navigation/Routes.kt` if it isn't there.
- In `navigation/AppNavHost.kt`, point the route at your `…Route`, turning its
  callbacks into `navController.navigate(...)`.
- To put it in the bar or rail, add a `NavItem` to that role's `primary` or
  `secondary` list in `NavMenus`, and a title in `NavMenus.titleRes`.

### 4. Checklist before you commit

- [ ] Every user-facing string is in `res/values/` — `strings.xml`, or a file
      for your area such as `strings_worker.xml`, which keeps merge conflicts
      between team members down.
- [ ] Every meaningful icon has a `contentDescription`; decorative icons
      (next to a text label) use `null`.
- [ ] Tappable things are at least 48dp; primary actions use `PrimaryButton`
      (56dp).
- [ ] Colours come from `MaterialTheme.colorScheme` or a `Tone` — no hex values
      in screens.
- [ ] Previews render at both sizes.
- [ ] `./gradlew testDebugUnitTest` passes. Add a ViewModel test (see
      `FoundationScreensViewModelTest` for the pattern).

## Shared components (`ui/components`)

| Component | Use it for |
|---|---|
| `ScreenScaffold` | The frame for every screen: gutters, scrolling, width cap on wide windows. Pass `title` + `onBack` for a pushed screen (adds a back button and the sync indicator). |
| `SyncStatusIndicator` | The online / offline / syncing pill. Already in every app bar; drop it in anywhere else with no arguments. |
| `PrimaryButton` | The main action. 56dp tall, full width, `loading` state. |
| `AccentButton` | The single most important action on a screen (gold). One per screen at most. |
| `SecondaryButton` | Secondary actions. |
| `StatusChip` | A large, high-contrast status label — for headers and anything read at arm's length. |
| `StatusPill` | A compact status label for dense lists. |
| `EmptyState` | What a list shows when it has nothing — never leave a blank screen. |
| `LoadingState` | What a screen shows before its first data arrives. |
| `PlaceholderScreen` | Stand-in for a routed screen that isn't built yet. |
| `ListRow`, `LeadingIcon` | The standard list item. |
| `AppCard`, `SectionHeader`, `KeyValueRow`, `InfoRow`, `Banner` | Grouping and detail layouts. |
| `SwitchRow` | An on/off setting where the whole row is the touch target. |
| `AppTextField` | Labelled text field with error text and keyboard actions. |
| `SegmentedFilter` | A row of filter chips. |
| `MetricTile`, `MetricStrip`, `ProgressTrack` | Dashboard figures. |
| `ChoiceChip`, `ChoiceChipGroup` | Large, glove-friendly picks — use instead of a text field whenever the answer is one of a few options. |
| `PhotoAttachmentRow`, `rememberPhotoPicker` | Photo thumbnails with an "Add photo" tile, and the Android photo picker behind it (no storage permission needed). |
| `ActionFeedbackHost` | Wraps a screen and shows its `UiMessage` as a snackbar — every action gets a visible confirmation or error. |
| `ListDetailLayout`, `selectedOutline` | A list with a detail pane beside it on wide windows, and the outline that marks the open item. |
| `SearchField` | A search box with a clear button, for filtering a list as you type. |

ViewModels report one-off feedback as a `UiMessage` (in `ui/common`) — a
string resource plus arguments, so ViewModels stay free of Android resources —
and clear it when the screen calls back. `WorkerDashboardViewModel` and
`TaskDetailViewModel` show the pattern.

ViewModels that need "now" or "today" take the `Clock` from `AppContainer`
rather than reading the system clock, so tests can pin the date.

For a status, take the label and tone from `ui/ModelStyle.kt` so it matches
everywhere:

```kotlin
StatusChip(stringResource(task.status.labelRes), task.status.tone)
```

## Adaptive layout

`AppRoot` measures the window with Material 3 Adaptive
(`currentWindowAdaptiveInfo()`) and hands the result down as
`LocalWindowLayout`:

| Width | Navigation | Items |
|---|---|---|
| Compact (< 600dp, phones) | Bottom bar | The role's primary items |
| Medium and expanded (≥ 600dp) | Navigation rail | Primary items, plus as many secondary items as the window height fits |

| Role | Phone bar | Rail adds |
|---|---|---|
| Worker | Home, Tasks, Messages, Profile | Attendance, Daily report, Safety, Documents |
| Manager | Dashboard, Projects, Employees, Inventory, Reports | Tasks, Documents, Safety, Settings |
| Administrator | Dashboard, Users, Audit log, Profile | System settings, Employees, Reports, Settings |

Everything not in a role's phone bar is reachable from Profile (the account
button in the app bar, for managers).

Screens that change layout on wide windows read the layout rather than
measuring:

```kotlin
if (LocalWindowLayout.current.isExpanded) TwoColumns() else OneColumn()
```

The dashboards, Project Details and Employee Profile are working examples.
For list-and-detail screens — tasks, messages, projects, employees, inventory,
reports and user management — the list keeps a detail pane beside it on
expanded windows instead of pushing a new screen. `ListDetailLayout` does the
layout; the route passes a `detailPane` that shows the detail screen with
`embedded = true` (no app bar of its own, one column). See
`ProjectListRoute` in `AppNavHost`.

To see it live, run the app and resize the emulator window, or use Android
Studio's resizable device. The screenshot tooling can also force a width with
`adb shell am start -n com.mcc.construction/.MainActivity --es forceWidthClass expanded`.

## Theme and design rules

- **Brand colours** (`ui/theme/Color.kt`): navy `#173061` (primary) and gold
  `#DEAC2D` (accent) are sampled from the MCC logo; white `#FFFFFF`
  background, light grey `#F4F4F4` cards, green `#28A745` success, red
  `#DC3545` error.
- **Never put raw gold or green text on white** — both fail contrast
  outdoors. `Tone` gives each status a container, a figure colour for dots and
  icons, and a darker ink colour for text.
- **Type** (`ui/theme/Type.kt`): headings bold 24sp (`headlineSmall`),
  subheadings 20sp (`titleLarge`), body 16sp (`bodyLarge`).
- **Spacing** (`ui/theme/Spacing.kt`): use `itemGap`, `blockGap` and
  `sectionGap` between things; `touchTarget` (48dp) and `minTouch` (56dp) for
  sizes.
- **The logo** needs a light background — never place it on navy or a
  dark-theme surface.
- **Workers wear gloves:** icon-first, large targets, as little typing as
  possible, and keep worker flows to three taps or fewer.

## Fields beyond the ERD

The front end needs a few fields the ERD does not have yet. They are in the
models now; the backend team should add them to the database or the API
response.

| Model | Field | Why |
|---|---|---|
| `User` | `jobTitle`, `employeeNumber` | Shown on profile and employee screens |
| `User` | `isActive` | Suspended accounts (`false`) cannot sign in |
| `Document` | `category` (drawing, permit, contract, safety, report, other) | Filtering documents by kind |
| `SystemSettings` | whole type | Session timeout, sync interval, offline mode, daily checklist, low stock alerts |
| `DailyReport` | `status` (`SUBMITTED` / `APPROVED`) | Backs `Manager.approveReport()` in the class diagram |
| `Inventory` | `unitCost` | Rand values on material screens |
| `Notification` | `type` | Picks the icon and the screen a notification opens |
| `SafetyChecklistItem` | whole type | The checklist template a worker completes; the result is a `SafetyInspection` |
| `IncidentSeverity` | meaning of `SeverityLevel` | 1 = near miss, 2 = minor, 3 = moderate, 4 = serious, 5 = critical |
| `IncidentReport` | `photoUris` | Photos attached to an incident |
| `DailyReport` | `photoUris` | Photos attached to a daily report |
| `TaskPhoto` | whole type | Proof-of-work photos on a task (`Task.uploadImage()` in the class diagram) |
| `Message` | `isRead` | Unread badges in the chat list |
| `Weather` | stored values | The fixed values written to `DAILY_REPORTS.Weather` |

`PasswordHash` is deliberately not in `User`: it never leaves the server.

## Tests

```
./gradlew testDebugUnitTest
```

- `data/fake/…Test` — seed data integrity, auth, sync queue, notifications,
  attendance, and the manager–worker loop (`FakeManagerWorkflowTest`).
- `ui/screens/…ViewModelTest` — every screen ViewModel (sign-in, splash,
  foundation, worker, manager and administrator screens), run against the
  fakes. `ManagerWorkflowTest` walks the whole loop through the ViewModels.
- `navigation/NavMenusTest` — which items each role sees at each size.

Tests use `testContainer()` (a `FakeAppContainer` with a fixed clock and no
simulated delay) and `MainDispatcherRule`; `workerContainer()` and
`keepCollecting()` in `ui/screens/WorkerTestSupport.kt` sign in the demo worker
and keep `WhileSubscribed` state flowing; `managerContainer()` and `switchTo()`
in `ManagerTestSupport.kt` do the same for the manager and for switching
between accounts on shared data.
