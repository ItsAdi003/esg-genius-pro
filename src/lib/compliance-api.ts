import { apiFetch } from "@/lib/api-config";
import { formatInstant } from "@/lib/document-api";

/** Backend AnalysisStatus enum — dev.esgenius.entity.AnalysisStatus */
export type AnalysisStatus = "IN_PROGRESS" | "COMPLETED" | "FAILED";

/** Poll interval while an analysis is running (ms). */
export const ANALYSIS_POLLING_INTERVAL_MS = 2000;

/** TanStack Query refetch interval — polls only while status is IN_PROGRESS. */
export function resolveAnalysisPollingInterval(
  status: AnalysisStatus | undefined,
): number | false {
  if (status === "IN_PROGRESS") {
    return ANALYSIS_POLLING_INTERVAL_MS;
  }
  return false;
}

/** Backend AssessmentStatus enum — dev.esgenius.entity.AssessmentStatus */
export type AssessmentStatus =
  | "COVERED"
  | "PARTIALLY_COVERED"
  | "NOT_COVERED"
  | "HUMAN_REVIEW_REQUIRED"
  | "NOT_APPLICABLE"
  | "EVIDENCE_RETRIEVED"
  | "NO_EVIDENCE_FOUND";

export interface EvidenceChunk {
  chunkIndex: number;
  pageNumber: number | null;
  text: string;
  retrievalScore: number;
}

export function formatEvidenceSourceLabel(chunk: EvidenceChunk): string {
  if (chunk.pageNumber != null) {
    return `Page ${chunk.pageNumber}`;
  }
  return `Source chunk ${chunk.chunkIndex}`;
}

export function getPrimaryEvidenceChunk(
  chunks: EvidenceChunk[],
): EvidenceChunk | undefined {
  if (chunks.length === 0) {
    return undefined;
  }
  return chunks.reduce((best, chunk) =>
    chunk.retrievalScore > best.retrievalScore ? chunk : best,
  );
}

export interface RequirementAssessment {
  requirementId: number;
  requirementCode: string;
  requirementTitle: string;
  category: string;
  assessmentStatus: AssessmentStatus;
  confidence: number | null;
  explanation: string | null;
  gap: string | null;
  recommendation: string | null;
  retrievalScore: number | null;
  evidenceText: string | null;
  evidenceChunks: EvidenceChunk[];
  /** BRSR section ("A", "B", "P1".."P9") and indicator type; null for analyses run on the earlier 14-item set. */
  section?: string | null;
  indicatorType?: string | null;
  /** Reviewer's reason when the status is NOT_APPLICABLE. */
  notApplicableReason?: string | null;
}

export interface ComplianceAnalysis {
  id: number;
  documentId: number;
  frameworkId: number;
  frameworkCode: string;
  frameworkName: string;
  status: AnalysisStatus;
  startedAt: string;
  completedAt: string | null;
  failureReason: string | null;
  requirementCount: number;
  assessments: RequirementAssessment[];
}

export interface ComplianceAnalysisSummary {
  id: number;
  documentId: number;
  frameworkId: number;
  frameworkCode: string;
  frameworkName: string;
  status: AnalysisStatus;
  startedAt: string;
  completedAt: string | null;
  failureReason: string | null;
  requirementCount: number;
  coveredCount: number;
  partiallyCoveredCount: number;
  notCoveredCount: number;
  humanReviewRequiredCount: number;
  evidenceRetrievedCount: number;
  noEvidenceFoundCount: number;
}

/** Backend StartAnalysisRequest — frameworkId or frameworkCode required */
export interface StartAnalysisRequest {
  frameworkId?: number | null;
  frameworkCode?: string | null;
}

export interface AssessmentSummaryCounts {
  covered: number;
  partiallyCovered: number;
  notCovered: number;
  humanReviewRequired: number;
  evidenceRetrieved: number;
  noEvidenceFound: number;
}

