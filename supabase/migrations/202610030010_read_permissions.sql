begin;

create policy roles_read
on public.roles
for select
to authenticated
using (
    private.is_active_user()
);

create policy users_read
on public.users
for select
to authenticated
using (
    auth_user_id = auth.uid()
    or (
        private.is_active_user()
        and (
            is_active = true
            or private.is_manager_or_administrator()
        )
    )
);

create policy projects_read
on public.projects
for select
to authenticated
using (
    private.can_read_project(project_id)
);

create policy project_members_read
on public.project_members
for select
to authenticated
using (
    private.can_read_project(project_id)
);

create policy tasks_read
on public.tasks
for select
to authenticated
using (
    private.can_read_task(task_id)
);

create policy task_assignments_read
on public.task_assignments
for select
to authenticated
using (
    private.can_read_task(task_id)
);

create policy task_photos_read
on public.task_photos
for select
to authenticated
using (
    private.can_read_task(task_id)
);

create policy attendance_read
on public.attendance
for select
to authenticated
using (
    private.is_active_user()
    and (
        user_id = private.current_user_id()
        or private.is_manager_or_administrator()
    )
);

create policy daily_reports_read
on public.daily_reports
for select
to authenticated
using (
    private.is_active_user()
    and (
        user_id = private.current_user_id()
        or private.is_manager_or_administrator()
    )
);

create policy suppliers_read
on public.suppliers
for select
to authenticated
using (
    private.is_active_user()
);

create policy inventory_read
on public.inventory
for select
to authenticated
using (
    private.can_read_project(project_id)
);

create policy stock_movements_read
on public.stock_movements
for select
to authenticated
using (
    private.is_manager_or_administrator()
);

create policy documents_read
on public.documents
for select
to authenticated
using (
    private.can_read_project(project_id)
);

create policy safety_checklist_items_read
on public.safety_checklist_items
for select
to authenticated
using (
    private.is_active_user()
    and (
        is_active = true
        or private.is_manager_or_administrator()
    )
);

create policy safety_inspections_read
on public.safety_inspections
for select
to authenticated
using (
    private.can_read_project(project_id)
);

create policy incident_reports_read
on public.incident_reports
for select
to authenticated
using (
    private.can_read_inspection(inspection_id)
);

create policy notifications_read
on public.notifications
for select
to authenticated
using (
    private.is_active_user()
    and user_id = private.current_user_id()
);

create policy messages_read
on public.messages
for select
to authenticated
using (
    private.is_active_user()
    and (
        sender_id = private.current_user_id()
        or receiver_id = private.current_user_id()
    )
);

create policy audit_logs_read
on public.audit_logs
for select
to authenticated
using (
    private.is_administrator()
);

create policy system_settings_read
on public.system_settings
for select
to authenticated
using (
    private.is_active_user()
);

create policy offline_sync_read
on public.offline_sync
for select
to authenticated
using (
    private.is_active_user()
    and (
        user_id = private.current_user_id()
        or private.is_administrator()
    )
);

grant select on
    public.roles,
    public.users,
    public.projects,
    public.project_members,
    public.tasks,
    public.task_assignments,
    public.task_photos,
    public.attendance,
    public.daily_reports,
    public.suppliers,
    public.inventory,
    public.stock_movements,
    public.documents,
    public.safety_checklist_items,
    public.safety_inspections,
    public.incident_reports,
    public.notifications,
    public.messages,
    public.audit_logs,
    public.system_settings,
    public.offline_sync
to authenticated;

commit;