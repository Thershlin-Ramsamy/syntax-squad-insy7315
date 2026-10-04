begin;

create function private.can_upload_file(
    p_bucket text,
    p_path text
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select private.is_active_user()
       and exists (
            select 1
            from public.file_uploads as f
            where f.bucket_id = p_bucket
              and f.storage_path = p_path
              and f.user_id = private.current_user_id()
              and f.state = 'RESERVED'
              and f.created_at >= now() - interval '7 days'
              and (
                    (
                        f.bucket_id = 'mcc-documents'
                        and private.can_manage_project(f.project_id)
                    )
                    or
                    (
                        f.bucket_id = 'mcc-photos'
                        and private.can_read_project(f.project_id)
                    )
              )
       );
$$;

create function private.can_delete_file(
    p_bucket text,
    p_path text
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select private.is_active_user()
       and exists (
            select 1
            from public.file_uploads as f
            where f.bucket_id = p_bucket
              and f.storage_path = p_path
              and (
                    (
                        f.state = 'RESERVED'
                        and f.user_id = private.current_user_id()
                    )
                    or
                    (
                        f.state = 'REMOVED'
                        and (
                            f.user_id = private.current_user_id()
                            or private.can_manage_project(f.project_id)
                        )
                    )
              )
       );
$$;

revoke all on function private.can_upload_file(text, text)
    from public, anon, authenticated;

revoke all on function private.can_delete_file(text, text)
    from public, anon, authenticated;

grant execute on function private.can_upload_file(text, text)
    to authenticated;

grant execute on function private.can_delete_file(text, text)
    to authenticated;

create policy mcc_storage_insert
on storage.objects
for insert
to authenticated
with check (
    bucket_id in ('mcc-photos', 'mcc-documents')
    and private.can_upload_file(bucket_id, name)
);

create policy mcc_storage_select
on storage.objects
for select
to authenticated
using (
    bucket_id in ('mcc-photos', 'mcc-documents')
    and (
        private.can_read_file(bucket_id, name)
        or private.can_delete_file(bucket_id, name)
    )
);

create policy mcc_storage_delete
on storage.objects
for delete
to authenticated
using (
    bucket_id in ('mcc-photos', 'mcc-documents')
    and private.can_delete_file(bucket_id, name)
);

-- Add application tables without replacing existing publication membership.
do $$
declare
    v_table text;
begin
    if not exists (
        select 1
        from pg_publication
        where pubname = 'supabase_realtime'
    ) then
        create publication supabase_realtime;
    end if;

    foreach v_table in array array[
        'users',
        'projects',
        'project_members',
        'tasks',
        'task_assignments',
        'task_photos',
        'attendance',
        'daily_reports',
        'inventory',
        'documents',
        'safety_inspections',
        'incident_reports',
        'notifications',
        'messages',
        'audit_logs',
        'system_settings',
        'offline_sync',
        'file_uploads'
    ]
    loop
        if not exists (
            select 1
            from pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = v_table
        ) then
            execute format(
                'alter publication supabase_realtime add table public.%I',
                v_table
            );
        end if;
    end loop;
end;
$$;

commit;