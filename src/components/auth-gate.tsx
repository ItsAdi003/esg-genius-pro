import { useNavigate, useRouterState } from "@tanstack/react-router";
import type { Session } from "@supabase/supabase-js";
import { useEffect, useState, type ReactNode } from "react";

import { getSupabaseClient, isSupabaseConfigured } from "@/lib/supabase-client";

function isLoginPath(pathname: string): boolean {
  return pathname === "/login" || pathname === "/login/";
}

function SessionStatus({ label }: { label: string }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <p className="text-sm text-muted-foreground">{label}</p>
    </div>
  );
}

/**
 * Redirects to /login when there is no Supabase session.
 * /login itself stays reachable and sends an existing session back home.
 */
export function AuthGate({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const isLogin = isLoginPath(pathname);
  const configured = isSupabaseConfigured();
  const [ready, setReady] = useState(false);
  const [session, setSession] = useState<Session | null>(null);

  useEffect(() => {
    if (!configured) {
      return;
    }

    let cancelled = false;
    const supabase = getSupabaseClient();

    void supabase.auth.getSession().then(({ data }) => {
      if (cancelled) {
        return;
      }
      setSession(data.session);
      setReady(true);
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      if (cancelled) {
        return;
      }
      setSession(nextSession);
      setReady(true);
    });

    return () => {
      cancelled = true;
      subscription.unsubscribe();
    };
  }, [configured]);

  useEffect(() => {
    if (!configured || !ready) {
      return;
    }
    if (!session && !isLogin) {
      void navigate({ to: "/login", replace: true });
    } else if (session && isLogin) {
      void navigate({ to: "/", replace: true });
    }
  }, [configured, ready, session, isLogin, navigate]);

  if (!configured) {
    return (
      <SessionStatus label="Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY." />
    );
  }

  if (!ready) {
    return <SessionStatus label="Checking session…" />;
  }

  if (!session && !isLogin) {
    return <SessionStatus label="Redirecting to sign in…" />;
  }

  if (session && isLogin) {
    return <SessionStatus label="Redirecting…" />;
  }

  return children;
}
