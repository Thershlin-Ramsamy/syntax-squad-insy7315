begin;

create schema if not exists private;

revoke all on schema private from public;
grant usage on schema private to authenticated;

create table public.roles (
    role_id integer primary key,
    role_name text not null unique,
    description text not null,
    constraint roles_known_id check (role_id in (1, 2, 3))
);

insert into public.roles (
    role_id,
    role_name,
    description
)
values
    (
        1,
        'Administrator',
        'Manages accounts, organisation settings and audit records.'
    ),
    (
        2,
        'Manager',
        'Manages projects, task assignments, inventory and reports.'
    ),
    (
        3,
        'Worker',
        'Completes assigned tasks, attendance and site reports.'
    );

create table public.users (
    user_id integer generated always as identity primary key,
    auth_user_id uuid not null unique references auth.users(id),
    role_id integer not null default 3 references public.roles(role_id),
    first_name text not null,
    last_name text not null,
    email text not null,
    phone text not null default '',
    job_title text not null default '',
    employee_number text not null unique,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint users_first_name_valid
        check (char_length(btrim(first_name)) between 1 and 100),

    constraint users_last_name_valid
        check (char_length(btrim(last_name)) between 1 and 100),

    constraint users_email_valid
        check (
            email = lower(btrim(email))
            and char_length(email) between 3 and 254
            and position('@' in email) > 1
        ),

    constraint users_employee_number_valid
        check (char_length(btrim(employee_number)) between 1 and 50)
);

create unique index users_email_unique
    on public.users (lower(email));

create index users_role_active_index
    on public.users (role_id, is_active);

alter table public.roles enable row level security;
alter table public.users enable row level security;

revoke all on public.roles from anon, authenticated;
revoke all on public.users from anon, authenticated;

commit;