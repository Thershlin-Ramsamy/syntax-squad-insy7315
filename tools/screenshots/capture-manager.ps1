# The Manager role.
#   .\capture-manager.ps1 phone

. "$PSScriptRoot\drive.ps1"

$P = if ($args[0]) { $args[0] } else { "phone" }

Enable-DemoMode

Write-Host "== manager =="
Restart-App
Expect "Demo accounts" | Out-Null
Sign-In "Manager"
Expect "Good morning" | Out-Null
Shot "$P-20-manager-dashboard"

Scroll-Down 2
Shot "$P-21-manager-dashboard-projects"
Scroll-Top

Tap-Text "Projects" 0 2200 | Out-Null
Shot "$P-22-projects-list"

Tap-Text "Riverside Estate" 0 2400 | Out-Null
Expect "Project details" | Out-Null
Shot "$P-23-project-detail"
Scroll-Down 2
Shot "$P-24-project-detail-tasks"
Go-Back

Tap-Text "Employees" 0 2200 | Out-Null
Shot "$P-25-employees"

Tap-Text "Sipho Dlamini" 0 2400 | Out-Null
Shot "$P-26-employee-profile"
Go-Back

# Tasks is on the rail at wider sizes; on a phone it is a Profile shortcut,
# and Profile is the account button in the app bar.
Tap-Text "Open your profile" 0 2200 | Out-Null
Tap-Text "Tasks" 0 2200 -Exact | Out-Null
Shot "$P-27-manager-task-list"

Tap-Text "Inventory" 0 2400 -Exact | Out-Null
Shot "$P-28-inventory"

Tap-Text "Rebar Y12" 0 2400 | Out-Null
Shot "$P-29-material-detail"
Go-Back
Go-Back

Tap-Text "Reports" 0 2400 -Exact | Out-Null
Shot "$P-30-reports"
Scroll-Down 2
Shot "$P-31-reports-incidents"

Write-Host "== done: manager =="
