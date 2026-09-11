import { apiUrl } from "@/lib/api-config";
import { formatInstant } from "@/lib/document-api";

/** Backend AnalysisStatus enum — dev.esgenius.entity.AnalysisStatus */
export type AnalysisStatus = "IN_PROGRESS" | "COMPLETED" | "FAILED";

/** Backend AssessmentStatus enum — dev.esgenius.entity.AssessmentStatus */
export type AssessmentStatus =
  | "COVERED"
  | "PARTIALLY_COVERED"
  | "NOT_COVERED"
  | "HUMAN_REVIEW_REQUIRED"
  | "EVIDENCE_RETRIEVED"
  | "NO_EVIDENCE_FOUND";

export interface EvidenceChunk {
  chunkIndex: number;
  text: string;
  retrievalScore: number;
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

export const complianceQueryKeys = {
  all: ["compliance"] as const,
  analysis: (analysisId: number) => [...complianceQueryKeys.all, "analysis", analysisId] as const,
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
 */
export async function createComplianceAnalysis(
  documentId: number,
  frameworkCode = "BRSR",
): Promise<ComplianceAnalysis> {
  const body: StartAnalysisRequest = { frameworkCode };
  const response = await fetch(apiUrl(`/api/v1/documents/${documentId}/analyses`), {
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
  const response = await fetch(apiUrl(`/api/v1/analyses/${analysisId}`));
  return parseJsonResponse(response, `Failed to fetch analysis ${analysisId}`);
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

export function findAssessmentByCode(
  analysis: ComplianceAnalysis,
  requirementCode: string,
): RequirementAssessment | undefined {
  return analysis.assessments.find(
    (assessment) => assessment.requirementCode.toLowerCase() === requirementCode.toLowerCase(),
  );
}

export { formatInstant };
