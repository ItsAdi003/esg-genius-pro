import { describe, expect, it } from "vitest";

import {
  ComplianceApiError,
  formatAnalysisStatus,
  formatAnalysisSummaryCounts,
  formatAssessmentStatus,
  formatConfidencePercent,
  formatEsgCategory,
  formatEvidenceSourceLabel,
  formatRetrievalScore,
  findAssessmentByCode,
  getPrimaryEvidenceChunk,
  isLegacyRetrievalStatus,
  parseAnalysisIdSearch,
  summarizeAssessments,
  summarizeAnalysisSummaryCounts,
  type ComplianceAnalysis,
  type ComplianceAnalysisSummary,
  type EvidenceChunk,
  type RequirementAssessment,
} from "@/lib/compliance-api";

describe("formatEvidenceSourceLabel", () => {
  const base: EvidenceChunk = { chunkIndex: 3, pageNumber: null, text: "x", retrievalScore: 0.5 };

  it("prefers page number when present", () => {
    expect(formatEvidenceSourceLabel({ ...base, pageNumber: 7 })).toBe("Page 7");
  });

  it("falls back to chunk index", () => {
    expect(formatEvidenceSourceLabel(base)).toBe("Source chunk 3");
  });
});

describe("getPrimaryEvidenceChunk", () => {
  it("returns undefined for empty list", () => {
    expect(getPrimaryEvidenceChunk([])).toBeUndefined();
  });

  it("returns chunk with highest retrieval score", () => {
    const chunks: EvidenceChunk[] = [
      { chunkIndex: 0, pageNumber: 1, text: "a", retrievalScore: 0.2 },
      { chunkIndex: 1, pageNumber: 2, text: "b", retrievalScore: 0.9 },
      { chunkIndex: 2, pageNumber: 3, text: "c", retrievalScore: 0.5 },
    ];
    expect(getPrimaryEvidenceChunk(chunks)?.chunkIndex).toBe(1);
  });
});

describe("parseAnalysisIdSearch", () => {
  it("accepts positive integers", () => {
    expect(parseAnalysisIdSearch(10)).toBe(10);
  });

  it("rejects non-positive or non-integer numbers", () => {
    expect(parseAnalysisIdSearch(0)).toBeUndefined();
    expect(parseAnalysisIdSearch(-1)).toBeUndefined();
    expect(parseAnalysisIdSearch(1.5)).toBeUndefined();
  });

  it("parses trimmed numeric strings and strips JSON quotes", () => {
    expect(parseAnalysisIdSearch("10")).toBe(10);
    expect(parseAnalysisIdSearch('  "42"  ')).toBe(42);
  });

  it("rejects empty or invalid strings and other types", () => {
    expect(parseAnalysisIdSearch("")).toBeUndefined();
    expect(parseAnalysisIdSearch("   ")).toBeUndefined();
    expect(parseAnalysisIdSearch("abc")).toBeUndefined();
    expect(parseAnalysisIdSearch(null)).toBeUndefined();
  });
});

describe("isLegacyRetrievalStatus", () => {
  it("detects legacy retrieval statuses", () => {
    expect(isLegacyRetrievalStatus("EVIDENCE_RETRIEVED")).toBe(true);
    expect(isLegacyRetrievalStatus("NO_EVIDENCE_FOUND")).toBe(true);
    expect(isLegacyRetrievalStatus("COVERED")).toBe(false);
  });
});

describe("formatEsgCategory", () => {
  it("maps known categories", () => {
    expect(formatEsgCategory("ENVIRONMENTAL")).toBe("Environmental");
    expect(formatEsgCategory("SOCIAL")).toBe("Social");
    expect(formatEsgCategory("GOVERNANCE")).toBe("Governance");
  });

  it("replaces underscores then uppercases word-initial letters only", () => {
    // Remaining letters stay as-is (e.g. CUSTOM stays uppercase).
    expect(formatEsgCategory("CUSTOM_RISK")).toBe("CUSTOM RISK");
  });
});

describe("formatAssessmentStatus", () => {
  it("maps known assessment statuses", () => {
    expect(formatAssessmentStatus("PARTIALLY_COVERED")).toBe("Partially Covered");
    expect(formatAssessmentStatus("HUMAN_REVIEW_REQUIRED")).toBe("Human Review Required");
  });

  it("replaces underscores for unknown statuses", () => {
    expect(formatAssessmentStatus("CUSTOM_STATUS")).toBe("CUSTOM STATUS");
  });
});

describe("formatAnalysisStatus", () => {
  it("maps known analysis statuses", () => {
    expect(formatAnalysisStatus("IN_PROGRESS")).toBe("In Progress");
  });

  it("replaces underscores for unknown statuses", () => {
    expect(formatAnalysisStatus("CUSTOM")).toBe("CUSTOM");
  });
});

