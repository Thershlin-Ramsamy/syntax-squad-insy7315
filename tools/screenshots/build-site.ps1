# Builds the static gallery site from docs/screenshots into build/site.
#
# Images stay as separate full-resolution files referenced by relative path.
# Push build/site to the public gallery repo, which GitHub Pages serves at
#   https://thershlin-ramsamy.github.io/mcc-construction-screens/

Add-Type -AssemblyName System.Drawing

$repo     = (Resolve-Path "$PSScriptRoot\..\..").Path
$shots    = Join-Path $repo "docs\screenshots"
$template = Join-Path $PSScriptRoot "gallery-template.html"
$site     = Join-Path $repo "build\site"

New-Item -ItemType Directory -Force -Path (Join-Path $site "screenshots") | Out-Null

function Get-Section($group, [int]$n) {
    if ($group -eq 'phone') {
        if ($n -le 4)  { return 'Entry' }
        if ($n -le 19) { return 'Worker' }
        if ($n -le 31) { return 'Manager' }
        return 'Administrator'
    }
    if ($n -le 1) { return 'Entry' }
    if ($n -le 5) { return 'Worker' }
    return 'Manager'
}

function Get-Title($slug) {
    $words = ($slug -split '-') -join ' '
    $words.Substring(0,1).ToUpper() + $words.Substring(1)
}

$items = @()

Get-ChildItem $shots -Filter *.png | Sort-Object Name | ForEach-Object {
    if ($_.Name -notmatch '^(phone|tablet|desktop)-(\d+)-(.+)\.png$') { return }
    $group = $Matches[1]; $n = [int]$Matches[2]; $slug = $Matches[3]

    Copy-Item $_.FullName (Join-Path $site "screenshots\$($_.Name)") -Force

    $img = [System.Drawing.Image]::FromFile($_.FullName)
    $w = $img.Width; $h = $img.Height
    $img.Dispose()

    $items += [pscustomobject]@{
        g = $group; n = $n; s = (Get-Section $group $n); t = (Get-Title $slug)
        f = $_.Name; w = $w; h = $h; src = "screenshots/$($_.Name)"
    }
}

$json = $items | ConvertTo-Json -Compress -Depth 4
$html = (Get-Content $template -Raw).Replace('/*__DATA__*/', "const SHOTS = $json;")

Set-Content -Path (Join-Path $site "index.html") -Value $html -Encoding UTF8
# Serve the files verbatim rather than through Jekyll.
Set-Content -Path (Join-Path $site ".nojekyll") -Value "" -Encoding UTF8

$png = Get-ChildItem (Join-Path $site "screenshots") -Filter *.png
Write-Host ("index.html : {0:N0} KB" -f ((Get-Item (Join-Path $site 'index.html')).Length / 1KB))
Write-Host ("screenshots: {0} files, {1:N1} MB" -f $png.Count, (($png | Measure-Object Length -Sum).Sum / 1MB))
Write-Host "output     : $site"
