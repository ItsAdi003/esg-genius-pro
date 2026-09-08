import { createFileRoute, Link } from "@tanstack/react-router";
import { useState, useMemo } from "react";
import {
    TrendingUp,
    TrendingDown,
    AlertCircle,
    CheckCircle2,
    AlertTriangle,
    ChevronDown,
    HelpCircle,
    ExternalLink,
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
    Cell,
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
import {
    Tooltip as UITooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from "@/components/ui/tooltip";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import {
    mockCompanies,
    getCompanyById,
    type Company,
    type RatingBand,
} from "@/lib/esg-data";

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

const getSeverityColor = (severity: "Low" | "Medium" | "High"): string => {
    const colors = {
        Low: "text-info",
        Medium: "text-warning",
        High: "text-danger",
    };
    return colors[severity];
};

const getSeverityBg = (severity: "Low" | "Medium" | "High"): string => {
    const colors = {
        Low: "bg-info/10",
        Medium: "bg-warning/10",
        High: "bg-danger/10",
    };
    return colors[severity];
};

const getRiskLevelColor = (level: "Strong" | "Moderate" | "Weak"): string => {
    const colors = {
        Strong: "text-success",
        Moderate: "text-warning",
        Weak: "text-danger",
    };
    return colors[level];
};

/**
 * Generate comparison insight based on deterministic mock data
 */
function generateComparisonInsight(company1: Company, company2: Company): string {
    const scoreGap = Math.abs(company1.overallScore - company2.overallScore);
    const leader = company1.overallScore > company2.overallScore ? company1 : company2;
    const follower = company1.overallScore > company2.overallScore ? company2 : company1;

    let insight = `${leader.name} currently has a higher prototype ESGenius score primarily because of `;

    const reasons: string[] = [];

    // Environmental comparison
    if (leader.environmentalScore > follower.environmentalScore + 0.5) {
        reasons.push("stronger environmental scores");
    }

    // Social comparison
    if (leader.socialScore > follower.socialScore + 0.5) {
        reasons.push("better social performance");
    }

    // Governance comparison
    if (leader.governanceScore > follower.governanceScore + 0.5) {
        reasons.push("superior governance");
    }

    // Controversy comparison
    if (leader.activeControversies < follower.activeControversies) {
        reasons.push(`fewer high-severity ESG controversies (${leader.activeControversies} vs ${follower.activeControversies})`);
    }

    if (reasons.length === 0) {
        reasons.push("consistent execution across ESG pillars");
    }

    insight += reasons.join(", ");

    // Add comparative strength note
    insight += `. ${follower.name} remains comparatively strong in `;
    const followerStrengths: string[] = [];

    if (
        follower.governanceScore > follower.environmentalScore &&
        follower.governanceScore > follower.socialScore
    ) {
        followerStrengths.push("Governance");
    } else if (
        follower.socialScore > follower.environmentalScore &&
        follower.socialScore > follower.governanceScore
    ) {
        followerStrengths.push("Social");
    } else if (
        follower.environmentalScore > follower.socialScore &&
        follower.environmentalScore > follower.governanceScore
    ) {
        followerStrengths.push("Environmental");
    }

    if (followerStrengths.length > 0) {
        insight += `${followerStrengths.join(" and ")} but has lower scores in selected material issues.`;
    } else {
        insight += "multiple areas but lags in overall execution.";
    }

    return insight;
}

function CompanyComparison() {
    const [companyAId, setCompanyAId] = useState("infosys");
    const [companyBId, setCompanyBId] = useState("tcs");

    const companyA = getCompanyById(companyAId);
    const companyB = getCompanyById(companyBId);

    if (!companyA || !companyB) {
        return (
            <AppLayout title="Company Comparison" description="">
                <div className="flex items-center justify-center py-12">
                    <p className="text-muted-foreground">Unable to load company data</p>
                </div>
            </AppLayout>
        );
    }

    // Ensure different companies selected
    const handleCompanyAChange = (id: string) => {
        if (id !== companyBId) {
            setCompanyAId(id);
        }
    };

    const handleCompanyBChange = (id: string) => {
        if (id !== companyAId) {
            setCompanyBId(id);
        }
    };

    // Prepare data for charts
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
        {
            category: "Environmental",
            [companyA.name]: parseFloat(companyA.environmentalScore.toFixed(1)),
            [companyB.name]: parseFloat(companyB.environmentalScore.toFixed(1)),
        },
        {
            category: "Social",
            [companyA.name]: parseFloat(companyA.socialScore.toFixed(1)),
            [companyB.name]: parseFloat(companyB.socialScore.toFixed(1)),
        },
        {
            category: "Governance",
            [companyA.name]: parseFloat(companyA.governanceScore.toFixed(1)),
            [companyB.name]: parseFloat(companyB.governanceScore.toFixed(1)),
        },
    ];

    const trendData = [
        ...companyA.historicalScores.map((item) => ({
            quarter: item.quarter,
            [companyA.name]: parseFloat(item.score.toFixed(1)),
        })),
    ];

    // Merge trend data for both companies
    const mergedTrendData = trendData.map((item) => ({
        ...item,
        [companyB.name]: parseFloat(
            (companyB.historicalScores.find((s) => s.quarter === item.quarter)?.score || 0).toFixed(1),
        ),
    }));

    const comparisonInsight = generateComparisonInsight(companyA, companyB);

    const tooltipStyle = {
        contentStyle: {
            borderRadius: 10,
            border: "1px solid var(--color-border)",
            background: "var(--color-card)",
            fontSize: 12,
        },
    };

    return (
        <AppLayout
            title="Company ESG Comparison"
            description="Compare ESG performance, ratings, key issues and risk indicators across companies."
        >
            {/* Informational note */}
            <div className="mb-6 rounded-lg border border-info/30 bg-info/5 p-4">
                <div className="flex gap-3">
                    <HelpCircle className="size-5 shrink-0 text-info" />
                    <p className="text-sm text-muted-foreground">
                        Ratings shown in the current prototype are illustrative ESGenius ratings and are not
                        official MSCI ratings.
                    </p>
                </div>
            </div>

            {/* Company Selectors */}
            <div className="surface-card mb-6 grid gap-6 p-6 md:grid-cols-2">
                <div>
                    <label className="text-xs uppercase tracking-wide text-muted-foreground">
                        Company A
                    </label>
                    <Select value={companyAId} onValueChange={handleCompanyAChange}>
                        <SelectTrigger className="mt-2">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {mockCompanies.map((company) => (
                                <SelectItem key={company.id} value={company.id}>
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
                    <Select value={companyBId} onValueChange={handleCompanyBChange}>
                        <SelectTrigger className="mt-2">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {mockCompanies.map((company) => (
                                <SelectItem key={company.id} value={company.id}>
                                    {company.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            </div>

            {/* Rating Comparison Hero */}
            <div className="surface-card mb-6 grid gap-8 p-8 md:grid-cols-2">
                {[companyA, companyB].map((company) => (
                    <div key={company.id} className="flex flex-col items-center text-center">
                        <div
                            className={`rounded-lg ${getRatingBgColor(company.ratingBand)} px-6 py-4 mb-3`}
                        >
                            <p className={`text-5xl font-bold ${getRatingColor(company.ratingBand)}`}>
                                {company.ratingBand}
                            </p>
                        </div>
                        <p className="text-4xl font-semibold tabular-nums mb-2">
                            {company.overallScore.toFixed(1)}
                        </p>
                        <p className="text-sm text-muted-foreground mb-4">ESGenius Rating</p>
                        <div className="flex items-center gap-1 justify-center">
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
                ))}
            </div>

            {/* E/S/G Pillar Comparison */}
            <Card className="surface-card mb-6">
                <CardHeader>
                    <CardTitle>ESG Pillar Comparison</CardTitle>
                    <CardDescription>
                        Environmental, Social and Governance scores for each company
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="space-y-6">
                        {pillarData.map((pillar) => (
                            <div key={pillar.pillar}>
                                <div className="mb-3 flex items-center justify-between">
                                    <p className="font-semibold">{pillar.pillar}</p>
                                </div>
                                <div className="space-y-2">
                                    <div>
                                        <div className="mb-1 flex items-center justify-between">
                                            <span className="text-sm text-muted-foreground">{companyA.name}</span>
                                            <span className="text-sm font-medium">
                                                {(pillar as Record<string, number>)[companyA.name].toFixed(1)}
                                            </span>
                                        </div>
                                        <Progress
                                            value={((pillar as Record<string, number>)[companyA.name] / 10) * 100}
                                            className="h-2"
                                        />
                                    </div>
                                    <div>
                                        <div className="mb-1 flex items-center justify-between">
                                            <span className="text-sm text-muted-foreground">{companyB.name}</span>
                                            <span className="text-sm font-medium">
                                                {(pillar as Record<string, number>)[companyB.name].toFixed(1)}
                                            </span>
                                        </div>
                                        <Progress
                                            value={((pillar as Record<string, number>)[companyB.name] / 10) * 100}
                                            className="h-2"
                                        />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* ESG Score Comparison Chart */}
            <Card className="surface-card mb-6">
                <CardHeader>
                    <CardTitle>Overall Score Comparison</CardTitle>
                    <CardDescription>Across all ESG dimensions</CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="h-80 w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart
                                data={overallData}
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
                                <Bar dataKey={companyA.name} fill="var(--color-chart-1)" radius={[6, 6, 0, 0]} />
                                <Bar dataKey={companyB.name} fill="var(--color-chart-2)" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </CardContent>
            </Card>

            {/* Material ESG Issues */}
            <Card className="surface-card mb-6">
                <CardHeader>
                    <CardTitle>Material ESG Issues</CardTitle>
                    <CardDescription>
                        Key ESG areas affecting company ratings and risk profiles
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="space-y-4">
                        {companyA.materialIssues.map((issue, idx) => (
                            <div key={issue.title}>
                                <div className="mb-3">
                                    <p className="font-semibold mb-2">{issue.title}</p>
                                    <div className="grid gap-4 md:grid-cols-2">
                                        <div>
                                            <div className="flex items-center justify-between mb-2">
                                                <span className="text-sm text-muted-foreground">{companyA.name}</span>
                                                <div className="flex items-center gap-2">
                                                    <span className="text-sm font-medium">{issue.score.toFixed(1)}</span>
                                                    <Badge variant="outline" className="text-xs">
                                                        {companyA.materialIssues[idx]?.riskLevel}
                                                    </Badge>
                                                </div>
                                            </div>
                                            <Progress value={(issue.score / 10) * 100} className="h-2" />
                                        </div>
                                        <div>
                                            <div className="flex items-center justify-between mb-2">
                                                <span className="text-sm text-muted-foreground">{companyB.name}</span>
                                                <div className="flex items-center gap-2">
                                                    <span className="text-sm font-medium">
                                                        {companyB.materialIssues[idx]?.score.toFixed(1)}
                                                    </span>
                                                    <Badge variant="outline" className="text-xs">
                                                        {companyB.materialIssues[idx]?.riskLevel}
                                                    </Badge>
                                                </div>
                                            </div>
                                            <Progress
                                                value={(companyB.materialIssues[idx]?.score / 10) * 100}
                                                className="h-2"
                                            />
                                        </div>
                                    </div>
                                </div>
                                {idx < companyA.materialIssues.length - 1 && <Separator />}
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* Strengths and Weaknesses */}
            <div className="surface-card mb-6 grid gap-6 md:grid-cols-2">
                {[companyA, companyB].map((company) => (
                    <Card key={company.id} className="bg-transparent border-0 shadow-none p-0">
                        <CardHeader className="pb-4">
                            <CardTitle className="text-lg">{company.name}</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-6 p-0">
                            <div>
                                <div className="flex items-center gap-2 mb-3">
                                    <CheckCircle2 className="size-4 text-success" />
                                    <p className="font-semibold text-sm">Key Strengths</p>
                                </div>
                                <ul className="space-y-2">
                                    {company.keyStrengths.map((strength, idx) => (
                                        <li key={idx} className="flex gap-2 text-sm text-muted-foreground">
                                            <span className="text-success">•</span>
                                            <span>{strength}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                            <Separator />
                            <div>
                                <div className="flex items-center gap-2 mb-3">
                                    <AlertTriangle className="size-4 text-warning" />
                                    <p className="font-semibold text-sm">Areas Requiring Attention</p>
                                </div>
                                <ul className="space-y-2">
                                    {company.keyWeaknesses.map((weakness, idx) => (
                                        <li key={idx} className="flex gap-2 text-sm text-muted-foreground">
                                            <span className="text-warning">•</span>
                                            <span>{weakness}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </CardContent>
                    </Card>
                ))}
            </div>

            {/* Recent ESG Events & Controversies */}
            <Card className="surface-card mb-6">
                <CardHeader>
                    <CardTitle>Recent ESG Events & Controversies</CardTitle>
                    <CardDescription>
                        Latest developments and incidents affecting ESG ratings
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="space-y-4">
                        {[...companyA.recentEvents, ...companyB.recentEvents]
                            .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
                            .map((event) => {
                                const isCompanyAEvent = companyA.recentEvents.some((e) => e.id === event.id);
                                const company = isCompanyAEvent ? companyA : companyB;

                                return (
                                    <div key={event.id} className="rounded-lg border border-border/50 p-4">
                                        <div className="flex gap-3">
                                            {event.isPositive ? (
                                                <CheckCircle2 className="size-5 shrink-0 text-success mt-0.5" />
                                            ) : (
                                                <AlertTriangle className="size-5 shrink-0 text-warning mt-0.5" />
                                            )}
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-start justify-between gap-2 mb-1">
                                                    <p className="font-semibold text-sm">{event.title}</p>
                                                    <span className="text-xs text-muted-foreground whitespace-nowrap">
                                                        {new Date(event.date).toLocaleDateString("en-US", {
                                                            year: "numeric",
                                                            month: "short",
                                                            day: "numeric",
                                                        })}
                                                    </span>
                                                </div>
                                                <div className="flex flex-wrap gap-2 mb-2">
                                                    <Badge variant="outline" className="text-xs">
                                                        {event.category}
                                                    </Badge>
                                                    <Badge
                                                        variant="outline"
                                                        className={`text-xs ${getSeverityColor(event.severity)}`}
                                                    >
                                                        {event.severity} severity
                                                    </Badge>
                                                    <Badge variant="outline" className="text-xs">
                                                        {company.ticker}
                                                    </Badge>
                                                </div>
                                                <p className="text-sm text-muted-foreground mb-2">{event.description}</p>
                                                {event.scoreImpact && (
                                                    <p
                                                        className={`text-xs ${event.scoreImpact > 0 ? "text-success" : "text-danger"
                                                            }`}
                                                    >
                                                        Prototype impact: {event.scoreImpact > 0 ? "+" : ""}
                                                        {event.scoreImpact.toFixed(2)}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                    </div>
                </CardContent>
            </Card>

            {/* Comparison Insight */}
            <Card className="surface-card mb-6">
                <CardHeader>
                    <CardTitle>Comparison Insight</CardTitle>
                </CardHeader>
                <CardContent>
                    <p className="text-sm leading-relaxed text-foreground">{comparisonInsight}</p>
                </CardContent>
            </Card>

            {/* Rating Trend */}
            <Card className="surface-card mb-6">
                <CardHeader>
                    <CardTitle>Historical Rating Trend</CardTitle>
                    <CardDescription>ESGenius score progression over recent quarters</CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="h-72 w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart
                                data={mergedTrendData}
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
                                    domain={[5, 10]}
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
                </CardContent>
            </Card>

            {/* Methodology Explanation */}
            <Card className="surface-card mb-6">
                <CardHeader>
                    <CardTitle>How ESGenius Rating Will Work</CardTitle>
                </CardHeader>
                <CardContent>
                    <Collapsible defaultOpen>
                        <CollapsibleTrigger asChild>
                            <Button variant="ghost" className="h-auto p-0 justify-start">
                                <ChevronDown className="size-4 mr-2" />
                                <span className="font-semibold text-sm">Rating Methodology Overview</span>
                            </Button>
                        </CollapsibleTrigger>
                        <CollapsibleContent className="pt-4">
                            <div className="space-y-4">
                                <div className="bg-muted/30 rounded-lg p-4 space-y-3">
                                    <p className="text-sm font-semibold">ESGenius Rating Framework</p>
                                    <div className="space-y-2 text-sm text-muted-foreground">
                                        <div className="flex items-start gap-2">
                                            <span className="text-primary mt-1">→</span>
                                            <span>Company ESG disclosures and reports</span>
                                        </div>
                                        <div className="flex items-start gap-2">
                                            <span className="text-primary mt-1">+</span>
                                            <span>Industry-relevant ESG issues and priorities</span>
                                        </div>
                                        <div className="flex items-start gap-2">
                                            <span className="text-primary mt-1">+</span>
                                            <span>Risk exposure analysis and assessment</span>
                                        </div>
                                        <div className="flex items-start gap-2">
                                            <span className="text-primary mt-1">+</span>
                                            <span>Risk-management performance evaluation</span>
                                        </div>
                                        <div className="flex items-start gap-2">
                                            <span className="text-primary mt-1">+</span>
                                            <span>Governance assessment and practices</span>
                                        </div>
                                        <div className="flex items-start gap-2">
                                            <span className="text-primary mt-1">+</span>
                                            <span>ESG-related controversies and events</span>
                                        </div>
                                        <Separator className="my-2" />
                                        <div className="flex items-start gap-2">
                                            <span className="text-success font-semibold mt-1">↓</span>
                                            <span className="font-semibold">
                                                Weighted ESG score (0–10) → Industry adjustment → Final ESGenius rating
                                                (AAA–CCC)
                                            </span>
                                        </div>
                                    </div>
                                </div>

                                <div className="border-l-2 border-info/30 pl-4 py-2">
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

            {/* Links to Existing Functionality */}
            <div className="surface-card mb-6 grid gap-4 md:grid-cols-2 p-6">
                <Button variant="outline" asChild>
                    <Link to="/compliance">
                        View Compliance Analysis
                        <ExternalLink className="size-4 ml-2" />
                    </Link>
                </Button>
                <Button variant="outline" asChild>
                    <Link to="/frameworks">
                        View ESG Frameworks
                        <ExternalLink className="size-4 ml-2" />
                    </Link>
                </Button>
            </div>
        </AppLayout>
    );
}
