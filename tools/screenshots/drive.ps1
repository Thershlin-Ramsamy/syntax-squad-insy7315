# Shared helpers for driving the app over adb and capturing screenshots.
#
# Taps resolve by reading the on-screen text out of the accessibility tree
# rather than using fixed coordinates, so the same script runs unchanged at
# every window size. Dot-source this from a capture script:
#
#     . "$PSScriptRoot\drive.ps1"
#
# Override the defaults with environment variables if needed:
#     $env:MCC_ADB    = "D:\Android\platform-tools\adb.exe"
#     $env:MCC_DEVICE = "emulator-5556"

$global:REPO = (Resolve-Path "$PSScriptRoot\..\..").Path
$global:OUT  = Join-Path $global:REPO "build\screenshots"
$global:TMP  = Join-Path $env:TEMP "mcc-ui.xml"

$global:ADB =
    if ($env:MCC_ADB) { $env:MCC_ADB }
    elseif (Get-Command adb -ErrorAction SilentlyContinue) { (Get-Command adb).Source }
    else { Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe" }

if (-not (Test-Path $global:ADB)) {
    throw "adb not found at '$($global:ADB)'. Set `$env:MCC_ADB to its full path."
}

function Get-OnlineDevices {
    @(& $global:ADB devices |
        Select-String '^\S+\s+device$' |
        ForEach-Object { ($_ -split '\s+')[0] })
}

# BlueStacks listens on a loopback port that it REASSIGNS between restarts —
# 5555 one run, 5556 the next — so it silently disappears from `adb devices`
# after every geometry change. Probing the usual ports re-attaches it before we
# go looking. Harmless when only an emulator is attached.
foreach ($p in 5555, 5556, 5557) {
    & $global:ADB connect "127.0.0.1:$p" 2>&1 | Out-Null
}

# Pick a device: the override if it is actually online, else the only one
# attached. A pinned $env:MCC_DEVICE that has gone stale is a warning rather
# than a hard failure, because the reassignment above is routine.
# @() is load-bearing: a one-element array unrolls to a bare string on
# assignment, and then $online[0] indexes the *string* and yields "1" from
# "127.0.0.1:5556" — which fails later as a device id that does not exist.
$online = @(Get-OnlineDevices)

if ($env:MCC_DEVICE -and $online -contains $env:MCC_DEVICE) {
    $global:DEV = $env:MCC_DEVICE
} else {
    if ($env:MCC_DEVICE) {
        Write-Host "  warn: MCC_DEVICE '$($env:MCC_DEVICE)' is not online — rediscovering"
    }
    if ($online.Count -ne 1) {
        throw "Expected exactly one online device, found $($online.Count) ($($online -join ', ')). Set `$env:MCC_DEVICE."
    }
    $global:DEV = $online[0]
}

Write-Host "adb    : $($global:ADB)"
Write-Host "device : $($global:DEV)"

function A { & $global:ADB -s $global:DEV @args }

# Pins the status bar to a fixed clock, full battery and full signal so shots
# are consistent. Note: SystemUI stops honouring this after some reboots — if
# the clock in your captures drifts, crop the status bar instead (see
# crop-and-collect.ps1), which is what the committed set does.
function Enable-DemoMode {
    A shell settings put global sysui_demo_allowed 1 | Out-Null
    A shell am broadcast -a com.android.systemui.demo -e command enter | Out-Null
    A shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930 | Out-Null
    A shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false | Out-Null
    A shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 | Out-Null
    A shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4 | Out-Null
    A shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false | Out-Null
}

function Disable-DemoMode {
    A shell am broadcast -a com.android.systemui.demo -e command exit | Out-Null
}

# Dumps the accessibility tree.
#
# Retries, and deletes the device-side file first: `uiautomator dump`
# intermittently fails with "null root node" and does NOT write the file, so a
# naive pull returns the PREVIOUS screen's XML. That parses cleanly and
# silently answers questions about the wrong screen.
function Get-Tree {
    for ($i = 0; $i -lt 5; $i++) {
        Remove-Item $global:TMP -ErrorAction SilentlyContinue
        A shell rm -f /data/local/tmp/ui.xml 2>&1 | Out-Null
        $res = (A shell uiautomator dump /data/local/tmp/ui.xml 2>&1) -join ""
        if ($res -notmatch "dumped to") { Start-Sleep -Milliseconds 800; continue }
        A pull /data/local/tmp/ui.xml $global:TMP 2>&1 | Out-Null
        if (Test-Path $global:TMP) {
            try {
                $xml = [xml](Get-Content $global:TMP -Raw)
                if ($xml.SelectNodes("//node").Count -gt 1) { return $xml }
            } catch { }
        }
        Start-Sleep -Milliseconds 800
    }
    return $null
}

# Nodes matching $Text. CASE-SENSITIVE by default, clickable nodes first:
# the login screen has both a "Sign in" heading and a "Sign In" button, and a
# case-insensitive match taps the heading and does nothing.
function Find-Nodes($Text, [switch]$Exact, [switch]$IgnoreCase) {
    $tree = Get-Tree
    if (-not $tree) { return @() }
    $match = {
        param($v)
        if (-not $v) { return $false }
        if ($Exact) { if ($IgnoreCase) { return $v -eq $Text } else { return $v -ceq $Text } }
        if ($IgnoreCase) { return $v -like "*$Text*" } else { return $v -clike "*$Text*" }
    }
    $hits = $tree.SelectNodes("//node") | Where-Object {
        (& $match $_.text) -or (& $match $_.'content-desc')
    }
    @($hits | Sort-Object @{Expression = { if ($_.clickable -eq 'true') { 0 } else { 1 } } })
}

function Get-Center($node) {
    if ($node.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        [pscustomobject]@{ X = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
                           Y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2) }
    }
}

