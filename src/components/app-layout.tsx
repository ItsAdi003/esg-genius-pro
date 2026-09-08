import { Link, useRouterState } from "@tanstack/react-router";
import {
  LayoutDashboard,
  FileText,
  ClipboardCheck,
  Library,
  Bot,
  FileBarChart2,
  Settings,
  Bell,
  ChevronDown,
  Leaf,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  TrendingUp,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { GlobalSearch } from "@/components/global-search";
import { ThemeToggle } from "@/components/theme-toggle";
import { ORG } from "@/lib/esg-data";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTrigger, SheetTitle } from "@/components/ui/sheet";
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
  { to: "/reports", label: "Reports", icon: FileBarChart2 },
  { to: "/settings", label: "Settings", icon: Settings },
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
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-primary via-primary to-chart-2 text-primary-foreground shadow-[0_10px_25px_-12px_color-mix(in_oklab,var(--color-primary)_90%,transparent)] ring-1 ring-white/20">
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
                    ? "bg-sidebar-accent text-sidebar-accent-foreground shadow-[inset_0_1px_0_rgba(255,255,255,0.1),0_8px_20px_-15px_rgba(0,0,0,0.9)]"
                    : "text-muted-foreground hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground hover:translate-x-0.5",
                )}
              >
                {active && (
                  <span className="absolute left-0 top-1/2 h-6 w-1 -translate-y-1/2 rounded-r-full bg-primary" />
                )}
                <item.icon className="size-[18px] shrink-0" />
                {!collapsed && item.label}
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
          <div className="glass-panel m-3 p-3">
            <p className="text-xs font-medium">AI-Assisted Assessment</p>
            <p className="mt-1 text-[11px] leading-relaxed text-muted-foreground">
              ESGenius supports compliance professionals with evidence-linked analysis. It does not
              replace human review.
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

  return (
    <div className="relative min-h-screen bg-background">
      <div className="ambient-bg" aria-hidden />
      <div className="app-grid fixed inset-0 z-0 pointer-events-none" aria-hidden />

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 hidden overflow-hidden border-r border-sidebar-border bg-sidebar/80 backdrop-blur-xl transition-[width] duration-300 lg:block",
          collapsed ? "w-[72px]" : "w-64",
        )}
      >
        <div className="pointer-events-none absolute -left-24 top-0 size-64 rounded-full bg-primary/15 blur-3xl" aria-hidden />
        <SidebarContent collapsed={collapsed} />
      </aside>

      <div className={cn("relative z-10 transition-[padding] duration-300", collapsed ? "lg:pl-[72px]" : "lg:pl-64")}>
        <header className="sticky top-0 z-30 border-b border-border/55 bg-background/55 backdrop-blur-2xl shadow-[0_8px_30px_-20px_rgba(0,0,0,0.9)]">
          <div className="flex h-[4.5rem] items-center gap-3 px-4 sm:px-6">
            <Sheet open={open} onOpenChange={setOpen}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="lg:hidden">
                  <Menu className="size-5" />
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="w-64 bg-sidebar/90 p-0 backdrop-blur-xl border-r-white/5">
                <SheetTitle className="sr-only">Navigation</SheetTitle>
                <SidebarContent onNavigate={() => setOpen(false)} />
              </SheetContent>
            </Sheet>

            <Button
              variant="ghost"
              size="icon"
              className="hidden lg:inline-flex text-muted-foreground hover:text-foreground hover:bg-white/5 transition-colors"
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
              <Button variant="ghost" size="icon" className="relative hover:bg-white/5 transition-colors text-muted-foreground hover:text-foreground">
                <Bell className="size-[18px]" />
                <span className="status-pulse absolute right-2.5 top-2.5 size-2 rounded-full bg-primary shadow-[0_0_8px_rgba(20,184,166,0.8)]" />
              </Button>
              <div className="hidden items-center gap-2 rounded-full border border-white/5 bg-white/5 px-3 py-1.5 backdrop-blur sm:flex shadow-sm hover:bg-white/10 transition-colors cursor-pointer">
                <span className="flex size-6 items-center justify-center rounded-full bg-primary/20 text-[10px] font-semibold text-primary border border-primary/30">
                  AB
                </span>
                <span className="text-sm font-medium tracking-tight text-foreground/90">{ORG.name}</span>
                <ChevronDown className="size-4 text-muted-foreground" />
              </div>
              <div className="flex items-center gap-3 pl-2 border-l border-white/5 ml-1">
                <div className="hidden leading-tight lg:block text-right">
                  <p className="text-sm font-medium tracking-tight text-foreground/90">Priya Nair</p>
                  <p className="text-[11px] text-primary/80 font-medium">ESG Lead</p>
                </div>
                <div className="flex size-9 items-center justify-center rounded-full bg-gradient-to-br from-primary to-blue-500 text-xs font-semibold text-white shadow-md ring-2 ring-background">
                  PN
                </div>
              </div>
            </div>
          </div>
        </header>

        <AnimatePresence mode="wait">
          <motion.main
            key={pathname}
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.32, ease: [0.22, 1, 0.36, 1] }}
            className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8"
          >
            <div className="mb-8 flex flex-wrap items-end justify-between gap-3">
              <div className="space-y-1">
                <motion.h1
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.1, duration: 0.4 }}
                  className="text-3xl font-bold tracking-[-0.035em] text-foreground sm:text-[2rem]"
                >
                  {title}
                </motion.h1>
                {description && (
                  <motion.p
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.2, duration: 0.4 }}
                    className="text-[15px] text-muted-foreground max-w-2xl"
                  >
                    {description}
                  </motion.p>
                )}
              </div>
              {actions && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.2, duration: 0.4 }}
                  className="flex flex-wrap items-center gap-3"
                >
                  {actions}
                </motion.div>
              )}
            </div>
            {children}
          </motion.main>
        </AnimatePresence>
      </div>
    </div>
  );
}
