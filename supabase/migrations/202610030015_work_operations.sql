begin;

create function private.apply_work(
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    v_user_id integer := private.current_user_id();
    v_time timestamptz;
    v_shift public.attendance%rowtype;
    v_report public.daily_reports%rowtype;
    v_project public.projects%rowtype;
    v_project_id integer;
    v_paths text[];
    v_latest timestamptz;
begin
    if p_action in ('CLOCK_IN', 'CLOCK_OUT') then
        v_time := private.event_time(p_payload);

        select *
        into v_shift
        from public.attendance
        where user_id = v_user_id
          and clock_out is null
        for update;

        if p_action = 'CLOCK_IN' then
            if found then
                return to_jsonb(v_shift);
            end if;

            select max(clock_out)
            into v_latest
            from public.attendance
            where user_id = v_user_id;

            if v_latest is not null and v_time < v_latest then
                raise exception 'Clock-in overlaps an existing shift.'
                    using errcode = '22023';
            end if;

            insert into public.attendance (
                user_id,
                date,
                clock_in
            )
            values (
                v_user_id,
                (v_time at time zone 'Africa/Johannesburg')::date,
                v_time
            )
            returning * into v_shift;

            return to_jsonb(v_shift);
        end if;

        if not found then
            raise exception 'No open shift to clock out.'
                using errcode = '22023';
        end if;

        if v_time < v_shift.clock_in then
            raise exception 'Clock-out cannot precede clock-in.'
                using errcode = '22023';
        end if;

        update public.attendance
        set clock_out = v_time
        where attendance_id = v_shift.attendance_id
        returning * into v_shift;

        return to_jsonb(v_shift);
    end if;

    if p_action = 'REPORT_SUBMIT' then
        v_project_id := (p_payload ->> 'project_id')::integer;

        if not private.can_read_project(v_project_id) then
            raise exception 'You cannot report on this project.'
                using errcode = '42501';
        end if;

        select *
        into v_project
        from public.projects
        where project_id = v_project_id;

        if not found then
            raise exception 'Project not found.'
                using errcode = '22023';
        end if;

        v_time := private.event_time(p_payload);

        select coalesce(array_agg(value), array[]::text[])
        into v_paths
        from jsonb_array_elements_text(
            coalesce(p_payload -> 'photo_paths', '[]'::jsonb)
        );

        insert into public.daily_reports (
            project_id,
            user_id,
            date,
            weather,
            work_completed,
            problems_encountered,
            photo_paths,
            created_at
        )
        values (
            v_project_id,
            v_user_id,
            (v_time at time zone 'Africa/Johannesburg')::date,
            p_payload ->> 'weather',
            btrim(p_payload ->> 'work_completed'),
            coalesce(p_payload ->> 'problems_encountered', ''),
            v_paths,
            v_time
        )
        returning * into v_report;

        perform private.attach_photos(
            v_paths,
            v_project_id,
            'DAILY_REPORT',
            v_report.report_id
        );

        perform private.notify(
            v_project.manager_id,
            'REPORT',
            private.actor_name() || ' submitted a daily report for '
            || v_project.project_name || '.'
        );

        return to_jsonb(v_report);
    end if;

    if p_action = 'REPORT_APPROVE' then
        select *
        into v_report
        from public.daily_reports
        where report_id = (p_payload ->> 'report_id')::integer
        for update;

        if not found then
            raise exception 'Report not found.'
                using errcode = '22023';
        end if;

        perform private.require_project_manager(v_report.project_id);

        if v_report.status = 'APPROVED' then
            return to_jsonb(v_report);
        end if;

        update public.daily_reports
        set
            status = 'APPROVED',
            approved_by = v_user_id,
            approved_at = now()
        where report_id = v_report.report_id
        returning * into v_report;

        perform private.notify(
            v_report.user_id,
            'REPORT',
            private.actor_name() || ' approved your daily report for '
            || to_char(v_report.date, 'DD Mon') || '.'
        );

        return to_jsonb(v_report);
    end if;

    raise exception 'Unsupported work operation.'
        using errcode = '22023';
end;
$$;

revoke all on function private.apply_work(text, jsonb)
    from public, anon, authenticated;

commit;