function Tap-Text($Text, [int]$Index = 0, [int]$SettleMs = 1500, [switch]$Exact, [switch]$IgnoreCase) {
    $nodes = @(Find-Nodes $Text -Exact:$Exact -IgnoreCase:$IgnoreCase)
    if ($nodes.Count -le $Index) { Write-Host "  MISS: '$Text' (found $($nodes.Count))"; return $false }
    $c = Get-Center $nodes[$Index]
    if (-not $c) { Write-Host "  MISS: '$Text' no bounds"; return $false }
    A shell input tap $c.X $c.Y | Out-Null
    Start-Sleep -Milliseconds $SettleMs
    return $true
}

function Tap-XY([int]$X, [int]$Y, [int]$SettleMs = 1500) {
    A shell input tap $X $Y | Out-Null
    Start-Sleep -Milliseconds $SettleMs
}

function Type-Text($Text) { A shell input text $Text | Out-Null; Start-Sleep -Milliseconds 400 }

# Stops the soft IME appearing at all, so nothing ever covers a field and
# Hide-Keyboard has no work to do. Runtimes that present a hardware keyboard
# (BlueStacks) still accept `input text` with this set. Called once at startup.
function Suppress-SoftIme {
    A shell settings put secure show_ime_with_hard_keyboard 0 | Out-Null
}

# BACK closes the IME *when one is genuinely showing*, and only then falls
# through to the back stack.
#
# That assumption does not hold everywhere. BlueStacks reports
# mInputShown=true with no IME actually on screen, so the keyevent goes
# straight to the back stack and pops the app out to the launcher — after
# which every remaining Shot silently captures the home screen instead of the
# app. Suppress-SoftIme prevents it; this guard catches it if it happens
# anyway, because a wrong capture is far more expensive than a slow one.
function Hide-Keyboard {
    $shown = (A shell dumpsys input_method 2>&1 | Select-String "mInputShown=true") -join ""
    if (-not $shown) { return }

    A shell input keyevent 4 | Out-Null
    Start-Sleep -Milliseconds 900

    if ((Current-Focus) -notmatch 'com\.mcc\.construction') {
        Write-Host "  !! BACK left the app (no real IME was showing) — relaunching"
        A shell am start -n com.mcc.construction/.MainActivity | Out-Null
        Start-Sleep -Seconds 4
    }
}

