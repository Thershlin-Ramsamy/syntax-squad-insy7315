# syntax-squad-insy7315
Syntax Squad — INSY7315 Group Project

MCC Construction Mobile Management System: a native Android app (Kotlin,
Jetpack Compose, Material 3) for site workers, managers and administrators.
One adaptive app serves both layouts — a bottom navigation bar on phones and
a navigation rail on tablet and desktop-sized windows.

## Running it

1. Open the project in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator or device (Android 7.0+).
3. Sign in with a demo account — any password works:

   | Email | Role |
   |---|---|
   | `worker@mcc.co.za` | Worker |
   | `manager@mcc.co.za` | Manager |
   | `admin@mcc.co.za` | Administrator |

   The sign-in screen also lists them, so one tap fills in the details.

To see the desktop layout, resize the emulator window (or use a resizable or
tablet device): the bottom bar turns into a navigation rail and wide screens
reflow into two columns.

The app runs on realistic demo data held in memory, with no server yet. Use
**Offline mode** in Settings, or on the Offline & sync screen, to demonstrate
working offline and syncing afterwards.

## Tests

```
./gradlew testDebugUnitTest
```

## For the team

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — package structure, the
  repository / fake pattern, how to add a screen, and the shared components.
- [docs/screenshots](docs/screenshots) — captured screens at each size.
- [tools/screenshots](tools/screenshots) — scripts that re-capture them.
