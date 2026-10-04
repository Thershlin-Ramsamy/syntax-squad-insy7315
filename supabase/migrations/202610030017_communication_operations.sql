begin;

create function private.apply_communication(
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    v_user_id integer := private.current_user_id();
    v_other integer;
    v_message public.messages%rowtype;
    v_notification public.notifications%rowtype;
    v_sync public.offline_sync%rowtype;
    v_count integer;
begin
    if p_action = 'MESSAGE_SEND' then
        v_other := (p_payload ->> 'receiver_id')::integer;

        if v_other = v_user_id
           or not exists (
                select 1
                from public.users
                where user_id = v_other
                  and is_active
           ) then
            raise exception 'Choose another active user.'
                using errcode = '22023';
        end if;

        insert into public.messages (
            sender_id,
            receiver_id,
            content,
            timestamp
        )
        values (
            v_user_id,
            v_other,
            btrim(p_payload ->> 'content'),
            private.event_time(p_payload)
        )
        returning * into v_message;

        perform private.notify(
            v_other,
            'MESSAGE',
            'New message from ' || private.actor_name() || '.'
        );

        return to_jsonb(v_message);
    end if;

    if p_action = 'THREAD_READ' then
        v_other := (p_payload ->> 'other_user_id')::integer;

        update public.messages
        set is_read = true
        where receiver_id = v_user_id
          and sender_id = v_other
          and not is_read;

        get diagnostics v_count = row_count;

        return jsonb_build_object('updated', v_count);
    end if;

    if p_action = 'NOTIFICATION_READ' then
        update public.notifications
        set is_read = true
        where notification_id =
              (p_payload ->> 'notification_id')::integer
          and user_id = v_user_id
        returning * into v_notification;

        if not found then
            raise exception 'Notification not found.'
                using errcode = '22023';
        end if;

        return to_jsonb(v_notification);
    end if;

    if p_action = 'NOTIFICATIONS_READ' then
        update public.notifications
        set is_read = true
        where user_id = v_user_id
          and not is_read;

        get diagnostics v_count = row_count;

        return jsonb_build_object('updated', v_count);
    end if;

    if p_action = 'SYNC_RECORD' then
        insert into public.offline_sync (
            user_id,
            device_id,
            sync_status,
            uploaded_count,
            error_code
        )
        values (
            v_user_id,
            btrim(p_payload ->> 'device_id'),
            p_payload ->> 'sync_status',
            coalesce((p_payload ->> 'uploaded_count')::integer, 0),
            left(p_payload ->> 'error_code', 100)
        )
        returning * into v_sync;

        return to_jsonb(v_sync);
    end if;

    raise exception 'Unsupported communication operation.'
        using errcode = '22023';
end;
$$;

revoke all on function private.apply_communication(text, jsonb)
    from public, anon, authenticated;

commit;