export class ComplianceApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ComplianceApiError";
    this.status = status;
  }
}

export function shouldRetryAnalysisQuery(failureCount: number, error: unknown): boolean {
  if (failureCount >= 2) {
    return false;
  }
  if (error instanceof ComplianceApiError && error.status === 404) {
    return false;
  }
  return true;
}

/** TanStack Router JSON-stringifies string search values (e.g. analysisId="10"). Use number in navigation. */
export function parseAnalysisIdSearch(value: unknown): number | undefined {
  if (typeof value === "number") {
    return Number.isInteger(value) && value > 0 ? value : undefined;
  }

  if (typeof value === "string") {
    const normalized = value.trim().replace(/^"|"$/g, "");
    if (!normalized) {
      return undefined;
    }
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined;
  }

  return undefined;
}

export const complianceQueryKeys = {
  all: ["compliance"] as const,
  analysis: (analysisId: number) => [...complianceQueryKeys.all, "analysis", analysisId] as const,
  esgScore: (analysisId: number) => [...complianceQueryKeys.all, "esgScore", analysisId] as const,
  documentAnalyses: (documentId: number) =>
    [...complianceQueryKeys.all, "documentAnalyses", documentId] as const,
};

async function parseJsonResponse<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (!response.ok) {
    let message = fallbackMessage;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore non-JSON error bodies
    }
    throw new ComplianceApiError(message, response.status);
  }

  return response.json() as Promise<T>;
}

/**
 * POST /api/v1/documents/{documentId}/analyses
 * Returns quickly with status IN_PROGRESS (HTTP 202 Accepted).
 */
export async function createComplianceAnalysis(
  documentId: number,
  frameworkCode = "BRSR",
): Promise<ComplianceAnalysis> {
  const body: StartAnalysisRequest = { frameworkCode };
  const response = await apiFetch(`/api/v1/documents/${documentId}/analyses`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return parseJsonResponse(response, `Failed to start compliance analysis for document ${documentId}`);
}

/**
 * GET /api/v1/analyses/{analysisId}
 */
export async function getComplianceAnalysis(analysisId: number): Promise<ComplianceAnalysis> {
  const response = await apiFetch(`/api/v1/analyses/${analysisId}`);
  return parseJsonResponse(response, `Failed to fetch analysis ${analysisId}`);
}

/**
 * GET /api/v1/analyses/{analysisId}/report.pdf
 */
export async function fetchGapAssessmentPdf(analysisId: number): Promise<Blob> {
  const response = await apiFetch(`/api/v1/analyses/${analysisId}/report.pdf`);
  if (!response.ok) {
    let message = `Failed to download gap assessment PDF for analysis ${analysisId}`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore non-JSON error bodies
    }
    throw new ComplianceApiError(message, response.status);
  }
  return response.blob();
}

/**
 * GET /api/v1/documents/{documentId}/analyses
 */
export async function getDocumentAnalyses(documentId: number): Promise<ComplianceAnalysisSummary[]> {
  const response = await apiFetch(`/api/v1/documents/${documentId}/analyses`);
  return parseJsonResponse(response, `Failed to fetch analyses for document ${documentId}`);
}

export function isLegacyRetrievalStatus(status: AssessmentStatus | string): boolean {
  return status === "EVIDENCE_RETRIEVED" || status === "NO_EVIDENCE_FOUND";
}

export function formatEsgCategory(category: string): string {
  switch (category) {
    case "ENVIRONMENTAL":
      return "Environmental";
    case "SOCIAL":
      return "Social";
    case "GOVERNANCE":
      return "Governance";
    default:
      return category.replaceAll("_", " ").replace(/\b\w/g, (char) => char.toUpperCase());
  }
}

