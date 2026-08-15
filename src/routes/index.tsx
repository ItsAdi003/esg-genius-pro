import { createFileRoute, Link } from "@tanstack/react-router";
import {
  Gauge,
  CheckCircle2,
  AlertTriangle,
  SearchX,
  FileStack,
  ArrowRight,
  Sparkles,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  Legend,
} from "recharts";
import { AppLayout } from "@/components/app-layout";
import { RadialScore } from "@/components/radial-score";
import { useCountUp } from "@/hooks/use-count-up";
import { PriorityBadge, StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  ORG,
  aiInsights,
  categoryScores,
  gapBreakdown,
  monthlyImprovement,
  priorityGaps,
  statusDistribution,
} from "@/lib/esg-data";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "ESG Readiness Dashboard | ESGenius" },
      {
        name: "description",
        content:
          "Track SEBI BRSR reporting readiness, compliance gaps and AI-assisted ESG recommendations for ABC Industries Ltd.",
      },
      { property: "og:title", content: "ESG Readiness Dashboard | ESGenius" },
      {
        property: "og:description",
        content:
          "Overall ESG reporting readiness, category scores, priority gaps and AI recommendations in one enterprise dashboard.",
      },
    ],
  }),
  component: Dashboard,
});

const tooltipStyle = {
  contentStyle: {
    borderRadius: 10,
    border: "1px solid var(--color-border)",
    background: "var(--color-card)",
    fontSize: 12,
  },
};

function StatCard({
  label,
  value,
  suffix,
  icon: Icon,
  tone = "primary",
  delay = 0,
}: {
  label: string;
  value: number;
  suffix?: string;
  icon: React.ElementType;
  tone?: "primary" | "success" | "warning" | "danger" | "info";
  delay?: number;
}) {
  const animated = useCountUp(value);
  const tones = {
    primary: "bg-accent text-accent-foreground",
    success: "bg-success-soft text-success",
    warning: "bg-warning-soft text-warning",
    danger: "bg-danger-soft text-danger",
    info: "bg-info-soft text-info",
  } as const;
  return (
    <div className="glass-panel glass-hover p-4" style={{ animationDelay: `${delay}ms` }}>
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm text-muted-foreground">{label}</p>
        <span className={`flex size-8 items-center justify-center rounded-lg ${tones[tone]}`}>
          <Icon className="size-4" />
        </span>
      </div>
      <p className="mt-3 text-3xl font-semibold tabular-nums tracking-tight">
        {Math.round(animated)}
        {suffix && <span className="text-lg text-muted-foreground">{suffix}</span>}
      </p>
    </div>
  );
}

function Panel({
  title,
  subtitle,
  children,
  className,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section className={`glass-panel glass-hover p-5 ${className ?? ""}`}>
      <header className="mb-4">
        <h2 className="text-sm font-semibold">{title}</h2>
        {subtitle && <p className="text-xs text-muted-foreground">{subtitle}</p>}
      </header>
      {children}
    </section>
  );
}

