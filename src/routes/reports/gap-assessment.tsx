import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Download, Leaf, Printer } from "lucide-react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { PriorityBadge, StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { ORG, aiInsights, requirements } from "@/lib/esg-data";

export const Route = createFileRoute("/reports/gap-assessment")({
  head: () => ({
    meta: [
      { title: "ESG Gap Assessment Report | ESGenius" },
      {
        name: "description",
        content:
          "Professional ESG gap assessment preview for ABC Industries Ltd. against SEBI BRSR for FY 2025-26.",
      },
      { property: "og:title", content: "ESG Gap Assessment Report | ESGenius" },
      {
        property: "og:description",
        content:
          "Readiness score, category performance, covered and missing disclosures with AI recommendations.",
      },
    ],
  }),
  component: GapAssessmentReport,
});

function ScoreTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-border bg-muted/40 p-4 text-center">
      <p className="text-3xl font-semibold tabular-nums text-primary">{value}%</p>
      <p className="mt-1 text-xs text-muted-foreground">{label}</p>
    </div>
  );
}

function SectionTitle({ n, children }: { n: string; children: React.ReactNode }) {
  return (
    <h2 className="mt-8 border-b border-border pb-2 text-sm font-semibold uppercase tracking-wide">
      <span className="mr-2 text-primary">{n}</span>
      {children}
    </h2>
  );
}

function GapAssessmentReport() {
  const covered = requirements.filter((r) => r.status === "Covered");
  const partial = requirements.filter((r) => r.status === "Partially Covered");
  const missing = requirements.filter(
    (r) => r.status === "Evidence Not Found" || r.status === "Human Review Required",
  );
  const high = requirements.filter((r) => r.priority === "High");

  return (
    <AppLayout
      title="ESG Gap Assessment Report"
      description="Report preview · generated from AI-assisted analysis"
      actions={
        <>
          <Button variant="outline" asChild>
            <Link to="/reports">
              <ArrowLeft className="size-4" /> Back to reports
            </Link>
          </Button>
          <Button variant="outline" onClick={() => toast("Print dialog would open")}>
            <Printer className="size-4" /> Print
          </Button>
          <Button onClick={() => toast("Download started")}>
            <Download className="size-4" /> Download PDF
          </Button>
        </>
      }
    >
      <article className="surface-card mx-auto max-w-4xl px-6 py-8 sm:px-10 sm:py-12">
        <header className="flex flex-wrap items-start justify-between gap-4 border-b border-border pb-6">
          <div className="flex items-center gap-3">
            <span className="flex size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Leaf className="size-5" />
            </span>
            <div>
              <p className="text-sm font-semibold">ESGenius</p>
              <p className="text-xs text-muted-foreground">AI-Assisted ESG Assessment</p>
            </div>
          </div>
          <div className="text-right text-xs text-muted-foreground">
            <p>Report ID: RPT-1042</p>
            <p>Generated 28 Mar 2026</p>
          </div>
        </header>

        <div className="mt-8">
          <p className="text-xs uppercase tracking-widest text-muted-foreground">
            ESG Gap Assessment Report
          </p>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight">{ORG.name}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Framework: {ORG.framework} · Reporting period: {ORG.reportingPeriod}
          </p>
        </div>

        <div className="mt-6 grid gap-3 sm:grid-cols-4">
          <ScoreTile label="Overall Reporting Readiness" value={ORG.readiness} />
          <ScoreTile label="Environmental" value={ORG.environmental} />
          <ScoreTile label="Social" value={ORG.social} />
          <ScoreTile label="Governance" value={ORG.governance} />
        </div>

        <SectionTitle n="1.">Executive Summary</SectionTitle>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
          {ORG.name} achieves an ESG reporting readiness of {ORG.readiness}% against the{" "}
          {ORG.framework} disclosure set for {ORG.reportingPeriod}, based on AI-assisted analysis of{" "}
          {ORG.documentsAnalyzed} organizational documents. {ORG.requirementsCovered} requirements
          are supported by clear evidence, {ORG.partiallyCovered} are partially covered and{" "}
          {ORG.evidenceMissing} have no retrieved supporting evidence. Environmental disclosures are
          the weakest category at {ORG.environmental}%, driven primarily by the absence of Scope 3
          value-chain emissions and the lack of quantified waste-recovery data. Governance (
          {ORG.governance}%) and social ({ORG.social}%) disclosures are comparatively mature, with
          the vigil mechanism and human rights due-diligence disclosures as the remaining
          exceptions. This assessment is AI-assisted and intended to support, not replace, review by
          qualified compliance professionals.
        </p>

        <SectionTitle n="2.">Covered Requirements</SectionTitle>
        <ul className="mt-3 space-y-2">
          {covered.map((r) => (
            <li
              key={r.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border px-3 py-2 text-sm"
            >
              <span>
                <span className="font-mono text-xs text-muted-foreground">{r.id}</span>{" "}
                <span className="font-medium">{r.title}</span>
              </span>
              <span className="flex items-center gap-3 text-xs text-muted-foreground">
                {r.evidence && `${r.evidence.document}, p.${r.evidence.page}`}
                <StatusBadge status={r.status} />
              </span>
            </li>
          ))}
        </ul>

        <SectionTitle n="3.">Partially Covered Requirements</SectionTitle>
        <ul className="mt-3 space-y-3">
          {partial.map((r) => (
            <li key={r.id} className="rounded-lg border border-warning/30 bg-warning-soft/40 p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-medium">
                  <span className="font-mono text-xs text-muted-foreground">{r.id}</span> {r.title}
                </p>
                <StatusBadge status={r.status} />
              </div>
              <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">{r.gap}</p>
            </li>
          ))}
        </ul>

        <SectionTitle n="4.">Missing Evidence</SectionTitle>
        <ul className="mt-3 space-y-3">
          {missing.map((r) => (
            <li key={r.id} className="rounded-lg border border-danger/25 bg-danger-soft/40 p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-medium">
                  <span className="font-mono text-xs text-muted-foreground">{r.id}</span> {r.title}
                </p>
                <StatusBadge status={r.status} />
              </div>
              <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">{r.analysis}</p>
            </li>
          ))}
        </ul>

        <SectionTitle n="5.">High-Priority Gaps</SectionTitle>
        <ol className="mt-3 space-y-2">
          {high.map((r, i) => (
            <li
              key={r.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border px-3 py-2 text-sm"
            >
              <span>
                <span className="mr-2 text-muted-foreground">{i + 1}.</span>
                <span className="font-mono text-xs text-muted-foreground">{r.id}</span>{" "}
                <span className="font-medium">{r.title}</span>
              </span>
              <PriorityBadge priority={r.priority} />
            </li>
          ))}
        </ol>

        <SectionTitle n="6.">AI Recommendations</SectionTitle>
        <ul className="mt-3 space-y-3">
          {aiInsights.map((a) => (
            <li key={a.title} className="rounded-lg border border-border bg-muted/40 p-3">
              <p className="text-sm font-medium">{a.title}</p>
              <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{a.body}</p>
            </li>
          ))}
        </ul>

        <footer className="mt-10 border-t border-border pt-4 text-[11px] leading-relaxed text-muted-foreground">
          This document presents an AI-assisted ESG reporting readiness assessment based on
          organizational documents and framework requirement sources. It does not constitute a
          statement of regulatory compliance or an assurance opinion. Findings should be validated
          by qualified compliance professionals before external disclosure.
        </footer>
      </article>
    </AppLayout>
  );
}
