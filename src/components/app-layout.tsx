import { useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import {
  LayoutDashboard,
  FileText,
  ClipboardCheck,
  Library,
  Bot,
  FileBarChart2,
  Settings,
  Bell,
  Leaf,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  TrendingUp,
  LogOut,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { motion } from "framer-motion";
import { GlobalSearch } from "@/components/global-search";
import { PrototypeBadge } from "@/components/prototype-notice";
import { ThemeToggle } from "@/components/theme-toggle";
import { getSupabaseClient } from "@/lib/supabase-client";
import {
  getSessionDisplayName,
  getSessionInitials,
  useSessionUser,
} from "@/lib/use-session-user";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTrigger, SheetTitle } from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

const nav = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/documents", label: "Documents", icon: FileText },
  { to: "/compliance", label: "Compliance Analysis", icon: ClipboardCheck },
  { to: "/comparison", label: "Company Comparison", icon: TrendingUp },
  { to: "/frameworks", label: "ESG Frameworks", icon: Library },
  { to: "/assistant", label: "AI ESG Assistant", icon: Bot },
  { to: "/reports", label: "Reports", icon: FileBarChart2, preview: true },
  { to: "/settings", label: "Settings", icon: Settings, preview: true },
];

function SidebarContent({
  onNavigate,
  collapsed = false,
}: {
  onNavigate?: () => void;
  collapsed?: boolean;
}) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  return (
    <TooltipProvider delayDuration={100}>
      <div className="flex h-full flex-col">
        <div
          className={cn(
            "flex items-center gap-2.5 py-5 transition-all duration-300",
            collapsed ? "justify-center px-2" : "px-5",
          )}
        >
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-primary via-primary to-chart-2 text-primary-foreground shadow-[0_6px_16px_-8px_color-mix(in_oklab,var(--color-primary)_80%,transparent)] ring-1 ring-foreground/10">
            <Leaf className="size-5" />
          </div>
          {!collapsed && (
            <div className="leading-tight">
              <p className="text-[15px] font-semibold tracking-tight">ESGenius</p>
              <p className="text-[11px] text-muted-foreground">ESG Compliance Assistant</p>
            </div>
          )}
        </div>

        {!collapsed && <p className="px-5 pb-2 pt-3 text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground/70">Workspace</p>}
        <nav className={cn("flex-1 space-y-1 py-1", collapsed ? "px-2" : "px-3")}>
          {nav.map((item) => {
            const active = item.to === "/" ? pathname === "/" : pathname.startsWith(item.to);
            const link = (
              <Link
                key={item.to}
                to={item.to}
                onClick={onNavigate}
                className={cn(
                  "group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200",
                  collapsed && "justify-center px-2",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground shadow-sm"
                    : "text-sidebar-foreground hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground",
                )}
              >
                {active && (
                  <span className="absolute left-0 top-1/2 h-6 w-1 -translate-y-1/2 rounded-r-full bg-primary" />
                )}
                <item.icon className="size-[18px] shrink-0" />
                {!collapsed && (
                  <span className="flex min-w-0 flex-1 items-center justify-between gap-2">
                    <span className="truncate">{item.label}</span>
                    {item.preview && <PrototypeBadge />}
                  </span>
                )}
              </Link>
            );

            return collapsed ? (
              <Tooltip key={item.to}>
                <TooltipTrigger asChild>{link}</TooltipTrigger>
                <TooltipContent side="right">{item.label}</TooltipContent>
              </Tooltip>
            ) : (
              link
            );
          })}
        </nav>

        {!collapsed && (
          <div className="m-3 space-y-2">
            <div className="glass-panel p-3">
              <p className="text-xs font-medium">AI-Assisted Assessment</p>
              <p className="mt-1 text-[11px] leading-relaxed text-muted-foreground">
                ESGenius supports compliance professionals with evidence-linked analysis. It does not
                replace human review.
              </p>
            </div>
            <p className="px-1 text-center text-[10px] text-muted-foreground/80">
              ESGenius · Research Prototype
            </p>
          </div>
        )}
      </div>
    </TooltipProvider>
  );
}

