begin;

create function private.recalculate_progress(p_project_id integer)
returns void
language sql
set search_path = ''
as $$
    update public.projects
    set progress_percent = (
        select coalesce(
            round(
                100.0
                * count(*) filter (where status = 'COMPLETED')
                / nullif(count(*), 0),
                2
            ),
            0
        )
        from public.tasks
        where project_id = p_project_id
    )
    where project_id = p_project_id;
$$;

create function private.assign_worker(
    p_task_id integer,
    p_user_id integer
)
returns public.task_assignments
language plpgsql
set search_path = ''
as $$
declare
    v_task public.tasks%rowtype;
    v_assignment public.task_assignments%rowtype;
begin
    select *
    into v_task
    from public.tasks
    where task_id = p_task_id;

    if not found then
        raise exception 'Task not found.'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from public.users
        where user_id = p_user_id
          and is_active
          and role_id = 3
    ) then
        raise exception 'Only active workers can be assigned.'
            using errcode = '22023';
    end if;

    insert into public.project_members (project_id, user_id)
    values (v_task.project_id, p_user_id)
    on conflict (project_id, user_id) do nothing;

    insert into public.task_assignments (
        user_id,
        task_id,
        assigned_by
    )
    values (
        p_user_id,
        p_task_id,
        private.current_user_id()
    )
    on conflict (task_id, user_id) do nothing
    returning * into v_assignment;

    if found then
        perform private.notify(
            p_user_id,
            'TASK',
            private.actor_name()
            || ' assigned you "' || v_task.title || '".'
        );
    else
        select *
        into v_assignment
        from public.task_assignments
        where task_id = p_task_id
          and user_id = p_user_id;
    end if;

    return v_assignment;
end;
$$;

create function private.apply_task(
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    v_project public.projects%rowtype;
    v_task public.tasks%rowtype;
    v_assignment public.task_assignments%rowtype;
    v_project_id integer;
    v_task_id integer;
    v_worker integer;
    v_workers jsonb;
    v_status text;
    v_time timestamptz;
    v_revision integer;
begin
    if p_action = 'TASK_CREATE' then
        v_project_id := (p_payload ->> 'project_id')::integer;

        perform private.require_project_manager(v_project_id);

        select *
        into v_project
        from public.projects
        where project_id = v_project_id
        for update;

        if v_project.status = 'COMPLETED' then
            raise exception 'Completed projects cannot take new tasks.'
                using errcode = '22023';
        end if;

        if (p_payload ->> 'deadline')::date is null
           or (p_payload ->> 'deadline')::date
              < (now() at time zone 'Africa/Johannesburg')::date then
            raise exception 'A deadline today or later is required.'
                using errcode = '22023';
        end if;

        v_workers := p_payload -> 'worker_ids';

        if v_workers is null
           or jsonb_typeof(v_workers) <> 'array' then
            raise exception 'worker_ids must be an array.'
                using errcode = '22023';
        end if;

        if jsonb_array_length(v_workers) < 1
           or jsonb_array_length(v_workers) > 100 then
            raise exception 'Select between one and 100 workers.'
                using errcode = '22023';
        end if;

        insert into public.tasks (
            project_id,
            title,
            description,
            deadline,
            priority,
            created_by
        )
        values (
            v_project_id,
            btrim(p_payload ->> 'title'),
            coalesce(p_payload ->> 'description', ''),
            (p_payload ->> 'deadline')::date,
            coalesce(p_payload ->> 'priority', 'MEDIUM'),
            private.current_user_id()
        )
        returning * into v_task;

        for v_worker in
            select distinct value::integer
            from jsonb_array_elements_text(v_workers)
        loop
            perform private.assign_worker(v_task.task_id, v_worker);
        end loop;

        perform private.recalculate_progress(v_project_id);

        return to_jsonb(v_task);
    end if;

    v_task_id := (p_payload ->> 'task_id')::integer;

    select project_id
    into v_project_id
    from public.tasks
    where task_id = v_task_id;

    if not found then
        raise exception 'Task not found.'
            using errcode = '22023';
    end if;

    select *
    into v_project
    from public.projects
    where project_id = v_project_id
    for update;

    select *
    into v_task
    from public.tasks
    where task_id = v_task_id
    for update;

    if p_action = 'TASK_ASSIGN' then
        perform private.require_project_manager(v_project_id);

        if v_project.status = 'COMPLETED' then
            raise exception 'Cannot assign work in a completed project.'
                using errcode = '22023';
        end if;

        v_assignment := private.assign_worker(
            v_task_id,
            (p_payload ->> 'user_id')::integer
        );

        return to_jsonb(v_assignment);
    end if;

    if p_action = 'TASK_STATUS' then
        if not private.can_manage_project(v_project_id)
           and not (
                private.current_role_id() = 3
                and exists (
                    select 1
                    from public.task_assignments
                    where task_id = v_task_id
                      and user_id = private.current_user_id()
                )
           ) then
            raise exception 'You cannot update this task.'
                using errcode = '42501';
        end if;

        v_status := p_payload ->> 'status';

        if v_status is null
           or v_status not in (
                'PENDING',
                'IN_PROGRESS',
                'COMPLETED',
                'BLOCKED'
           ) then
            raise exception 'Invalid task status.'
                using errcode = '22023';
        end if;

        if v_task.status = v_status then
            return to_jsonb(v_task);
        end if;

        v_revision := (p_payload ->> 'expected_revision')::integer;

        if v_revision is null or v_revision <> v_task.revision then
            raise exception 'TASK_CONFLICT: refresh the task before updating.'
                using errcode = '40001';
        end if;

        if v_project.status = 'COMPLETED' then
            raise exception 'Cannot change tasks in a completed project.'
                using errcode = '22023';
        end if;

        v_time := private.event_time(p_payload);

        if v_status = 'IN_PROGRESS'
           and (
                select require_daily_checklist
                from public.system_settings
                where settings_id = 1
           )
           and not exists (
                select 1
                from public.safety_inspections
                where project_id = v_project_id
                  and inspector_id = private.current_user_id()
                  and date = (
                      v_time at time zone 'Africa/Johannesburg'
                  )::date
                  and passed
                  and created_at <= v_time
           ) then
            raise exception 'Pass today''s safety checklist before starting.'
                using errcode = '22023';
        end if;

        update public.tasks
        set
            status = v_status,
            revision = revision + 1
        where task_id = v_task_id
        returning * into v_task;

        perform private.recalculate_progress(v_project_id);

        perform private.notify(
            v_project.manager_id,
            'TASK',
            private.actor_name() || ' '
            || case v_status
                when 'IN_PROGRESS' then 'started'
                when 'COMPLETED' then 'completed'
                when 'BLOCKED' then 'marked as blocked'
                else 'moved back to pending'
               end
            || ' "' || v_task.title || '".'
        );

        return to_jsonb(v_task);
    end if;

    raise exception 'Unsupported task operation.'
        using errcode = '22023';
end;
$$;

revoke all on function private.recalculate_progress(integer)
    from public, anon, authenticated;

revoke all on function private.assign_worker(integer, integer)
    from public, anon, authenticated;

revoke all on function private.apply_task(text, jsonb)
    from public, anon, authenticated;

commit;