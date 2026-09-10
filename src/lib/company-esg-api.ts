/**
 * API client for company ESG comparison endpoints.
 *
 * All scores and ratings are illustrative prototype data served by the backend.
 */

import { apiUrl } from "@/lib/api-config";

export type RatingBand = "AAA" | "AA" | "A" | "BBB" | "BB" | "B" | "CCC";

export interface CompanySummary {
  id: number;
  name: string;
  ticker: string;
  industry: string;
}

export interface EsgRating {
  overallScore: number;
  environmentalScore: number;
  socialScore: number;
  governanceScore: number;
  ratingBand: string;
  assessmentDate: string;
}

export interface RatingHistory {
  quarter: string;
  score: number;
}

export interface KeyIssueAssessment {
  id: number;
  issueCode: string;
  issueName: string;
  pillar: string;
  score: number;
  riskLevel: string;
}

export interface EsgEvent {
  id: number;
  title: string;
  description: string;
  pillar: string;
  severity: string;
  eventDate: string;
  scoreImpact: number | null;
  isPrototype: boolean;
}

export interface CompanyEsgProfile {
  id: number;
  name: string;
  ticker: string;
  industry: string;
  currentRating: EsgRating;
  ratingHistory: RatingHistory[];
  materialIssues: KeyIssueAssessment[];
  recentEvents: EsgEvent[];
}

export interface CompanyComparison {
  companyA: CompanyEsgProfile;
  companyB: CompanyEsgProfile;
  comparisonInsight: string;
}

export class CompanyEsgApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "CompanyEsgApiError";
    this.status = status;
  }
}

export const companyEsgQueryKeys = {
  all: ["company-esg"] as const,
  companies: () => [...companyEsgQueryKeys.all, "companies"] as const,
  comparison: (companyAId: number, companyBId: number) =>
    [...companyEsgQueryKeys.all, "comparison", companyAId, companyBId] as const,
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
    throw new CompanyEsgApiError(message, response.status);
  }
  return response.json() as Promise<T>;
}

/**
 * GET /api/v1/companies
 */
export async function getCompanies(): Promise<CompanySummary[]> {
  const response = await fetch(apiUrl("/api/v1/companies"));
  return parseJsonResponse(response, "Failed to fetch companies");
}

/**
 * GET /api/v1/companies/{companyId}/esg
 */
export async function getCompanyEsgProfile(companyId: number): Promise<CompanyEsgProfile> {
  const response = await fetch(apiUrl(`/api/v1/companies/${companyId}/esg`));
  return parseJsonResponse(response, `Failed to fetch ESG profile for company ${companyId}`);
}

/**
 * GET /api/v1/companies/compare?companyA={id}&companyB={id}
 */
export async function compareCompanies(
  companyAId: number,
  companyBId: number,
): Promise<CompanyComparison> {
  const params = new URLSearchParams({
    companyA: String(companyAId),
    companyB: String(companyBId),
  });
  const response = await fetch(apiUrl(`/api/v1/companies/compare?${params.toString()}`));
  return parseJsonResponse(response, "Failed to compare companies");
}

/** @deprecated Use getCompanies */
export const companyApi = {
  listCompanies: getCompanies,
  getCompanyEsgProfile,
  compareCompanies,
};
