import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { ArrowLeft, FileText, Sparkles, TriangleAlert, Lightbulb, Quote } from "lucide-react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { ConfidenceMeter, PriorityBadge, StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ORG, getRequirement } from "@/lib/esg-data";

export const Route = createFileRoute("/compliance/$requirementId")({
  loader: ({ params }) => {
    const requirement = getRequirement(params.requirementId);
    if (!requirement) throw notFound();
    return { requirement };
  },
  head: ({ loaderData }) => {
    const r = loaderData?.requirement;
    const title = r ? `${r.id} — ${r.title} | ESGenius` : "Requirement Details | ESGenius";
    const description = r
      ? `${r.status} at ${r.confidence}% confidence against SEBI BRSR, with retrieved evidence and AI-assisted recommendation.`
      : "Requirement-level ESG compliance assessment detail.";
    return {
      meta: [
        { title },
        { name: "description", content: description },
        { property: "og:title", content: title },
        { property: "og:description", content: description },
      ],
    };
  },
  component: RequirementDetails,
});

function Block({
  title,
  icon: Icon,
  children,
  tone = "default",
}: {
  title: string;
  icon: React.ElementType;
  children: React.ReactNode;
  tone?: "default" | "warning";
}) {
  return (
    <section
      className={`surface-card p-5 ${tone === "warning" ? "border-warning/40 bg-warning-soft/40" : ""}`}
    >
      <h2 className="mb-2 flex items-center gap-2 text-sm font-semibold">
        <Icon className="size-4 text-primary" />
        {title}
      </h2>
      <div className="text-sm leading-relaxed text-muted-foreground">{children}</div>
    </section>
  );
}

function RequirementDetails() {
  const { requirement: r } = Route.useLoaderData();

  return (
    <AppLayout
      title={`${r.id} — ${r.title}`}
      description={`${ORG.framework} · ${r.category} · ${ORG.reportingPeriod}`}
      actions={
        <Button variant="outline" asChild>
          <Link to="/compliance">
            <ArrowLeft className="size-4" /> Back to analysis
          </Link>
        </Button>
      }
    >
      <div className="surface-card grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Framework</p>
          <p className="mt-1.5 font-medium">{ORG.framework}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Category</p>
          <p className="mt-1.5 font-medium">{r.category}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Status</p>
          <div className="mt-1.5 flex flex-wrap items-center gap-2">
            <StatusBadge status={r.status} />
            <PriorityBadge priority={r.priority} />
          </div>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Confidence</p>
          <div className="mt-2">
            <ConfidenceMeter value={r.confidence} />
          </div>
        </div>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Block title="Framework Requirement" icon={FileText}>
            {r.frameworkText}
          </Block>

          <section className="surface-card p-5">
            <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold">
              <Quote className="size-4 text-primary" />
              Evidence {r.evidence ? "Found" : "Not Found"}
            </h2>
            {r.evidence ? (
              <div className="rounded-lg border-l-4 border-primary bg-accent/50 p-4">
                <div className="flex flex-wrap items-center gap-2 text-xs">
                  <span className="rounded border border-border bg-card px-2 py-0.5 font-medium">
                    {r.evidence.document}
                  </span>
                  <span className="rounded border border-border bg-card px-2 py-0.5 text-muted-foreground">
                    Page {r.evidence.page}
                  </span>
                </div>
                <blockquote className="mt-3 text-sm italic leading-relaxed">
                  “{r.evidence.snippet}”
                </blockquote>
              </div>
            ) : (
              <div className="rounded-lg border border-dashed border-danger/40 bg-danger-soft/50 p-4 text-sm text-danger">
                No supporting passage was retrieved for this requirement across the analysed
                document set.
              </div>
            )}
          </section>

          <Block title="AI Analysis" icon={Sparkles}>
            {r.analysis}
          </Block>

          <Block title="Gap" icon={TriangleAlert} tone="warning">
            {r.gap}
          </Block>

          <Block title="Recommendation" icon={Lightbulb}>
            {r.recommendation}
          </Block>
        </div>

        <div className="space-y-4">
          <section className="surface-card p-5">
            <h2 className="text-sm font-semibold">Reviewer Actions</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              AI-assisted assessment. Final determination rests with the compliance professional.
            </p>
            <div className="mt-4 flex flex-col gap-2">
              <Button onClick={() => toast.success("AI assessment accepted for " + r.id)}>
                Accept AI Assessment
              </Button>
              <Button
                variant="outline"
                onClick={() => toast("Marked for human review", { description: r.id })}
              >
                Mark for Human Review
              </Button>
            </div>
          </section>

          <section className="surface-card p-5">
            <h2 className="text-sm font-semibold">Add Comment</h2>
            <Textarea
              className="mt-3"
              rows={4}
              placeholder="Add context for the reviewer, e.g. where the missing data can be sourced…"
            />
            <Button
              variant="secondary"
              className="mt-3 w-full"
              onClick={() => toast.success("Comment added")}
            >
              Add Comment
            </Button>
          </section>

          <section className="surface-card p-5">
            <h2 className="text-sm font-semibold">Metadata</h2>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Requirement ID</dt>
                <dd className="font-mono text-xs">{r.id}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Disclosure type</dt>
                <dd>{r.mandatory ? "Mandatory" : "Optional"}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Framework version</dt>
                <dd>{r.version}</dd>
              </div>
            </dl>
          </section>
        </div>
      </div>
    </AppLayout>
  );
}
