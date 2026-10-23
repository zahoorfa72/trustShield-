-- TrustShield anonymous AI endpoint rate limiting.
-- The raw client IP is never stored; the Edge Function hashes it before calling this RPC.

create table if not exists public.ai_request_limits (
  bucket_key text primary key,
  window_started_at timestamptz not null default now(),
  request_count integer not null default 0 check (request_count >= 0)
);

alter table public.ai_request_limits enable row level security;
revoke all on table public.ai_request_limits from public, anon, authenticated;
grant all on table public.ai_request_limits to service_role;

create or replace function public.consume_ai_quota(
  p_bucket_key text,
  p_limit integer default 30,
  p_window_seconds integer default 60
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  current_count integer;
begin
  if p_bucket_key is null or length(p_bucket_key) <> 64
     or p_limit < 1 or p_window_seconds < 1 then
    return false;
  end if;

  insert into public.ai_request_limits (bucket_key, window_started_at, request_count)
  values (p_bucket_key, now(), 1)
  on conflict (bucket_key) do update
  set window_started_at = case
        when now() >= ai_request_limits.window_started_at + make_interval(secs => p_window_seconds)
        then now() else ai_request_limits.window_started_at end,
      request_count = case
        when now() >= ai_request_limits.window_started_at + make_interval(secs => p_window_seconds)
        then 1 else ai_request_limits.request_count + 1 end
  returning request_count into current_count;

  return current_count <= p_limit;
end;
$$;

revoke all on function public.consume_ai_quota(text, integer, integer) from public, anon, authenticated;
grant execute on function public.consume_ai_quota(text, integer, integer) to service_role;

-- Periodically remove stale keys (run from a scheduled maintenance task if needed).
