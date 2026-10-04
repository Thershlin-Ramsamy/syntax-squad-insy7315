begin;

create function private.apply_catalog(
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    v_project public.projects%rowtype;
    v_supplier public.suppliers%rowtype;
    v_material public.inventory%rowtype;
    v_project_id integer;
    v_user_id integer;
    v_manager_id integer;
begin
    perform private.require_manager();

    if p_action = 'PROJECT_CREATE' then
        v_manager_id := coalesce(
            (p_payload ->> 'manager_id')::integer,
            private.current_user_id()
        );

        if not private.is_administrator()
           and v_manager_id <> private.current_user_id() then
            raise exception 'Managers can only create projects they manage.'
                using errcode = '42501';
        end if;

        if not exists (
            select 1
            from public.users
            where user_id = v_manager_id
              and is_active
              and role_id in (1, 2)
        ) then
            raise exception 'An active manager is required.'
                using errcode = '22023';
        end if;

        insert into public.projects (
            manager_id,
            project_name,
            location,
            budget,
            start_date,
            end_date,
            status
        )
        values (
            v_manager_id,
            btrim(p_payload ->> 'project_name'),
            btrim(p_payload ->> 'location'),
            (p_payload ->> 'budget')::numeric,
            (p_payload ->> 'start_date')::date,
            (p_payload ->> 'end_date')::date,
            coalesce(p_payload ->> 'status', 'PLANNING')
        )
        returning * into v_project;

        insert into public.project_members (project_id, user_id)
        values (v_project.project_id, v_manager_id);

        perform private.audit(
            'Created project ' || v_project.project_name
        );

        return to_jsonb(v_project);
    end if;

    if p_action in (
        'PROJECT_UPDATE',
        'PROJECT_MEMBER',
        'MATERIAL_CREATE'
    ) then
        v_project_id := (p_payload ->> 'project_id')::integer;

        perform private.require_project_manager(v_project_id);

        select *
        into v_project
        from public.projects
        where project_id = v_project_id
        for update;
    end if;

    if p_action = 'PROJECT_UPDATE' then
        update public.projects
        set
            project_name = coalesce(
                p_payload ->> 'project_name',
                v_project.project_name
            ),
            location = coalesce(
                p_payload ->> 'location',
                v_project.location
            ),
            budget = coalesce(
                (p_payload ->> 'budget')::numeric,
                v_project.budget
            ),
            start_date = coalesce(
                (p_payload ->> 'start_date')::date,
                v_project.start_date
            ),
            end_date = coalesce(
                (p_payload ->> 'end_date')::date,
                v_project.end_date
            ),
            status = coalesce(
                p_payload ->> 'status',
                v_project.status
            )
        where project_id = v_project_id
        returning * into v_project;

        perform private.audit(
            'Updated project ' || v_project.project_name
        );

        return to_jsonb(v_project);
    end if;

    if p_action = 'PROJECT_MEMBER' then
        v_user_id := (p_payload ->> 'user_id')::integer;

        if not exists (
            select 1
            from public.users
            where user_id = v_user_id
              and is_active
        ) then
            raise exception 'An active user is required.'
                using errcode = '22023';
        end if;

        insert into public.project_members (project_id, user_id)
        values (v_project_id, v_user_id)
        on conflict (project_id, user_id) do nothing;

        return jsonb_build_object(
            'project_id', v_project_id,
            'user_id', v_user_id
        );
    end if;

    if p_action = 'SUPPLIER_CREATE' then
        insert into public.suppliers (
            company_name,
            contact_person,
            phone,
            email
        )
        values (
            btrim(p_payload ->> 'company_name'),
            coalesce(p_payload ->> 'contact_person', ''),
            coalesce(p_payload ->> 'phone', ''),
            coalesce(p_payload ->> 'email', '')
        )
        returning * into v_supplier;

        return to_jsonb(v_supplier);
    end if;

    if p_action = 'MATERIAL_CREATE' then
        insert into public.inventory (
            project_id,
            supplier_id,
            material_name,
            quantity_available,
            minimum_quantity,
            unit_type,
            unit_cost
        )
        values (
            v_project_id,
            (p_payload ->> 'supplier_id')::integer,
            btrim(p_payload ->> 'material_name'),
            (p_payload ->> 'quantity_available')::integer,
            (p_payload ->> 'minimum_quantity')::integer,
            btrim(p_payload ->> 'unit_type'),
            (p_payload ->> 'unit_cost')::numeric
        )
        returning * into v_material;

        return to_jsonb(v_material);
    end if;

    raise exception 'Unsupported catalogue operation.'
        using errcode = '22023';
end;
$$;

revoke all on function private.apply_catalog(text, jsonb)
    from public, anon, authenticated;

commit;