function Get-ScreenSize {
    $sz = (A shell wm size) -join ""
    if ($sz -match 'Override size: (\d+)x(\d+)')      { return @([int]$Matches[1], [int]$Matches[2]) }
    elseif ($sz -match 'Physical size: (\d+)x(\d+)')  { return @([int]$Matches[1], [int]$Matches[2]) }
    return @(1080, 2400)
}

function Scroll-Down([int]$Times = 1) {
    $s = Get-ScreenSize; $w = $s[0]; $h = $s[1]
    for ($i = 0; $i -lt $Times; $i++) {
        A shell input swipe ([int]($w/2)) ([int]($h*0.75)) ([int]($w/2)) ([int]($h*0.25)) 400 | Out-Null
        Start-Sleep -Milliseconds 900
    }
}

function Scroll-Up([int]$Times = 1) {
    $s = Get-ScreenSize; $w = $s[0]; $h = $s[1]
    for ($i = 0; $i -lt $Times; $i++) {
        A shell input swipe ([int]($w/2)) ([int]($h*0.25)) ([int]($w/2)) ([int]($h*0.75)) 400 | Out-Null
        Start-Sleep -Milliseconds 700
    }
}

function Scroll-Top { Scroll-Up 6 }

function Go-Back { A shell input keyevent 4 | Out-Null; Start-Sleep -Milliseconds 1600 }

function Shot($Name) {
    New-Item -ItemType Directory -Force -Path $global:OUT | Out-Null
    # /sdcard is not writable on Android 17; /data/local/tmp is.
    A shell screencap -p /data/local/tmp/s.png | Out-Null
    A pull /data/local/tmp/s.png (Join-Path $global:OUT "$Name.png") 2>&1 | Out-Null
    $p = Join-Path $global:OUT "$Name.png"
    if (Test-Path $p) {
        Write-Host ("  shot: {0} ({1} KB)" -f $Name, [math]::Round((Get-Item $p).Length / 1KB))
    } else {
        Write-Host "  SHOT FAILED: $Name"
    }
}

# `monkey` can trip a recents gesture that sends the app to the background;
# `am start` is dependable.
function Restart-App {
    A shell am force-stop com.mcc.construction | Out-Null
    Start-Sleep -Seconds 2
    A shell am start -n com.mcc.construction/.MainActivity | Out-Null
    Start-Sleep -Seconds 6
}

function Current-Focus {
    (A shell dumpsys window 2>&1 | Select-String "mCurrentFocus" | Select-Object -First 1) -join ""
}

# Catches a tap that missed before it cascades into every later step.
function Expect($Text) {
    if (@(Find-Nodes $Text).Count -gt 0) { Write-Host "  ok: '$Text'"; return $true }
    Write-Host "  !! EXPECTED '$Text' NOT FOUND"; return $false
}

function Sign-In($Role, $User) {
    Tap-Text "s.dlamini" 0 1500 | Out-Null     # the placeholder, not the "Username" label
    Type-Text $User
    # Dismiss the IME before reaching for the next field. On a short window the
    # keyboard covers the password field, so the tap lands on the keyboard,
    # focus never moves, and the password is appended to the username.
    Hide-Keyboard
    Tap-Text "Enter your password" 0 1500 | Out-Null
    Type-Text "sitepass1"
    Hide-Keyboard
    Tap-Text $Role 0 1000 | Out-Null
    if (-not (Tap-Text "Sign In" 0 3000 -Exact)) {
        Scroll-Down 1
        Tap-Text "Sign In" 0 3000 -Exact | Out-Null
    }
}

# Session setup. Runs last because dot-sourcing executes top to bottom, so the
# functions above have to exist before anything calls them.
A shell settings put global hide_error_dialogs 1 | Out-Null
Suppress-SoftIme
