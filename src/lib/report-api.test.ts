import { afterEach, describe, expect, it, vi } from "vitest";

import { listReportExports, parseReportExports, ReportApiError } from "@/lib/report-api";

vi.mock("@/lib/api-config", () => ({
  apiFetch: vi.fn(),
}));

import { apiFetch } from "@/lib/api-config";

const validRow = {
  id: 12,
  analysisId: 15,
  documentId: 6,
  documentName: "report.pdf",
  frameworkCode: "BRSR",
  format: "PDF",
  sizeBytes: 3800,
  generatedAt: "2026-10-09T10:15:00Z",
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.resetAllMocks();
});

describe("parseReportExports", () => {
  it("keeps well-formed rows", () => {
    expect(parseReportExports([validRow])).toEqual([validRow]);
  });

  it("drops malformed rows without discarding the valid ones", () => {
    const parsed = parseReportExports([
      validRow,
      { ...validRow, id: "13" },
      { ...validRow, sizeBytes: null },
      null,
      "nope",
    ]);
    expect(parsed).toEqual([validRow]);
  });

  it("returns an empty list for non-array payloads", () => {
    expect(parseReportExports({ rows: [validRow] })).toEqual([]);
    expect(parseReportExports(null)).toEqual([]);
  });
});

describe("listReportExports", () => {
  it("returns the parsed list on success", async () => {
    vi.mocked(apiFetch).mockResolvedValue(jsonResponse([validRow]));
    await expect(listReportExports()).resolves.toEqual([validRow]);
    expect(apiFetch).toHaveBeenCalledWith("/api/v1/reports");
  });

  it("treats 404 (backend without the endpoint) as nothing exported yet", async () => {
    vi.mocked(apiFetch).mockResolvedValue(jsonResponse({ message: "Not found" }, 404));
    await expect(listReportExports()).resolves.toEqual([]);
  });

  it("surfaces the server message and status on other errors", async () => {
    vi.mocked(apiFetch).mockResolvedValue(jsonResponse({ message: "Boom" }, 500));
    const error = await listReportExports().catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ReportApiError);
    expect((error as ReportApiError).message).toBe("Boom");
    expect((error as ReportApiError).status).toBe(500);
  });

  it("falls back to a generic message for non-JSON error bodies", async () => {
    vi.mocked(apiFetch).mockResolvedValue(new Response("oops", { status: 502 }));
    const error = await listReportExports().catch((e: unknown) => e);
    expect((error as ReportApiError).message).toBe("Failed to fetch exported reports");
  });
});
