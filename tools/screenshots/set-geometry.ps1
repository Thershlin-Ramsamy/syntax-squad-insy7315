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

# Stops ANR dialogs stealing focus mid-capture.
A shell settings put global hide_error_dialogs 1 | Out-Null
A shell wm size $g.size | Out-Null
A shell wm density $g.density | Out-Null
Start-Sleep -Seconds 3

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