export function AppLayout({
  title,
  description,
  actions,
  children,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, ready: sessionReady } = useSessionUser();
  const displayName = getSessionDisplayName(user);
  const initials = displayName ? getSessionInitials(displayName) : "";

  async function signOut() {
    await getSupabaseClient().auth.signOut();
    queryClient.clear();
    await navigate({ to: "/login" });
  }

  return (
    <div className="relative min-h-screen bg-background">
      <div className="ambient-bg" aria-hidden />
      <div className="app-grid fixed inset-0 z-0 pointer-events-none" aria-hidden />

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 hidden overflow-hidden border-r border-sidebar-border bg-sidebar transition-[width] duration-300 lg:block",
          collapsed ? "w-[72px]" : "w-64",
        )}
      >
        <SidebarContent collapsed={collapsed} />
      </aside>

      <div className={cn("relative z-10 transition-[padding] duration-300", collapsed ? "lg:pl-[72px]" : "lg:pl-64")}>
        <header className="sticky top-0 z-30 border-b border-border bg-background/80 backdrop-blur-xl">
          <div className="flex h-[4.5rem] items-center gap-3 px-4 sm:px-6">
            <Sheet open={open} onOpenChange={setOpen}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="lg:hidden">
                  <Menu className="size-5" />
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="w-64 bg-sidebar p-0">
                <SheetTitle className="sr-only">Navigation</SheetTitle>
                <SidebarContent onNavigate={() => setOpen(false)} />
              </SheetContent>
            </Sheet>

            <Button
              variant="ghost"
              size="icon"
              className="hidden lg:inline-flex text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
              onClick={() => setCollapsed((c) => !c)}
              aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            >
              {collapsed ? (
                <PanelLeftOpen className="size-[18px]" />
              ) : (
                <PanelLeftClose className="size-[18px]" />
              )}
            </Button>

            <GlobalSearch />

            <div className="ml-auto flex items-center gap-2 sm:gap-3">
              <ThemeToggle />
              <Button variant="ghost" size="icon" className="relative text-muted-foreground transition-colors hover:bg-accent hover:text-foreground">
                <Bell className="size-[18px]" />
                <span className="status-pulse absolute right-2.5 top-2.5 size-2 rounded-full bg-primary" />
              </Button>
              {!sessionReady ? (
                <div className="flex items-center gap-3 pl-2 border-l border-border ml-1">
                  <div className="hidden space-y-1 leading-tight lg:block text-right">
                    <Skeleton className="ml-auto h-4 w-24" />
                    <Skeleton className="ml-auto h-3 w-32" />
                  </div>
                  <Skeleton className="size-9 rounded-full" />
                </div>
              ) : (
                <div className="flex items-center gap-3 pl-2 border-l border-border ml-1">
                  <div className="hidden leading-tight lg:block text-right">
                    <p className="text-sm font-medium tracking-tight text-foreground/90">{displayName}</p>
                    {user?.email && (
                      <p className="text-[11px] text-muted-foreground">{user.email}</p>
                    )}
                  </div>
                  <div className="flex size-9 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground ring-2 ring-background">
                    {initials}
                  </div>
                </div>
              )}
              <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground hover:text-foreground"
                onClick={() => void signOut()}
                aria-label="Sign out"
              >
                <LogOut className="size-4" />
                <span className="hidden sm:inline">Sign out</span>
              </Button>
            </div>
          </div>
        </header>

        <motion.main
          key={pathname}
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
          className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8"
        >
          <div className="mb-8 flex flex-wrap items-end justify-between gap-3">
            <div className="space-y-1.5">
              <h1 className="text-2xl font-semibold tracking-tight text-foreground sm:text-[1.75rem]">
                {title}
              </h1>
              {description && (
                <p className="max-w-2xl text-[15px] text-muted-foreground">{description}</p>
              )}
            </div>
            {actions && <div className="flex flex-wrap items-center gap-3">{actions}</div>}
          </div>
          {children}
        </motion.main>
      </div>
    </div>
  );
}