describe("formatConfidencePercent", () => {
  it("returns null for nullish confidence", () => {
    expect(formatConfidencePercent(null)).toBeNull();
    expect(formatConfidencePercent(undefined)).toBeNull();
  });

  it("rounds fractional confidence to a whole percent", () => {
    expect(formatConfidencePercent(0.456)).toBe(46);
  });
});

describe("formatRetrievalScore", () => {
  it("returns em dash for nullish score", () => {
    expect(formatRetrievalScore(null)).toBe("—");
  });

  it("formats score with two decimal places", () => {
    expect(formatRetrievalScore(0.1)).toBe("0.10");
  });
});

function minimalSummary(overrides: Partial<ComplianceAnalysisSummary> = {}): ComplianceAnalysisSummary {
  return {
    id: 1,
    documentId: 2,
    frameworkId: 3,
    frameworkCode: "BRSR",
    frameworkName: "BRSR",
    status: "COMPLETED",
    startedAt: "2024-01-01T00:00:00Z",
    completedAt: null,
    failureReason: null,
    requirementCount: 0,
    coveredCount: 0,
    partiallyCoveredCount: 0,
    notCoveredCount: 0,
    humanReviewRequiredCount: 0,
    evidenceRetrievedCount: 0,
    noEvidenceFoundCount: 0,
    ...overrides,
  };
}

describe("summarizeAnalysisSummaryCounts", () => {
  it("copies count fields from summary", () => {
    const summary = minimalSummary({
      coveredCount: 2,
      partiallyCoveredCount: 1,
      notCoveredCount: 3,
      humanReviewRequiredCount: 0,
      evidenceRetrievedCount: 4,
      noEvidenceFoundCount: 5,
    });
    expect(summarizeAnalysisSummaryCounts(summary)).toEqual({
      covered: 2,
      partiallyCovered: 1,
      notCovered: 3,
      humanReviewRequired: 0,
      evidenceRetrieved: 4,
      noEvidenceFound: 5,
    });
  });
});

describe("formatAnalysisSummaryCounts", () => {
  it("omits zero buckets and joins with middle dot", () => {
    const summary = minimalSummary({
      coveredCount: 1,
      partiallyCoveredCount: 0,
      notCoveredCount: 2,
      humanReviewRequiredCount: 0,
      evidenceRetrievedCount: 0,
      noEvidenceFoundCount: 0,
    });
    expect(formatAnalysisSummaryCounts(summary)).toBe("1 Covered · 2 Not Covered");
  });

  it("returns empty string when all counts are zero", () => {
    expect(formatAnalysisSummaryCounts(minimalSummary())).toBe("");
  });
});

function assessment(status: RequirementAssessment["assessmentStatus"]): RequirementAssessment {
  return {
    requirementId: 1,
    requirementCode: "R1",
    requirementTitle: "Title",
    category: "ENVIRONMENTAL",
    assessmentStatus: status,
    confidence: null,
    explanation: null,
    gap: null,
    recommendation: null,
    retrievalScore: null,
    evidenceText: null,
    evidenceChunks: [],
  };
}

describe("summarizeAssessments", () => {
  it("counts each assessment status", () => {
    const counts = summarizeAssessments([
      assessment("COVERED"),
      assessment("COVERED"),
      assessment("NOT_COVERED"),
      assessment("EVIDENCE_RETRIEVED"),
      assessment("UNKNOWN" as RequirementAssessment["assessmentStatus"]),
    ]);
    expect(counts).toEqual({
      covered: 2,
      partiallyCovered: 0,
      notCovered: 1,
      humanReviewRequired: 0,
      evidenceRetrieved: 1,
      noEvidenceFound: 0,
    });
  });
});

describe("findAssessmentByCode", () => {
  it("matches requirement codes case-insensitively", () => {
    const analysis: ComplianceAnalysis = {
      id: 1,
      documentId: 1,
      frameworkId: 1,
      frameworkCode: "BRSR",
      frameworkName: "BRSR",
      status: "COMPLETED",
      startedAt: "",
      completedAt: null,
      failureReason: null,
      requirementCount: 1,
      assessments: [assessment("COVERED")],
    };
    analysis.assessments[0]!.requirementCode = "brsr-1a";
    expect(findAssessmentByCode(analysis, "BRSR-1A")?.requirementCode).toBe("brsr-1a");
  });
});

describe("ComplianceApiError", () => {
  it("exposes status for retry logic consumers", () => {
    const err = new ComplianceApiError("nope", 404);
    expect(err.name).toBe("ComplianceApiError");
    expect(err.status).toBe(404);
  });
});
