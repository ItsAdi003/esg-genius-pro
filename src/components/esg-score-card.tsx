import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  complianceQueryKeys,
  ESG_INDUSTRIES,
  formatScore,
  getEsgScore,
  scorePercent,
  setAnalysisIndustry,
  type EsgKeyIssueScore,
} from "@/lib/compliance-api";

const NO_INDUSTRY = "none";

const PILLARS = [
  { key: "ENVIRONMENTAL", label: "Environmental", bar: "bg-emerald-500", text: "text-emerald-600" },
  { key: "SOCIAL", label: "Social", bar: "bg-sky-500", text: "text-sky-600" },
  { key: "GOVERNANCE", label: "Governance", bar: "bg-violet-500", text: "text-violet-600" },
] as const;

const BAND_TONE: Record<string, string> = {
  AAA: "bg-emerald-600",
  AA: "bg-emerald-500",
  A: "bg-lime-500",
  BBB: "bg-amber-400",
  BB: "bg-orange-500",
  B: "bg-red-500",
  CCC: "bg-red-700",
};

function pillarStyle(pillar: EsgKeyIssueScore["pillar"]) {
  return PILLARS.find((p) => p.key === pillar) ?? PILLARS[0];
}

function ScoreBar({ score, className }: { score: number | null; className: string }) {
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
      <div className={`h-full rounded-full ${className}`} style={{ width: `${scorePercent(score)}%` }} />
    </div>
  );
}