function Dashboard() {
  return (
    <AppLayout
      title="ESG Reporting Readiness"
      description={`${ORG.name} · ${ORG.framework} · ${ORG.reportingPeriod}`}
      actions={
        <>
          <Button variant="outline" asChild>
            <Link to="/reports">Generate Report</Link>
          </Button>
          <Button asChild>
            <Link to="/compliance">Open Compliance Analysis</Link>
          </Button>
        </>
      }
    >
      <div className="mb-4 grid gap-4 lg:grid-cols-[260px_1fr]">
        <div className="glass-panel glass-hover flex flex-col items-center justify-center p-5">
          <RadialScore value={ORG.readiness} label="Overall Readiness" />
          <p className="mt-3 text-center text-xs text-muted-foreground">
            {ORG.framework} · {ORG.reportingPeriod}
          </p>
        </div>
        <div className="glass-panel glass-hover flex flex-col justify-center gap-3 p-5">
          {categoryScores.map((c, i) => (
            <div key={c.category}>
              <div className="flex items-baseline justify-between">
                <p className="text-sm font-medium">{c.category}</p>
                <p className="text-sm font-semibold tabular-nums">{c.score}%</p>
              </div>
              <Progress
                value={c.score}
                className="mt-1.5 h-2 transition-all duration-700"
                style={{ transitionDelay: `${i * 120}ms` }}
              />
            </div>
          ))}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <StatCard label="ESG Reporting Readiness" value={ORG.readiness} suffix="%" icon={Gauge} />
        <StatCard
          label="Requirements Covered"
          value={ORG.requirementsCovered}
          icon={CheckCircle2}
          tone="success"
        />
        <StatCard
          label="Partially Covered"
          value={ORG.partiallyCovered}
          icon={AlertTriangle}
          tone="warning"
        />
        <StatCard label="Evidence Missing" value={ORG.evidenceMissing} icon={SearchX} tone="danger" />
        <StatCard
          label="Documents Analyzed"
          value={ORG.documentsAnalyzed}
          icon={FileStack}
          tone="info"
        />
      </div>




      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Panel title="ESG Category Readiness" subtitle="Score by category (%)">
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={categoryScores} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
              <XAxis dataKey="category" tickLine={false} axisLine={false} fontSize={12} />
              <YAxis domain={[0, 100]} tickLine={false} axisLine={false} fontSize={12} />
              <Tooltip {...tooltipStyle} />
              <Bar dataKey="score" fill="var(--color-chart-1)" radius={[6, 6, 0, 0]} barSize={54} />
            </BarChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="Compliance Status Distribution" subtitle="Requirements by assessment status">
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie
                data={statusDistribution}
                dataKey="value"
                nameKey="name"
                innerRadius={54}
                outerRadius={84}
                paddingAngle={2}
              >
                {statusDistribution.map((s) => (
                  <Cell key={s.name} fill={s.color} />
                ))}
              </Pie>
              <Tooltip {...tooltipStyle} />
              <Legend
                verticalAlign="bottom"
                iconType="circle"
                wrapperStyle={{ fontSize: 12, paddingTop: 8 }}
              />
            </PieChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="Monthly Compliance Improvement" subtitle="Overall readiness trend (%)">
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={monthlyImprovement} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
              <XAxis dataKey="month" tickLine={false} axisLine={false} fontSize={12} />
              <YAxis domain={[40, 100]} tickLine={false} axisLine={false} fontSize={12} />
              <Tooltip {...tooltipStyle} />
              <Line
                type="monotone"
                dataKey="score"
                stroke="var(--color-chart-1)"
                strokeWidth={2.5}
                dot={{ r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="Gap Breakdown by Category" subtitle="Requirement counts by status">
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={gapBreakdown} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
              <XAxis dataKey="category" tickLine={false} axisLine={false} fontSize={12} />
              <YAxis tickLine={false} axisLine={false} fontSize={12} />
              <Tooltip {...tooltipStyle} />
              <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              <Bar dataKey="covered" name="Covered" stackId="a" fill="var(--color-success)" />
              <Bar dataKey="partial" name="Partial" stackId="a" fill="var(--color-warning)" />
              <Bar
                dataKey="missing"
                name="Evidence Not Found"
                stackId="a"
                fill="var(--color-danger)"
                radius={[6, 6, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        </Panel>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-3">
        <Panel
          title="Priority Compliance Gaps"
          subtitle="Requirements ranked by remediation priority"
          className="xl:col-span-2"
        >
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-24">ID</TableHead>
                <TableHead>Requirement</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Priority</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {priorityGaps.map((g) => (
                <TableRow key={g.id}>
                  <TableCell className="font-mono text-xs text-muted-foreground">{g.id}</TableCell>
                  <TableCell className="font-medium">{g.title}</TableCell>
                  <TableCell>
                    <StatusBadge status={g.status} />
                  </TableCell>
                  <TableCell>
                    <PriorityBadge priority={g.priority} />
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" asChild>
                      <Link to="/compliance/$requirementId" params={{ requirementId: g.id }}>
                        View <ArrowRight className="size-3.5" />
                      </Link>
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Panel>

        <Panel
          title="AI Insights & Recommendations"
          subtitle="AI-assisted assessment · verify before disclosure"
        >
          <ul className="space-y-3">
            {aiInsights.map((i) => (
              <li key={i.title} className="rounded-lg border border-border bg-muted/40 p-3">
                <div className="flex items-start gap-2">
                  <Sparkles className="mt-0.5 size-4 shrink-0 text-primary" />
                  <div>
                    <p className="text-sm font-medium">{i.title}</p>
                    <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{i.body}</p>
                    <span className="mt-2 inline-block rounded-full border border-border bg-card px-2 py-0.5 text-[11px] text-muted-foreground">
                      {i.impact}
                    </span>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </AppLayout>
  );
}
