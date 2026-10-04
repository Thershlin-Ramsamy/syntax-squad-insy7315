begin;

create table public.file_uploads (
    upload_id uuid primary key default gen_random_uuid(),
    project_id integer not null
        references public.projects(project_id),
    user_id integer not null
        references public.users(user_id),
    bucket_id text not null,
    storage_path text not null,
    file_name text not null,
    mime_type text not null,
    size_bytes bigint not null,
    state text not null default 'RESERVED',
    target_type text,
    target_id integer,
    created_at timestamptz not null default now(),

    unique (bucket_id, storage_path),

    constraint file_uploads_bucket_valid
        check (
            bucket_id in (
                'mcc-photos',
                'mcc-documents'
            )
        ),

    constraint file_uploads_state_valid
        check (
            state in (
                'RESERVED',
                'ATTACHED',
                'REMOVED'
            )
        ),

    constraint file_uploads_size_valid
        check (
            size_bytes > 0
            and size_bytes <= 20971520
        ),

    constraint file_uploads_target_valid
        check (
            (
                state = 'RESERVED'
                and target_type is null
                and target_id is null
            )
            or
            (
                state in ('ATTACHED', 'REMOVED')
                and target_type in (
                    'TASK',
                    'DAILY_REPORT',
                    'INCIDENT',
                    'DOCUMENT'
                )
                and target_id is not null
            )
        )
);

create index file_uploads_user_state_index
    on public.file_uploads (user_id, state);

alter table public.file_uploads
    enable row level security;

revoke all on public.file_uploads
    from anon, authenticated;

grant select on public.file_uploads
    to authenticated;

create function private.check_uploaded_object(
    p_upload public.file_uploads
)
returns void
language plpgsql
set search_path = ''
as $$
declare
    v_metadata jsonb;
    v_owner text;
begin
    select metadata, owner_id
    into v_metadata, v_owner
    from storage.objects
    where bucket_id = p_upload.bucket_id
      and name = p_upload.storage_path;

    if not found then
        raise exception
            'Upload the file bytes before finalising.'
            using errcode = '22023';
    end if;

    if v_owner is distinct from auth.uid()::text then
        raise exception
            'Uploaded object does not belong to this account.'
            using errcode = '42501';
    end if;

    if (v_metadata ->> 'size')::bigint
       is distinct from p_upload.size_bytes then
        raise exception
            'Uploaded size differs from reserved size.'
            using errcode = '22023';
    end if;

    if lower(v_metadata ->> 'mimetype')
       is distinct from lower(p_upload.mime_type) then
        raise exception
            'Uploaded MIME type differs from reserved type.'
            using errcode = '22023';
    end if;
end;
$$;

create function private.attach_photos(
    p_paths text[],
    p_project_id integer,
    p_target_type text,
    p_target_id integer
)
returns void
language plpgsql
set search_path = ''
as $$
declare
    v_path text;
    v_upload public.file_uploads%rowtype;
begin
    if p_target_type is null
       or p_target_type not in (
            'TASK',
            'DAILY_REPORT',
            'INCIDENT'
       ) then
        raise exception 'Invalid photograph target.'
            using errcode = '22023';
    end if;

    if p_paths is null then
        raise exception 'Photograph paths must be an array.'
            using errcode = '22023';
    end if;

    if cardinality(p_paths) > 10 then
        raise exception
            'A maximum of ten photographs is allowed.'
            using errcode = '22023';
    end if;

    if cardinality(p_paths) <> (
        select count(distinct value)
        from unnest(p_paths) as paths(value)
    ) then
        raise exception
            'Photograph paths must be unique and non-null.'
            using errcode = '22023';
    end if;

    foreach v_path in array p_paths
    loop
        select *
        into v_upload
        from public.file_uploads
        where bucket_id = 'mcc-photos'
          and storage_path = v_path
          and project_id = p_project_id
          and user_id = private.current_user_id()
          and state = 'RESERVED'
        for update;

        if not found then
            raise exception
                'Photograph has no usable upload reservation.'
                using errcode = '42501';
        end if;

        perform private.check_uploaded_object(v_upload);

        update public.file_uploads
        set
            state = 'ATTACHED',
            target_type = p_target_type,
            target_id = p_target_id
        where upload_id = v_upload.upload_id;
    end loop;
end;
$$;

