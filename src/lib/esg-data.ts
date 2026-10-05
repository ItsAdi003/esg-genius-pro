export type Status =
  | "Covered"
  | "Partially Covered"
  | "Evidence Not Found"
  | "Human Review Required";

export type Priority = "High" | "Medium" | "Low";

export const reportTemplates = [
  {
    id: "gap-assessment",
    name: "ESG Gap Assessment Report",
    description:
      "Full requirement-by-requirement readiness assessment against SEBI BRSR with evidence citations and recommendations.",
    pages: 24,
  },
  {
    id: "environmental-summary",
    name: "Environmental Compliance Summary",
    description:
      "Focused view of Principle 6 disclosures: energy, emissions, water and waste readiness with open gaps.",
    pages: 12,
  },
  {
    id: "executive-summary",
    name: "Executive Sustainability Summary",
    description:
      "Board-ready one-pager covering readiness score, category performance and top priority actions.",
    pages: 4,
  },
  {
    id: "missing-evidence",
    name: "Missing Evidence Report",
    description:
      "Working list of requirements where no supporting evidence was retrieved, with suggested source owners.",
    pages: 8,
  },
];

export const suggestedQuestions = [
  "What ESG requirements are currently missing?",
  "Which environmental disclosures need attention?",
  "Why is our renewable energy disclosure marked partial?",
  "What evidence was found for Scope 1 emissions?",
  "Summarize our top five compliance gaps.",
];
