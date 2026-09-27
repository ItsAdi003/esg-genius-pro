import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Leaf } from "lucide-react";
import { useState, type FormEvent } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getSupabaseClient, isSupabaseConfigured } from "@/lib/supabase-client";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Sign in | ESGenius" },
      { name: "description", content: "Sign in to ESGenius." },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const configured = isSupabaseConfigured();

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!configured || submitting) {
      return;
    }

    setSubmitting(true);
    setError(null);
    const { error: signInError } = await getSupabaseClient().auth.signInWithPassword({
      email: email.trim(),
      password,
    });
    setSubmitting(false);

    if (signInError) {
      setError(signInError.message);
      return;
    }

    await navigate({ to: "/" });
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-background px-4">
      <div className="ambient-bg" aria-hidden />
      <div className="relative z-10 w-full max-w-md rounded-2xl border border-border/60 bg-card/80 p-8 shadow-xl backdrop-blur-xl">
        <div className="mb-8 flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-gradient-to-br from-primary via-primary to-chart-2 text-primary-foreground">
            <Leaf className="size-5" />
          </div>
          <div>
            <p className="text-[15px] font-semibold tracking-tight">ESGenius</p>
            <p className="text-[11px] text-muted-foreground">ESG Compliance Assistant</p>
          </div>
        </div>

        <h1 className="text-2xl font-bold tracking-tight">Sign in</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Use the email and password for your ESGenius account.
        </p>

        <form className="mt-6 space-y-4" onSubmit={(event) => void onSubmit(event)}>
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>
          {error && (
            <p className="text-sm text-destructive" role="alert">
              {error}
            </p>
          )}
          <Button type="submit" className="w-full" disabled={!configured || submitting}>
            {submitting ? "Signing in…" : "Sign in"}
          </Button>
        </form>
      </div>
    </div>
  );
}
