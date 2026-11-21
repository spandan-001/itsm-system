import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;
export const supabase = url && key?.startsWith('sb_publishable_')
  ? createClient(url, key, { auth: { flowType: 'pkce' } })
  : null;

export async function api(token, path, { method = 'GET', body, signal } = {}) {
  const response = await fetch('/api' + path, {
    method,
    signal,
    headers: {
      Authorization: 'Bearer ' + token,
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.json().catch(() => null);
  if (!response.ok || data === null) {
    const error = new Error(data?.detail || data?.message || `Request failed (HTTP ${response.status}).`);
    error.status = response.status;
    error.fields = data?.errors;
    throw error;
  }
  return data;
}

export const label = value => value?.toLowerCase().replaceAll('_', ' ').replace(/^./, c => c.toUpperCase()) ?? '—';

export function dateTime(value) {
  return value ? new Date(value).toLocaleString() : '—';
}
