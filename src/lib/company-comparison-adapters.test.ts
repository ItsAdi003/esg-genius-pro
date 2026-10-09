import { describe, expect, it } from "vitest";

import type { CompanyEsgProfile, EsgEvent, KeyIssueAssessment } from "@/lib/company-esg-api";
import {
  deriveActiveControversies,
  deriveKeyStrengths,
  deriveKeyWeaknesses,
  deriveScoreChange,
  formatPillar,
  formatSeverity,
  mapRiskLevelToDisplay,
  mergeMaterialIssues,
  toComparisonCompanyView,
} from "@/lib/company-comparison-adapters";

describe("mapRiskLevelToDisplay", () => {
  it("maps backend risk levels", () => {
    expect(mapRiskLevelToDisplay("low")).toBe("Strong");
    expect(mapRiskLevelToDisplay("MODERATE")).toBe("Moderate");
    expect(mapRiskLevelToDisplay("HIGH")).toBe("Weak");
  });

  it("defaults unknown levels to Moderate", () => {
    expect(mapRiskLevelToDisplay("unknown")).toBe("Moderate");
  });
});

describe("formatPillar", () => {
  it("maps known pillars", () => {
    expect(formatPillar("environmental")).toBe("Environmental");
  });

  it("title-cases unknown pillars", () => {
    expect(formatPillar("customPillar")).toBe("Custompillar");
  });
});

describe("formatSeverity", () => {
  it("maps severities case-insensitively", () => {
    expect(formatSeverity("high")).toBe("High");
    expect(formatSeverity("LOW")).toBe("Low");
  });

  it("defaults unknown severity to Medium", () => {
    expect(formatSeverity("critical")).toBe("Medium");
  });
});

describe("deriveScoreChange", () => {
  it("returns 0 with fewer than two history points", () => {
    expect(deriveScoreChange([])).toBe(0);
    expect(deriveScoreChange([{ quarter: "Q1", score: 5 }])).toBe(0);
  });

  it("rounds delta between last two scores to two decimals", () => {
    expect(
      deriveScoreChange([
        { quarter: "Q1", score: 4 },
        { quarter: "Q2", score: 4.333 },
      ]),
    ).toBe(0.33);
  });
});

function event(overrides: Partial<EsgEvent> = {}): EsgEvent {
  return {
    id: 1,
    title: "Event",
    description: "Desc",
    pillar: "SOCIAL",
    severity: "LOW",
    eventDate: "2024-01-01",
    scoreImpact: null,
    isPrototype: true,
    ...overrides,
  };
}

describe("deriveActiveControversies", () => {
  it("counts HIGH severity or negative score impact", () => {
    const events = [
      event({ severity: "HIGH" }),
      event({ scoreImpact: -1 }),
      event({ scoreImpact: 0 }),
      event({ severity: "LOW", scoreImpact: null }),
    ];
    expect(deriveActiveControversies(events)).toBe(2);
  });

  it("does not count null scoreImpact alone unless severity is HIGH", () => {
    expect(deriveActiveControversies([event({ scoreImpact: null, severity: "MEDIUM" })])).toBe(0);
  });
});

function issue(overrides: Partial<KeyIssueAssessment> = {}): KeyIssueAssessment {
  return {
    id: 1,
    issueCode: "WATER",
    issueName: "Water",
    pillar: "ENVIRONMENTAL",
    score: 5,
    riskLevel: "LOW",
    ...overrides,
  };
}

describe("deriveKeyStrengths", () => {
  it("returns placeholder when no issues", () => {
    expect(deriveKeyStrengths([])).toEqual(["No material issue assessments available yet."]);
  });

  it("returns top issues by score", () => {
    const strengths = deriveKeyStrengths(
      [issue({ issueName: "A", score: 3 }), issue({ issueName: "B", score: 9 })],
      1,
    );
    expect(strengths).toEqual(["B — 9.0"]);
  });
});

describe("deriveKeyWeaknesses", () => {
  it("prioritizes HIGH risk then lower score", () => {
    const weaknesses = deriveKeyWeaknesses(
      [
        issue({ issueName: "Low risk", riskLevel: "LOW", score: 1 }),
        issue({ issueName: "High risk", riskLevel: "HIGH", score: 9 }),
        issue({ issueName: "Moderate", riskLevel: "MODERATE", score: 2 }),
      ],
      2,
    );
    expect(weaknesses[0]).toBe("High risk — 9.0");
    expect(weaknesses[1]).toBe("Moderate — 2.0");
  });
});

describe("mergeMaterialIssues", () => {
  it("merges issues by code from both companies", () => {
    const rows = mergeMaterialIssues(
      [issue({ issueCode: "A", issueName: "Alpha", score: 1, riskLevel: "LOW" })],
      [issue({ issueCode: "B", issueName: "Beta", score: 2, riskLevel: "HIGH" })],
    );
    expect(rows).toHaveLength(2);
    expect(rows.find((r) => r.issueCode === "A")).toEqual({
      issueCode: "A",
      title: "Alpha",
      companyA: { score: 1, riskLevel: "Strong" },
      companyB: null,
    });
    expect(rows.find((r) => r.issueCode === "B")?.companyB).toEqual({
      score: 2,
      riskLevel: "Weak",
    });
  });
});

function minimalProfile(overrides: Partial<CompanyEsgProfile> = {}): CompanyEsgProfile {
  return {
    id: 10,
    name: "Acme",
    ticker: "ACME",
    industry: "Manufacturing",
    currentRating: {
      overallScore: 7,
      environmentalScore: 6,
      socialScore: 7,
      governanceScore: 8,
      ratingBand: "A",
      assessmentDate: "2024-01-01",
    },
    ratingHistory: [
      { quarter: "Q1", score: 6 },
      { quarter: "Q2", score: 7 },
    ],
    materialIssues: [issue()],
    recentEvents: [
      event({
        id: 99,
        title: "Launch",
        pillar: "governance",
        severity: "medium",
        scoreImpact: 0.5,
      }),
      event({
        id: 100,
        title: "Incident",
        severity: "HIGH",
        scoreImpact: null,
      }),
    ],
    ...overrides,
  };
}

describe("toComparisonCompanyView", () => {
  it("maps profile fields and derived values", () => {
    const view = toComparisonCompanyView(minimalProfile());

    expect(view.id).toBe(10);
    expect(view.name).toBe("Acme");
    expect(view.scoreChange).toBe(1);
    expect(view.activeControversies).toBe(1);
    expect(view.materialIssues[0]).toEqual({
      title: "Water",
      score: 5,
      riskLevel: "Strong",
    });
  });

  it("omits scoreImpact when null and sets isPositive from severity", () => {
    const view = toComparisonCompanyView(minimalProfile());
    const withImpact = view.recentEvents.find((e) => e.id === 99);
    const withoutImpact = view.recentEvents.find((e) => e.id === 100);

    expect(withImpact).toMatchObject({
      scoreImpact: 0.5,
      isPositive: true,
      category: "Governance",
      severity: "Medium",
    });
    expect(withoutImpact).not.toHaveProperty("scoreImpact");
    expect(withoutImpact?.isPositive).toBe(false);
  });
});
