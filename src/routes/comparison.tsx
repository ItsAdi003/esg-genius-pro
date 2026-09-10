import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import {
  TrendingUp,
  TrendingDown,
  CheckCircle2,
  AlertTriangle,
  ChevronDown,
  HelpCircle,
  ExternalLink,
  RefreshCw,
} from "lucide-react";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  ResponsiveContainer,
  CartesianGrid,
  Tooltip,
  XAxis,
  YAxis,
  Legend,
} from "recharts";
import { AppLayout } from "@/components/app-layout";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import {
  mergeMaterialIssues,
  toComparisonCompanyView,
  type ComparisonCompanyView,
  type ComparisonEventView,
  type DisplayRiskLevel,
  type DisplaySeverity,
} from "@/lib/company-comparison-adapters";
import {
  compareCompanies,
  companyEsgQueryKeys,
  getCompanies,
  type RatingBand,
} from "@/lib/company-esg-api";

export const Route = createFileRoute("/comparison")({
  head: () => ({
    meta: [
      { title: "Company ESG Comparison | ESGenius" },
      {
        name: "description",
        content:
          "Compare ESG performance, ratings, key issues and risk indicators across companies with ESGenius prototype ratings.",
      },
      { property: "og:title", content: "Company ESG Comparison | ESGenius" },
      {
        property: "og:description",
        content: "Side-by-side comparison of company ESG scores, ratings and material issues.",
      },
    ],
  }),
  component: CompanyComparison,
});

const PROTOTYPE_DISCLAIMER =
  "Current ESGenius scores, ratings and events are illustrative prototype data stored in the development database. They are not official MSCI or other third-party ESG ratings.";

const tooltipStyle = {
  contentStyle: {
    borderRadius: 10,
    border: "1px solid var(--color-border)",
    background: "var(--color-card)",
    fontSize: 12,
  },
};

const getRatingColor = (rating: RatingBand): string => {
  const colors: Record<RatingBand, string> = {
    AAA: "text-success",
    AA: "text-success",
    A: "text-success",
    BBB: "text-warning",
    BB: "text-warning",
    B: "text-danger",
    CCC: "text-danger",
  };
  return colors[rating] || "text-foreground";
};

const getRatingBgColor = (rating: RatingBand): string => {
  const colors: Record<RatingBand, string> = {
    AAA: "bg-success/10",
    AA: "bg-success/10",
    A: "bg-success/10",
    BBB: "bg-warning/10",
    BB: "bg-warning/10",
    B: "bg-danger/10",
    CCC: "bg-danger/10",
  };
  return colors[rating] || "bg-accent/10";
};

const getSeverityColor = (severity: DisplaySeverity): string => {
  const colors: Record<DisplaySeverity, string> = {
    Low: "text-info",
    Medium: "text-warning",
    High: "text-danger",
  };
  return colors[severity];
};

const getRiskLevelColor = (level: DisplayRiskLevel): string => {
  const colors: Record<DisplayRiskLevel, string> = {
    Strong: "text-success",
    Moderate: "text-warning",
    Weak: "text-danger",
  };
  return colors[level];
};

function PrototypeDisclaimer() {
  return (
    <div className="mb-6 rounded-lg border border-info/30 bg-info/5 p-4">
      <div className="flex gap-3">
        <HelpCircle className="size-5 shrink-0 text-info" />
        <p className="text-sm text-muted-foreground">{PROTOTYPE_DISCLAIMER}</p>
      </div>
    </div>
  );
}

function ComparisonSkeleton() {
  return (
    <div className="space-y-6">
      <div className="surface-card grid gap-6 p-6 md:grid-cols-2">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
      <div className="surface-card grid gap-8 p-8 md:grid-cols-2">
        <Skeleton className="mx-auto h-40 w-full max-w-xs" />
        <Skeleton className="mx-auto h-40 w-full max-w-xs" />
      </div>
      <Skeleton className="h-72 w-full rounded-xl" />
      <Skeleton className="h-80 w-full rounded-xl" />
      <Skeleton className="h-96 w-full rounded-xl" />
    </div>
  );
}

function ComparisonErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="surface-card flex flex-col items-center justify-center gap-4 px-6 py-16 text-center">
      <AlertTriangle className="size-10 text-warning" />
      <div className="max-w-md space-y-2">
        <p className="font-semibold">Unable to load ESG comparison data.</p>
        <p className="text-sm text-muted-foreground">
          Ensure the ESGenius backend is running and try again.
        </p>
      </div>
      <Button variant="outline" onClick={onRetry}>
        <RefreshCw className="size-4" />
        Retry
      </Button>
    </div>
  );
}

