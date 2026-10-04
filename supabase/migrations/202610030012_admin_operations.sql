begin;

create function private.apply_admin(
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    v_user public.users%rowtype;
    v_auth_id uuid;
    v_email text;
    v_role integer;
    v_active boolean;
    v_old public.system_settings%rowtype;
    v_settings public.system_settings%rowtype;
    v_key text;
begin
    perform private.require_admin();

    -- Serialise administration operations.
    perform pg_advisory_xact_lock(7315, 1);

    if p_action = 'USER_PROVISION' then
        v_auth_id := (p_payload ->> 'auth_user_id')::uuid;
        v_role := coalesce((p_payload ->> 'role_id')::integer, 3);

        select lower(email)
        into v_email
        from auth.users
        where id = v_auth_id;

        if not found or v_email is null then
            raise exception 'Supabase Auth account does not exist.'
                using errcode = '22023';
        end if;

        insert into public.users (
            auth_user_id,
            role_id,
            first_name,
            last_name,
            email,
            phone,
            job_title,
            employee_number
        )
        values (
            v_auth_id,
            v_role,
            btrim(p_payload ->> 'first_name'),
            btrim(p_payload ->> 'last_name'),
            v_email,
            coalesce(p_payload ->> 'phone', ''),
            coalesce(p_payload ->> 'job_title', ''),
            btrim(p_payload ->> 'employee_number')
        )
        returning * into v_user;

        perform private.audit('Provisioned account ' || v_user.email);

        return to_jsonb(v_user);
    end if;

    if p_action in ('USER_ROLE', 'USER_ACTIVE') then
        select *
        into v_user
        from public.users
        where user_id = (p_payload ->> 'user_id')::integer
        for update;

        if not found then
            raise exception 'User not found.'
                using errcode = '22023';
        end if;

        if v_user.user_id = private.current_user_id() then
            raise exception 'You cannot change your own role or active status.'
                using errcode = '42501';
        end if;

        if p_action = 'USER_ROLE' then
            v_role := (p_payload ->> 'role_id')::integer;

            if v_role is null or v_role not in (1, 2, 3) then
                raise exception 'Invalid role.'
                    using errcode = '22023';
            end if;

            if v_user.role_id = v_role then
                return to_jsonb(v_user);
            end if;

            update public.users
            set role_id = v_role
            where user_id = v_user.user_id
            returning * into v_user;

            perform private.audit(
                'Changed role of ' || v_user.first_name || ' '
                || v_user.last_name || ' to '
                || (
                    select role_name
                    from public.roles
                    where role_id = v_role
                )
            );
        else
            v_active := (p_payload ->> 'active')::boolean;

            if v_active is null then
                raise exception 'Active status is required.'
                    using errcode = '22023';
            end if;

            if v_user.is_active = v_active then
                return to_jsonb(v_user);
            end if;

            update public.users
            set is_active = v_active
            where user_id = v_user.user_id
            returning * into v_user;

            perform private.audit(
                case when v_active then 'Restored account '
                     else 'Suspended account '
                end || v_user.email
            );
        end if;

        return to_jsonb(v_user);
    end if;

    if p_action = 'SYSTEM_SETTINGS' then
        select *
        into v_old
        from public.system_settings
        where settings_id = 1
        for update;

        update public.system_settings
        set
            session_timeout_minutes = coalesce(
                (p_payload ->> 'session_timeout_minutes')::integer,
                v_old.session_timeout_minutes
            ),
            require_daily_checklist = coalesce(
                (p_payload ->> 'require_daily_checklist')::boolean,
                v_old.require_daily_checklist
            ),
            allow_offline_mode = coalesce(
                (p_payload ->> 'allow_offline_mode')::boolean,
                v_old.allow_offline_mode
            ),
            low_stock_alerts = coalesce(
                (p_payload ->> 'low_stock_alerts')::boolean,
                v_old.low_stock_alerts
            ),
            sync_interval_minutes = coalesce(
                (p_payload ->> 'sync_interval_minutes')::integer,
                v_old.sync_interval_minutes
            )
        where settings_id = 1
        returning * into v_settings;

        foreach v_key in array array[
            'session_timeout_minutes',
            'require_daily_checklist',
            'allow_offline_mode',
            'low_stock_alerts',
            'sync_interval_minutes'
        ]
        loop
            if to_jsonb(v_old) -> v_key
               is distinct from to_jsonb(v_settings) -> v_key then
                perform private.audit(
                    'Updated system setting: ' || v_key
                    || ' = ' || (to_jsonb(v_settings) ->> v_key)
                );
            end if;
        end loop;

        return to_jsonb(v_settings);
    end if;

    raise exception 'Unsupported administration operation.'
        using errcode = '22023';
end;
$$;

revoke all on function private.apply_admin(text, jsonb)
    from public, anon, authenticated;

commit;