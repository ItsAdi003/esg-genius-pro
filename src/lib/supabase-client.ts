import { createClient, type SupabaseClient } from "@supabase/supabase-js";

function readEnv(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

export function isSupabaseConfigured(): boolean {
  return (
    readEnv(import.meta.env.VITE_SUPABASE_URL).length > 0 &&
    readEnv(import.meta.env.VITE_SUPABASE_ANON_KEY).length > 0
  );
}

let client: SupabaseClient | null = null;

export function getSupabaseClient(): SupabaseClient {
  if (client) {
    return client;
  }

  const url = readEnv(import.meta.env.VITE_SUPABASE_URL);
  const anonKey = readEnv(import.meta.env.VITE_SUPABASE_ANON_KEY);
  if (!url || !anonKey) {
    throw new Error("Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY.");
  }

  client = createClient(url, anonKey, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: false,
    },
  });
  return client;
}