export function EsgScoreCard({ analysisId, canEdit = true }: { analysisId: number; canEdit?: boolean }) {
  const queryClient = useQueryClient();
  const scoreQuery = useQuery({
    queryKey: complianceQueryKeys.esgScore(analysisId),
    queryFn: () => getEsgScore(analysisId),
  });

  const industryMutation = useMutation({
    mutationFn: (industry: string | null) => setAnalysisIndustry(analysisId, industry),
    onSuccess: (score) => {
      queryClient.setQueryData(complianceQueryKeys.esgScore(analysisId), score);
      void queryClient.invalidateQueries({ queryKey: complianceQueryKeys.esgScore(analysisId) });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  if (scoreQuery.isLoading) {
    return <Skeleton className="mt-4 h-64 w-full" />;
  }
  if (scoreQuery.isError || !scoreQuery.data) {
    return (
      <Alert className="mt-4">
        <AlertDescription>The ESG score could not be loaded for this analysis.</AlertDescription>
      </Alert>
    );
  }

  const score = scoreQuery.data;
  const pillarScores = {
    ENVIRONMENTAL: score.environmentalScore,
    SOCIAL: score.socialScore,
    GOVERNANCE: score.governanceScore,
  };
  const pillarWeights = {
    ENVIRONMENTAL: score.environmentalWeight,
    SOCIAL: score.socialWeight,
    GOVERNANCE: score.governanceWeight,
  };

  return (
    <section className="surface-card mt-4 p-5" aria-label="MSCI-style ESG score">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold">MSCI-style ESG score</h2>
          <p className="mt-1 max-w-xl text-xs text-muted-foreground">
            Disclosure quality scored on the MSCI structure: key issue scores, weighted average,
            industry adjustment, AAA–CCC band. Not an official MSCI rating.
          </p>
        </div>
        <div className="w-60">
          <p className="mb-1 text-xs uppercase tracking-wide text-muted-foreground">Industry</p>
          <Select
            value={score.industry ?? NO_INDUSTRY}
            disabled={!canEdit || industryMutation.isPending}
            onValueChange={(value) => industryMutation.mutate(value === NO_INDUSTRY ? null : value)}
          >
            <SelectTrigger aria-label="Industry">
              <SelectValue placeholder="Choose an industry" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NO_INDUSTRY}>Not set (equal pillar weights)</SelectItem>
              {ESG_INDUSTRIES.map((industry) => (
                <SelectItem key={industry.value} value={industry.value}>
                  {industry.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {!score.scorable ? (
        <Alert className="mt-4">
          <AlertDescription>
            No requirement could be scored yet: every item is waiting for human review or marked not
            applicable.
          </AlertDescription>
        </Alert>
      ) : (
        <>
          <div className="mt-5 grid gap-4 lg:grid-cols-[220px_1fr]">
            <div className="flex flex-col items-center justify-center rounded-xl border border-border p-5 text-center">
              <span
                className={`rounded-lg px-5 py-2 text-3xl font-bold text-white ${BAND_TONE[score.ratingBand ?? ""] ?? "bg-muted"}`}
              >
                {score.ratingBand}
              </span>
              <p className="mt-3 text-3xl font-semibold tabular-nums">
                {formatScore(score.industryAdjustedScore)}
                <span className="text-sm font-normal text-muted-foreground"> / 10</span>
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {score.industryAdjusted
                  ? `Industry-adjusted against ${score.peerCount} ${score.industryLabel} analyses`
                  : score.industry
                    ? `Not adjusted: needs at least 3 ${score.industryLabel} analyses (has ${score.peerCount})`
                    : "Not adjusted: choose an industry"}
              </p>
              <p className="mt-2 text-xs text-muted-foreground">
                Weighted average key issue score{" "}
                <span className="font-medium text-foreground">
                  {formatScore(score.weightedAverageKeyIssueScore)}
                </span>
              </p>
            </div>

            <div className="space-y-3">
              {PILLARS.map((pillar) => (
                <div key={pillar.key}>
                  <div className="mb-1 flex items-baseline justify-between text-sm">
                    <span className={`font-medium ${pillar.text}`}>{pillar.label}</span>
                    <span className="tabular-nums text-muted-foreground">
                      {formatScore(pillarScores[pillar.key])}
                      {pillarWeights[pillar.key] != null && (
                        <span className="text-xs"> · weight {Math.round((pillarWeights[pillar.key] ?? 0) * 100)}%</span>
                      )}
                    </span>
                  </div>
                  <ScoreBar score={pillarScores[pillar.key]} className={pillar.bar} />
                </div>
              ))}
              <p className="pt-1 text-xs text-muted-foreground">
                {score.scoredRequirements} of {score.totalRequirements} requirements scored
                {score.notApplicableRequirements > 0 &&
                  ` · ${score.notApplicableRequirements} not applicable (excluded)`}
                {score.humanReviewRequirements > 0 &&
                  ` · ${score.humanReviewRequirements} awaiting human review (excluded)`}
              </p>
            </div>
          </div>

          <div className="mt-5 overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
                  <th className="py-2 pr-3 font-medium">Key issue</th>
                  <th className="py-2 pr-3 font-medium">BRSR</th>
                  <th className="py-2 pr-3 font-medium">Weight</th>
                  <th className="w-48 py-2 pr-3 font-medium">Score</th>
                  <th className="py-2 font-medium">Requirements</th>
                </tr>
              </thead>
              <tbody>
                {score.keyIssues.map((issue) => {
                  const style = pillarStyle(issue.pillar);
                  return (
                    <tr key={issue.code} className="border-b border-border/60">
                      <td className="py-2 pr-3">
                        <span className={`mr-2 inline-block size-2 rounded-full ${style.bar}`} />
                        {issue.name}
                      </td>
                      <td className="py-2 pr-3 text-muted-foreground">{issue.sections.replace(",", ", ")}</td>
                      <td className="py-2 pr-3 tabular-nums text-muted-foreground">
                        {issue.weight == null ? "—" : `${(issue.weight * 100).toFixed(1)}%`}
                      </td>
                      <td className="py-2 pr-3">
                        <div className="flex items-center gap-2">
                          <ScoreBar score={issue.score} className={style.bar} />
                          <span className="w-8 text-right tabular-nums">{formatScore(issue.score)}</span>
                        </div>
                      </td>
                      <td className="py-2 text-xs text-muted-foreground">
                        {issue.scoredRequirements} scored
                        {issue.notApplicableRequirements > 0 && ` · ${issue.notApplicableRequirements} n/a`}
                        {issue.humanReviewRequirements > 0 && ` · ${issue.humanReviewRequirements} review`}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
      <p className="mt-4 text-xs text-muted-foreground">{score.note}</p>
    </section>
  );
}
