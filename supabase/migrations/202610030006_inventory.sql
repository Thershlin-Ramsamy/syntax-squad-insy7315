begin;

create table public.suppliers (
    supplier_id integer generated always as identity primary key,
    company_name text not null,
    contact_person text not null default '',
    phone text not null default '',
    email text not null default '',

    constraint suppliers_company_name_valid
        check (char_length(btrim(company_name)) between 1 and 200)
);

create table public.inventory (
    material_id integer generated always as identity primary key,
    project_id integer not null references public.projects(project_id),
    supplier_id integer not null references public.suppliers(supplier_id),
    material_name text not null,
    quantity_available integer not null default 0,
    minimum_quantity integer not null default 0,
    unit_type text not null,
    unit_cost numeric(12, 2) not null default 0,
    updated_at timestamptz not null default now(),

    constraint inventory_material_name_valid
        check (char_length(btrim(material_name)) between 1 and 200),

    constraint inventory_quantity_valid
        check (quantity_available >= 0),

    constraint inventory_minimum_valid
        check (minimum_quantity >= 0),

    constraint inventory_unit_valid
        check (char_length(btrim(unit_type)) between 1 and 50),

    constraint inventory_cost_valid
        check (unit_cost >= 0)
);

create table public.stock_movements (
    movement_id integer generated always as identity primary key,
    material_id integer not null references public.inventory(material_id),
    changed_by integer not null references public.users(user_id),
    quantity_change integer not null,
    resulting_quantity integer not null,
    created_at timestamptz not null default now(),

    constraint stock_movements_change_valid
        check (quantity_change <> 0),

    constraint stock_movements_result_valid
        check (resulting_quantity >= 0)
);

create index inventory_project_index
    on public.inventory (project_id);

create index inventory_supplier_index
    on public.inventory (supplier_id);

create index stock_movements_material_index
    on public.stock_movements (material_id, created_at desc);

create trigger inventory_touch_updated_at
before update on public.inventory
for each row
execute function private.touch_updated_at();

alter table public.suppliers enable row level security;
alter table public.inventory enable row level security;
alter table public.stock_movements enable row level security;

revoke all on public.suppliers from anon, authenticated;
revoke all on public.inventory from anon, authenticated;
revoke all on public.stock_movements from anon, authenticated;

commit;