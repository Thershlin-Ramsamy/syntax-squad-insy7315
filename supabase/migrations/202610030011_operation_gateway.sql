begin;

alter default privileges in schema private
    revoke execute on functions from public, anon, authenticated;

create function private.require_manager()
returns void
language plpgsql
set search_path = ''
as $$
begin
    if not private.is_manager_or_administrator() then
        raise exception 'Manager or administrator permission required.'
            using errcode = '42501';
    end if;
end;
$$;

create function private.require_admin()
returns void
language plpgsql
set search_path = ''
as $$
begin
    if not private.is_administrator() then
        raise exception 'Administrator permission required.'
            using errcode = '42501';
    end if;
end;
$$;

create function private.require_project_manager(p_project_id integer)
returns void
language plpgsql
set search_path = ''
as $$
begin
    if not private.can_manage_project(p_project_id) then
        raise exception 'You cannot manage this project.'
            using errcode = '42501';
    end if;
end;
$$;

create function private.notify(
    p_recipient integer,
    p_type text,
    p_content text
)
returns void
language plpgsql
set search_path = ''
as $$
begin
    if p_recipient is null
       or p_recipient = private.current_user_id()
       or not exists (
            select 1
            from public.users
            where user_id = p_recipient
              and is_active
       ) then
        return;
    end if;

    insert into public.notifications (
        user_id,
        type,
        message_content
    )
    values (
        p_recipient,
        p_type,
        p_content
    );
end;
$$;

create function private.audit(p_action text)
returns void
language plpgsql
set search_path = ''
as $$
begin
    insert into public.audit_logs (user_id, action)
    values (private.require_current_user(), p_action);
end;
$$;

create function private.actor_name()
returns text
language sql
stable
set search_path = ''
as $$
    select first_name || ' ' || last_name
    from public.users
    where user_id = private.current_user_id();
$$;

create function private.event_time(p_payload jsonb)
returns timestamptz
language plpgsql
set search_path = ''
as $$
declare
    v_time timestamptz;
begin
    v_time := coalesce(
        (p_payload ->> 'occurred_at')::timestamptz,
        now()
    );

    if v_time < now() - interval '7 days'
       or v_time > now() + interval '5 minutes' then
        raise exception 'Event time must be within the last seven days.'
            using errcode = '22023';
    end if;

    return v_time;
end;
$$;

create function private.run_operation(
    p_operation_id uuid,
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id integer;
    v_existing private.operation_receipts%rowtype;
    v_result jsonb;
begin
    if p_operation_id is null
       or p_action is null
       or p_payload is null
       or jsonb_typeof(p_payload) <> 'object' then
        raise exception 'Operation ID, action and object payload are required.'
            using errcode = '22023';
    end if;

    v_user_id := private.require_current_user();

    -- Serialise writes from this account and recheck suspension.
    perform 1
    from public.users
    where user_id = v_user_id
      and is_active
    for update;

    if not found then
        raise exception 'Account is inactive.'
            using errcode = '42501';
    end if;

    select *
    into v_existing
    from private.operation_receipts
    where user_id = v_user_id
      and operation_id = p_operation_id;

    if found then
        if v_existing.operation_type <> p_action
           or v_existing.request_payload <> p_payload then
            raise exception 'Operation ID was already used for different data.'
                using errcode = '22023';
        end if;

        return v_existing.response_payload;
    end if;

    case
        when p_action in (
            'USER_PROVISION',
            'USER_ROLE',
            'USER_ACTIVE',
            'SYSTEM_SETTINGS'
        ) then
            v_result := private.apply_admin(p_action, p_payload);

        when p_action in (
            'PROJECT_CREATE',
            'PROJECT_UPDATE',
            'PROJECT_MEMBER',
            'SUPPLIER_CREATE',
            'MATERIAL_CREATE'
        ) then
            v_result := private.apply_catalog(p_action, p_payload);

        when p_action in (
            'TASK_CREATE',
            'TASK_ASSIGN',
            'TASK_STATUS'
        ) then
            v_result := private.apply_task(p_action, p_payload);

        when p_action in (
            'CLOCK_IN',
            'CLOCK_OUT',
            'REPORT_SUBMIT',
            'REPORT_APPROVE'
        ) then
            v_result := private.apply_work(p_action, p_payload);

        when p_action in (
            'INSPECTION_SUBMIT',
            'INCIDENT_SUBMIT',
            'STOCK_ADJUST'
        ) then
            v_result := private.apply_site(p_action, p_payload);

        when p_action in (
            'MESSAGE_SEND',
            'THREAD_READ',
            'NOTIFICATION_READ',
            'NOTIFICATIONS_READ',
            'SYNC_RECORD'
        ) then
            v_result := private.apply_communication(p_action, p_payload);

        when p_action in (
            'FILE_RESERVE',
            'DOCUMENT_FINALIZE',
            'TASK_PHOTO_FINALIZE',
            'TASK_PHOTO_REMOVE'
        ) then
            v_result := private.apply_file(p_action, p_payload);

        else
            raise exception 'Unknown operation: %', p_action
                using errcode = '22023';
    end case;

    if v_result is null then
        raise exception 'Operation returned no result.';
    end if;

    insert into private.operation_receipts (
        user_id,
        operation_id,
        operation_type,
        request_payload,
        response_payload
    )
    values (
        v_user_id,
        p_operation_id,
        p_action,
        p_payload,
        v_result
    );

    return v_result;
end;
$$;

create function public.mcc_execute(
    p_operation_id uuid,
    p_action text,
    p_payload jsonb
)
returns jsonb
language sql
security invoker
set search_path = ''
as $$
    select private.run_operation(
        p_operation_id,
        p_action,
        p_payload
    );
$$;

revoke all on function private.run_operation(uuid, text, jsonb)
    from public, anon, authenticated;

grant execute on function private.run_operation(uuid, text, jsonb)
    to authenticated;

revoke all on function public.mcc_execute(uuid, text, jsonb)
    from public, anon, authenticated;

grant execute on function public.mcc_execute(uuid, text, jsonb)
    to authenticated;

commit;