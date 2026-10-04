begin;

create table public.safety_checklist_items (
    item_id integer generated always as identity primary key,
    title text not null,
    detail text not null,
    sort_order integer not null,
    is_active boolean not null default true,

    constraint safety_checklist_title_valid
        check (char_length(btrim(title)) between 1 and 200),

    constraint safety_checklist_detail_valid
        check (char_length(btrim(detail)) between 1 and 2000),

    unique (sort_order)
);

insert into public.safety_checklist_items (
    title,
    detail,
    sort_order
)
values
    (
        'Personal protective equipment',
        'Required hard hat, boots, high-visibility clothing and task-specific protection are available and worn.',
        1
    ),
    (
        'Work area',
        'Walkways and work areas are clear of avoidable trip hazards.',
        2
    ),
    (
        'Tools and equipment',
        'Tools and equipment have been checked and are safe for the planned work.',
        3
    ),
    (
        'Emergency arrangements',
        'Emergency routes, assembly points and first-aid arrangements are known.',
        4
    ),
    (
        'Task briefing',
        'The worker understands the task, relevant hazards and required controls.',
        5
    );

create table public.safety_inspections (
    inspection_id integer generated always as identity primary key,
    project_id integer not null references public.projects(project_id),
    inspector_id integer not null references public.users(user_id),

    date date not null default (
        (now() at time zone 'Africa/Johannesburg')::date
    ),

    comments text not null default '',
    passed boolean not null,
    created_at timestamptz not null default now(),

    constraint safety_inspections_comments_valid
        check (char_length(comments) <= 10000)
);

create table public.incident_reports (
    incident_id integer generated always as identity primary key,
    inspection_id integer not null
        references public.safety_inspections(inspection_id),

    reported_by integer not null references public.users(user_id),
    severity_level integer not null,
    description text not null,
    date_reported timestamptz not null default now(),
    photo_paths text[] not null default '{}',

    constraint incident_reports_severity_valid
        check (severity_level between 1 and 5),

    constraint incident_reports_description_valid
        check (char_length(btrim(description)) between 1 and 10000),

    constraint incident_reports_photos_limit
        check (cardinality(photo_paths) <= 10)
);

create index safety_inspections_project_date_index
    on public.safety_inspections (project_id, date desc);

create index safety_inspections_inspector_date_index
    on public.safety_inspections (inspector_id, date desc);

create index incident_reports_inspection_index
    on public.incident_reports (inspection_id);

create index incident_reports_reporter_index
    on public.incident_reports (reported_by, date_reported desc);

create function private.can_read_inspection(p_inspection_id integer)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.safety_inspections as i
        where i.inspection_id = p_inspection_id
          and private.can_read_project(i.project_id)
    );
$$;

revoke all on function private.can_read_inspection(integer)
    from public;

grant execute on function private.can_read_inspection(integer)
    to authenticated;

alter table public.safety_checklist_items enable row level security;
alter table public.safety_inspections enable row level security;
alter table public.incident_reports enable row level security;

revoke all on public.safety_checklist_items from anon, authenticated;
revoke all on public.safety_inspections from anon, authenticated;
revoke all on public.incident_reports from anon, authenticated;

commit;