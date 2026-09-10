import type {
  CompanyEsgProfile,
  EsgEvent,
  KeyIssueAssessment,
  RatingBand,
} from "@/lib/company-esg-api";

export type DisplayRiskLevel = "Strong" | "Moderate" | "Weak";
export type DisplaySeverity = "Low" | "Medium" | "High";

/**
 * Backend risk level → UI performance label.
 * LOW backend risk indicates strong management (displayed as "Strong").
 * HIGH backend risk indicates weak performance exposure (displayed as "Weak").
 */
export function mapRiskLevelToDisplay(riskLevel: string): DisplayRiskLevel {
  switch (riskLevel.toUpperCase()) {
    case "LOW":
      return "Strong";
    case "MODERATE":
      return "Moderate";
    case "HIGH":
      return "Weak";
    default:
      return "Moderate";
  }
}

export function formatPillar(pillar: string): string {
  switch (pillar.toUpperCase()) {
    case "ENVIRONMENTAL":
      return "Environmental";
    case "SOCIAL":
      return "Social";
    case "GOVERNANCE":
      return "Governance";
    default:
      return pillar.charAt(0).toUpperCase() + pillar.slice(1).toLowerCase();
  }
}

export function formatSeverity(severity: string): DisplaySeverity {
  switch (severity.toUpperCase()) {
    case "LOW":
      return "Low";
    case "MEDIUM":
      return "Medium";
    case "HIGH":
      return "High";
    default:
      return "Medium";
  }
}

/**
 * Latest overall score minus the immediately previous score.
 * Expects backend ratingHistory in chronological order (oldest → newest).
 */
export function deriveScoreChange(ratingHistory: { quarter: string; score: number }[]): number {
  if (ratingHistory.length < 2) {
    return 0;
  }
  const latest = ratingHistory[ratingHistory.length - 1]?.score ?? 0;
  const previous = ratingHistory[ratingHistory.length - 2]?.score ?? latest;
  return Math.round((latest - previous) * 100) / 100;
}

/**
 * Count events with negative prototype score impact or HIGH severity.
 */
export function deriveActiveControversies(events: EsgEvent[]): number {
  return events.filter(
    (event) =>
      event.severity.toUpperCase() === "HIGH" ||
      (event.scoreImpact != null && event.scoreImpact < 0),
  ).length;
}

export function deriveKeyStrengths(issues: KeyIssueAssessment[], limit = 3): string[] {
  if (issues.length === 0) {
    return ["No material issue assessments available yet."];
  }
  return [...issues]
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((issue) => `${issue.issueName} — ${issue.score.toFixed(1)}`);
}

export function deriveKeyWeaknesses(issues: KeyIssueAssessment[], limit = 3): string[] {
  if (issues.length === 0) {
    return ["No material issue assessments available yet."];
  }
  const sorted = [...issues].sort((a, b) => {
    const rank = (riskLevel: string) => {
      switch (riskLevel.toUpperCase()) {
        case "HIGH":
          return 0;
        case "MODERATE":
          return 1;
        default:
          return 2;
      }
    };
    const riskDiff = rank(a.riskLevel) - rank(b.riskLevel);
    if (riskDiff !== 0) {
      return riskDiff;
    }
    return a.score - b.score;
  });
  return sorted.slice(0, limit).map((issue) => `${issue.issueName} — ${issue.score.toFixed(1)}`);
}

export interface ComparisonEventView {
  id: number;
  title: string;
  category: string;
  date: string;
  severity: DisplaySeverity;
  description: string;
  scoreImpact?: number;
  isPositive: boolean;
}

export interface ComparisonCompanyView {
  id: number;
  name: string;
  ticker: string;
  industry: string;
  ratingBand: RatingBand;
  overallScore: number;
  environmentalScore: number;
  socialScore: number;
  governanceScore: number;
  scoreChange: number;
  activeControversies: number;
  keyStrengths: string[];
  keyWeaknesses: string[];
  materialIssues: {
    title: string;
    score: number;
    riskLevel: DisplayRiskLevel;
  }[];
  recentEvents: ComparisonEventView[];
  historicalScores: { quarter: string; score: number }[];
}

export interface ComparisonMaterialIssueRow {
  issueCode: string;
  title: string;
  companyA: { score: number; riskLevel: DisplayRiskLevel } | null;
  companyB: { score: number; riskLevel: DisplayRiskLevel } | null;
}

export function mergeMaterialIssues(
  issuesA: KeyIssueAssessment[],
  issuesB: KeyIssueAssessment[],
): ComparisonMaterialIssueRow[] {
  const codes = [
    ...new Set([...issuesA.map((issue) => issue.issueCode), ...issuesB.map((issue) => issue.issueCode)]),
  ];

  return codes.map((code) => {
    const issueA = issuesA.find((issue) => issue.issueCode === code);
    const issueB = issuesB.find((issue) => issue.issueCode === code);
    return {
      issueCode: code,
      title: issueA?.issueName ?? issueB?.issueName ?? code,
      companyA: issueA
        ? { score: issueA.score, riskLevel: mapRiskLevelToDisplay(issueA.riskLevel) }
        : null,
      companyB: issueB
        ? { score: issueB.score, riskLevel: mapRiskLevelToDisplay(issueB.riskLevel) }
        : null,
    };
  });
}

export function toComparisonCompanyView(profile: CompanyEsgProfile): ComparisonCompanyView {
  const rating = profile.currentRating;

  return {
    id: profile.id,
    name: profile.name,
    ticker: profile.ticker,
    industry: profile.industry,
    ratingBand: rating.ratingBand as RatingBand,
    overallScore: rating.overallScore,
    environmentalScore: rating.environmentalScore,
    socialScore: rating.socialScore,
    governanceScore: rating.governanceScore,
    scoreChange: deriveScoreChange(profile.ratingHistory),
    activeControversies: deriveActiveControversies(profile.recentEvents),
    keyStrengths: deriveKeyStrengths(profile.materialIssues),
    keyWeaknesses: deriveKeyWeaknesses(profile.materialIssues),
    materialIssues: profile.materialIssues.map((issue) => ({
      title: issue.issueName,
      score: issue.score,
      riskLevel: mapRiskLevelToDisplay(issue.riskLevel),
    })),
    recentEvents: profile.recentEvents.map((event) => ({
      id: event.id,
      title: event.title,
      category: formatPillar(event.pillar),
      date: event.eventDate,
      severity: formatSeverity(event.severity),
      description: event.description,
      scoreImpact: event.scoreImpact ?? undefined,
      isPositive:
        event.scoreImpact != null
          ? event.scoreImpact >= 0
          : event.severity.toUpperCase() !== "HIGH",
    })),
    historicalScores: profile.ratingHistory,
  };
}
