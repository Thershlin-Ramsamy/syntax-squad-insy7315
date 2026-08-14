# The Manager role.
#   .\capture-manager.ps1 phone

. "$PSScriptRoot\drive.ps1"

$P = if ($args[0]) { $args[0] } else { "phone" }

Enable-DemoMode

Write-Host "== manager =="
Restart-App
Expect "Continue as" | Out-Null
Sign-In "Projects, teams" "n.khumalo"
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

Tap-Text "Tasks" 0 2200 | Out-Null
Shot "$P-27-manager-task-list"

# Inventory and Reports are rail-only destinations; on a phone they are
# reached from the dashboard's quick actions.
Tap-Text "Dashboard" 0 2200 | Out-Null
Scroll-Down 3
Tap-Text "Inventory" 0 2400 | Out-Null
Shot "$P-28-inventory"

Tap-Text "Rebar Y12" 0 2400 | Out-Null
Shot "$P-29-material-detail"
Go-Back
Go-Back

Tap-Text "Dashboard" 0 2200 | Out-Null
Scroll-Down 3
Tap-Text "Reports" 0 2400 | Out-Null
Shot "$P-30-reports"
Scroll-Down 2
Shot "$P-31-reports-incidents"

Write-Host "== done: manager =="
