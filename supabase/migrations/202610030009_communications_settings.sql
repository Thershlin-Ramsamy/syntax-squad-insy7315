begin;

create table public.notifications (
    notification_id integer generated always as identity primary key,
    user_id integer not null references public.users(user_id),
    type text not null,
    message_content text not null,
    sent_date timestamptz not null default now(),
    is_read boolean not null default false,

    constraint notifications_type_valid
        check (
            type in (
                'TASK',
                'SAFETY',
                'MESSAGE',
                'INVENTORY',
                'REPORT',
                'SYSTEM'
            )
        ),

    constraint notifications_content_valid
        check (char_length(btrim(message_content)) between 1 and 2000)
);

create table public.messages (
    message_id integer generated always as identity primary key,
    sender_id integer not null references public.users(user_id),
    receiver_id integer not null references public.users(user_id),
    content text not null,
    timestamp timestamptz not null default now(),
    is_read boolean not null default false,

    constraint messages_different_participants
        check (sender_id <> receiver_id),

    constraint messages_content_valid
        check (char_length(btrim(content)) between 1 and 5000)
);

create table public.audit_logs (
    log_id integer generated always as identity primary key,
    user_id integer not null references public.users(user_id),
    action text not null,
    timestamp timestamptz not null default now(),

    constraint audit_logs_action_valid
        check (char_length(btrim(action)) between 1 and 4000)
);

create table public.system_settings (
    settings_id integer primary key default 1,
    session_timeout_minutes integer not null default 30,
    require_daily_checklist boolean not null default true,
    allow_offline_mode boolean not null default true,
    low_stock_alerts boolean not null default true,
    sync_interval_minutes integer not null default 15,
    updated_at timestamptz not null default now(),

    constraint system_settings_single_row
        check (settings_id = 1),

    constraint system_settings_session_timeout_valid
        check (session_timeout_minutes in (15, 30, 60, 120)),

    constraint system_settings_sync_interval_valid
        check (sync_interval_minutes in (5, 15, 30, 60))
);

insert into public.system_settings (settings_id)
values (1);

create table public.offline_sync (
    sync_id integer generated always as identity primary key,
    user_id integer not null references public.users(user_id),
    device_id text not null,
    sync_status text not null,
    timestamp timestamptz not null default now(),
    uploaded_count integer not null default 0,
    error_code text,

    constraint offline_sync_device_valid
        check (char_length(btrim(device_id)) between 1 and 200),

    constraint offline_sync_status_valid
        check (sync_status in ('COMPLETED', 'FAILED')),

    constraint offline_sync_count_valid
        check (uploaded_count >= 0)
);

create table private.operation_receipts (
    user_id integer not null references public.users(user_id),
    operation_id uuid not null,
    operation_type text not null,
    request_payload jsonb not null,
    response_payload jsonb not null,
    created_at timestamptz not null default now(),

    primary key (user_id, operation_id)
);

create index notifications_user_date_index
    on public.notifications (user_id, sent_date desc);

create index notifications_unread_index
    on public.notifications (user_id)
    where is_read = false;

create index messages_sender_receiver_time_index
    on public.messages (sender_id, receiver_id, timestamp, message_id);

create index messages_receiver_sender_time_index
    on public.messages (receiver_id, sender_id, timestamp, message_id);

create index messages_unread_receiver_index
    on public.messages (receiver_id)
    where is_read = false;

create index audit_logs_timestamp_index
    on public.audit_logs (timestamp desc, log_id desc);

create index offline_sync_user_time_index
    on public.offline_sync (user_id, timestamp desc);

create trigger system_settings_touch_updated_at
before update on public.system_settings
for each row
execute function private.touch_updated_at();

alter table public.notifications enable row level security;
alter table public.messages enable row level security;
alter table public.audit_logs enable row level security;
alter table public.system_settings enable row level security;
alter table public.offline_sync enable row level security;
alter table private.operation_receipts enable row level security;

revoke all on public.notifications from anon, authenticated;
revoke all on public.messages from anon, authenticated;
revoke all on public.audit_logs from anon, authenticated;
revoke all on public.system_settings from anon, authenticated;
revoke all on public.offline_sync from anon, authenticated;

revoke all on private.operation_receipts
    from public, anon, authenticated;

commit;