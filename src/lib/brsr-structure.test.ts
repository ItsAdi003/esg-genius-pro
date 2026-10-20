import { describe, expect, it } from "vitest";

import {
  BRSR_SECTIONS,
  countBySection,
  formatIndicatorType,
  formatPlacement,
  formatSectionLabel,
  formatSectionShort,
  hasStructuredSections,
} from "./brsr-structure";

describe("brsr-structure", () => {
  it("lists Section A, Section B and the nine principles in framework order", () => {
    expect(BRSR_SECTIONS.map((section) => section.value)).toEqual([
      "A",
      "B",
      "P1",
      "P2",
      "P3",
      "P4",
      "P5",
      "P6",
      "P7",
      "P8",
      "P9",
    ]);
  });

  it("formats section names", () => {
    expect(formatSectionShort("A")).toBe("Section A");
    expect(formatSectionShort("P6")).toBe("Principle 6");
    expect(formatSectionLabel("P6")).toBe("Principle 6 — Protecting and restoring the environment");
  });

  it("falls back to the raw value for unknown sections and to empty for missing ones", () => {
    expect(formatSectionShort("Z9")).toBe("Z9");
    expect(formatSectionShort(null)).toBe("");
    expect(formatSectionShort(undefined)).toBe("");
    expect(formatSectionLabel(null)).toBe("");
  });

  it("formats indicator types", () => {
    expect(formatIndicatorType("ESSENTIAL")).toBe("Essential");
    expect(formatIndicatorType("LEADERSHIP")).toBe("Leadership");
    expect(formatIndicatorType("GENERAL")).toBe("General");
    expect(formatIndicatorType(null)).toBe("");
    expect(formatIndicatorType("OTHER")).toBe("OTHER");
  });

  it("composes the compact placement label", () => {
    expect(formatPlacement("P6", "ESSENTIAL")).toBe("Principle 6 · Essential");
    expect(formatPlacement("P3", "LEADERSHIP")).toBe("Principle 3 · Leadership");
    // Sections A and B carry the indicator type implicitly.
    expect(formatPlacement("A", "GENERAL")).toBe("Section A");
    expect(formatPlacement("B", "MANAGEMENT")).toBe("Section B");
    // Legacy requirements have no placement at all.
    expect(formatPlacement(null, null)).toBe("");
  });

  it("detects structured data and counts items per section in order", () => {
    const items = [
      { section: "P6" },
      { section: "A" },
      { section: "P6" },
      { section: null },
      { section: "B" },
    ];

    expect(hasStructuredSections(items)).toBe(true);
    expect(hasStructuredSections([{ section: null }, {}])).toBe(false);
    expect(countBySection(items)).toEqual([
      { value: "A", label: "Section A", count: 1 },
      { value: "B", label: "Section B", count: 1 },
      { value: "P6", label: "Principle 6", count: 2 },
    ]);
  });
});
