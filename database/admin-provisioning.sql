-- Apply after account-links.sql and supabase-auth-passwords.sql.
begin;
create or replace function public.link_admin_created_account()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
    profile jsonb := new.raw_app_meta_data -> 'itsm_profile';
    account_id bigint;
begin
    if profile is null then return new; end if;
    case profile ->> 'role'
        when 'EMPLOYEE' then
            insert into public.employees(name,email,department)
            values(profile->>'name',new.email,profile->>'department') returning id into account_id;
            insert into public.account_links(supabase_user_id,employee_id) values(new.id,account_id);
        when 'TECHNICIAN' then
            insert into public.technicians(name,email,domain)
            values(profile->>'name',new.email,profile->>'domain') returning id into account_id;
            insert into public.technician_skills(technician_id,skill) values(account_id,profile->>'domain');
            insert into public.account_links(supabase_user_id,technician_id) values(new.id,account_id);
        else raise exception 'Unknown ITSM role';
    end case;
    return new;
end $$;
revoke all on function public.link_admin_created_account() from public, anon, authenticated;
create or replace trigger link_admin_created_account after insert on auth.users
for each row execute function public.link_admin_created_account();
commit;
