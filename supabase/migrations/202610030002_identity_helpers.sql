begin;

create function private.current_user_id()
returns integer
language sql
stable
security definer
set search_path = ''
as $$
    select u.user_id
    from public.users as u
    where u.auth_user_id = auth.uid()
      and u.is_active = true;
$$;

create function private.current_role_id()
returns integer
language sql
stable
security definer
set search_path = ''
as $$
    select u.role_id
    from public.users as u
    where u.auth_user_id = auth.uid()
      and u.is_active = true;
$$;

create function private.is_active_user()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select private.current_user_id() is not null;
$$;

create function private.is_administrator()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce(private.current_role_id() = 1, false);
$$;

create function private.is_manager_or_administrator()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce(private.current_role_id() in (1, 2), false);
$$;

create function private.require_current_user()
returns integer
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_user_id integer;
begin
    v_user_id := private.current_user_id();

    if v_user_id is null then
        raise exception 'An active application account is required.'
            using errcode = '42501';
    end if;

    return v_user_id;
end;
$$;

create function private.touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create trigger users_touch_updated_at
before update on public.users
for each row
execute function private.touch_updated_at();

revoke all on function private.current_user_id() from public;
revoke all on function private.current_role_id() from public;
revoke all on function private.is_active_user() from public;
revoke all on function private.is_administrator() from public;
revoke all on function private.is_manager_or_administrator() from public;
revoke all on function private.require_current_user() from public;
revoke all on function private.touch_updated_at() from public;

grant execute on function private.current_user_id()
    to authenticated;

grant execute on function private.current_role_id()
    to authenticated;

grant execute on function private.is_active_user()
    to authenticated;

grant execute on function private.is_administrator()
    to authenticated;

grant execute on function private.is_manager_or_administrator()
    to authenticated;

commit;