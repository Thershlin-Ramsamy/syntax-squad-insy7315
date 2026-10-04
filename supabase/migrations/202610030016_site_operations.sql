begin;

create function private.apply_site(
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    v_project public.projects%rowtype;
    v_inspection public.safety_inspections%rowtype;
    v_incident public.incident_reports%rowtype;
    v_material public.inventory%rowtype;
    v_project_id integer;
    v_inspection_id integer;
    v_time timestamptz;
    v_passed boolean;
    v_change integer;
    v_old_quantity integer;
    v_paths text[];
begin
    if p_action = 'STOCK_ADJUST' then
        select *
        into v_material
        from public.inventory
        where material_id = (p_payload ->> 'material_id')::integer
        for update;

        if not found then
            raise exception 'Material not found.'
                using errcode = '22023';
        end if;

        perform private.require_project_manager(v_material.project_id);

        v_change := (p_payload ->> 'change')::integer;

        if v_change is null or v_change = 0 then
            raise exception 'A non-zero stock adjustment is required.'
                using errcode = '22023';
        end if;

        if v_material.quantity_available::bigint + v_change::bigint < 0
           or v_material.quantity_available::bigint + v_change::bigint
              > 2147483647 then
            raise exception 'Adjustment would produce an invalid quantity.'
                using errcode = '22023';
        end if;

        v_old_quantity := v_material.quantity_available;

        update public.inventory
        set quantity_available = quantity_available + v_change
        where material_id = v_material.material_id
        returning * into v_material;

        insert into public.stock_movements (
            material_id,
            changed_by,
            quantity_change,
            resulting_quantity
        )
        values (
            v_material.material_id,
            private.current_user_id(),
            v_change,
            v_material.quantity_available
        );

        select *
        into v_project
        from public.projects
        where project_id = v_material.project_id;

        if v_old_quantity > v_material.minimum_quantity
           and v_material.quantity_available <= v_material.minimum_quantity
           and (
                select low_stock_alerts
                from public.system_settings
                where settings_id = 1
           ) then
            perform private.notify(
                v_project.manager_id,
                'INVENTORY',
                v_material.material_name
                || ' is at or below its minimum level at '
                || v_project.project_name || '.'
            );
        end if;

        return to_jsonb(v_material);
    end if;

    v_project_id := (p_payload ->> 'project_id')::integer;

    if not private.can_read_project(v_project_id) then
        raise exception 'You cannot submit safety records for this project.'
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

    if p_action = 'INSPECTION_SUBMIT' then
        v_passed := (p_payload ->> 'passed')::boolean;

        if v_passed is null then
            raise exception 'Checklist result is required.'
                using errcode = '22023';
        end if;

        insert into public.safety_inspections (
            project_id,
            inspector_id,
            date,
            comments,
            passed,
            created_at
        )
        values (
            v_project_id,
            private.current_user_id(),
            (v_time at time zone 'Africa/Johannesburg')::date,
            coalesce(p_payload ->> 'comments', ''),
            v_passed,
            v_time
        )
        returning * into v_inspection;

        if not v_passed then
            perform private.notify(
                v_project.manager_id,
                'SAFETY',
                private.actor_name() || '''s safety checklist failed at '
                || v_project.project_name || '.'
            );
        end if;

        return to_jsonb(v_inspection);
    end if;

    if p_action = 'INCIDENT_SUBMIT' then
        v_inspection_id := (p_payload ->> 'inspection_id')::integer;

        if v_inspection_id is null then
            insert into public.safety_inspections (
                project_id,
                inspector_id,
                date,
                comments,
                passed,
                created_at
            )
            values (
                v_project_id,
                private.current_user_id(),
                (v_time at time zone 'Africa/Johannesburg')::date,
                'Inspection created for a standalone incident.',
                false,
                v_time
            )
            returning inspection_id into v_inspection_id;
        else
            select *
            into v_inspection
            from public.safety_inspections
            where inspection_id = v_inspection_id;

            if not found then
                raise exception 'Inspection not found.'
                    using errcode = '22023';
            end if;

            if v_inspection.project_id <> v_project_id
               or (
                    v_inspection.inspector_id <> private.current_user_id()
                    and not private.can_manage_project(v_project_id)
               ) then
                raise exception 'Inspection does not belong to this operation.'
                    using errcode = '42501';
            end if;
        end if;

        select coalesce(array_agg(value), array[]::text[])
        into v_paths
        from jsonb_array_elements_text(
            coalesce(p_payload -> 'photo_paths', '[]'::jsonb)
        );

        insert into public.incident_reports (
            inspection_id,
            reported_by,
            severity_level,
            description,
            date_reported,
            photo_paths
        )
        values (
            v_inspection_id,
            private.current_user_id(),
            (p_payload ->> 'severity_level')::integer,
            btrim(p_payload ->> 'description'),
            v_time,
            v_paths
        )
        returning * into v_incident;

        perform private.attach_photos(
            v_paths,
            v_project_id,
            'INCIDENT',
            v_incident.incident_id
        );

        perform private.notify(
            v_project.manager_id,
            'SAFETY',
            private.actor_name() || ' reported a level '
            || v_incident.severity_level || ' incident at '
            || v_project.project_name || '.'
        );

        return to_jsonb(v_incident);
    end if;

    raise exception 'Unsupported site operation.'
        using errcode = '22023';
end;
$$;

revoke all on function private.apply_site(text, jsonb)
    from public, anon, authenticated;

commit;