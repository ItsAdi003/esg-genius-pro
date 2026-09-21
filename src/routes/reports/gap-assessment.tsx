import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  ArrowLeft,
  Download,
  FileText,
  Leaf,
  Printer,
  RefreshCw,
} from "lucide-react";
import { useMemo } from "react";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  complianceQueryKeys,
  formatAnalysisStatus,
  formatAssessmentStatus,
  formatConfidencePercent,
  formatEsgCategory,
  formatEvidenceSourceLabel,
  formatInstant,
  formatRetrievalScore,
  getComplianceAnalysis,
  isLegacyRetrievalStatus,
  parseAnalysisIdSearch,
  summarizeAssessments,
  type AssessmentStatus,
  type RequirementAssessment,
} from "@/lib/compliance-api";
import {
  documentQueryKeys,
  formatReportingYear,
  getDocument,
} from "@/lib/document-api";

type GapAssessmentSearch = {
  analysisId?: number;
  analysisIdInvalid?: boolean;
};

const ESG_CATEGORIES = ["ENVIRONMENTAL", "SOCIAL", "GOVERNANCE"] as const;

function validateGapAssessmentSearch(search: Record<string, unknown>): GapAssessmentSearch {
  const raw = search.analysisId;
  if (raw === undefined || raw === null || raw === "") {
    return {};
  }
  const analysisId = parseAnalysisIdSearch(raw);
  if (analysisId == null) {
    return { analysisIdInvalid: true };
  }
  return { analysisId };
}

export const Route = createFileRoute("/reports/gap-assessment")({
  validateSearch: validateGapAssessmentSearch,
  head: () => ({
    meta: [
      { title: "ESG Gap Assessment Report | ESGenius" },
      {
        name: "description",
        content:
          "Requirement-level gap assessment report generated from a saved compliance analysis.",
      },
      { property: "og:title", content: "ESG Gap Assessment Report | ESGenius" },
      {
        property: "og:description",
        content:
          "Coverage summary, identified gaps, human review items and evidence-backed disclosures.",
      },
    ],
  }),
  component: GapAssessmentReport,
});

function SectionTitle({ n, children }: { n: string; children: React.ReactNode }) {
  return (
    <h2 className="mt-8 border-b border-border pb-2 text-sm font-semibold uppercase tracking-wide print:mt-6 print:break-after-avoid">
      <span className="mr-2 text-primary">{n}</span>
      {children}
    </h2>
  );
}

function SummaryStat({ label, value, tone }: { label: string; value: number; tone?: string }) {
  return (
    <div className="rounded-lg border border-border bg-muted/40 p-4 text-center print:break-inside-avoid">
      <p className={`text-3xl font-semibold tabular-nums ${tone ?? "text-primary"}`}>{value}</p>
      <p className="mt-1 text-xs text-muted-foreground">{label}</p>
    </div>
  );
}

