import type { User } from "@supabase/supabase-js";
import { useEffect, useState } from "react";

import { getSupabaseClient } from "@/lib/supabase-client";

export function getSessionDisplayName(user: User | null): string {
  if (!user) {
    return "";
  }

  const fullName = user.user_metadata?.["full_name"];
  if (typeof fullName === "string" && fullName.trim()) {
    return fullName.trim();
  }

  const email = user.email ?? "";
  const localPart = email.split("@")[0]?.trim();
  return localPart || email;
}

export function getSessionInitials(displayName: string): string {
  const parts = displayName.trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return `${parts[0]![0] ?? ""}${parts[1]![0] ?? ""}`.toUpperCase();
  }
  return displayName.trim().slice(0, 2).toUpperCase();
}

export function useSessionUser() {
  const [ready, setReady] = useState(false);
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    let cancelled = false;
    const supabase = getSupabaseClient();

    void supabase.auth.getSession().then(({ data }) => {
      if (cancelled) {
        return;
      }
      setUser(data.session?.user ?? null);
      setReady(true);
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      if (cancelled) {
        return;
      }
      setUser(session?.user ?? null);
      setReady(true);
    });

    return () => {
      cancelled = true;
      subscription.unsubscribe();
    };
  }, []);

  return { user, ready };
}