export function formatAssessmentStatus(status: AssessmentStatus | string): string {
  switch (status) {
    case "COVERED":
      return "Covered";
    case "PARTIALLY_COVERED":
      return "Partially Covered";
    case "NOT_COVERED":
      return "Not Covered";
    case "HUMAN_REVIEW_REQUIRED":
      return "Human Review Required";
    case "NOT_APPLICABLE":
      return "Not Applicable";
    case "EVIDENCE_RETRIEVED":
      return "Evidence Retrieved";
    case "NO_EVIDENCE_FOUND":
      return "No Evidence Found";
    default:
      return status.replaceAll("_", " ");
  }
}

export function formatAnalysisStatus(status: AnalysisStatus | string): string {
  switch (status) {
    case "IN_PROGRESS":
      return "In Progress";
    case "COMPLETED":
      return "Completed";
    case "FAILED":
      return "Failed";
    default:
      return status.replaceAll("_", " ");
  }
}

export function formatConfidencePercent(confidence: number | null | undefined): number | null {
  if (confidence == null) return null;
  return Math.round(confidence * 100);
}

export function formatRetrievalScore(score: number | null | undefined): string {
  if (score == null) return "—";
  return score.toFixed(2);
}

export function summarizeAnalysisSummaryCounts(
  summary: ComplianceAnalysisSummary,
): AssessmentSummaryCounts {
  return {
    covered: summary.coveredCount,
    partiallyCovered: summary.partiallyCoveredCount,
    notCovered: summary.notCoveredCount,
    humanReviewRequired: summary.humanReviewRequiredCount,
    evidenceRetrieved: summary.evidenceRetrievedCount,
    noEvidenceFound: summary.noEvidenceFoundCount,
  };
}

export function formatAnalysisSummaryCounts(summary: ComplianceAnalysisSummary): string {
  const counts = summarizeAnalysisSummaryCounts(summary);
  const parts: string[] = [];

  if (counts.covered > 0) parts.push(`${counts.covered} Covered`);
  if (counts.partiallyCovered > 0) parts.push(`${counts.partiallyCovered} Partially Covered`);
  if (counts.notCovered > 0) parts.push(`${counts.notCovered} Not Covered`);
  if (counts.humanReviewRequired > 0) parts.push(`${counts.humanReviewRequired} Human Review`);
  if (counts.evidenceRetrieved > 0) parts.push(`${counts.evidenceRetrieved} Evidence Retrieved`);
  if (counts.noEvidenceFound > 0) parts.push(`${counts.noEvidenceFound} No Evidence Found`);

  return parts.join(" · ");
}

export function summarizeAssessments(assessments: RequirementAssessment[]): AssessmentSummaryCounts {
  return assessments.reduce<AssessmentSummaryCounts>(
    (counts, assessment) => {
      switch (assessment.assessmentStatus) {
        case "COVERED":
          counts.covered += 1;
          break;
        case "PARTIALLY_COVERED":
          counts.partiallyCovered += 1;
          break;
        case "NOT_COVERED":
          counts.notCovered += 1;
          break;
        case "HUMAN_REVIEW_REQUIRED":
          counts.humanReviewRequired += 1;
          break;
        case "EVIDENCE_RETRIEVED":
          counts.evidenceRetrieved += 1;
          break;
        case "NO_EVIDENCE_FOUND":
          counts.noEvidenceFound += 1;
          break;
      }
      return counts;
    },
    {
      covered: 0,
      partiallyCovered: 0,
      notCovered: 0,
      humanReviewRequired: 0,
      evidenceRetrieved: 0,
      noEvidenceFound: 0,
    },
  );
}

export function countNotApplicable(assessments: RequirementAssessment[]): number {
  return assessments.filter((assessment) => assessment.assessmentStatus === "NOT_APPLICABLE").length;
}

export function findAssessmentByCode(
  analysis: ComplianceAnalysis,
  requirementCode: string,
): RequirementAssessment | undefined {
  return analysis.assessments.find(
    (assessment) => assessment.requirementCode.toLowerCase() === requirementCode.toLowerCase(),
  );
}

export { formatInstant };