function RequirementCard({
  assessment,
  analysisId,
  variant = "default",
}: {
  assessment: RequirementAssessment;
  analysisId: number;
  variant?: "default" | "gap" | "review" | "covered" | "legacy";
}) {
  const legacy = isLegacyRetrievalStatus(assessment.assessmentStatus);
  const confidence = legacy ? null : formatConfidencePercent(assessment.confidence);
  const borderClass =
    variant === "gap"
      ? "border-danger/25 bg-danger-soft/30"
      : variant === "review"
        ? "border-warning/30 bg-warning-soft/30"
        : variant === "covered"
          ? "border-success/25 bg-success-soft/20"
          : variant === "legacy"
            ? "border-border bg-muted/40"
            : "border-border bg-muted/20";

  const evidenceChunks = assessment.evidenceChunks ?? [];
  const hasEvidence =
    evidenceChunks.length > 0 ||
    (assessment.evidenceText != null && assessment.evidenceText.trim().length > 0);

  return (
    <li className={`rounded-lg border p-4 print:break-inside-avoid ${borderClass}`}>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium">
            <span className="font-mono text-xs text-muted-foreground">
              {assessment.requirementCode}
            </span>{" "}
            {assessment.requirementTitle}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {formatEsgCategory(assessment.category)}
          </p>
        </div>
        <StatusBadge status={formatAssessmentStatus(assessment.assessmentStatus)} />
      </div>

      <dl className="mt-3 grid gap-2 text-xs sm:grid-cols-2">
        {confidence != null && (
          <div>
            <dt className="text-muted-foreground">Confidence</dt>
            <dd className="mt-0.5 font-medium tabular-nums">{confidence}%</dd>
          </div>
        )}
        {assessment.retrievalScore != null && (
          <div>
            <dt className="text-muted-foreground">Retrieval score</dt>
            <dd className="mt-0.5 font-medium tabular-nums">
              {formatRetrievalScore(assessment.retrievalScore)}
            </dd>
          </div>
        )}
      </dl>

      {assessment.explanation && (
        <p className="mt-3 text-xs leading-relaxed text-muted-foreground whitespace-pre-wrap">
          {assessment.explanation}
        </p>
      )}
      {assessment.gap && (
        <p className="mt-2 text-xs leading-relaxed text-foreground whitespace-pre-wrap">
          <span className="font-medium">Gap: </span>
          {assessment.gap}
        </p>
      )}
      {assessment.recommendation && (
        <p className="mt-2 text-xs leading-relaxed text-muted-foreground whitespace-pre-wrap">
          <span className="font-medium text-foreground">Recommendation: </span>
          {assessment.recommendation}
        </p>
      )}

      {hasEvidence && (
        <div className="mt-3 space-y-2">
          <p className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
            <FileText className="size-3.5" /> Evidence
          </p>
          {evidenceChunks.length > 0
            ? evidenceChunks.map((chunk) => (
                <blockquote
                  key={chunk.chunkIndex}
                  className="rounded border-l-4 border-primary bg-background/60 px-3 py-2 text-xs leading-relaxed text-muted-foreground"
                >
                  <p className="mb-1 font-medium text-foreground/80">
                    {formatEvidenceSourceLabel(chunk)}
                    <span className="ml-2 tabular-nums font-normal text-muted-foreground">
                      · score {formatRetrievalScore(chunk.retrievalScore)}
                    </span>
                  </p>
                  <p className="whitespace-pre-wrap">{chunk.text}</p>
                </blockquote>
              ))
            : assessment.evidenceText && (
                <blockquote
                  className="rounded border-l-4 border-primary bg-background/60 px-3 py-2 text-xs leading-relaxed text-muted-foreground whitespace-pre-wrap"
                >
                  {assessment.evidenceText}
                </blockquote>
              )}
        </div>
      )}

      <div className="mt-3 print:hidden">
        <Button variant="outline" size="sm" asChild>
          <Link
            to="/compliance/$requirementId"
            params={{ requirementId: assessment.requirementCode }}
            search={{ analysisId }}
          >
            View requirement details
          </Link>
        </Button>
      </div>
    </li>
  );
}

function filterByStatuses(
  assessments: RequirementAssessment[],
  statuses: AssessmentStatus[],
): RequirementAssessment[] {
  return assessments.filter((a) => statuses.includes(a.assessmentStatus));
}

