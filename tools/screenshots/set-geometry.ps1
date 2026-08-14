# Puts the emulator into one of the three window size classes and reboots so
# it comes up clean.
#
#   .\set-geometry.ps1 phone
#   .\set-geometry.ps1 tablet
#   .\set-geometry.ps1 desktop
#
# Density is the useful lever, not resolution. Forcing a large `wm size` on a
# software-rendered emulator makes SystemUI ANR repeatedly and blanks the
# screen; changing density reaches the same dp width with no scaling cost.

. "$PSScriptRoot\drive.ps1"

$target = if ($args[0]) { $args[0] } else { "phone" }

$geometries = @{
    phone   = @{ size = "1080x2400"; density = 420; dp = 411; chrome = "bottom bar" }
    tablet  = @{ size = "1536x2048"; density = 320; dp = 768; chrome = "navigation rail" }
    desktop = @{ size = "1920x1200"; density = 320; dp = 960; chrome = "permanent drawer" }
}

if (-not $geometries.ContainsKey($target)) {
    throw "Unknown geometry '$target'. Use: phone, tablet or desktop."
}

$g = $geometries[$target]
Write-Host "$target -> $($g.size) @ $($g.density) dpi = $($g.dp) dp wide ($($g.chrome))"

function Get-EffectiveDensity {
    $d = (A shell wm density) -join " "
    if ($d -match 'Override density:\s*(\d+)')     { return [int]$Matches[1] }
    elseif ($d -match 'Physical density:\s*(\d+)') { return [int]$Matches[1] }
    return 0
}

# Stops ANR dialogs stealing focus mid-capture.
A shell settings put global hide_error_dialogs 1 | Out-Null
A shell wm size $g.size | Out-Null
A shell wm density $g.density | Out-Null
Start-Sleep -Seconds 3

# Verify the density override actually landed, because the failure is silent
# and expensive. BlueStacks accepts `wm size` and ignores `wm density` without
# error: at a fixed 240 dpi the phone preset then comes out 720 dp rather than
# 411 dp, which is the *expanded* size class. The capture completes, every
# screenshot looks plausible, and the whole phone set is quietly a tablet set
# showing a navigation rail instead of a bottom bar.
$effective = Get-EffectiveDensity
if ($effective -ne $g.density) {
    $px = $g.size -split 'x'
    throw @"
Density override was refused: asked for $($g.density) dpi, device reports $effective dpi.

This runtime manages its own density (BlueStacks does). Carrying on would
capture $target at $([int]([int]$px[0] * 160 / $effective)) dp instead of $($g.dp) dp — the wrong size class entirely.

For BlueStacks, set the geometry in its config and restart the instance
instead of using this script. With HD-Player closed, in
%ProgramData%\BlueStacks_nxt\bluestacks.conf:

    bst.instance.<name>.dpi="$($g.density)"
    bst.instance.<name>.fb_width="$($px[0])"
    bst.instance.<name>.fb_height="$($px[1])"

That reaches the same pixel size and dp width as the emulator preset, so the
cropped output stays dimension-identical to the rest of the set. Note that
BlueStacks reassigns its adb port on restart; drive.ps1 re-probes 5555-5557.
"@
}

Write-Host "rebooting..."
A reboot | Out-Null
Start-Sleep -Seconds 20
A wait-for-device | Out-Null

$deadline = (Get-Date).AddMinutes(8)
while ((Get-Date) -lt $deadline) {
    if ((A shell getprop sys.boot_completed 2>&1).Trim() -eq "1") { break }
    Start-Sleep -Seconds 10
}
# The first launch straight after boot_completed is still too cold.
Start-Sleep -Seconds 20

Write-Host "ready:"
A shell wm size
A shell wm density

# Re-check after the reboot: an override that applied before it can still be
# dropped on the way back up, and that would silently mis-size the whole run.
$after = Get-EffectiveDensity
if ($after -ne $g.density) {
    throw "Density is $after dpi after reboot, expected $($g.density). Re-run before capturing."
}
