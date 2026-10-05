# MCC Construction Mobile Management System

**Syntax Squad — INSY7315 Work Integrated Learning**

MCC Construction is a native Android application for coordinating
construction projects, site workers, managers and administrators.

## Technology

| Area | Technology |
|---|---|
| Language | Kotlin |
| Interface | Jetpack Compose and Material 3 |
| Architecture | ViewModels, StateFlow and repository interfaces |
| Authentication | Supabase Auth |
| Database and API | Supabase PostgreSQL and PostgREST |
| Validated writes | PostgreSQL `mcc_execute` RPC |
| Files | Private Supabase Storage buckets |
| Connected updates | Supabase Realtime |
| Local message queue | Encrypted persistent outbox |
| Images | Coil |
| Minimum SDK | API 24 |
| Compile SDK | API 36 |

## Application roles

### Worker

- View assigned tasks.
- Update task status.
- Attach proof-of-work photographs.
- Clock in and out.
- Submit daily reports.
- Complete safety inspections and report incidents.
- Send messages and view notifications.

### Manager

- View project progress and workers.
- Create tasks and assignments.
- Review and approve reports.
- View attendance.
- Adjust inventory.
- Upload project documents.

### Administrator

- Manage application roles.
- Suspend and restore application accounts.
- Review audit records.
- Configure organisation settings.

## Authentication

The running application uses Supabase authentication.

A user requires:

1. A Supabase Auth account.
2. A linked `public.users` profile.
3. An active application account with an assigned role.

Passwords are checked by Supabase Auth. Arbitrary passwords are not accepted.

Authentication UUIDs and integer application user IDs are separate identifiers.

Never put a secret or service-role key in the Android application.

## Local configuration

Configure these values through the project's supported Gradle properties
or local configuration:

- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

Keep machine-specific configuration and credentials out of commits.

## Backend setup

Follow `supabase/README.md`.

The backend includes:

- Versioned SQL migrations.
- Foreign keys, validation constraints and indexes.
- Row Level Security.
- Validated server operations.
- Transactional task creation and assignments.
- Duplicate-operation receipts.
- Task revision conflict handling.
- Private file reservations and upload ownership checks.

Migration files must be applied in order.

If migrations were applied manually, reconcile migration history before
switching to CLI-managed deployment.

## Submission behaviour

Attendance, daily reports, safety inspections, incidents, task updates,
stock adjustments, administrator changes and uploads require connectivity
with offline mode switched off.

A failed request:

- Shows a user-facing explanation.
- Resets its loading indicator.
- Preserves entered form information.
- Does not show a success confirmation.

A connection can fail after a server operation has completed.
Check the relevant history or current record before retrying an uncertain
submission.

Server duplicate-operation protection applies when the same operation UUID
and payload are reused. A fresh user submission may create a new UUID.

## Offline support

Persistent offline queuing currently applies to messages.

Queued messages:

- Are stored in an encrypted local outbox.
- Belong to the originating account.
- Retain their operation UUID for retries.
- Remain stored when an upload fails.
- Are acknowledged locally after a successful server response.

Other submissions are not queued offline.

Form preservation after a request failure is ViewModel state.
It is not a persistent draft guarantee after process termination.

## Administrator settings

### Offline permission

Disabling offline permission prevents enabling offline mode and prevents
new offline message submissions.

Existing queued messages are retained and can upload when connectivity returns.

Online messages use a direct confirmed send when queuing is disabled.

The application enforces its most recently received organisation settings.
A disconnected device cannot immediately receive a new administrator policy.

### Sync interval

The administrator selects a supported interval:

- 5 minutes
- 15 minutes
- 30 minutes
- 60 minutes

Foreground sync scheduling restarts when the interval, connectivity,
offline mode or signed-in account changes.

A mutex prevents overlapping sync uploads.

Automatic scheduling runs while the activity is started.
This release does not implement WorkManager synchronization after the app closes.

Users can also request manual synchronization.

### Idle session timeout

The administrator selects:

- 15 minutes
- 30 minutes
- 60 minutes
- 120 minutes

User interaction resets the idle timer before expiry.
An expired session cannot be revived by a later interaction.

The application checks elapsed idle time when returning from the background.
Protected content is hidden while an expired session signs out.

The timer survives activity recreation within the current process.
Idle timing is not persisted across process termination.

Application idle timeout does not replace server token expiry or remotely
revoke every copy of an existing token.

## Responsive interface

The app uses window-size classes and adaptive navigation.

- Compact windows use phone layouts.
- Larger windows use navigation rails and expanded content.

Large-window Android layouts do not imply a separate web implementation.

## Testing

Run tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Run the complete Android checks:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Regression tests cover:

- Safety checklist submission failures.
- Busy-state cleanup.
- Duplicate submission prevention.
- Offline write rejection.
- Administrator offline permission.
- Configured sync interval conversion.
- Idle timeout boundaries and activity reset behaviour.

`supabase/tests/verify_backend.sql` checks backend structure and grants.
It does not replace authenticated integration tests.

## Live verification

Before submission, verify:

1. Login using worker, manager and administrator accounts.
2. Task creation and assignment.
3. Worker status updates visible to the manager.
4. Attendance and report persistence.
5. Failed requests and retry behaviour.
6. Private file uploads and access.
7. Worker denial for unrelated records.
8. Suspended-user denial.
9. Offline message queue persistence and upload.
10. Administrator setting changes affecting behaviour.
11. Realtime refresh and reconnection.
12. Successful CI results.

## Version control

Use focused branches and meaningful commits.

Open pull requests for review and retain successful check results.

The Android workflow builds, tests and lints the app.
It should not be described as automated backend deployment unless a deployment
job is implemented and its successful execution is demonstrated.

## Project structure

```text
app/src/main/java/com/mcc/construction/
  data/model/
  data/repository/
  data/remote/
  data/fake/
  di/
  navigation/
  security/
  ui/

supabase/
  migrations/
  tests/
```

Fake repositories support isolated tests and previews.
The real application container uses Supabase repositories.

## Documentation

- `docs/ARCHITECTURE.md`
- `supabase/README.md`
- `docs/screenshots/`
- `WIL Diagrams/`
