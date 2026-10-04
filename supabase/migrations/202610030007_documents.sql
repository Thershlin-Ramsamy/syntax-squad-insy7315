begin;

create table public.documents (
    document_id integer generated always as identity primary key,
    project_id integer not null references public.projects(project_id),
    file_name text not null,

    normalized_file_name text generated always as (
        lower(btrim(file_name))
    ) stored,

    version integer not null,
    upload_date timestamptz not null default now(),
    category text not null default 'OTHER',
    storage_path text not null unique,
    uploaded_by integer not null references public.users(user_id),
    mime_type text not null,
    size_bytes bigint not null,

    constraint documents_file_name_valid
        check (
            char_length(btrim(file_name)) between 1 and 255
            and position('/' in file_name) = 0
            and position(chr(92) in file_name) = 0
        ),

    constraint documents_version_valid
        check (version >= 1),

    constraint documents_category_valid
        check (
            category in (
                'DRAWING',
                'PERMIT',
                'CONTRACT',
                'SAFETY',
                'REPORT',
                'OTHER'
            )
        ),

    constraint documents_storage_path_valid
        check (
            char_length(btrim(storage_path)) between 1 and 1024
            and storage_path not like 'content://%'
            and storage_path not like 'http%'
        ),

    constraint documents_size_valid
        check (size_bytes > 0 and size_bytes <= 20971520),

    unique (
        project_id,
        normalized_file_name,
        version
    )
);

create index documents_project_upload_index
    on public.documents (project_id, upload_date desc);

alter table public.documents enable row level security;

revoke all on public.documents from anon, authenticated;

commit;