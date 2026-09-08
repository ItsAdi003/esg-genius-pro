/**
 * API client for company ESG comparison endpoints.
 * 
 * All scores and ratings are illustrative prototype data.
 */

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

const API_BASE = "/api/v1";

export const companyApi = {
    /**
     * GET /api/v1/companies
     * List all companies available for comparison.
     */
    listCompanies: async (): Promise<CompanySummary[]> => {
        const response = await fetch(`${API_BASE}/companies`);
        if (!response.ok) throw new Error("Failed to fetch companies");
        return response.json();
    },

    /**
     * GET /api/v1/companies/{companyId}/esg
     * Get complete ESG profile for a single company.
     */
    getCompanyEsgProfile: async (companyId: number): Promise<CompanyEsgProfile> => {
        const response = await fetch(`${API_BASE}/companies/${companyId}/esg`);
        if (!response.ok) throw new Error(`Failed to fetch ESG profile for company ${companyId}`);
        return response.json();
    },

    /**
     * GET /api/v1/companies/compare?companyA={id}&companyB={id}
     * Compare two companies and get detailed comparison data.
     */
    compareCompanies: async (companyAId: number, companyBId: number): Promise<CompanyComparison> => {
        const response = await fetch(
            `${API_BASE}/companies/compare?companyA=${companyAId}&companyB=${companyBId}`
        );
        if (!response.ok) throw new Error("Failed to compare companies");
        return response.json();
    },
};
