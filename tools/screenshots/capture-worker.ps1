# Entry screens and the full Worker role.
#   .\capture-worker.ps1 phone

. "$PSScriptRoot\drive.ps1"

$P = if ($args[0]) { $args[0] } else { "phone" }

Enable-DemoMode

Write-Host "== entry =="
A shell am force-stop com.mcc.construction | Out-Null
Start-Sleep -Seconds 1
A shell am start -n com.mcc.construction/.MainActivity | Out-Null
Start-Sleep -Milliseconds 1400
Shot "$P-01-splash"

Start-Sleep -Seconds 4
Expect "Sign in" | Out-Null
Shot "$P-02-login"

Tap-Text "Sign In" 0 1500 -Exact | Out-Null      # submit empty to show validation
Hide-Keyboard
Shot "$P-03-login-validation-error"

Tap-Text "Forgot password" 0 2200 | Out-Null
Hide-Keyboard
Shot "$P-04-forgot-password"
Go-Back
Start-Sleep -Seconds 1

Write-Host "== worker =="
Expect "Continue as" | Out-Null
Sign-In "Today's tasks" "s.dlamini"
Expect "Good morning" | Out-Null
Shot "$P-05-worker-dashboard"

Scroll-Down 2
Shot "$P-06-worker-dashboard-quick-actions"
Scroll-Top

Tap-Text "Tasks" 0 2000 | Out-Null
Shot "$P-07-task-list"

Tap-Text "Pour concrete" 0 2000 | Out-Null
Expect "Task details" | Out-Null
Shot "$P-08-task-detail"
Go-Back

Tap-Text "Attendance" 0 2000 | Out-Null
Shot "$P-09-attendance"

Tap-Text "Clock In" 0 2000 | Out-Null
Shot "$P-10-attendance-clocked-in"

Tap-Text "Messages" 0 2000 | Out-Null
Shot "$P-11-messages"

Tap-Text "Johan Botha" 0 2200 | Out-Null
Shot "$P-12-chat"
Go-Back

Tap-Text "Profile" 0 2000 | Out-Null
Expect "Employee ID" | Out-Null
Shot "$P-13-profile"

Scroll-Down 2
Shot "$P-14-profile-more"

Tap-Text "Documents" 0 2200 | Out-Null
Shot "$P-15-documents"
Go-Back
Scroll-Down 2

Tap-Text "Offline & sync" 0 2200 | Out-Null
Shot "$P-16-sync-status"
Go-Back
Scroll-Down 2

Tap-Text "Safety checklist" 0 2200 | Out-Null
Shot "$P-17-safety-checklist"
Go-Back
Scroll-Down 2

Tap-Text "Daily report" 0 2200 | Out-Null
Shot "$P-18-daily-report"
Go-Back
Scroll-Down 3

Tap-Text "Settings" 0 2200 | Out-Null
Shot "$P-19-settings"
Go-Back

Write-Host "== done: worker =="
