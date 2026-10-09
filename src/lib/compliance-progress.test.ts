import { describe, expect, it } from "vitest";
import {
  analysisProgressPercent,
  formatAssessedProgress,
  formatElapsedDuration,
  resolveRequirementTotal,
} from "@/lib/compliance-progress";

describe("resolveRequirementTotal", () => {
  it("uses the analysis count when it is already known", () => {
    expect(resolveRequirementTotal(14, 14)).toBe(14);
    expect(resolveRequirementTotal(14, undefined)).toBe(14);
  });

  it("falls back to the framework count while the analysis total is still 0", () => {
    expect(resolveRequirementTotal(0, 14)).toBe(14);
  });

  it("returns 0 when neither source has a total", () => {
    expect(resolveRequirementTotal(0, undefined)).toBe(0);
    expect(resolveRequirementTotal(0, 0)).toBe(0);
  });
});

describe("analysisProgressPercent", () => {
  it("returns 0 when the total is unknown or zero", () => {
    expect(analysisProgressPercent(0, 0)).toBe(0);
    expect(analysisProgressPercent(7, 0)).toBe(0);
  });

  it("returns 0 of 14 as 0 percent", () => {
    expect(analysisProgressPercent(0, 14)).toBe(0);
  });

  it("returns 7 of 14 as 50 percent", () => {
    expect(analysisProgressPercent(7, 14)).toBe(50);
  });

  it("caps completed work at 100 percent", () => {
    expect(analysisProgressPercent(14, 14)).toBe(100);
    expect(analysisProgressPercent(20, 14)).toBe(100);
  });
});

describe("formatAssessedProgress", () => {
  it("includes the total when it is known", () => {
    expect(formatAssessedProgress(0, 14)).toBe("0 of 14 requirements assessed");
    expect(formatAssessedProgress(7, 14)).toBe("7 of 14 requirements assessed");
  });

  it("falls back to a running count when no total is available", () => {
    expect(formatAssessedProgress(0, 0)).toBe("0 requirements assessed so far");
    expect(formatAssessedProgress(3, 0)).toBe("3 requirements assessed so far");
  });
});

describe("formatElapsedDuration", () => {
  const startedAt = "2026-10-09T12:00:00.000Z";
  const startedMs = Date.parse(startedAt);

  it("formats seconds and minutes as m:ss", () => {
    expect(formatElapsedDuration(startedAt, startedMs)).toBe("0:00");
    expect(formatElapsedDuration(startedAt, startedMs + 9_000)).toBe("0:09");
    expect(formatElapsedDuration(startedAt, startedMs + 75_000)).toBe("1:15");
  });

  it("includes hours after 60 minutes", () => {
    expect(formatElapsedDuration(startedAt, startedMs + 3_661_000)).toBe("1:01:01");
  });

  it("does not go negative if now is before startedAt", () => {
    expect(formatElapsedDuration(startedAt, startedMs - 5_000)).toBe("0:00");
  });

  it("treats an invalid startedAt as zero elapsed", () => {
    expect(formatElapsedDuration("not-a-date", startedMs)).toBe("0:00");
  });
});
