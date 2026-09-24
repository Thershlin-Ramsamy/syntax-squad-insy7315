# A representative subset at a wide window size — enough to evidence the
# navigation chrome and how content reflows, without re-shooting all 37.
#
#   .\capture-wide.ps1 tablet
#   .\capture-wide.ps1 desktop
#
# Set the geometry first with set-geometry.ps1, and give the emulator time to
# settle after the reboot — the first launch after a reboot is often too cold
# and the app never reaches the foreground.

. "$PSScriptRoot\drive.ps1"

$P = if ($args[0]) { $args[0] } else { "tablet" }

Enable-DemoMode

Write-Host "== $P : worker =="
Restart-App
Start-Sleep -Seconds 4
Expect "Sign in" | Out-Null
Shot "$P-01-login"

Sign-In "Worker"
Expect "Good morning" | Out-Null
Shot "$P-02-worker-dashboard"

Tap-Text "Tasks" 0 2400 | Out-Null
Shot "$P-03-task-list"

Tap-Text "Pour concrete" 0 2400 | Out-Null
Shot "$P-04-task-detail"
Go-Back

Tap-Text "Attendance" 0 2400 | Out-Null
Shot "$P-05-attendance"

Write-Host "== $P : manager =="
Restart-App
Start-Sleep -Seconds 4
Sign-In "Manager"
Expect "Good morning" | Out-Null
Shot "$P-06-manager-dashboard"

Tap-Text "Projects" 0 2400 | Out-Null
Shot "$P-07-projects-list"

Tap-Text "Riverside Estate" 0 2600 | Out-Null
Shot "$P-08-project-detail"
Go-Back

Tap-Text "Employees" 0 2400 | Out-Null
Shot "$P-09-employees"

Tap-Text "Inventory" 0 2400 | Out-Null
Shot "$P-10-inventory"

Tap-Text "Reports" 0 2400 | Out-Null
Shot "$P-11-reports"

Tap-Text "Notifications" 0 2400 | Out-Null
Shot "$P-12-notifications"

Write-Host "== done: $P =="
