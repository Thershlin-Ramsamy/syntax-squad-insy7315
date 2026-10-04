begin;

create table public.projects (
    project_id integer generated always as identity primary key,
    manager_id integer not null references public.users(user_id),
    project_name text not null,
    location text not null,
    budget numeric(12, 2) not null default 0,
    start_date date not null,
    end_date date not null,
    status text not null default 'PLANNING',
    progress_percent numeric(5, 2) not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint projects_name_valid
        check (char_length(btrim(project_name)) between 1 and 200),

    constraint projects_location_valid
        check (char_length(btrim(location)) between 1 and 300),

    constraint projects_budget_valid
        check (budget >= 0),

    constraint projects_dates_valid
        check (end_date >= start_date),

    constraint projects_status_valid
        check (
            status in (
                'PLANNING',
                'ACTIVE',
                'ON_HOLD',
                'COMPLETED'
            )
        ),

    constraint projects_progress_valid
        check (progress_percent between 0 and 100)
);

create table public.project_members (
    project_id integer not null
        references public.projects(project_id) on delete cascade,

    user_id integer not null
        references public.users(user_id),

    joined_at timestamptz not null default now(),

    primary key (project_id, user_id)
);

create index projects_manager_index
    on public.projects (manager_id);

create index project_members_user_index
    on public.project_members (user_id, project_id);

create trigger projects_touch_updated_at
before update on public.projects
for each row
execute function private.touch_updated_at();

create function private.can_read_project(p_project_id integer)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select private.is_active_user()
       and (
            private.is_manager_or_administrator()
            or exists (
                select 1
                from public.project_members as pm
                where pm.project_id = p_project_id
                  and pm.user_id = private.current_user_id()
            )
       );
$$;

create function private.can_manage_project(p_project_id integer)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select private.is_active_user()
       and exists (
            select 1
            from public.projects as p
            where p.project_id = p_project_id
              and (
                    private.is_administrator()
                    or (
                        private.current_role_id() = 2
                        and p.manager_id = private.current_user_id()
                    )
              )
       );
$$;

revoke all on function private.can_read_project(integer)
    from public;

revoke all on function private.can_manage_project(integer)
    from public;

grant execute on function private.can_read_project(integer)
    to authenticated;

grant execute on function private.can_manage_project(integer)
    to authenticated;

alter table public.projects enable row level security;
alter table public.project_members enable row level security;

revoke all on public.projects from anon, authenticated;
revoke all on public.project_members from anon, authenticated;

commit;