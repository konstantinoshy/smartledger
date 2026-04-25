-- Schema για το ενεργό Supabase backend.
-- Εκτέλεση μία φορά από το Supabase SQL Editor.

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
