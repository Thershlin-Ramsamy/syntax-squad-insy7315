begin;

create table public.attendance (
    attendance_id integer generated always as identity primary key,
    user_id integer not null references public.users(user_id),

    date date not null default (
        (now() at time zone 'Africa/Johannesburg')::date
    ),

    clock_in timestamptz not null default now(),
    clock_out timestamptz,

    total_hours numeric(10, 4) generated always as (
        case
            when clock_out is null then null
            else extract(epoch from (clock_out - clock_in)) / 3600.0
        end
    ) stored,

    constraint attendance_clock_order_valid
        check (clock_out is null or clock_out >= clock_in)
);

create unique index attendance_one_open_shift
    on public.attendance (user_id)
    where clock_out is null;

create index attendance_user_date_index
    on public.attendance (user_id, date desc);

create index attendance_date_index
    on public.attendance (date desc);

create table public.daily_reports (
    report_id integer generated always as identity primary key,
    project_id integer not null references public.projects(project_id),
    user_id integer not null references public.users(user_id),

    date date not null default (
        (now() at time zone 'Africa/Johannesburg')::date
    ),

    weather text not null,
    work_completed text not null,
    problems_encountered text not null default '',
    status text not null default 'SUBMITTED',
    photo_paths text[] not null default '{}',
    approved_by integer references public.users(user_id),
    approved_at timestamptz,
    created_at timestamptz not null default now(),

    constraint daily_reports_weather_valid
        check (
            weather in (
                'Sunny',
                'Cloudy',
                'Rainy',
                'Windy',
                'Hot',
                'Cold'
            )
        ),

    constraint daily_reports_work_valid
        check (char_length(btrim(work_completed)) between 1 and 10000),

    constraint daily_reports_problems_valid
        check (char_length(problems_encountered) <= 10000),

    constraint daily_reports_status_valid
        check (status in ('SUBMITTED', 'APPROVED')),

    constraint daily_reports_photos_limit
        check (cardinality(photo_paths) <= 10),

    constraint daily_reports_approval_valid
        check (
            (
                status = 'SUBMITTED'
                and approved_by is null
                and approved_at is null
            )
            or
            (
                status = 'APPROVED'
                and approved_by is not null
                and approved_at is not null
            )
        )
);

create index daily_reports_project_date_index
    on public.daily_reports (project_id, date desc, report_id desc);

create index daily_reports_user_date_index
    on public.daily_reports (user_id, date desc, report_id desc);

alter table public.attendance enable row level security;
alter table public.daily_reports enable row level security;

revoke all on public.attendance from anon, authenticated;
revoke all on public.daily_reports from anon, authenticated;

commit;