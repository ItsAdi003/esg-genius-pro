import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Search, ArrowRight, FileText, MinusCircle, RefreshCw, AlertCircle, Loader2 } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { ConfidenceMeter, StatusBadge } from "@/components/status-badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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
  getPrimaryEvidenceChunk,
  isLegacyRetrievalStatus,
  parseAnalysisIdSearch,
  resolveAnalysisPollingInterval,
  shouldRetryAnalysisQuery,
  summarizeAssessments,
  type AssessmentStatus,
} from "@/lib/compliance-api";

type ComplianceSearch = {
  analysisId?: number;
};

export const Route = createFileRoute("/compliance/")({
  validateSearch: (search: Record<string, unknown>): ComplianceSearch => ({
    analysisId: parseAnalysisIdSearch(search.analysisId),
  }),
  head: () => ({
    meta: [
      { title: "Compliance Gap Analysis | ESGenius" },
      {
        name: "description",
        content:
          "Requirement-level SEBI BRSR gap analysis with evidence citations, confidence scores and remediation priority.",
      },
      { property: "og:title", content: "Compliance Gap Analysis | ESGenius" },
      {
        property: "og:description",
        content:
          "Review every BRSR requirement with its assessment status, retrieved evidence and priority.",
      },
    ],
  }),
  component: ComplianceAnalysis,
});

const STATUS_FILTER_OPTIONS: { value: AssessmentStatus | "all"; label: string }[] = [
  { value: "all", label: "All Statuses" },
  { value: "COVERED", label: "Covered" },
  { value: "PARTIALLY_COVERED", label: "Partially Covered" },
  { value: "NOT_COVERED", label: "Not Covered" },
  { value: "HUMAN_REVIEW_REQUIRED", label: "Human Review Required" },
  { value: "EVIDENCE_RETRIEVED", label: "Evidence Retrieved (legacy)" },
  { value: "NO_EVIDENCE_FOUND", label: "No Evidence Found (legacy)" },
];

