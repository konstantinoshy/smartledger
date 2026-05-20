-- Schema για το ενεργό Supabase backend.
-- Εκτέλεση μία φορά από το Supabase SQL Editor.

-- ═══════════════════════════════════════════════════
-- Expenses (υπάρχον)
-- ═══════════════════════════════════════════════════

create table if not exists public.expenses (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    amount_minor integer not null check (amount_minor > 0),
    currency char(3) not null default 'USD',
    category text not null check (category in ('Food', 'Transport', 'Entertainment', 'Rent', 'Travel', 'General')),
    description text not null check (char_length(description) between 1 and 120),
    spent_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_expenses_user_spent_at
    on public.expenses(user_id, spent_at desc);

create index if not exists idx_expenses_user_category
    on public.expenses(user_id, category);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists set_expenses_updated_at on public.expenses;
create trigger set_expenses_updated_at
before update on public.expenses
for each row
execute function public.set_updated_at();

alter table public.expenses enable row level security;

drop policy if exists "Users can read own expenses" on public.expenses;
create policy "Users can read own expenses"
on public.expenses
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "Users can insert own expenses" on public.expenses;
create policy "Users can insert own expenses"
on public.expenses
for insert
to authenticated
with check (auth.uid() = user_id);

drop policy if exists "Users can update own expenses" on public.expenses;
create policy "Users can update own expenses"
on public.expenses
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists "Users can delete own expenses" on public.expenses;
create policy "Users can delete own expenses"
on public.expenses
for delete
to authenticated
using (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════
-- Bill Split — Groups, Members, Shared Expenses
-- ═══════════════════════════════════════════════════

create table if not exists public.split_groups (
    id          uuid primary key default gen_random_uuid(),
    owner_id    uuid not null references auth.users(id) on delete cascade,
    name        text not null check (char_length(name) between 1 and 60),
    created_at  timestamptz not null default now()
);

create index if not exists idx_split_groups_owner
    on public.split_groups(owner_id);

alter table public.split_groups enable row level security;

drop policy if exists "Users can read own groups" on public.split_groups;
create policy "Users can read own groups"
on public.split_groups for select to authenticated
using (auth.uid() = owner_id);

drop policy if exists "Users can insert own groups" on public.split_groups;
create policy "Users can insert own groups"
on public.split_groups for insert to authenticated
with check (auth.uid() = owner_id);

drop policy if exists "Users can delete own groups" on public.split_groups;
create policy "Users can delete own groups"
on public.split_groups for delete to authenticated
using (auth.uid() = owner_id);

-- Split Members
create table if not exists public.split_members (
    id              uuid primary key default gen_random_uuid(),
    group_id        uuid not null references public.split_groups(id) on delete cascade,
    display_name    text not null check (char_length(display_name) between 1 and 60),
    created_at      timestamptz not null default now()
);

create index if not exists idx_split_members_group
    on public.split_members(group_id);

alter table public.split_members enable row level security;

drop policy if exists "Users can read group members" on public.split_members;
create policy "Users can read group members"
on public.split_members for select to authenticated
using (exists (
    select 1 from public.split_groups g where g.id = group_id and g.owner_id = auth.uid()
));

drop policy if exists "Users can insert group members" on public.split_members;
create policy "Users can insert group members"
on public.split_members for insert to authenticated
with check (exists (
    select 1 from public.split_groups g where g.id = group_id and g.owner_id = auth.uid()
));

-- Split Expenses
create table if not exists public.split_expenses (
    id              uuid primary key default gen_random_uuid(),
    group_id        uuid not null references public.split_groups(id) on delete cascade,
    paid_by         text not null,
    amount_minor    integer not null check (amount_minor > 0),
    description     text not null check (char_length(description) between 1 and 120),
    is_settled      boolean not null default false,
    created_at      timestamptz not null default now()
);

create index if not exists idx_split_expenses_group
    on public.split_expenses(group_id);

alter table public.split_expenses enable row level security;

drop policy if exists "Users can read group expenses" on public.split_expenses;
create policy "Users can read group expenses"
on public.split_expenses for select to authenticated
using (exists (
    select 1 from public.split_groups g where g.id = group_id and g.owner_id = auth.uid()
));

drop policy if exists "Users can insert group expenses" on public.split_expenses;
create policy "Users can insert group expenses"
on public.split_expenses for insert to authenticated
with check (exists (
    select 1 from public.split_groups g where g.id = group_id and g.owner_id = auth.uid()
));

drop policy if exists "Users can update group expenses" on public.split_expenses;
create policy "Users can update group expenses"
on public.split_expenses for update to authenticated
using (exists (
    select 1 from public.split_groups g where g.id = group_id and g.owner_id = auth.uid()
));

-- ═══════════════════════════════════════════════════
-- Crypto Portfolio — Assets & Transactions
-- ═══════════════════════════════════════════════════

create table if not exists public.portfolio_assets (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references auth.users(id) on delete cascade,
    symbol          text not null,
    name            text not null,
    quantity        numeric(20,8) not null default 0 check (quantity >= 0),
    average_price   numeric(20,2) not null default 0 check (average_price >= 0),
    asset_type      text not null default 'crypto',
    updated_at      timestamptz not null default now(),
    unique(user_id, symbol)
);

create index if not exists idx_portfolio_assets_user
    on public.portfolio_assets(user_id);

alter table public.portfolio_assets enable row level security;

drop policy if exists "Users can read own assets" on public.portfolio_assets;
create policy "Users can read own assets"
on public.portfolio_assets for select to authenticated
using (auth.uid() = user_id);

drop policy if exists "Users can insert own assets" on public.portfolio_assets;
create policy "Users can insert own assets"
on public.portfolio_assets for insert to authenticated
with check (auth.uid() = user_id);

drop policy if exists "Users can update own assets" on public.portfolio_assets;
create policy "Users can update own assets"
on public.portfolio_assets for update to authenticated
using (auth.uid() = user_id);

drop policy if exists "Users can delete own assets" on public.portfolio_assets;
create policy "Users can delete own assets"
on public.portfolio_assets for delete to authenticated
using (auth.uid() = user_id);

-- Portfolio Transactions
create table if not exists public.portfolio_transactions (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references auth.users(id) on delete cascade,
    symbol      text not null,
    action      text not null check (action in ('BUY', 'SELL')),
    quantity    numeric(20,8) not null,
    price       numeric(20,2) not null,
    created_at  timestamptz not null default now()
);

create index if not exists idx_portfolio_tx_user
    on public.portfolio_transactions(user_id);

alter table public.portfolio_transactions enable row level security;

drop policy if exists "Users can read own transactions" on public.portfolio_transactions;
create policy "Users can read own transactions"
on public.portfolio_transactions for select to authenticated
using (auth.uid() = user_id);

drop policy if exists "Users can insert own transactions" on public.portfolio_transactions;
create policy "Users can insert own transactions"
on public.portfolio_transactions for insert to authenticated
with check (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════
-- User Settings — Budget (cloud-synced)
-- ═══════════════════════════════════════════════════

create table if not exists public.user_settings (
    user_id     uuid primary key references auth.users(id) on delete cascade,
    monthly_budget_minor integer not null default 500000,
    currency    char(3) not null default 'USD', -- Ενοποίηση νομίσματος σε USD
    updated_at  timestamptz not null default now()
);

-- Trigger: auto-update updated_at on every change
drop trigger if exists set_user_settings_updated_at on public.user_settings;
create trigger set_user_settings_updated_at
before update on public.user_settings
for each row
execute function public.set_updated_at();

alter table public.user_settings enable row level security;

drop policy if exists "Users can read own settings" on public.user_settings;
create policy "Users can read own settings"
on public.user_settings for select to authenticated
using (auth.uid() = user_id);

drop policy if exists "Users can insert own settings" on public.user_settings;
create policy "Users can insert own settings"
on public.user_settings for insert to authenticated
with check (auth.uid() = user_id);

drop policy if exists "Users can update own settings" on public.user_settings;
create policy "Users can update own settings"
on public.user_settings for update to authenticated
using (auth.uid() = user_id);
