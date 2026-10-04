# MCC Construction Supabase Backend

## Scope

The MCC Android application uses Supabase PostgreSQL, Auth, private Storage
and Realtime.

The database preserves the frontend's integer application IDs. Application
users link to Supabase Auth through users.auth_user_id.

The Android package is com.mcc.construction.

This backend provides server operations for:

- User provisioning, roles and account suspension.
- Organisation settings and audit records.
- Projects and membership.
- Task creation, assignments and status changes.
- Attendance and daily reports.
- Safety inspections and incidents.
- Suppliers, inventory and stock movements.
- Messages and notifications.
- Upload reservations, document versions and task photographs.
- Sync history and operation retry receipts.

Android repository integration is delivered separately. Creating the database
does not replace FakeAppContainer.

## Migration procedure

Use a new Supabase project.

Execute each complete migration in filename order through Supabase SQL Editor.

Each migration includes a transaction. Clear text selections before running it
so the complete transaction executes.

Do not rerun a successful create migration. If a migration fails, resolve that
failure before continuing.

Keep the same SQL files committed in this repository.

Manual SQL Editor execution does not populate the Supabase CLI migration
history. Before switching this database to CLI-managed migrations, reconcile
its migration history rather than applying all migrations again.

## Storage buckets

Create these private buckets through the Supabase Storage dashboard:

| Bucket | Public | Maximum size |
| --- | --- | --- |
| mcc-photos | No | 10 MB |
| mcc-documents | No | 20 MB |

Photograph MIME types:

- image/jpeg
- image/png
- image/webp

Document MIME types:

- application/pdf
- image/jpeg
- image/png
- application/msword
- application/vnd.openxmlformats-officedocument.wordprocessingml.document
- application/vnd.ms-excel
- application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

Storage policies require a reservation before upload.

Uploads use a server-generated path. Existing objects cannot be overwritten.

Metadata checks confirm the declared size and MIME type. They are not antivirus
scanning or independent file-content inspection.

## Authentication and profile provisioning

Create Auth accounts through Supabase Authentication or a trusted server.

Passwords belong to Supabase Auth. The application profile table does not
contain passwords.

Create the first administrator profile through a trusted SQL Editor session.
Use the setup instructions supplied with the backend.

After bootstrap, authenticated administrators can provision other application
profiles through USER_PROVISION.

USER_PROVISION expects an existing Supabase Auth account. It does not create
the Auth account or set its password.

No signup metadata is trusted to assign administrator or manager roles.

Suspension immediately removes operational database and RPC permissions.
The Android Auth repository must also check is_active and close an inactive
application session.

An inactive profile does not itself ban the Supabase Auth account.

## RPC contract

Call public.mcc_execute with:

```json
{
  "p_operation_id": "a-client-generated-uuid",
  "p_action": "ACTION_NAME",
  "p_payload": {}
}
```

Generate one operation UUID for each intended change.

Persist that UUID with an offline queue item. Use the same UUID and identical
payload when retrying the item.

A successful retry returns the stored response without repeating side effects.

If an operation fails, its transaction and receipt are rolled back.

The authenticated user is the actor. Payload sender_id, inspector_id and
user_id fields cannot impersonate another account.

## Operation payloads

| Action | Required or supported payload fields |
| --- | --- |
| USER_PROVISION | auth_user_id, first_name, last_name, employee_number; optional role_id, phone, job_title |
| USER_ROLE | user_id, role_id |
| USER_ACTIVE | user_id, active |
| SYSTEM_SETTINGS | Any supported system setting fields |
| PROJECT_CREATE | project_name, location, budget, start_date, end_date; optional manager_id, status |
| PROJECT_UPDATE | project_id; optional project_name, location, budget, start_date, end_date, status |
| PROJECT_MEMBER | project_id, user_id |
| SUPPLIER_CREATE | company_name; optional contact_person, phone, email |
| MATERIAL_CREATE | project_id, supplier_id, material_name, quantity_available, minimum_quantity, unit_type, unit_cost |
| TASK_CREATE | project_id, title, deadline, worker_ids; optional description, priority |
| TASK_ASSIGN | task_id, user_id |
| TASK_STATUS | task_id, status, expected_revision; optional occurred_at |
| CLOCK_IN | Optional occurred_at |
| CLOCK_OUT | Optional occurred_at |
| REPORT_SUBMIT | project_id, weather, work_completed; optional problems_encountered, photo_paths, occurred_at |
| REPORT_APPROVE | report_id |
| INSPECTION_SUBMIT | project_id, passed; optional comments, occurred_at |
| INCIDENT_SUBMIT | project_id, severity_level, description; optional inspection_id, photo_paths, occurred_at |
| STOCK_ADJUST | material_id, change |
| MESSAGE_SEND | receiver_id, content; optional occurred_at |
| THREAD_READ | other_user_id |
| NOTIFICATION_READ | notification_id |
| NOTIFICATIONS_READ | Empty object |
| SYNC_RECORD | device_id, sync_status; optional uploaded_count, error_code |
| FILE_RESERVE | project_id, bucket_id, file_name, mime_type, size_bytes |
| DOCUMENT_FINALIZE | upload_id; optional category |
| TASK_PHOTO_FINALIZE | upload_id, task_id |
| TASK_PHOTO_REMOVE | photo_id |