function ComplianceAnalysis() {
  const { analysisId } = Route.useSearch();
  const [category, setCategory] = useState("all");
  const [status, setStatus] = useState<AssessmentStatus | "all">("all");
  const [query, setQuery] = useState("");

  const analysisQuery = useQuery({
    queryKey: analysisId != null
      ? complianceQueryKeys.analysis(analysisId)
      : [...complianceQueryKeys.all, "analysis", "none"],
    queryFn: () => getComplianceAnalysis(analysisId!),
    enabled: analysisId != null,
    retry: (failureCount, error) => shouldRetryAnalysisQuery(failureCount, error),
    refetchInterval: (query) =>
      resolveAnalysisPollingInterval(query.state.data?.status),
  });

  const analysis = analysisQuery.data;
  const assessments = analysis?.assessments ?? [];

  const rows = useMemo(
    () =>
      assessments.filter((assessment) => {
        const categoryLabel = formatEsgCategory(assessment.category);
        const matchesCategory = category === "all" || categoryLabel === category;
        const matchesStatus =
          status === "all" || assessment.assessmentStatus === status;
        const haystack = `${assessment.requirementCode} ${assessment.requirementTitle}`.toLowerCase();
        const matchesQuery =
          query.trim() === "" || haystack.includes(query.trim().toLowerCase());
        return matchesCategory && matchesStatus && matchesQuery;
      }),
    [assessments, category, status, query],
  );

  const summary = useMemo(() => summarizeAssessments(assessments), [assessments]);
  const hasLegacyStatuses = summary.evidenceRetrieved > 0 || summary.noEvidenceFound > 0;

  if (analysisId == null) {
    return (
      <AppLayout
        title="Compliance Analysis"
        description="AI-assisted assessment of disclosures against framework requirements"
      >
        <div className="glass-panel p-10 text-center">
          <AlertCircle className="mx-auto size-10 text-muted-foreground" />
          <p className="mt-4 text-sm font-medium">No compliance analysis selected</p>
          <p className="mx-auto mt-2 max-w-lg text-sm text-muted-foreground">
            Upload or open a READY document and run a BRSR analysis to view real compliance
            results.
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
      <AppLayout
        title="Compliance Analysis"
        description="Loading analysis results from the backend"
      >
        <div className="space-y-4">
          <Skeleton className="h-28 w-full" />
          <div className="grid gap-4 md:grid-cols-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} className="h-20 w-full" />
            ))}
          </div>
          <Skeleton className="h-[50vh] w-full" />
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
        title={notFound ? "Analysis not found" : "Unable to load analysis"}
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

  const isInProgress = analysis.status === "IN_PROGRESS";

  return (
    <AppLayout
      title="Compliance Analysis"
      description="AI-assisted assessment of disclosures against framework requirements"
      actions={
        !isInProgress ? (
          <Button variant="outline" asChild>
            <Link to="/reports/gap-assessment" search={{ analysisId: analysis.id }}>
              View Gap Assessment
            </Link>
          </Button>
        ) : undefined
      }
    >
      {isInProgress && (
        <Alert className="mb-4">
          <Loader2 className="size-4 animate-spin" />
          <AlertTitle>Analysis in progress</AlertTitle>
          <AlertDescription>
            ESGenius is retrieving evidence and evaluating BRSR requirements. This page will update
            automatically.
            {analysis.requirementCount > 0 && (
              <> {analysis.requirementCount} requirements processed so far.</>
            )}
          </AlertDescription>
        </Alert>
      )}

      {analysis.status === "FAILED" && analysis.failureReason && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Analysis failed</AlertTitle>
          <AlertDescription>{analysis.failureReason}</AlertDescription>
        </Alert>
      )}

      {hasLegacyStatuses && (
        <Alert className="mb-4">
          <AlertTitle>Legacy retrieval-only analysis</AlertTitle>
          <AlertDescription>
            Some requirements use Phase 3C-1 retrieval statuses (Evidence Retrieved / No Evidence
            Found). These rows show document evidence only — AI classification fields may be absent.
          </AlertDescription>
        </Alert>
      )}

      <div className="surface-card flex flex-wrap items-center gap-x-10 gap-y-4 p-5">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Framework</p>
          <p className="mt-1 font-medium">{analysis.frameworkName}</p>
          <p className="text-xs text-muted-foreground">{analysis.frameworkCode}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Document</p>
          <p className="mt-1 font-medium">Document #{analysis.documentId}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Analysis Status</p>
          <p className="mt-1 font-medium">{formatAnalysisStatus(analysis.status)}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Started</p>
          <p className="mt-1 font-medium">{formatInstant(analysis.startedAt)}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Completed</p>
          <p className="mt-1 font-medium">{formatInstant(analysis.completedAt)}</p>
        </div>
        <div className="ml-auto text-right">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Requirements</p>
          <p className="mt-1 text-3xl font-semibold tabular-nums text-primary">
            {analysis.requirementCount}
          </p>
        </div>
      </div>

      {!isInProgress && (
        <>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {[
              { label: "Covered", value: summary.covered, tone: "text-success" },
              { label: "Partially Covered", value: summary.partiallyCovered, tone: "text-warning" },
              { label: "Not Covered", value: summary.notCovered, tone: "text-danger" },
              {
                label: "Human Review Required",
                value: summary.humanReviewRequired,
                tone: "text-warning",
              },
            ].map((card) => (
              <div key={card.label} className="surface-card p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">{card.label}</p>
                <p className={`mt-2 text-2xl font-semibold tabular-nums ${card.tone}`}>{card.value}</p>
              </div>
            ))}
          </div>

          {hasLegacyStatuses && (
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <div className="surface-card p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">
                  Evidence Retrieved (legacy)
                </p>
                <p className="mt-2 text-2xl font-semibold tabular-nums text-muted-foreground">
                  {summary.evidenceRetrieved}
                </p>
              </div>
              <div className="surface-card p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">
                  No Evidence Found (legacy)
                </p>
                <p className="mt-2 text-2xl font-semibold tabular-nums text-muted-foreground">
                  {summary.noEvidenceFound}
                </p>
              </div>
            </div>
          )}
        </>
      )}

      {!isInProgress && (
      <div className="surface-card mt-4 grid gap-3 p-4 md:grid-cols-3">
        <div className="relative md:col-span-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search requirement"
            className="pl-9"
          />
        </div>
        <Select value={category} onValueChange={setCategory}>
          <SelectTrigger>
            <SelectValue placeholder="ESG Category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All ESG Categories</SelectItem>
            <SelectItem value="Environmental">Environmental</SelectItem>
            <SelectItem value="Social">Social</SelectItem>
            <SelectItem value="Governance">Governance</SelectItem>
          </SelectContent>
        </Select>
        <Select
          value={status}
          onValueChange={(value) => setStatus(value as AssessmentStatus | "all")}
        >
          <SelectTrigger>
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            {STATUS_FILTER_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      )}

      {!isInProgress && (
      <div className="surface-card mt-4 overflow-hidden">
        <div className="flex items-center justify-between border-b border-border px-5 py-3">
          <p className="text-sm font-semibold">Requirement Assessment</p>
          <p className="text-xs text-muted-foreground">
            {rows.length} of {assessments.length} requirements
          </p>
        </div>
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                <TableHead className="w-24">Req. ID</TableHead>
                <TableHead className="min-w-[240px]">Requirement</TableHead>
                <TableHead>ESG Category</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Confidence</TableHead>
                <TableHead>Retrieval</TableHead>
                <TableHead className="min-w-[200px]">Evidence</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((assessment) => {
                const legacy = isLegacyRetrievalStatus(assessment.assessmentStatus);
                const confidence = legacy
                  ? null
                  : formatConfidencePercent(assessment.confidence);
                const evidencePreview = assessment.evidenceText?.trim();
                const primaryChunk = getPrimaryEvidenceChunk(assessment.evidenceChunks);
                const evidenceSourceLabel = primaryChunk
                  ? formatEvidenceSourceLabel(primaryChunk)
                  : null;

                return (
                  <TableRow key={assessment.requirementCode}>
                    <TableCell className="font-mono text-xs text-muted-foreground">
                      {assessment.requirementCode}
                    </TableCell>
                    <TableCell className="font-medium">{assessment.requirementTitle}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatEsgCategory(assessment.category)}
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={formatAssessmentStatus(assessment.assessmentStatus)} />
                    </TableCell>
                    <TableCell>
                      <ConfidenceMeter value={confidence} />
                    </TableCell>
                    <TableCell className="text-xs tabular-nums text-muted-foreground">
                      {formatRetrievalScore(assessment.retrievalScore)}
                    </TableCell>
                    <TableCell>
                      {evidencePreview ? (
                        <div className="flex items-start gap-2 text-xs">
                          <FileText className="mt-0.5 size-3.5 shrink-0 text-primary" />
                          <div className="min-w-0">
                            {evidenceSourceLabel && (
                              <p className="mb-1 font-medium text-muted-foreground">
                                {evidenceSourceLabel}
                              </p>
                            )}
                            <span className="line-clamp-2 text-muted-foreground">{evidencePreview}</span>
                          </div>
                        </div>
                      ) : (
                        <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <MinusCircle className="size-3.5" /> No retrieved evidence
                        </span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="outline" size="sm" asChild>
                        <Link
                          to="/compliance/$requirementId"
                          params={{ requirementId: assessment.requirementCode }}
                          search={{ analysisId: analysis.id }}
                        >
                          View Details <ArrowRight className="size-3.5" />
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8} className="py-10 text-center text-sm text-muted-foreground">
                    No requirements match the current filters.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      </div>
      )}
    </AppLayout>
  );
}
