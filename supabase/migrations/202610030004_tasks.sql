begin;

create table public.tasks (
    task_id integer generated always as identity primary key,
    project_id integer not null references public.projects(project_id),
    title text not null,
    description text not null default '',
    deadline date not null,
    priority text not null default 'MEDIUM',
    status text not null default 'PENDING',
    created_by integer not null references public.users(user_id),
    revision integer not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint tasks_title_valid
        check (char_length(btrim(title)) between 1 and 200),

    constraint tasks_description_valid
        check (char_length(description) <= 10000),

    constraint tasks_priority_valid
        check (priority in ('LOW', 'MEDIUM', 'HIGH')),

    constraint tasks_status_valid
        check (
            status in (
                'PENDING',
                'IN_PROGRESS',
                'COMPLETED',
                'BLOCKED'
            )
        ),

    constraint tasks_revision_valid
        check (revision >= 1)
);

create table public.task_assignments (
    assignment_id integer generated always as identity primary key,
    user_id integer not null references public.users(user_id),
    task_id integer not null
        references public.tasks(task_id) on delete cascade,

    assigned_by integer not null references public.users(user_id),
    assigned_date timestamptz not null default now(),

    unique (task_id, user_id)
);

create table public.task_photos (
    photo_id integer generated always as identity primary key,
    task_id integer not null references public.tasks(task_id),
    user_id integer not null references public.users(user_id),
    storage_path text not null unique,
    added_at timestamptz not null default now(),

    constraint task_photos_path_valid
        check (
            char_length(btrim(storage_path)) between 1 and 1024
            and storage_path not like 'content://%'
            and storage_path not like 'http%'
        )
);

create index tasks_project_status_index
    on public.tasks (project_id, status);

create index tasks_deadline_index
    on public.tasks (deadline);

create index task_assignments_user_index
    on public.task_assignments (user_id, task_id);

create index task_photos_task_index
    on public.task_photos (task_id, added_at);

create trigger tasks_touch_updated_at
before update on public.tasks
for each row
execute function private.touch_updated_at();

create function private.can_read_task(p_task_id integer)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select private.is_active_user()
       and exists (
            select 1
            from public.tasks as t
            where t.task_id = p_task_id
              and (
                    private.is_manager_or_administrator()
                    or exists (
                        select 1
                        from public.task_assignments as a
                        where a.task_id = t.task_id
                          and a.user_id = private.current_user_id()
                    )
              )
       );
$$;

revoke all on function private.can_read_task(integer)
    from public;

grant execute on function private.can_read_task(integer)
    to authenticated;

alter table public.tasks enable row level security;
alter table public.task_assignments enable row level security;
alter table public.task_photos enable row level security;

revoke all on public.tasks from anon, authenticated;
revoke all on public.task_assignments from anon, authenticated;
revoke all on public.task_photos from anon, authenticated;

commit;