## Task creation

TASK_CREATE requires at least one worker and creates initial assignments
inside the same transaction.

The Android assignment ViewModel must use this atomic operation.

Only active workers can be assigned. Assignments also create project membership.

Completed projects cannot receive new tasks or assignments.

Status updates require expected_revision.

A stale update raises TASK_CONFLICT with SQLSTATE 40001. Refresh the task and
let the user resolve the conflict. Do not silently replace expected_revision
and retry an offline status change.

When the daily checklist setting is enabled, the actor must have a passed
inspection for the event's South African date before starting a task.

Project progress is derived from completed tasks divided by total tasks.

## Attendance and offline event times

occurred_at is an ISO-8601 timestamp with an offset, preferably UTC.

If omitted, the server uses its current time.

Submitted event times must fall between seven days ago and five minutes ahead.

Attendance operates on the authenticated user's shifts.

A user has at most one open shift. Clock-in cannot overlap a previously closed
shift, and clock-out cannot precede clock-in.

Client-supplied offline times are validated declarations; they are not proof
of physical presence at a site.

Upload queued events in their original order.

## File workflow

1. Call FILE_RESERVE.
2. Upload bytes to the returned bucket_id and storage_path using Storage API.
3. Call DOCUMENT_FINALIZE or TASK_PHOTO_FINALIZE.

For daily reports and incidents, upload each reserved photograph first.
Include its storage_path in photo_paths when submitting the record.

Only reserved photographs owned by the actor and belonging to the same
project can be attached.

Document filenames are versioned without case sensitivity.

Signed URLs are temporary presentation URLs. Store storage_path as the durable
reference and regenerate signed URLs when needed.

TASK_PHOTO_REMOVE marks the proof record is_removed = true.

Android must exclude removed proof photographs. After removal, delete the
underlying object through Storage API using the returned or cached path.
A failed object deletion can be retried; the proof remains hidden.

Use Storage API to clean up unused reservations. Never delete storage.objects
records through SQL.

## Realtime

Application tables are members of supabase_realtime.

Android must establish subscriptions, load initial records, and refresh after
changes and reconnection.

Assignment events should trigger task-list refreshes because an assignment can
make an existing task newly visible to a worker.

Subscription delivery is subject to the authenticated user's permissions.

Realtime supplies connected in-app updates. It does not deliver operating-system
push notifications to a closed application.

## Permissions

Public application tables have RLS enabled.

Authenticated clients have SELECT grants and policy-controlled read access.

Direct INSERT, UPDATE and DELETE on application tables are not granted.

Writes go through mcc_execute.

Workers read their assigned tasks and project-accessible information.
Managers and administrators read wider operational records.

Project-management writes require the responsible manager or an administrator.

Audit records are administrator-readable and written by server operations.

Do not expose the private schema through the Data API.

Never place a secret or service_role key in the Android app.

## Settings enforcement

Server operations enforce daily checklist and low-stock alert settings.

Android integration must enforce idle session timeout, offline-mode permission
and background sync interval.

The session timeout setting does not automatically change Supabase Auth JWT
expiry or revoke a stolen access token.

## Verification

Run supabase/tests/verify_backend.sql through SQL Editor.

This verifies tables, RLS flags, grants, private buckets and publication entries.

It does not test a real JWT session, end-to-end workflows, Storage transfer or
Realtime delivery.

Integration tests must additionally cover:

- Worker access denial for unrelated tasks.
- Suspended-user write denial.
- Administrator self-role and self-suspension denial.
- Atomic task creation and assignment.
- Duplicate operation retries.
- Task revision conflicts.
- Daily checklist enforcement.
- Attendance chronology.
- Report approval and notifications.
- Negative stock rejection and low-stock crossing behaviour.
- Message privacy and receiver-only read marking.
- Upload ownership and document versioning.
- Realtime refresh and reconnect.
- Offline queue persistence and ordered replay.