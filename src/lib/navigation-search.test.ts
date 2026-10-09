import { describe, expect, it } from "vitest";

import { searchNavigation } from "@/lib/navigation-search";

describe("searchNavigation", () => {
  it("returns no results for empty or whitespace query", () => {
    expect(searchNavigation("")).toEqual([]);
    expect(searchNavigation("   ")).toEqual([]);
  });

  it("matches title case-insensitively", () => {
    const results = searchNavigation("dashboard");
    expect(results.some((r) => r.id === "dashboard")).toBe(true);
  });

  it("matches subtitle text", () => {
    const results = searchNavigation("pdf export");
    expect(results.some((r) => r.id === "reports-gap")).toBe(true);
  });

  it("matches entry id", () => {
    const results = searchNavigation("frameworks-brsr");
    expect(results).toHaveLength(1);
    expect(results[0]?.to).toBe("/frameworks/brsr");
  });

  it("returns multiple matches when query is broad", () => {
    const results = searchNavigation("report");
    expect(results.length).toBeGreaterThan(1);
    expect(results.every((r) => r.title.toLowerCase().includes("report") || r.subtitle.toLowerCase().includes("report"))).toBe(
      true,
    );
  });
});
