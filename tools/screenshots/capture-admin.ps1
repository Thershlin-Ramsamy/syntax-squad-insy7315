# The Administrator role, plus Notifications.
#   .\capture-admin.ps1 phone

. "$PSScriptRoot\drive.ps1"

$P = if ($args[0]) { $args[0] } else { "phone" }

Enable-DemoMode

Write-Host "== administrator =="
Restart-App
Expect "Continue as" | Out-Null
Sign-In "Users, settings" "r.vanwyk"
Expect "System overview" | Out-Null
Shot "$P-32-admin-dashboard"

Scroll-Down 2
Shot "$P-33-admin-dashboard-activity"
Scroll-Top

Tap-Text "Users" 0 2400 | Out-Null
Shot "$P-34-user-management"

Scroll-Down 1
Shot "$P-35-user-management-roles"
Scroll-Top

Tap-Text "Audit" 0 2400 | Out-Null
Shot "$P-36-audit-log"

Tap-Text "Notifications" 0 2400 | Out-Null
Shot "$P-37-notifications"

Write-Host "== done: administrator =="
