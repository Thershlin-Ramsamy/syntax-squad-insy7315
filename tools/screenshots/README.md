# Screenshot tooling

Scripts that drive the app on an emulator, capture every screen at three
window size classes, and build the public gallery site from the results.

They exist because the capture set has to be redone whenever the UI changes,
and doing it by hand is both slow and inconsistent.

Written against the Android Studio emulator. BlueStacks works too but needs a
different route to set the size class — see "Running on BlueStacks".

## Requirements

- Windows PowerShell 7 (`pwsh`)
- `adb` on `PATH`, or set `$env:MCC_ADB` to its full path
- Exactly one *online* device, or set `$env:MCC_DEVICE` (e.g. `emulator-5556`).
  Offline entries are ignored, and a stale `$env:MCC_DEVICE` downgrades to a
  warning and rediscovery rather than failing the run.
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

## Running on BlueStacks

`set-geometry.ps1` does not work here and will stop with an explanation rather
than let you find out from the images. BlueStacks accepts `wm size` but
**silently ignores `wm density`**, so the phone preset lands at 720 dp instead
of 411 dp — the expanded size class. The run completes, every screenshot looks
plausible, and the entire phone set is quietly a tablet set with a navigation
rail where the bottom bar should be.

Set the geometry in its config instead. Close HD-Player first, or it rewrites
the file on exit, then edit `%ProgramData%\BlueStacks_nxt\bluestacks.conf`:

| Preset | `dpi` | `fb_width` | `fb_height` |
|---|---|---|---|
| phone | 420 | 1080 | 2400 |
| tablet | 320 | 1536 | 2048 |
| desktop | 320 | 1920 | 1200 |

```
bst.instance.<instance>.dpi="420"
bst.instance.<instance>.fb_width="1080"
bst.instance.<instance>.fb_height="2400"
```

Restart the instance and clear any leftover override with `adb shell wm size
reset`. These are the same pixel sizes and densities as the emulator presets,
so cropped output stays dimension-identical and the set remains uniform.

Two further differences:

- **The adb port moves between restarts** (5555 one run, 5556 the next), so a
  pinned `$env:MCC_DEVICE` goes stale after every geometry change. `drive.ps1`
  re-probes 5555-5557 on load, so usually just unset it.
- **BlueStacks is API 25**, against the emulator's 37. Platform text rendering
  differs slightly, so avoid mixing runtimes within one breakpoint if you can.

## Size classes

| Preset | Resolution | Density | Width | Navigation chrome |
|---|---|---|---|---|
| `phone` | 1080×2400 | 420 dpi | 411 dp | Bottom bar |
| `tablet` | 1536×2048 | 320 dpi | 768 dp | Navigation rail |
| `desktop` | 1920×1200 | 320 dpi | 960 dp | Navigation rail |

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
- **BACK does not reliably close the IME.** It only falls through to the back
  stack when no keyboard is showing — but BlueStacks reports `mInputShown=true`
  with nothing on screen, so `Hide-Keyboard` popped the app out to the launcher
  and every later `Shot` captured the home screen. `drive.ps1` now calls
  `Suppress-SoftIme` at load (`show_ime_with_hard_keyboard 0`, which still
  allows `input text`) and verifies the app kept focus after any BACK. ESCAPE
  (keyevent 111) has exactly the same problem and is not a workaround.
- **A white screen can mean "captured nothing".** The splash is white on white,
  so a cold-start frame taken before the composable renders is indistinguishable
  from a real capture. Warm the app once before the run that shoots the splash,
  and eyeball `*-01-splash` afterwards.
- **A killed emulator does not release its port.** The next one comes up as
  `emulator-5556`, so anything polling `emulator-5554` waits forever on a dead
  entry. `adb kill-server` clears the stale rows.
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
