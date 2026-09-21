import { describe, expect, it } from "vitest";
import {
  ANALYSIS_POLLING_INTERVAL_MS,
  ComplianceApiError,
  resolveAnalysisPollingInterval,
  shouldRetryAnalysisQuery,
} from "@/lib/compliance-api";

describe("resolveAnalysisPollingInterval", () => {
  it("polls while analysis is in progress", () => {
    expect(resolveAnalysisPollingInterval("IN_PROGRESS")).toBe(ANALYSIS_POLLING_INTERVAL_MS);
  });

  it("stops polling when analysis completes", () => {
    expect(resolveAnalysisPollingInterval("COMPLETED")).toBe(false);
  });

  it("stops polling when analysis fails", () => {
    expect(resolveAnalysisPollingInterval("FAILED")).toBe(false);
  });

  it("does not poll before the first status is known", () => {
    expect(resolveAnalysisPollingInterval(undefined)).toBe(false);
  });
});

describe("shouldRetryAnalysisQuery", () => {
  it("does not retry not-found errors", () => {
    expect(shouldRetryAnalysisQuery(0, new ComplianceApiError("missing", 404))).toBe(false);
  });

  it("retries a transient error once", () => {
    expect(shouldRetryAnalysisQuery(0, new ComplianceApiError("timeout", 500))).toBe(true);
    expect(shouldRetryAnalysisQuery(1, new ComplianceApiError("timeout", 500))).toBe(true);
    expect(shouldRetryAnalysisQuery(2, new ComplianceApiError("timeout", 500))).toBe(false);
  });
});
