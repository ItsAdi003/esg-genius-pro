import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  CheckCircle2,
  FileStack,
  ClipboardCheck,
  TrendingUp,
  Library,
  ArrowRight,
  ShieldCheck,
  ArrowUpRight,
  Sparkles,
  Bot,
  FileBarChart2,
} from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { Button } from "@/components/ui/button";
import { motion } from "framer-motion";
import { Skeleton } from "@/components/ui/skeleton";
import {
  findFrameworkByCode,
  frameworkQueryKeys,
  listFrameworks,
} from "@/lib/framework-api";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "ESG Readiness Dashboard | ESGenius" },
      {
        name: "description",
        content:
          "ESGenius product overview — document ingestion, BRSR compliance analysis, and company comparison.",
      },
      { property: "og:title", content: "ESG Readiness Dashboard | ESGenius" },
      {
        property: "og:description",
        content:
          "Research prototype workspace for ESG document analysis, BRSR compliance review, and company comparison.",
      },
    ],
  }),
  component: Dashboard,
});

function CapabilityCard({
  title,
  description,
  icon: Icon,
  href,
  cta,
  stat,
  statLabel,
  delay = 0,
  loading = false,
}: {
  title: string;
  description: string;
  icon: React.ElementType;
  href: string;
  cta: string;
  stat?: string | number;
  statLabel?: string;
  delay?: number;
  loading?: boolean;
}) {
  return (
    <motion.article
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: delay / 1000, duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -4, transition: { duration: 0.18 } }}
      className="glass-panel flex h-full flex-col p-5"
    >
      <div className="flex items-start justify-between gap-3">
        <span className="flex size-10 items-center justify-center rounded-xl bg-accent text-accent-foreground">
          <Icon className="size-5" />
        </span>
        {statLabel && (
          <div className="text-right">
            {loading ? (
              <Skeleton className="ml-auto h-7 w-10" />
            ) : (
              <p className="text-2xl font-semibold tabular-nums tracking-tight">{stat ?? "—"}</p>
            )}
            <p className="text-[11px] text-muted-foreground">{statLabel}</p>
          </div>
        )}
      </div>
      <h2 className="mt-4 text-base font-semibold tracking-tight">{title}</h2>
      <p className="mt-2 flex-1 text-sm leading-relaxed text-muted-foreground">{description}</p>
      <Button variant="outline" size="sm" className="mt-5 w-fit" asChild>
        <Link to={href}>
          {cta} <ArrowRight className="size-3.5" />
        </Link>
      </Button>
    </motion.article>
  );
}

function PreviewCard({
  title,
  description,
  icon: Icon,
  href,
  delay = 0,
}: {
  title: string;
  description: string;
  icon: React.ElementType;
  href: string;
  delay?: number;
}) {
  return (
    <motion.article
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: delay / 1000, duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
      className="rounded-2xl border border-dashed border-border/80 bg-muted/20 p-5"
    >
      <div className="flex items-start gap-3">
        <span className="flex size-9 items-center justify-center rounded-lg bg-muted text-muted-foreground">
          <Icon className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-sm font-semibold">{title}</h2>
            <span className="rounded-full border border-border bg-muted/60 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
              Planned
            </span>
          </div>
          <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">{description}</p>
          <Link
            to={href}
            className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary transition-transform hover:translate-x-0.5"
          >
            View prototype preview <ArrowUpRight className="size-3.5" />
          </Link>
        </div>
      </div>
    </motion.article>
  );
}

function Dashboard() {
  const { data: frameworks, isLoading } = useQuery({
    queryKey: frameworkQueryKeys.list(),
    queryFn: listFrameworks,
  });

  const brsr = findFrameworkByCode(frameworks ?? [], "BRSR");

  return (
    <AppLayout
      title="ESG Reporting Workspace"
      description="Research prototype · document-grounded BRSR compliance analysis"
      actions={
        <>
          <Button variant="outline" asChild>
            <Link to="/documents">Upload Documents</Link>
          </Button>
          <Button asChild>
            <Link to="/compliance">Open Compliance Analysis</Link>
          </Button>
        </>
      }
    >
      <motion.section
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
        className="dashboard-hero mb-5 overflow-hidden"
      >
        <div className="relative p-5 sm:p-7">
          <div className="relative z-10">
            <div className="mb-5 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span className="flex size-5 items-center justify-center rounded-full bg-primary/15 text-primary">
                <ShieldCheck className="size-3.5" />
              </span>
              Product overview
            </div>
            <div className="max-w-2xl">
              <h2 className="text-xl font-semibold tracking-[-0.03em] text-foreground sm:text-2xl">
                Evidence-linked ESG compliance analysis for SEBI BRSR.
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                ESGenius connects uploaded documents to a prototype BRSR requirement library and
                produces evidence-backed compliance assessments. Aggregate readiness scores and
                report generation are not shown here unless you have run a specific analysis in
                Compliance Analysis.
              </p>
            </div>

            <div className="mt-6 flex flex-wrap gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full border border-success/25 bg-success-soft px-3 py-1 text-xs font-medium text-success">
                <CheckCircle2 className="size-3.5" /> Documents · live
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-full border border-success/25 bg-success-soft px-3 py-1 text-xs font-medium text-success">
                <CheckCircle2 className="size-3.5" /> Compliance · live
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-full border border-success/25 bg-success-soft px-3 py-1 text-xs font-medium text-success">
                <CheckCircle2 className="size-3.5" /> Comparison · live
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-full border border-border bg-muted/50 px-3 py-1 text-xs font-medium text-muted-foreground">
                Assistant · preview
              </span>
            </div>
          </div>
        </div>
      </motion.section>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <CapabilityCard
          title="BRSR Prototype Requirements"
          description="Browse the configured SEBI BRSR requirement subset used for compliance analysis. This is a 14-item MVP library, not the full disclosure set."
          icon={Library}
          href="/frameworks/brsr"
          cta="View requirements"
          stat={brsr?.requirementCount}
          statLabel="configured"
          delay={80}
          loading={isLoading}
        />
        <CapabilityCard
          title="Document Ingestion"
          description="Upload sustainability reports, BRSR filings, and policies. Documents are stored and processed by the backend for text extraction."
          icon={FileStack}
          href="/documents"
          cta="Open documents"
          delay={140}
        />
        <CapabilityCard
          title="Compliance Analysis"
          description="Run BRSR gap analysis against uploaded documents. View per-requirement status, evidence chunks, and human-review flags from real analyses."
          icon={ClipboardCheck}
          href="/compliance"
          cta="View analyses"
          delay={200}
        />
        <CapabilityCard
          title="Company Comparison"
          description="Compare prototype ESG profiles, rating bands, and events across seeded companies. Scores are illustrative, not third-party ratings."
          icon={TrendingUp}
          href="/comparison"
          cta="Compare companies"
          delay={260}
        />
      </div>

      <div className="mt-5 grid gap-3 lg:grid-cols-3">
        <PreviewCard
          title="AI ESG Assistant"
          description="Demonstration chat interface. Document-grounded responses are planned for a later RAG phase."
          icon={Bot}
          href="/assistant"
          delay={80}
        />
        <PreviewCard
          title="Report Generation"
          description="Gap assessment and executive report layouts are available as UI previews. PDF export is not implemented."
          icon={FileBarChart2}
          href="/reports"
          delay={140}
        />
        <PreviewCard
          title="Workspace Settings"
          description="Configuration forms illustrate intended workspace preferences. Changes are not persisted to a backend."
          icon={Sparkles}
          href="/settings"
          delay={200}
        />
      </div>
    </AppLayout>
  );
}