function CompanyComparison() {
  const [companyAId, setCompanyAId] = useState<number | null>(null);
  const [companyBId, setCompanyBId] = useState<number | null>(null);
  const [selectionInitialized, setSelectionInitialized] = useState(false);

  const companiesQuery = useQuery({
    queryKey: companyEsgQueryKeys.companies(),
    queryFn: getCompanies,
  });

  const companies = companiesQuery.data ?? [];
  const canCompare =
    companyAId != null && companyBId != null && companyAId !== companyBId;

  useEffect(() => {
    if (selectionInitialized || companies.length === 0) {
      return;
    }
    if (companies.length >= 2) {
      setCompanyAId(companies[0].id);
      setCompanyBId(companies[1].id);
    } else if (companies.length === 1) {
      setCompanyAId(companies[0].id);
    }
    setSelectionInitialized(true);
  }, [companies, selectionInitialized]);

  const comparisonQuery = useQuery({
    queryKey:
      canCompare && companyAId != null && companyBId != null
        ? companyEsgQueryKeys.comparison(companyAId, companyBId)
        : companyEsgQueryKeys.all,
    queryFn: () => compareCompanies(companyAId!, companyBId!),
    enabled: canCompare,
  });

  const companyA = useMemo(
    () =>
      comparisonQuery.data
        ? toComparisonCompanyView(comparisonQuery.data.companyA)
        : null,
    [comparisonQuery.data],
  );
  const companyB = useMemo(
    () =>
      comparisonQuery.data
        ? toComparisonCompanyView(comparisonQuery.data.companyB)
        : null,
    [comparisonQuery.data],
  );

  const materialIssueRows = useMemo(() => {
    if (!comparisonQuery.data) {
      return [];
    }
    return mergeMaterialIssues(
      comparisonQuery.data.companyA.materialIssues,
      comparisonQuery.data.companyB.materialIssues,
    );
  }, [comparisonQuery.data]);

  const chartData = useMemo(() => {
    if (!companyA || !companyB) {
      return null;
    }

    const pillarData = [
      {
        pillar: "Environmental",
        [companyA.name]: parseFloat(companyA.environmentalScore.toFixed(1)),
        [companyB.name]: parseFloat(companyB.environmentalScore.toFixed(1)),
      },
      {
        pillar: "Social",
        [companyA.name]: parseFloat(companyA.socialScore.toFixed(1)),
        [companyB.name]: parseFloat(companyB.socialScore.toFixed(1)),
      },
      {
        pillar: "Governance",
        [companyA.name]: parseFloat(companyA.governanceScore.toFixed(1)),
        [companyB.name]: parseFloat(companyB.governanceScore.toFixed(1)),
      },
    ];

    const overallData = [
      {
        category: "Overall",
        [companyA.name]: parseFloat(companyA.overallScore.toFixed(1)),
        [companyB.name]: parseFloat(companyB.overallScore.toFixed(1)),
      },
      ...pillarData.map((pillar) => ({
        category: pillar.pillar,
        [companyA.name]: pillar[companyA.name] as number,
        [companyB.name]: pillar[companyB.name] as number,
      })),
    ];

    const quarters = [
      ...new Set([
        ...companyA.historicalScores.map((item) => item.quarter),
        ...companyB.historicalScores.map((item) => item.quarter),
      ]),
    ];

    const mergedTrendData = quarters.map((quarter) => ({
      quarter,
      [companyA.name]: parseFloat(
        (companyA.historicalScores.find((item) => item.quarter === quarter)?.score ?? 0).toFixed(
          1,
        ),
      ),
      [companyB.name]: parseFloat(
        (companyB.historicalScores.find((item) => item.quarter === quarter)?.score ?? 0).toFixed(
          1,
        ),
      ),
    }));

    const trendScores = mergedTrendData.flatMap((row) => [
      row[companyA.name] as number,
      row[companyB.name] as number,
    ]);
    const trendMin = trendScores.length > 0 ? Math.min(...trendScores) : 5;
    const trendMax = trendScores.length > 0 ? Math.max(...trendScores) : 10;

    return {
      pillarData,
      overallData,
      mergedTrendData,
      trendDomain: [Math.max(0, Math.floor(trendMin - 0.5)), Math.ceil(trendMax + 0.5)] as [
        number,
        number,
      ],
    };
  }, [companyA, companyB]);

  const combinedEvents = useMemo(() => {
    if (!companyA || !companyB) {
      return [];
    }
    return [...companyA.recentEvents, ...companyB.recentEvents].sort(
      (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
    );
  }, [companyA, companyB]);

  const handleRetry = () => {
    companiesQuery.refetch();
    if (canCompare) {
      comparisonQuery.refetch();
    }
  };

  const handleCompanyAChange = (value: string) => {
    const id = Number(value);
    if (id !== companyBId) {
      setCompanyAId(id);
    }
  };

  const handleCompanyBChange = (value: string) => {
    const id = Number(value);
    if (id !== companyAId) {
      setCompanyBId(id);
    }
  };

  const isLoading =
    companiesQuery.isLoading ||
    (canCompare && (comparisonQuery.isLoading || comparisonQuery.isFetching));

  const hasError = companiesQuery.isError || (canCompare && comparisonQuery.isError);

  return (
    <AppLayout
      title="Company ESG Comparison"
      description="Compare ESG performance, ratings, key issues and risk indicators across companies."
    >
      <PrototypeDisclaimer />

      {companiesQuery.isLoading && <ComparisonSkeleton />}

      {companiesQuery.isError && !companiesQuery.isLoading && (
        <ComparisonErrorState onRetry={handleRetry} />
      )}

      {companiesQuery.isSuccess && companies.length === 0 && (
        <div className="surface-card px-6 py-12 text-center text-sm text-muted-foreground">
          No comparable companies are available yet. Seed listed companies in the backend database
          to enable comparison.
        </div>
      )}

      {companiesQuery.isSuccess && companies.length === 1 && (
        <div className="surface-card mb-6 px-6 py-8 text-center text-sm text-muted-foreground">
          At least two listed companies are required for side-by-side comparison. Only one company
          is currently available in the backend.
        </div>
      )}

      {companiesQuery.isSuccess && companies.length >= 2 && (
        <div className="surface-card mb-6 grid gap-6 p-6 md:grid-cols-2">
          <div>
            <label className="text-xs uppercase tracking-wide text-muted-foreground">
              Company A
            </label>
            <Select
              value={companyAId != null ? String(companyAId) : undefined}
              onValueChange={handleCompanyAChange}
              disabled={isLoading}
            >
              <SelectTrigger className="mt-2">
                <SelectValue placeholder="Select company" />
              </SelectTrigger>
              <SelectContent>
                {companies.map((company) => (
                  <SelectItem key={company.id} value={String(company.id)}>
                    {company.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <label className="text-xs uppercase tracking-wide text-muted-foreground">
              Company B
            </label>
            <Select
              value={companyBId != null ? String(companyBId) : undefined}
              onValueChange={handleCompanyBChange}
              disabled={isLoading}
            >
              <SelectTrigger className="mt-2">
                <SelectValue placeholder="Select company" />
              </SelectTrigger>
              <SelectContent>
                {companies.map((company) => (
                  <SelectItem key={company.id} value={String(company.id)}>
                    {company.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      )}

      {companiesQuery.isSuccess &&
        companies.length >= 2 &&
        canCompare &&
        comparisonQuery.isError && <ComparisonErrorState onRetry={handleRetry} />}

      {companiesQuery.isSuccess &&
        companies.length >= 2 &&
        canCompare &&
        isLoading &&
        !comparisonQuery.isError && <ComparisonSkeleton />}

      {!hasError &&
        !isLoading &&
        companyA &&
        companyB &&
        chartData &&
        comparisonQuery.data && (
          <>
            <div className="surface-card mb-6 grid gap-8 p-8 md:grid-cols-2">
              {[companyA, companyB].map((company) => (
                <RatingHero key={company.id} company={company} />
              ))}
            </div>

            <Card className="surface-card mb-6">
              <CardHeader>
                <CardTitle>ESG Pillar Comparison</CardTitle>
                <CardDescription>
                  Environmental, Social and Governance scores for each company
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-6">
                  {chartData.pillarData.map((pillar) => (
                    <div key={pillar.pillar}>
                      <div className="mb-3 flex items-center justify-between">
                        <p className="font-semibold">{pillar.pillar}</p>
                      </div>
                      <div className="space-y-2">
                        <PillarProgressRow
                          companyName={companyA.name}
                          score={pillar[companyA.name] as number}
                        />
                        <PillarProgressRow
                          companyName={companyB.name}
                          score={pillar[companyB.name] as number}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            <Card className="surface-card mb-6">
              <CardHeader>
                <CardTitle>Overall Score Comparison</CardTitle>
                <CardDescription>Across all ESG dimensions</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="h-80 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={chartData.overallData}
                      margin={{ top: 20, right: 30, left: 0, bottom: 0 }}
                    >
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="var(--color-border)"
                        vertical={false}
                      />
                      <XAxis
                        dataKey="category"
                        tick={{ fontSize: 12 }}
                        stroke="var(--color-muted-foreground)"
                      />
                      <YAxis
                        domain={[0, 10]}
                        tick={{ fontSize: 12 }}
                        stroke="var(--color-muted-foreground)"
                      />
                      <Tooltip {...tooltipStyle} />
                      <Legend />
                      <Bar
                        dataKey={companyA.name}
                        fill="var(--color-chart-1)"
                        radius={[6, 6, 0, 0]}
                      />
                      <Bar
                        dataKey={companyB.name}
                        fill="var(--color-chart-2)"
                        radius={[6, 6, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            <Card className="surface-card mb-6">
              <CardHeader>
                <CardTitle>Material ESG Issues</CardTitle>
                <CardDescription>
                  Key ESG areas affecting company ratings and risk profiles
                </CardDescription>
              </CardHeader>
              <CardContent>
                {materialIssueRows.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    No material issue assessments are available for the selected companies.
                  </p>
                ) : (
                  <div className="space-y-4">
                    {materialIssueRows.map((issue, idx) => (
                      <div key={issue.issueCode}>
                        <div className="mb-3">
                          <p className="mb-2 font-semibold">{issue.title}</p>
                          <div className="grid gap-4 md:grid-cols-2">
                            <MaterialIssueCell
                              companyName={companyA.name}
                              data={issue.companyA}
                            />
                            <MaterialIssueCell
                              companyName={companyB.name}
                              data={issue.companyB}
                            />
                          </div>
                        </div>
                        {idx < materialIssueRows.length - 1 && <Separator />}
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>

            <div className="surface-card mb-6 grid gap-6 md:grid-cols-2">
              {[companyA, companyB].map((company) => (
                <Card key={company.id} className="border-0 bg-transparent p-0 shadow-none">
                  <CardHeader className="pb-4">
                    <CardTitle className="text-lg">{company.name}</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-6 p-0">
                    <StrengthWeaknessList
                      title="Key Strengths"
                      icon={CheckCircle2}
                      iconClassName="text-success"
                      bulletClassName="text-success"
                      items={company.keyStrengths}
                    />
                    <Separator />
                    <StrengthWeaknessList
                      title="Areas Requiring Attention"
                      icon={AlertTriangle}
                      iconClassName="text-warning"
                      bulletClassName="text-warning"
                      items={company.keyWeaknesses}
                    />
                  </CardContent>
                </Card>
              ))}
            </div>

            <Card className="surface-card mb-6">
              <CardHeader>
                <CardTitle>Recent ESG Events & Controversies</CardTitle>
                <CardDescription>
                  Latest developments and incidents affecting ESG ratings
                </CardDescription>
              </CardHeader>
              <CardContent>
                {combinedEvents.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    No recent ESG events are recorded for the selected companies.
                  </p>
                ) : (
                  <div className="space-y-4">
                    {combinedEvents.map((event) => {
                      const company =
                        companyA.recentEvents.some((item) => item.id === event.id)
                          ? companyA
                          : companyB;
                      return (
                        <EventCard key={`${company.ticker}-${event.id}`} event={event} company={company} />
                      );
                    })}
                  </div>
                )}
              </CardContent>
            </Card>

            <Card className="surface-card mb-6">
              <CardHeader>
                <CardTitle>Comparison Insight</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm leading-relaxed text-foreground">
                  {comparisonQuery.data.comparisonInsight}
                </p>
              </CardContent>
            </Card>

            <Card className="surface-card mb-6">
              <CardHeader>
                <CardTitle>Historical Rating Trend</CardTitle>
                <CardDescription>ESGenius score progression over recent quarters</CardDescription>
              </CardHeader>
              <CardContent>
                {chartData.mergedTrendData.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Rating history is not available for the selected companies.
                  </p>
                ) : (
                  <div className="h-72 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart
                        data={chartData.mergedTrendData}
                        margin={{ top: 20, right: 30, left: 0, bottom: 0 }}
                      >
                        <CartesianGrid
                          strokeDasharray="3 3"
                          stroke="var(--color-border)"
                          vertical={false}
                        />
                        <XAxis
                          dataKey="quarter"
                          tick={{ fontSize: 12 }}
                          stroke="var(--color-muted-foreground)"
                        />
                        <YAxis
                          domain={chartData.trendDomain}
                          tick={{ fontSize: 12 }}
                          stroke="var(--color-muted-foreground)"
                        />
                        <Tooltip {...tooltipStyle} />
                        <Legend />
                        <Line
                          type="monotone"
                          dataKey={companyA.name}
                          stroke="var(--color-chart-1)"
                          strokeWidth={2}
                          dot={{ r: 4 }}
                        />
                        <Line
                          type="monotone"
                          dataKey={companyB.name}
                          stroke="var(--color-chart-2)"
                          strokeWidth={2}
                          dot={{ r: 4 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                )}
              </CardContent>
            </Card>

            <MethodologyCard />

            <div className="surface-card mb-6 grid gap-4 p-6 md:grid-cols-2">
              <Button variant="outline" asChild>
                <Link to="/compliance">
                  View Compliance Analysis
                  <ExternalLink className="ml-2 size-4" />
                </Link>
              </Button>
              <Button variant="outline" asChild>
                <Link to="/frameworks">
                  View ESG Frameworks
                  <ExternalLink className="ml-2 size-4" />
                </Link>
              </Button>
            </div>
          </>
        )}
    </AppLayout>
  );
}

function RatingHero({ company }: { company: ComparisonCompanyView }) {
  return (
    <div className="flex flex-col items-center text-center">
      <div className={`mb-3 rounded-lg ${getRatingBgColor(company.ratingBand)} px-6 py-4`}>
        <p className={`text-5xl font-bold ${getRatingColor(company.ratingBand)}`}>
          {company.ratingBand}
        </p>
      </div>
      <p className="mb-2 text-4xl font-semibold tabular-nums">
        {company.overallScore.toFixed(1)}
      </p>
      <p className="mb-4 text-sm text-muted-foreground">ESGenius Rating</p>
      <div className="flex items-center justify-center gap-1">
        {company.scoreChange > 0 ? (
          <TrendingUp className="size-4 text-success" />
        ) : (
          <TrendingDown className="size-4 text-danger" />
        )}
        <span
          className={`text-sm font-medium ${company.scoreChange > 0 ? "text-success" : "text-danger"}`}
        >
          {company.scoreChange > 0 ? "+" : ""}
          {company.scoreChange.toFixed(1)}
        </span>
      </div>
      <p className="text-xs text-muted-foreground">since previous review</p>
      <p className="mt-4 font-semibold">{company.name}</p>
      <p className="text-sm text-muted-foreground">{company.ticker}</p>
    </div>
  );
}

function PillarProgressRow({
  companyName,
  score,
}: {
  companyName: string;
  score: number;
}) {
  return (
    <div>
      <div className="mb-1 flex items-center justify-between">
        <span className="text-sm text-muted-foreground">{companyName}</span>
        <span className="text-sm font-medium">{score.toFixed(1)}</span>
      </div>
      <Progress value={(score / 10) * 100} className="h-2" />
    </div>
  );
}

function MaterialIssueCell({
  companyName,
  data,
}: {
  companyName: string;
  data: { score: number; riskLevel: DisplayRiskLevel } | null;
}) {
  if (!data) {
    return (
      <div>
        <div className="mb-2 flex items-center justify-between">
          <span className="text-sm text-muted-foreground">{companyName}</span>
          <span className="text-xs text-muted-foreground">Not assessed</span>
        </div>
        <Progress value={0} className="h-2" />
      </div>
    );
  }

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <span className="text-sm text-muted-foreground">{companyName}</span>
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium">{data.score.toFixed(1)}</span>
          <Badge variant="outline" className={`text-xs ${getRiskLevelColor(data.riskLevel)}`}>
            {data.riskLevel}
          </Badge>
        </div>
      </div>
      <Progress value={(data.score / 10) * 100} className="h-2" />
    </div>
  );
}

function StrengthWeaknessList({
  title,
  icon: Icon,
  iconClassName,
  bulletClassName,
  items,
}: {
  title: string;
  icon: React.ElementType;
  iconClassName: string;
  bulletClassName: string;
  items: string[];
}) {
  return (
    <div>
      <div className="mb-3 flex items-center gap-2">
        <Icon className={`size-4 ${iconClassName}`} />
        <p className="text-sm font-semibold">{title}</p>
      </div>
      <ul className="space-y-2">
        {items.map((item) => (
          <li key={item} className="flex gap-2 text-sm text-muted-foreground">
            <span className={bulletClassName}>•</span>
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function EventCard({
  event,
  company,
}: {
  event: ComparisonEventView;
  company: ComparisonCompanyView;
}) {
  return (
    <div className="rounded-lg border border-border/50 p-4">
      <div className="flex gap-3">
        {event.isPositive ? (
          <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-success" />
        ) : (
          <AlertTriangle className="mt-0.5 size-5 shrink-0 text-warning" />
        )}
        <div className="min-w-0 flex-1">
          <div className="mb-1 flex items-start justify-between gap-2">
            <p className="text-sm font-semibold">{event.title}</p>
            <span className="whitespace-nowrap text-xs text-muted-foreground">
              {new Date(event.date).toLocaleDateString("en-US", {
                year: "numeric",
                month: "short",
                day: "numeric",
              })}
            </span>
          </div>
          <div className="mb-2 flex flex-wrap gap-2">
            <Badge variant="outline" className="text-xs">
              {event.category}
            </Badge>
            <Badge variant="outline" className={`text-xs ${getSeverityColor(event.severity)}`}>
              {event.severity} severity
            </Badge>
            <Badge variant="outline" className="text-xs">
              {company.ticker}
            </Badge>
          </div>
          <p className="mb-2 text-sm text-muted-foreground">{event.description}</p>
          {event.scoreImpact != null && (
            <p
              className={`text-xs ${event.scoreImpact > 0 ? "text-success" : "text-danger"}`}
            >
              Prototype impact: {event.scoreImpact > 0 ? "+" : ""}
              {event.scoreImpact.toFixed(2)}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

function MethodologyCard() {
  return (
    <Card className="surface-card mb-6">
      <CardHeader>
        <CardTitle>How ESGenius Rating Will Work</CardTitle>
      </CardHeader>
      <CardContent>
        <Collapsible defaultOpen>
          <CollapsibleTrigger asChild>
            <Button variant="ghost" className="h-auto justify-start p-0">
              <ChevronDown className="mr-2 size-4" />
              <span className="text-sm font-semibold">Rating Methodology Overview</span>
            </Button>
          </CollapsibleTrigger>
          <CollapsibleContent className="pt-4">
            <div className="space-y-4">
              <div className="space-y-3 rounded-lg bg-muted/30 p-4">
                <p className="text-sm font-semibold">ESGenius Rating Framework</p>
                <div className="space-y-2 text-sm text-muted-foreground">
                  <div className="flex items-start gap-2">
                    <span className="mt-1 text-primary">→</span>
                    <span>Company ESG disclosures and reports</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="mt-1 text-primary">+</span>
                    <span>Industry-relevant ESG issues and priorities</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="mt-1 text-primary">+</span>
                    <span>Risk exposure analysis and assessment</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="mt-1 text-primary">+</span>
                    <span>Risk-management performance evaluation</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="mt-1 text-primary">+</span>
                    <span>Governance assessment and practices</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="mt-1 text-primary">+</span>
                    <span>ESG-related controversies and events</span>
                  </div>
                  <Separator className="my-2" />
                  <div className="flex items-start gap-2">
                    <span className="mt-1 font-semibold text-success">↓</span>
                    <span className="font-semibold">
                      Weighted ESG score (0–10) → Industry adjustment → Final ESGenius rating
                      (AAA–CCC)
                    </span>
                  </div>
                </div>
              </div>

              <div className="border-l-2 border-info/30 py-2 pl-4">
                <p className="text-xs text-muted-foreground">
                  <strong>Important:</strong> The scoring methodology is currently under development
                  and will be based on transparent, publicly documented ESG-rating principles. AI
                  will be used for evidence extraction and event classification, while final score
                  calculations will use deterministic backend logic.
                </p>
              </div>
            </div>
          </CollapsibleContent>
        </Collapsible>
      </CardContent>
    </Card>
  );
}
