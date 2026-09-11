import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ArrowLeft,
  FileText,
  Sparkles,
  TriangleAlert,
  Lightbulb,
  RefreshCw,
  AlertCircle,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { ConfidenceMeter, StatusBadge } from "@/components/status-badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  complianceQueryKeys,
  findAssessmentByCode,
  formatAssessmentStatus,
  formatConfidencePercent,
  formatEsgCategory,
  formatInstant,
  formatRetrievalScore,
  getComplianceAnalysis,
  isLegacyRetrievalStatus,
} from "@/lib/compliance-api";

type RequirementSearch = {
  analysisId?: string;
};

export const Route = createFileRoute("/compliance/$requirementId")({
  validateSearch: (search: Record<string, unknown>): RequirementSearch => {
    const analysisId = search.analysisId;
    return {
      analysisId:
        typeof analysisId === "string" && analysisId.trim().length > 0 ? analysisId.trim() : undefined,
    };
  },
  head: () => ({
    meta: [
      { title: "Requirement Details | ESGenius" },
      {
        name: "description",
        content: "Requirement-level ESG compliance assessment detail.",
      },
      { property: "og:title", content: "Requirement Details | ESGenius" },
      {
        property: "og:description",
        content: "Requirement-level ESG compliance assessment detail.",
      },
    ],
  }),
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
  const { requirementId } = Route.useParams();
  const { analysisId: analysisIdParam } = Route.useSearch();

  const parsedAnalysisId =
    analysisIdParam != null ? Number(analysisIdParam) : Number.NaN;
  const isValidAnalysisId = Number.isInteger(parsedAnalysisId) && parsedAnalysisId > 0;

  const analysisQuery = useQuery({
    queryKey: isValidAnalysisId
      ? complianceQueryKeys.analysis(parsedAnalysisId)
      : [...complianceQueryKeys.all, "analysis", "none"],
    queryFn: () => getComplianceAnalysis(parsedAnalysisId),
    enabled: isValidAnalysisId,
  });

  const analysis = analysisQuery.data;
  const assessment =
    analysis != null ? findAssessmentByCode(analysis, requirementId) : undefined;
  const legacy =
    assessment != null ? isLegacyRetrievalStatus(assessment.assessmentStatus) : false;
  const confidence = legacy ? null : formatConfidencePercent(assessment?.confidence ?? null);
  const hasEvidence =
    (assessment?.evidenceText != null && assessment.evidenceText.trim().length > 0) ||
    (assessment?.evidenceChunks?.length ?? 0) > 0;

  if (!analysisIdParam) {
    return (
      <AppLayout title="Analysis required" description="">
        <div className="glass-panel p-8 text-center">
          <AlertCircle className="mx-auto size-8 text-muted-foreground" />
          <p className="mt-4 text-sm text-muted-foreground">
            Open this requirement from a compliance analysis to view real assessment data.
          </p>
          <Button className="mt-4" asChild>
            <Link to="/documents">Go to Documents</Link>
          </Button>
        </div>
      </AppLayout>
    );
  }

  if (!isValidAnalysisId) {
    return (
      <AppLayout title="Invalid analysis" description="">
        <div className="glass-panel p-8 text-center">
          <p className="text-sm text-muted-foreground">The analysis ID in the URL is invalid.</p>
          <Button className="mt-4" asChild>
            <Link to="/compliance">Back to compliance</Link>
          </Button>
        </div>
      </AppLayout>
    );
  }

  if (analysisQuery.isLoading) {
    return (
      <AppLayout title="Loading requirement…" description="">
        <div className="space-y-4">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      </AppLayout>
    );
  }

  if (analysisQuery.isError) {
    const notFound =
      analysisQuery.error.message.toLowerCase().includes("not found") ||
      (analysisQuery.error as { status?: number }).status === 404;

    return (
      <AppLayout
        title={notFound ? "Analysis not found" : "Unable to load requirement"}
        description=""
      >
        <div className="glass-panel p-8 text-center">
          <p className="text-sm text-muted-foreground">{analysisQuery.error.message}</p>
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            {!notFound && (
              <Button variant="outline" onClick={() => void analysisQuery.refetch()}>
                <RefreshCw className="size-4" /> Retry
              </Button>
            )}
            <Button asChild>
              <Link to="/compliance">Back to compliance</Link>
            </Button>
          </div>
        </div>
      </AppLayout>
    );
  }

  if (!analysis || !assessment) {
    return (
      <AppLayout title="Requirement not found" description="">
        <div className="glass-panel p-8 text-center">
          <p className="text-sm text-muted-foreground">
            Requirement {requirementId} was not found in analysis #{parsedAnalysisId}.
          </p>
          <Button className="mt-4" asChild>
            <Link to="/compliance" search={{ analysisId: String(parsedAnalysisId) }}>
              Back to analysis
            </Link>
          </Button>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout
      title={`${assessment.requirementCode} — ${assessment.requirementTitle}`}
      description={`${analysis.frameworkName} · ${formatEsgCategory(assessment.category)}`}
      actions={
        <Button variant="outline" asChild>
          <Link to="/compliance" search={{ analysisId: String(analysis.id) }}>
            <ArrowLeft className="size-4" /> Back to analysis
          </Link>
        </Button>
      }
    >
      {legacy && (
        <Alert className="mb-4">
          <AlertTitle>Legacy retrieval-only result</AlertTitle>
          <AlertDescription>
            This requirement uses a Phase 3C-1 retrieval status. Retrieved evidence is shown below;
            AI classification fields may be absent.
          </AlertDescription>
        </Alert>
      )}

      <div className="surface-card grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Framework</p>
          <p className="mt-1.5 font-medium">{analysis.frameworkName}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Category</p>
          <p className="mt-1.5 font-medium">{formatEsgCategory(assessment.category)}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Status</p>
          <div className="mt-1.5">
            <StatusBadge status={formatAssessmentStatus(assessment.assessmentStatus)} />
          </div>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Confidence</p>
          <div className="mt-2">
            <ConfidenceMeter value={confidence} />
          </div>
        </div>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <section className="surface-card p-5">
            <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold">
              <FileText className="size-4 text-primary" />
              Retrieved Evidence
            </h2>
            <p className="mb-3 text-xs text-muted-foreground">
              Passages retrieved from the submitted document. This is not generated by the
              classification model.
            </p>
            {hasEvidence ? (
              <div className="space-y-4">
                {assessment.evidenceChunks.length > 0 ? (
                  assessment.evidenceChunks.map((chunk) => (
                    <div
                      key={chunk.chunkIndex}
                      className="rounded-lg border-l-4 border-primary bg-accent/50 p-4"
                    >
                      <p className="text-xs font-medium text-muted-foreground">
                        Source chunk {chunk.chunkIndex}
                        <span className="ml-2 tabular-nums">
                          · score {formatRetrievalScore(chunk.retrievalScore)}
                        </span>
                      </p>
                      <pre className="mt-3 whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-foreground">
                        {chunk.text}
                      </pre>
                    </div>
                  ))
                ) : (
                  <div className="rounded-lg border-l-4 border-primary bg-accent/50 p-4">
                    <pre className="whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-foreground">
                      {assessment.evidenceText}
                    </pre>
                  </div>
                )}
              </div>
            ) : (
              <div className="rounded-lg border border-dashed border-danger/40 bg-danger-soft/50 p-4 text-sm text-danger">
                No supporting passage was retrieved for this requirement from the analysed document.
              </div>
            )}
          </section>

          {assessment.explanation ? (
            <Block title="AI Explanation" icon={Sparkles}>
              <p className="whitespace-pre-wrap">{assessment.explanation}</p>
            </Block>
          ) : !legacy ? (
            <Block title="AI Explanation" icon={Sparkles}>
              <span className="text-muted-foreground">No explanation was returned for this requirement.</span>
            </Block>
          ) : null}

          {assessment.gap ? (
            <Block title="Identified Gap" icon={TriangleAlert} tone="warning">
              <p className="whitespace-pre-wrap">{assessment.gap}</p>
            </Block>
          ) : null}

          {assessment.recommendation ? (
            <Block title="Recommendation" icon={Lightbulb}>
              <p className="whitespace-pre-wrap">{assessment.recommendation}</p>
            </Block>
          ) : null}
        </div>

        <div className="space-y-4">
          <section className="surface-card p-5">
            <h2 className="text-sm font-semibold">Reviewer Actions</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              AI-assisted assessment. Final determination rests with the compliance professional.
            </p>
            <div className="mt-4 flex flex-col gap-2">
              <Button
                onClick={() =>
                  toast.success(`AI assessment accepted for ${assessment.requirementCode}`)
                }
              >
                Accept AI Assessment
              </Button>
              <Button
                variant="outline"
                onClick={() =>
                  toast("Marked for human review", { description: assessment.requirementCode })
                }
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
                <dt className="text-muted-foreground">Requirement code</dt>
                <dd className="font-mono text-xs">{assessment.requirementCode}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Requirement ID</dt>
                <dd className="font-mono text-xs">{assessment.requirementId}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Document</dt>
                <dd>#{analysis.documentId}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Analysis</dt>
                <dd>#{analysis.id}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Retrieval score</dt>
                <dd className="tabular-nums">{formatRetrievalScore(assessment.retrievalScore)}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Analysis completed</dt>
                <dd>{formatInstant(analysis.completedAt)}</dd>
              </div>
            </dl>
          </section>
        </div>
      </div>
    </AppLayout>
  );
}
