begin;

do $$
declare
    v_table text;
    v_action text;
begin
    foreach v_table in array array[
        'roles',
        'users',
        'projects',
        'project_members',
        'tasks',
        'task_assignments',
        'task_photos',
        'attendance',
        'daily_reports',
        'suppliers',
        'inventory',
        'stock_movements',
        'documents',
        'safety_checklist_items',
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
        if to_regclass('public.' || v_table) is null then
            raise exception 'Missing table: %', v_table;
        end if;

        if not (
            select c.relrowsecurity
            from pg_class as c
            join pg_namespace as n on n.oid = c.relnamespace
            where n.nspname = 'public'
              and c.relname = v_table
        ) then
            raise exception 'RLS is disabled: %', v_table;
        end if;

        if not has_table_privilege(
            'authenticated',
            'public.' || v_table,
            'SELECT'
        ) then
            raise exception 'Authenticated SELECT missing: %', v_table;
        end if;

        foreach v_action in array array[
            'INSERT',
            'UPDATE',
            'DELETE',
            'TRUNCATE'
        ]
        loop
            if has_table_privilege(
                'authenticated',
                'public.' || v_table,
                v_action
            ) then
                raise exception 'Unexpected % permission on %',
                    v_action,
                    v_table;
            end if;
        end loop;

        if has_table_privilege(
            'anon',
            'public.' || v_table,
            'SELECT'
        ) then
            raise exception 'Unexpected anonymous SELECT on %', v_table;
        end if;
    end loop;

    if not has_function_privilege(
        'authenticated',
        'public.mcc_execute(uuid,text,jsonb)',
        'EXECUTE'
    ) then
        raise exception 'Authenticated gateway permission is missing.';
    end if;

    if has_function_privilege(
        'anon',
        'public.mcc_execute(uuid,text,jsonb)',
        'EXECUTE'
    ) then
        raise exception 'Anonymous users can execute the gateway.';
    end if;

    foreach v_action in array array[
        'apply_admin',
        'apply_catalog',
        'apply_task',
        'apply_work',
        'apply_site',
        'apply_communication',
        'apply_file'
    ]
    loop
        if has_function_privilege(
            'authenticated',
            'private.' || v_action || '(text,jsonb)',
            'EXECUTE'
        ) then
            raise exception 'Direct handler execution is allowed: %',
                v_action;
        end if;
    end loop;

    if has_table_privilege(
        'authenticated',
        'private.operation_receipts',
        'SELECT'
    ) then
        raise exception 'Operation receipts are readable by clients.';
    end if;

    if (select count(*) from public.roles) <> 3 then
        raise exception 'Expected three roles.';
    end if;

    if not exists (
        select 1
        from public.system_settings
        where settings_id = 1
    ) then
        raise exception 'System settings are missing.';
    end if;

    if not exists (
        select 1
        from storage.buckets
        where id = 'mcc-photos'
          and public = false
    ) then
        raise exception 'Private mcc-photos bucket is missing.';
    end if;

    if not exists (
        select 1
        from storage.buckets
        where id = 'mcc-documents'
          and public = false
    ) then
        raise exception 'Private mcc-documents bucket is missing.';
    end if;

    foreach v_table in array array[
        'tasks',
        'task_assignments',
        'notifications',
        'messages',
        'system_settings'
    ]
    loop
        if not exists (
            select 1
            from pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = v_table
        ) then
            raise exception 'Realtime publication missing table: %',
                v_table;
        end if;
    end loop;

    raise notice 'Backend structure and grant checks passed.';
end;
$$;

rollback;