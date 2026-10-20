/**
 * Structure of the SEBI BRSR format (Annexure I): Section A general disclosures, Section B management and
 * process, and Section C — nine principles, each with Essential and Leadership indicators. The backend
 * tags every requirement with a `section` ("A", "B", "P1".."P9") and an `indicatorType`.
 */

export const BRSR_SECTIONS = [
  { value: "A", short: "Section A", label: "Section A — General disclosures" },
  { value: "B", short: "Section B", label: "Section B — Management and process" },
  { value: "P1", short: "Principle 1", label: "Principle 1 — Ethics, transparency and accountability" },
  { value: "P2", short: "Principle 2", label: "Principle 2 — Sustainable and safe goods and services" },
  { value: "P3", short: "Principle 3", label: "Principle 3 — Well-being of employees" },
  { value: "P4", short: "Principle 4", label: "Principle 4 — Responsiveness to stakeholders" },
  { value: "P5", short: "Principle 5", label: "Principle 5 — Human rights" },
  { value: "P6", short: "Principle 6", label: "Principle 6 — Protecting and restoring the environment" },
  { value: "P7", short: "Principle 7", label: "Principle 7 — Responsible policy advocacy" },
  { value: "P8", short: "Principle 8", label: "Principle 8 — Inclusive growth and equitable development" },
  { value: "P9", short: "Principle 9", label: "Principle 9 — Value to consumers" },
] as const;

export const BRSR_INDICATOR_TYPES = [
  { value: "GENERAL", label: "General" },
  { value: "MANAGEMENT", label: "Management" },
  { value: "ESSENTIAL", label: "Essential" },
  { value: "LEADERSHIP", label: "Leadership" },
] as const;

/** "Section A", "Principle 6" — or an empty string for requirements without a section (legacy). */
export function formatSectionShort(section: string | null | undefined): string {
  if (!section) {
    return "";
  }
  return BRSR_SECTIONS.find((entry) => entry.value === section)?.short ?? section;
}

export function formatSectionLabel(section: string | null | undefined): string {
  if (!section) {
    return "";
  }
  return BRSR_SECTIONS.find((entry) => entry.value === section)?.label ?? section;
}

export function formatIndicatorType(indicatorType: string | null | undefined): string {
  if (!indicatorType) {
    return "";
  }
  return BRSR_INDICATOR_TYPES.find((entry) => entry.value === indicatorType)?.label ?? indicatorType;
}

/** "Principle 6 · Essential" — the compact label shown under a requirement title. */
export function formatPlacement(
  section: string | null | undefined,
  indicatorType: string | null | undefined,
): string {
  const parts = [formatSectionShort(section), formatIndicatorType(indicatorType)].filter(Boolean);
  // Sections A and B carry the indicator type implicitly; avoid "Section A · General".
  if (section === "A" || section === "B") {
    return parts[0] ?? "";
  }
  return parts.join(" · ");
}

/** True when at least one item carries a section, i.e. the data comes from the structured BRSR set. */
export function hasStructuredSections(items: { section?: string | null }[]): boolean {
  return items.some((item) => Boolean(item.section));
}

/** Count of items per section, in framework order, omitting sections with none. */
export function countBySection(
  items: { section?: string | null }[],
): { value: string; label: string; count: number }[] {
  return BRSR_SECTIONS.map((entry) => ({
    value: entry.value,
    label: entry.short,
    count: items.filter((item) => item.section === entry.value).length,
  })).filter((entry) => entry.count > 0);
}