create function private.can_read_file(
    p_bucket text,
    p_path text
)
returns boolean
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_upload public.file_uploads%rowtype;
begin
    if not private.is_active_user() then
        return false;
    end if;

    select *
    into v_upload
    from public.file_uploads
    where bucket_id = p_bucket
      and storage_path = p_path;

    if not found or v_upload.state = 'REMOVED' then
        return false;
    end if;

    if v_upload.state = 'RESERVED' then
        return v_upload.user_id = private.current_user_id();
    end if;

    case v_upload.target_type
        when 'TASK' then
            return private.can_read_task(
                v_upload.target_id
            );

        when 'DOCUMENT' then
            return private.can_read_project(
                v_upload.project_id
            );

        when 'INCIDENT' then
            return private.can_read_project(
                v_upload.project_id
            );

        when 'DAILY_REPORT' then
            return private.is_manager_or_administrator()
                or exists (
                    select 1
                    from public.daily_reports
                    where report_id = v_upload.target_id
                      and user_id = private.current_user_id()
                );

        else
            return false;
    end case;
end;
$$;

create policy file_uploads_read
on public.file_uploads
for select
to authenticated
using (
    private.can_read_file(
        bucket_id,
        storage_path
    )
);

create function private.apply_file(
    p_action text,
    p_payload jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    v_upload public.file_uploads%rowtype;
    v_document public.documents%rowtype;
    v_photo public.task_photos%rowtype;
    v_task public.tasks%rowtype;
    v_project_id integer;
    v_name text;
    v_mime text;
    v_size bigint;
    v_size_limit bigint;
    v_bucket text;
    v_id uuid;
    v_version integer;
begin
    if p_action = 'FILE_RESERVE' then
        v_project_id :=
            (p_payload ->> 'project_id')::integer;

        v_bucket := p_payload ->> 'bucket_id';
        v_name := btrim(p_payload ->> 'file_name');
        v_mime := lower(p_payload ->> 'mime_type');
        v_size := (p_payload ->> 'size_bytes')::bigint;

        if not private.can_read_project(v_project_id) then
            raise exception
                'You cannot upload to this project.'
                using errcode = '42501';
        end if;

        if v_bucket = 'mcc-documents' then
            perform private.require_project_manager(
                v_project_id
            );

            v_size_limit := 20971520;
        elsif v_bucket = 'mcc-photos' then
            v_size_limit := 10485760;
        else
            raise exception 'Invalid upload bucket.'
                using errcode = '22023';
        end if;

        if v_name is null
           or char_length(v_name) not between 1 and 255
           or position('/' in v_name) > 0
           or position(chr(92) in v_name) > 0
           or v_name ~ '[[:cntrl:]]' then
            raise exception 'Invalid filename.'
                using errcode = '22023';
        end if;

        if v_bucket = 'mcc-documents'
           and position('.' in v_name) = 0 then
            v_name := v_name || '.pdf';
        end if;

        if char_length(v_name) > 255 then
            raise exception 'Filename is too long.'
                using errcode = '22023';
        end if;

        if v_size is null
           or v_size <= 0
           or v_size > v_size_limit then
            raise exception
                'File is empty or exceeds the size limit.'
                using errcode = '22023';
        end if;

        if v_mime is null then
            raise exception 'MIME type is required.'
                using errcode = '22023';
        end if;

        if v_bucket = 'mcc-photos'
           and v_mime not in (
                'image/jpeg',
                'image/png',
                'image/webp'
           ) then
            raise exception
                'Photographs must be JPEG, PNG or WebP.'
                using errcode = '22023';
        end if;

        if v_bucket = 'mcc-documents'
           and v_mime not in (
                'application/pdf',
                'image/jpeg',
                'image/png',
                'application/msword',
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'application/vnd.ms-excel',
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
           ) then
            raise exception
                'Unsupported document type.'
                using errcode = '22023';
        end if;

        v_id := gen_random_uuid();

        insert into public.file_uploads (
            upload_id,
            project_id,
            user_id,
            bucket_id,
            storage_path,
            file_name,
            mime_type,
            size_bytes
        )
        values (
            v_id,
            v_project_id,
            private.current_user_id(),
            v_bucket,
            v_project_id::text
                || '/' || auth.uid()::text
                || '/' || v_id::text,
            v_name,
            v_mime,
            v_size
        )
        returning * into v_upload;

        return to_jsonb(v_upload);
    end if;

    if p_action = 'TASK_PHOTO_REMOVE' then
        select *
        into v_photo
        from public.task_photos
        where photo_id =
              (p_payload ->> 'photo_id')::integer
        for update;

        if not found then
            raise exception 'Photograph not found.'
                using errcode = '22023';
        end if;

        select *
        into v_task
        from public.tasks
        where task_id = v_photo.task_id;

        if not private.can_manage_project(v_task.project_id)
           and not (
                v_photo.user_id = private.current_user_id()
                and private.can_read_task(v_photo.task_id)
           ) then
            raise exception
                'You cannot remove this photograph.'
                using errcode = '42501';
        end if;

        update public.file_uploads
        set state = 'REMOVED'
        where bucket_id = 'mcc-photos'
          and storage_path = v_photo.storage_path;

        return jsonb_build_object(
            'removed', true
        );
    end if;

    select *
    into v_upload
    from public.file_uploads
    where upload_id =
          (p_payload ->> 'upload_id')::uuid
      and user_id = private.current_user_id()
    for update;

    if not found or v_upload.state <> 'RESERVED' then
        raise exception
            'Upload reservation not found or already used.'
            using errcode = '22023';
    end if;

    if p_action = 'DOCUMENT_FINALIZE' then
        perform private.require_project_manager(
            v_upload.project_id
        );

        if v_upload.bucket_id <> 'mcc-documents' then
            raise exception
                'A document upload is required.'
                using errcode = '22023';
        end if;

        perform 1
        from public.projects
        where project_id = v_upload.project_id
        for update;

        perform private.check_uploaded_object(v_upload);

        select coalesce(max(version), 0) + 1
        into v_version
        from public.documents
        where project_id = v_upload.project_id
          and normalized_file_name =
              lower(v_upload.file_name);

        insert into public.documents (
            project_id,
            file_name,
            version,
            category,
            storage_path,
            uploaded_by,
            mime_type,
            size_bytes
        )
        values (
            v_upload.project_id,
            v_upload.file_name,
            v_version,
            coalesce(
                p_payload ->> 'category',
                'OTHER'
            ),
            v_upload.storage_path,
            private.current_user_id(),
            v_upload.mime_type,
            v_upload.size_bytes
        )
        returning * into v_document;

        update public.file_uploads
        set
            state = 'ATTACHED',
            target_type = 'DOCUMENT',
            target_id = v_document.document_id
        where upload_id = v_upload.upload_id;

        return to_jsonb(v_document);
    end if;

    if p_action = 'TASK_PHOTO_FINALIZE' then
        select *
        into v_task
        from public.tasks
        where task_id =
              (p_payload ->> 'task_id')::integer;

        if not found then
            raise exception 'Task not found.'
                using errcode = '22023';
        end if;

        if v_task.project_id <> v_upload.project_id
           or not private.can_read_task(v_task.task_id)
           or (
                not private.can_manage_project(
                    v_task.project_id
                )
                and private.current_role_id() <> 3
           ) then
            raise exception
                'You cannot attach a photograph to this task.'
                using errcode = '42501';
        end if;

        insert into public.task_photos (
            task_id,
            user_id,
            storage_path
        )
        values (
            v_task.task_id,
            private.current_user_id(),
            v_upload.storage_path
        )
        returning * into v_photo;

        perform private.attach_photos(
            array[v_upload.storage_path],
            v_upload.project_id,
            'TASK',
            v_task.task_id
        );

        return to_jsonb(v_photo);
    end if;

    raise exception 'Unsupported file operation.'
        using errcode = '22023';
end;
$$;

alter table public.task_photos
    add column is_removed boolean not null default false;

create function private.mark_photo_removed()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.state = 'REMOVED'
       and old.state <> 'REMOVED'
       and new.target_type = 'TASK' then
        update public.task_photos
        set is_removed = true
        where storage_path = new.storage_path;
    end if;

    return new;
end;
$$;

create trigger file_uploads_mark_photo_removed
after update of state on public.file_uploads
for each row
execute function private.mark_photo_removed();

revoke all on function
    private.check_uploaded_object(public.file_uploads)
from public, anon, authenticated;

revoke all on function
    private.attach_photos(text[], integer, text, integer)
from public, anon, authenticated;

revoke all on function
    private.can_read_file(text, text)
from public, anon, authenticated;

grant execute on function
    private.can_read_file(text, text)
to authenticated;

revoke all on function
    private.apply_file(text, jsonb)
from public, anon, authenticated;

revoke all on function
    private.mark_photo_removed()
from public, anon, authenticated;

commit;