# Crops the status bar off every capture and collects the result into
# docs/screenshots/.
#
# Why crop rather than style: SystemUI demo mode (see Enable-DemoMode) pins the
# clock to 09:30, but it silently stops being honoured after some emulator
# reboots — the broadcast starts returning result=0 and never recovers. Cropping
# is deterministic, so the whole set stays consistent either way.
#
# Heights come from `adb shell dumpsys window displays | statusBars frame`.

Add-Type -AssemblyName System.Drawing

$repo = (Resolve-Path "$PSScriptRoot\..\..").Path
$src  = Join-Path $repo "build\screenshots"
$dst  = Join-Path $repo "docs\screenshots"

$crop = @{ phone = 63; tablet = 48; desktop = 48 }

if (-not (Test-Path $src)) { throw "No captures found at $src. Run a capture script first." }
New-Item -ItemType Directory -Force -Path $dst | Out-Null

$done = 0
Get-ChildItem $src -Filter *.png |
    Where-Object { $_.Name -match '^(phone|tablet|desktop)-\d' } |
    ForEach-Object {
        $prefix = ($_.Name -split '-')[0]
        $top = $crop[$prefix]
        $img = [System.Drawing.Image]::FromFile($_.FullName)
        $w = $img.Width; $h = $img.Height - $top
        $bmp = New-Object System.Drawing.Bitmap($w, $h)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.DrawImage($img,
            (New-Object System.Drawing.Rectangle(0, 0, $w, $h)),
            (New-Object System.Drawing.Rectangle(0, $top, $w, $h)),
            [System.Drawing.GraphicsUnit]::Pixel)
        $g.Dispose()
        $bmp.Save((Join-Path $dst $_.Name), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose(); $img.Dispose()
        $done++
    }

Write-Host "cropped and collected $done images into docs/screenshots"

Get-ChildItem $dst -Filter *.png |
    Group-Object { ($_.Name -split '-')[0] } |
    ForEach-Object {
        $i = [System.Drawing.Image]::FromFile($_.Group[0].FullName)
        Write-Host ("{0,-8} {1,2} images  {2}x{3}" -f $_.Name, $_.Count, $i.Width, $i.Height)
        $i.Dispose()
    }