/** Backend EsgIndustry enum: sector drives the pillar weights and the peer group of the ESG score. */
export const ESG_INDUSTRIES: { value: string; label: string }[] = [
  { value: "ENERGY", label: "Energy" },
  { value: "MATERIALS", label: "Materials" },
  { value: "UTILITIES", label: "Utilities" },
  { value: "INDUSTRIALS", label: "Industrials" },
  { value: "CONSUMER_DISCRETIONARY", label: "Consumer Discretionary" },
  { value: "CONSUMER_STAPLES", label: "Consumer Staples" },
  { value: "HEALTH_CARE", label: "Health Care" },
  { value: "FINANCIALS", label: "Financials" },
  { value: "INFORMATION_TECHNOLOGY", label: "Information Technology" },
  { value: "COMMUNICATION_SERVICES", label: "Communication Services" },
  { value: "REAL_ESTATE", label: "Real Estate" },
];

export interface EsgKeyIssueScore {
  code: string;
  name: string;
  pillar: "ENVIRONMENTAL" | "SOCIAL" | "GOVERNANCE";
  sections: string;
  weight: number | null;
  score: number | null;
  scoredRequirements: number;
  notApplicableRequirements: number;
  humanReviewRequirements: number;
  totalRequirements: number;
}

/** Backend EsgScoreResponse: MSCI-style score derived from BRSR disclosure coverage. */
export interface EsgScore {
  analysisId: number;
  industry: string | null;
  industryLabel: string | null;
  scorable: boolean;
  weightedAverageKeyIssueScore: number | null;
  industryAdjustedScore: number | null;
  industryAdjusted: boolean;
  peerCount: number;
  peerMinimum: number | null;
  peerMaximum: number | null;
  ratingBand: string | null;
  environmentalScore: number | null;
  socialScore: number | null;
  governanceScore: number | null;
  environmentalWeight: number | null;
  socialWeight: number | null;
  governanceWeight: number | null;
  scoredRequirements: number;
  notApplicableRequirements: number;
  humanReviewRequirements: number;
  totalRequirements: number;
  keyIssues: EsgKeyIssueScore[];
  note: string;
}

export interface ApplicabilityResult {
  requirementId: number;
  assessmentStatus: AssessmentStatus;
  notApplicableReason: string | null;
}

/** GET /api/v1/analyses/{analysisId}/esg-score */
export async function getEsgScore(analysisId: number): Promise<EsgScore> {
  const response = await apiFetch(`/api/v1/analyses/${analysisId}/esg-score`);
  return parseJsonResponse(response, `Failed to fetch ESG score for analysis ${analysisId}`);
}

/** PUT /api/v1/analyses/{analysisId}/industry — returns the recalculated score. */
export async function setAnalysisIndustry(
  analysisId: number,
  industry: string | null,
): Promise<EsgScore> {
  const response = await apiFetch(`/api/v1/analyses/${analysisId}/industry`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ industry }),
  });
  return parseJsonResponse(response, `Failed to set the industry for analysis ${analysisId}`);
}

/** PUT /api/v1/analyses/{analysisId}/requirements/{requirementId}/applicability */
export async function setRequirementApplicability(
  analysisId: number,
  requirementId: number,
  applicable: boolean,
  reason?: string,
): Promise<ApplicabilityResult> {
  const response = await apiFetch(
    `/api/v1/analyses/${analysisId}/requirements/${requirementId}/applicability`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ applicable, reason: reason ?? null }),
    },
  );
  return parseJsonResponse(response, "Failed to update requirement applicability");
}

/** Score 0-10 to a 0-100 percentage for bar widths, clamped. */
export function scorePercent(score: number | null | undefined): number {
  if (score == null || Number.isNaN(score)) return 0;
  return Math.max(0, Math.min(100, score * 10));
}

export function formatScore(score: number | null | undefined): string {
  return score == null ? "—" : score.toFixed(1);
}
