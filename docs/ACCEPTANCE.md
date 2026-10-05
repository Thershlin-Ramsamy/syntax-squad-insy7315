# Release acceptance checks

Record device, Android version, commit SHA, date, result and evidence for each
check. Use separate provisioned administrator, manager and worker accounts.
A blank result means unverified.

| Test | Expected result | Result |
| --- | --- | --- |
| Auth login for each role | Correct dashboard; no demo account shortcuts | |
| Wrong password / offline login | Feedback; spinner clears; no authenticated dashboard | |
| Reset email | Received link reaches a usable password-change page; new password logs in | |
| Suspended user | No operational reads/writes; application session ends after validation | |
| Idle timeout | No interaction for configured period signs out; user input extends deadline | |
| Restart after idle expiry | Old session cannot continue without signing in | |
| Admin role change / suspend / restore | Persisted change and audit row; self-lockout denied | |
| Organisation settings | Persist across devices; disabled offline mode cannot be enabled | |
| Atomic task assignment | Task and all worker assignments persist together | |
| Worker task start | Requires passed daily checklist if enabled | |
| Task update from two devices | Stale revision is rejected rather than overwriting newer state | |
| Task proof photo | Upload, display, remove, and refreshed private URL all work | |
| Attendance | Clock in/out persisted; no duplicate open shift | |
| Daily report + photos | Submit, display, approve; notification reaches worker | |
| Failed report submission | Draft retained; no success state; submit button reenabled | |
| Checklist / standalone incident | Correct project and severity; manager can view history | |
| Stock | Use/delivery persist; insufficient stock rejected; low-stock alert follows setting | |
| Documents | Upload bytes; version increments for same name; signed download opens | |
| In-app notifications | Correct recipient; read status survives reopen | |
| Messaging online | Sender/recipient see persisted message; unrelated user cannot read it | |
| Offline message + process restart | Queue survives; original UUID replayed after login online | |
| Account switch with queue | Previous account queue stays hidden and is not replayed | |
| Interrupted acknowledgement | Retry yields one server message, not duplicate side effects | |
| Unsupported action in offline mode | Clear error; no claimed save or queued success | |
| Realtime disconnect/reconnect | Lists refresh; banner indicates stale/unavailable server data | |
| Private Storage | Unrelated user cannot read object; bucket remains private | |
| Device rotation / narrow and wide layouts | Usable forms, navigation and keyboard interactions | |
| GitHub Actions | Unit tests, assembly and lint pass on the final source SHA | |

No checks above have been recorded as passing by the patch author. Existing
unit tests mainly use fake repositories; perform live tests separately.

## Remaining scope decisions

This version provides durable offline messages. Full offline attendance, tasks,
reports, photos and cached reads require a broader local database and mutation
queue. Closed-app push notifications require a push service. Neither capability
is supplied by the completion patch; do not claim them as implemented.