function GapAssessmentReport() {
  const { analysisId, analysisIdInvalid } = Route.useSearch();

  const analysisQuery = useQuery({
    queryKey: analysisId != null
      ? complianceQueryKeys.analysis(analysisId)
      : [...complianceQueryKeys.all, "analysis", "none"],
    queryFn: () => getComplianceAnalysis(analysisId!),
    enabled: analysisId != null,
  });

  const analysis = analysisQuery.data;
  const documentId = analysis?.documentId;

  const documentQuery = useQuery({
    queryKey: documentId != null
      ? documentQueryKeys.detail(documentId)
      : [...documentQueryKeys.all, "detail", "none"],
    queryFn: () => getDocument(documentId!),
    enabled: documentId != null,
  });

  const assessments = analysis?.assessments ?? [];

  const summary = useMemo(() => summarizeAssessments(assessments), [assessments]);
  const hasLegacyStatuses = summary.evidenceRetrieved > 0 || summary.noEvidenceFound > 0;

  const notCovered = useMemo(
    () => filterByStatuses(assessments, ["NOT_COVERED"]),
    [assessments],
  );
  const partiallyCovered = useMemo(
    () => filterByStatuses(assessments, ["PARTIALLY_COVERED"]),
    [assessments],
  );
  const humanReview = useMemo(
    () => filterByStatuses(assessments, ["HUMAN_REVIEW_REQUIRED"]),
    [assessments],
  );
  const covered = useMemo(() => filterByStatuses(assessments, ["COVERED"]), [assessments]);
  const legacyNoEvidence = useMemo(
    () => filterByStatuses(assessments, ["NO_EVIDENCE_FOUND"]),
    [assessments],
  );
  const legacyEvidenceRetrieved = useMemo(
    () => filterByStatuses(assessments, ["EVIDENCE_RETRIEVED"]),
    [assessments],
  );

  const gapItems = useMemo(
    () => [...notCovered, ...partiallyCovered],
    [notCovered, partiallyCovered],
  );

  const reviewItems = useMemo(
    () => [...humanReview],
    [humanReview],
  );

  const legacyItems = useMemo(
    () => [...legacyEvidenceRetrieved, ...legacyNoEvidence],
    [legacyEvidenceRetrieved, legacyNoEvidence],
  );

  const recommendations = useMemo(
    () =>
      assessments.filter(
        (a) => a.recommendation != null && a.recommendation.trim().length > 0,
      ),
    [assessments],
  );

  const categoryBreakdown = useMemo(() => {
    return ESG_CATEGORIES.map((category) => {
      const inCategory = assessments.filter((a) => a.category === category);
      const counts = summarizeAssessments(inCategory);
      return { category, counts, total: inCategory.length };
    }).filter((row) => row.total > 0);
  }, [assessments]);

  if (analysisIdInvalid) {
    return (
      <AppLayout title="Invalid analysis" description="">
        <div className="glass-panel p-10 text-center">
          <AlertCircle className="mx-auto size-10 text-muted-foreground" />
          <p className="mt-4 text-sm font-medium">Invalid analysis ID.</p>
          <p className="mx-auto mt-2 max-w-lg text-sm text-muted-foreground">
            The URL contains an analysis ID that could not be parsed. Open a compliance analysis
            from Documents to view its gap assessment.
          </p>
          <Button className="mt-6" asChild>
            <Link to="/documents">Go to Documents</Link>
          </Button>
        </div>
      </AppLayout>
    );
  }

  if (analysisId == null) {
    return (
      <AppLayout title="Gap Assessment Report" description="">
        <div className="glass-panel p-10 text-center">
          <AlertCircle className="mx-auto size-10 text-muted-foreground" />
          <p className="mt-4 text-sm font-medium">No compliance analysis selected.</p>
          <p className="mx-auto mt-2 max-w-lg text-sm text-muted-foreground">
            Open a completed compliance analysis to view its gap assessment.
          </p>
          <Button className="mt-6" asChild>
            <Link to="/documents">Go to Documents</Link>
          </Button>
        </div>
      </AppLayout>
    );
  }

  if (analysisQuery.isLoading) {
    return (
      <AppLayout title="Gap Assessment Report" description="Loading analysis data…">
        <div className="space-y-4">
          <Skeleton className="h-32 w-full max-w-4xl mx-auto" />
          <Skeleton className="h-96 w-full max-w-4xl mx-auto" />
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
        title={notFound ? "Analysis not found" : "Unable to load gap assessment"}
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
              <Link to="/documents">Go to Documents</Link>
            </Button>
          </div>
        </div>
      </AppLayout>
    );
  }

  if (!analysis) {
    return null;
  }

  const document = documentQuery.data;
  const totalAssessed = assessments.length;

  return (
    <div data-gap-assessment-report>
      <AppLayout
        title="ESG Gap Assessment Report"
        description={`Analysis #${analysis.id} · ${analysis.frameworkName}`}
        actions={
          <div className="print:hidden flex flex-wrap gap-2">
            <Button variant="outline" asChild>
              <Link to="/compliance" search={{ analysisId: analysis.id }}>
                <ArrowLeft className="size-4" /> Back to analysis
              </Link>
            </Button>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="size-4" /> Print Report
            </Button>
            <Button variant="outline" disabled>
              <Download className="size-4" /> Download PDF
            </Button>
          </div>
        }
      >
        {analysis.status === "FAILED" && (
          <Alert variant="destructive" className="mb-4 max-w-4xl mx-auto print:hidden">
            <AlertTitle>Analysis failed</AlertTitle>
            <AlertDescription>
              {analysis.failureReason ??
                "This analysis did not complete successfully. Report data may be incomplete."}
            </AlertDescription>
          </Alert>
        )}

        {totalAssessed === 0 && (
          <Alert className="mb-4 max-w-4xl mx-auto">
            <AlertTitle>No requirement assessments</AlertTitle>
            <AlertDescription>
              This analysis has no persisted requirement assessments to include in the report.
            </AlertDescription>
          </Alert>
        )}

        <article
          className="surface-card mx-auto max-w-4xl px-6 py-8 sm:px-10 sm:py-12 print:max-w-none print:border-0 print:shadow-none print:px-0 print:py-0"
        >
          <header className="flex flex-wrap items-start justify-between gap-4 border-b border-border pb-6 print:break-after-avoid">
            <div className="flex items-center gap-3">
              <span className="flex size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground print:border print:border-border">
                <Leaf className="size-5" />
              </span>
              <div>
                <p className="text-sm font-semibold">ESGenius</p>
                <p className="text-xs text-muted-foreground">AI-Assisted ESG Assessment</p>
              </div>
            </div>
            <div className="text-right text-xs text-muted-foreground">
              <p>Analysis #{analysis.id}</p>
              <p>Report generated {formatInstant(new Date().toISOString())}</p>
            </div>
          </header>

          <div className="mt-8 print:break-after-avoid">
            <p className="text-xs uppercase tracking-widest text-muted-foreground">
              ESG Gap Assessment Report
            </p>
            <h1 className="mt-2 text-2xl font-semibold tracking-tight">
              {document?.organizationName ?? `Document #${analysis.documentId}`}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Framework: {analysis.frameworkName} ({analysis.frameworkCode})
              {document?.reportingYear != null && (
                <> · Reporting year: {formatReportingYear(document.reportingYear)}</>
              )}
            </p>
            {document?.originalFilename && (
              <p className="mt-1 text-xs text-muted-foreground">
                Source document: {document.originalFilename}
              </p>
            )}
          </div>

          <dl className="mt-6 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3 print:break-inside-avoid">
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                Analysis status
              </dt>
              <dd className="mt-1 font-medium">{formatAnalysisStatus(analysis.status)}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground">Started</dt>
              <dd className="mt-1 font-medium">{formatInstant(analysis.startedAt)}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground">Completed</dt>
              <dd className="mt-1 font-medium">{formatInstant(analysis.completedAt)}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground">Document ID</dt>
              <dd className="mt-1 font-medium">#{analysis.documentId}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                Requirements in analysis
              </dt>
              <dd className="mt-1 font-medium tabular-nums">{analysis.requirementCount}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                Assessments returned
              </dt>
              <dd className="mt-1 font-medium tabular-nums">{totalAssessed}</dd>
            </div>
          </dl>

          <SectionTitle n="1.">Executive Summary</SectionTitle>
          <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
            This gap assessment report is generated from compliance analysis #{analysis.id} against{" "}
            {analysis.frameworkName} ({analysis.frameworkCode}) using document #{analysis.documentId}.
            {totalAssessed > 0 ? (
              <>
                {" "}
                Of {totalAssessed} requirement assessments, {summary.covered} are covered,{" "}
                {summary.partiallyCovered} are partially covered, {summary.notCovered} are not
                covered, and {summary.humanReviewRequired} require human review.
              </>
            ) : (
              <> No requirement assessments are available for this analysis.</>
            )}
            {hasLegacyStatuses && (
              <>
                {" "}
                Legacy retrieval-only statuses are reported separately: {summary.evidenceRetrieved}{" "}
                with evidence retrieved and {summary.noEvidenceFound} with no evidence found.
              </>
            )}
            {" "}
            This assessment is AI-assisted and intended to support, not replace, review by qualified
            compliance professionals.
          </p>

          <SectionTitle n="2.">Coverage Overview</SectionTitle>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <SummaryStat label="Total Requirements" value={analysis.requirementCount} />
            <SummaryStat label="Covered" value={summary.covered} tone="text-success" />
            <SummaryStat
              label="Partially Covered"
              value={summary.partiallyCovered}
              tone="text-warning"
            />
            <SummaryStat label="Not Covered" value={summary.notCovered} tone="text-danger" />
            <SummaryStat
              label="Human Review Required"
              value={summary.humanReviewRequired}
              tone="text-warning"
            />
          </div>

          {hasLegacyStatuses && (
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <SummaryStat
                label="Evidence Retrieved (legacy)"
                value={summary.evidenceRetrieved}
                tone="text-muted-foreground"
              />
              <SummaryStat
                label="No Evidence Found (legacy)"
                value={summary.noEvidenceFound}
                tone="text-muted-foreground"
              />
            </div>
          )}

          {categoryBreakdown.length > 0 && (
            <div className="mt-6 overflow-x-auto print:break-inside-avoid">
              <p className="mb-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                By ESG category
              </p>
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-border text-xs text-muted-foreground">
                    <th className="py-2 pr-4 font-medium">Category</th>
                    <th className="py-2 pr-4 font-medium tabular-nums">Total</th>
                    <th className="py-2 pr-4 font-medium tabular-nums">Covered</th>
                    <th className="py-2 pr-4 font-medium tabular-nums">Partial</th>
                    <th className="py-2 pr-4 font-medium tabular-nums">Not covered</th>
                    <th className="py-2 font-medium tabular-nums">Review</th>
                  </tr>
                </thead>
                <tbody>
                  {categoryBreakdown.map((row) => (
                    <tr key={row.category} className="border-b border-border/60">
                      <td className="py-2 pr-4">{formatEsgCategory(row.category)}</td>
                      <td className="py-2 pr-4 tabular-nums">{row.total}</td>
                      <td className="py-2 pr-4 tabular-nums">{row.counts.covered}</td>
                      <td className="py-2 pr-4 tabular-nums">{row.counts.partiallyCovered}</td>
                      <td className="py-2 pr-4 tabular-nums">{row.counts.notCovered}</td>
                      <td className="py-2 tabular-nums">{row.counts.humanReviewRequired}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <SectionTitle n="3.">Identified Disclosure Gaps</SectionTitle>
          {gapItems.length === 0 ? (
            <p className="mt-3 text-sm text-muted-foreground">
              No not-covered or partially covered gaps were identified in this analysis.
            </p>
          ) : (
            <ul className="mt-4 space-y-4">
              {gapItems.map((assessment) => (
                <RequirementCard
                  key={assessment.requirementCode}
                  assessment={assessment}
                  analysisId={analysis.id}
                  variant="gap"
                />
              ))}
            </ul>
          )}

          <SectionTitle n="4.">Human Review Required</SectionTitle>
          {reviewItems.length === 0 ? (
            <p className="mt-3 text-sm text-muted-foreground">
              No requirements are flagged for human review in this analysis.
            </p>
          ) : (
            <ul className="mt-4 space-y-4">
              {reviewItems.map((assessment) => (
                <RequirementCard
                  key={assessment.requirementCode}
                  assessment={assessment}
                  analysisId={analysis.id}
                  variant="review"
                />
              ))}
            </ul>
          )}

          <SectionTitle n="5.">Evidence-backed Covered Requirements</SectionTitle>
          {covered.length === 0 ? (
            <p className="mt-3 text-sm text-muted-foreground">
              No requirements are marked as covered in this analysis.
            </p>
          ) : (
            <ul className="mt-4 space-y-4">
              {covered.map((assessment) => (
                <RequirementCard
                  key={assessment.requirementCode}
                  assessment={assessment}
                  analysisId={analysis.id}
                  variant="covered"
                />
              ))}
            </ul>
          )}

          {hasLegacyStatuses && (
            <>
              <SectionTitle n="6.">Legacy Retrieval Results</SectionTitle>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
                These assessments were produced by an earlier retrieval-only version of ESGenius.
                They indicate whether evidence was retrieved, not a final disclosure-coverage
                classification.
              </p>
              <ul className="mt-4 space-y-4">
                {legacyItems.map((assessment) => (
                  <RequirementCard
                    key={assessment.requirementCode}
                    assessment={assessment}
                    analysisId={analysis.id}
                    variant="legacy"
                  />
                ))}
              </ul>
            </>
          )}

          <SectionTitle n={hasLegacyStatuses ? "7." : "6."}>Recommendations</SectionTitle>
          {recommendations.length === 0 ? (
            <p className="mt-3 text-sm text-muted-foreground">
              No recommendations were returned for requirements in this analysis.
            </p>
          ) : (
            <ul className="mt-4 space-y-3">
              {recommendations.map((assessment) => (
                <li
                  key={assessment.requirementCode}
                  className="rounded-lg border border-border bg-muted/30 p-4 print:break-inside-avoid"
                >
                  <p className="text-sm font-medium">
                    <span className="font-mono text-xs text-muted-foreground">
                      {assessment.requirementCode}
                    </span>{" "}
                    {assessment.requirementTitle}
                  </p>
                  <p className="mt-2 text-xs leading-relaxed text-muted-foreground whitespace-pre-wrap">
                    {assessment.recommendation}
                  </p>
                </li>
              ))}
            </ul>
          )}

          <footer className="mt-10 border-t border-border pt-4 text-[11px] leading-relaxed text-muted-foreground print:break-inside-avoid">
            This document presents an AI-assisted ESG reporting readiness assessment based on
            organizational documents and framework requirement sources. It does not constitute a
            statement of regulatory compliance or an assurance opinion. Findings should be validated
            by qualified compliance professionals before external disclosure.
          </footer>
        </article>
      </AppLayout>
    </div>
  );
}
