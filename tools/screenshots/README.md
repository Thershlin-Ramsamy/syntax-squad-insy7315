# Screenshot tooling

Scripts that drive the app on an emulator, capture every screen at three
window size classes, and build the public gallery site from the results.

They exist because the capture set has to be redone whenever the UI changes,
and doing it by hand is both slow and inconsistent.

## Requirements

- Windows PowerShell 7 (`pwsh`)
- `adb` on `PATH`, or set `$env:MCC_ADB` to its full path
- Exactly one attached device, or set `$env:MCC_DEVICE` (e.g. `emulator-5556`)
- The debug APK installed:
  `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Everything writes to `build/` (gitignored) until `crop-and-collect.ps1` moves
the finished images into `docs/screenshots/`.

## Full run

```powershell
cd tools/screenshots

# Phone — all 37 screens
./set-geometry.ps1 phone
./capture-worker.ps1  phone     # entry + Worker    (01-19)
./capture-manager.ps1 phone     # Manager           (20-31)
./capture-admin.ps1   phone     # Administrator     (32-37)

# Wider size classes — representative subset each
./set-geometry.ps1 tablet
./capture-wide.ps1 tablet

./set-geometry.ps1 desktop
./capture-wide.ps1 desktop

# Crop status bars, collect into docs/screenshots/
./crop-and-collect.ps1

# Build the static gallery into build/site
./build-site.ps1
```

Then push `build/site` to the public gallery repo — GitHub Pages serves it at
<https://thershlin-ramsamy.github.io/mcc-construction-screens/>.

## Size classes

| Preset | Resolution | Density | Width | Navigation chrome |
|---|---|---|---|---|
| `phone` | 1080×2400 | 420 dpi | 411 dp | Bottom bar |
| `tablet` | 1536×2048 | 320 dpi | 768 dp | Navigation rail |
| `desktop` | 1920×1200 | 320 dpi | 960 dp | Permanent drawer |

Density is the lever, not resolution. Forcing a large `wm size` on a
software-rendered emulator makes SystemUI ANR repeatedly and blanks the
screen; changing density reaches the same dp width with no scaling cost.

## How the driving works

`drive.ps1` resolves taps by reading on-screen text out of the accessibility
tree (`uiautomator dump`) instead of using fixed coordinates, so one script
runs unchanged at every window size. `Expect` asserts that a known string is
present after each navigation, which catches a tap that missed before it
cascades into every later step.

## Traps worth knowing

Each of these cost real debugging time. They are all handled in the scripts,
but they will bite anyone extending them.

- **A failed `uiautomator dump` leaves the previous screen's XML on the
  device.** It fails intermittently with `ERROR: null root node` and does not
  write the file, so pulling it returns a stale tree that parses cleanly and
  silently answers questions about the wrong screen. Delete
  `/data/local/tmp/ui.xml` before every dump.
- **Text matching must be case-sensitive.** The login screen has a "Sign in"
  heading and a "Sign In" button; a case-insensitive contains-match hits the
  heading and taps nothing.
- **Dismiss the keyboard between form fields.** On a short window the IME
  covers the next field, so the tap lands on the keyboard, focus never moves,
  and the text is appended to the previous field — a silent failure that still
  reports a successful tap.
- **`screencap` cannot write to `/sdcard` on Android 17.** Use
  `/data/local/tmp`.
- **Launch with `am start`, not `monkey`.** `monkey` can trip a recents
  gesture that sends the app straight to the background.
- **Install from `app/build/outputs/apk/debug/`.** A stale copy also sits
  under `app/build/intermediates/apk/debug/` and is easy to install by
  mistake, which silently tests the previous build.
- **Wait after a geometry reboot.** The first launch after `boot_completed` is
  often still too cold and the app never reaches the foreground.
- **SystemUI demo mode is unreliable.** It pins the status bar to a fixed
  clock and full battery, but stops being honoured after some reboots
  (broadcasts start returning `result=0`). `crop-and-collect.ps1` crops the
  status bar off instead, which is deterministic.
