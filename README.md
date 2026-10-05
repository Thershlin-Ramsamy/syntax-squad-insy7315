# MCC Construction — Syntax Squad

Native Android site management application for INSY7315. The active application
container uses Supabase Auth, PostgreSQL operations, private Storage and Realtime.
Fake repositories remain available to unit tests and Compose previews.

## Features

- Worker dashboard, attendance, task progress and proof photos.
- Daily reports, safety checklists and incident reporting.
- Manager task assignments, employee directory, inventory adjustments and approvals.
- Private documents with upload, version history and signed downloads.
- In-app messages, notifications and account profile.
- Administrator account roles, suspension, audit log and organisation settings.
- Adaptive phone/tablet interface; encrypted persistent offline message outbox.

## Technology

Kotlin, Jetpack Compose, MVVM/StateFlow, Supabase Kotlin, Ktor/OkHttp and Coil 3.
Minimum SDK 24; compile SDK 37; target SDK 36. Dependency versions and the Gradle
wrapper are pinned in the project. Use JDK 17 for Gradle.

## Setup

1. Open this project in Android Studio and install its required SDK packages.
2. Apply the SQL migrations in `supabase/migrations` in filename order to a new
   Supabase project. For an existing database, apply only migrations not already
   applied; do not rerun successful create migrations.
3. Follow `supabase/README.md` to configure private buckets, Auth and user profiles.
4. Retain `sdk.dir` in your untracked `local.properties` and add:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_YOUR_PUBLIC_CLIENT_KEY
```

The configuration validator accepts modern publishable keys. Do not use a
service-role or secret key. Configure the project's password-reset email and
its approved redirect/site URL through Supabase; test that the received link
opens a usable password-change page.

5. Sign in with provisioned Auth credentials. Every Auth account needs an active
   row in `public.users` with its matching Auth UUID. Production login has no demo
   shortcuts and does not accept arbitrary passwords.

## Build and tests

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

GitHub Actions performs the same checks on pushes and pull requests. Offline
unit tests use fakes; passing them does not verify a live Supabase deployment.
The SQL verification script checks structural permissions and setup, not a full
JWT-driven integration test. Complete `docs/ACCEPTANCE.md` on real accounts.

## Offline behaviour

Messages are persisted in an encrypted, account-isolated outbox and replayed
using the original operation UUID. Retry runs while the app process is alive.
Queueing requires a currently validated application session; a profile validation
failure can return the app to login. Reopening online validates the account and
restores its queued messages. A failed acknowledgement retains the retry item.

Attendance, task changes, reports, checklists, stock adjustments, approvals and
file uploads require connectivity. Offline mode blocks these writes. They are
not queued. Cached records are process-local and are not a complete offline
read database. Realtime supplies in-app updates, not operating-system push to
a closed application.

## System settings

The server enforces daily checklist and low-stock alert settings. The client
uses the configured idle timeout and offline-mode permission. New queued
messages and reconnection trigger an upload attempt promptly; configured sync
interval controls regular checks, with a shorter retry after failures. This is
process-based scheduling, not an Android background worker.

Idle expiry signs out the device session. It does not change JWT lifetime or
revoke tokens on every other device.

## Security

RLS and server operations enforce roles, ownership, membership, revision checks,
validation and audit records. Android role menus are supplementary controls.
Passwords stay in Supabase Auth. Storage buckets are private; signed presentation
URLs are temporary and regenerated. Failed writes preserve forms, clear loading
state and show generic feedback without exposing tokens or server internals.
The application disables cleartext traffic and backup of its private data.

## Documentation

- `supabase/README.md`: migrations, buckets, provisioning and server contracts.
- `docs/ACCEPTANCE.md`: manual role/security/file/retry checks.
- `docs/ARCHITECTURE.md`: original interface design; see this README for the
  active backend and current offline scope.

## Release status

The completion patch must pass the Android build, unit tests, lint and live
acceptance checks before being labelled verified. Cloud configuration, real
email recovery and device testing cannot be proved by source inspection.
