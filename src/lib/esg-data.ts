export const ORG = {
  name: "ABC Industries Ltd.",
  framework: "SEBI BRSR",
  reportingPeriod: "FY 2025-26",
  readiness: 78,
  environmental: 72,
  social: 84,
  governance: 81,
  requirementsCovered: 42,
  partiallyCovered: 8,
  evidenceMissing: 6,
  documentsAnalyzed: 12,
};

export type Status =
  | "Covered"
  | "Partially Covered"
  | "Evidence Not Found"
  | "Human Review Required";

export type Priority = "High" | "Medium" | "Low";
export type Category = "Environmental" | "Social" | "Governance";

export interface Requirement {
  id: string;
  title: string;
  category: Category;
  status: Status;
  confidence: number;
  priority: Priority;
  evidence: {
    document: string;
    page: number;
    snippet: string;
  } | null;
  frameworkText: string;
  analysis: string;
  gap: string;
  recommendation: string;
  mandatory: boolean;
  description: string;
  version: string;
}

export const requirements: Requirement[] = [
  {
    id: "ENV-001",
    title: "Total Energy Consumption",
    category: "Environmental",
    status: "Covered",
    confidence: 96,
    priority: "Low",
    evidence: {
      document: "Sustainability Report FY2025-26",
      page: 27,
      snippet:
        "Total energy consumption of ABC Industries for FY2025-26 stood at 148,500 GJ across all manufacturing and office facilities.",
    },
    frameworkText:
      "The entity shall disclose total energy consumed from all sources during the reporting period, expressed in joules or multiples, along with energy intensity per rupee of turnover.",
    analysis:
      "Total energy consumption is disclosed with unit and reporting period, and energy intensity per rupee of turnover is provided in the same table.",
    gap: "No material gap identified for this disclosure.",
    recommendation:
      "Maintain the current disclosure structure and add prior-year comparatives to strengthen trend visibility.",
    mandatory: true,
    description:
      "Disclosure of total energy consumed from renewable and non-renewable sources with energy intensity ratios.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "ENV-002",
    title: "Renewable vs Non-Renewable Energy Breakdown",
    category: "Environmental",
    status: "Partially Covered",
    confidence: 87,
    priority: "Medium",
    evidence: {
      document: "Sustainability Report FY2025-26",
      page: 28,
      snippet:
        "ABC Industries consumed 120,000 kWh of electricity during FY2025-26, of which approximately 30% was supplied from renewable sources.",
    },
    frameworkText:
      "The entity shall separately disclose the total energy consumed from renewable sources and from non-renewable sources during the reporting period, including electricity, fuel and other sources, with corresponding intensity ratios.",
    analysis:
      "Total electricity consumption and renewable-energy contribution are disclosed, but a separate value for non-renewable consumption is not clearly provided.",
    gap: "Explicit renewable and non-renewable energy totals are required for complete disclosure.",
    recommendation:
      "Provide separate annual values for renewable and non-renewable electricity consumption for the reporting period.",
    mandatory: true,
    description:
      "Split of energy consumption between renewable and non-renewable sources, reported separately for electricity, fuel and others.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "ENV-003",
    title: "Scope 1 GHG Emissions",
    category: "Environmental",
    status: "Covered",
    confidence: 94,
    priority: "Low",
    evidence: {
      document: "Sustainability Report FY2025-26",
      page: 31,
      snippet:
        "Direct (Scope 1) greenhouse gas emissions for FY2025-26 were 18,420 tCO2e, calculated using the GHG Protocol Corporate Standard.",
    },
    frameworkText:
      "The entity shall disclose total Scope 1 emissions in metric tonnes of CO2 equivalent, along with the methodology and emission factors used.",
    analysis:
      "Scope 1 emissions are quantified in tCO2e with a stated methodology aligned to the GHG Protocol.",
    gap: "No material gap identified for this disclosure.",
    recommendation:
      "Include the emission factor source and assurance status to further strengthen the disclosure.",
    mandatory: true,
    description: "Direct greenhouse gas emissions from owned or controlled sources.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "ENV-004",
    title: "Scope 2 GHG Emissions",
    category: "Environmental",
    status: "Covered",
    confidence: 92,
    priority: "Low",
    evidence: {
      document: "Energy Consumption Report",
      page: 12,
      snippet:
        "Indirect (Scope 2) emissions from purchased electricity totalled 9,760 tCO2e for the reporting year, location-based method.",
    },
    frameworkText:
      "The entity shall disclose total Scope 2 emissions in metric tonnes of CO2 equivalent, specifying whether a location-based or market-based method is used.",
    analysis:
      "Scope 2 emissions are disclosed with the calculation method explicitly identified as location-based.",
    gap: "Market-based Scope 2 figure is not disclosed alongside the location-based figure.",
    recommendation:
      "Add a market-based Scope 2 figure to complement the location-based disclosure.",
    mandatory: true,
    description: "Indirect emissions from purchased electricity, steam, heating and cooling.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "ENV-005",
    title: "Scope 3 GHG Emissions",
    category: "Environmental",
    status: "Evidence Not Found",
    confidence: 81,
    priority: "High",
    evidence: null,
    frameworkText:
      "The entity shall disclose Scope 3 emissions, covering material upstream and downstream value-chain categories, together with the categories considered and the basis of estimation.",
    analysis:
      "No passage in the analysed document set quantifies value-chain emissions or identifies which Scope 3 categories were assessed.",
    gap: "Scope 3 value-chain emissions and category coverage are not disclosed in any analysed document.",
    recommendation:
      "Conduct a Scope 3 screening across purchased goods, logistics and business travel, then disclose quantified categories with the estimation basis.",
    mandatory: true,
    description:
      "Other indirect value-chain emissions across upstream and downstream categories.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "ENV-006",
    title: "Water Withdrawal Disclosure",
    category: "Environmental",
    status: "Covered",
    confidence: 90,
    priority: "Low",
    evidence: {
      document: "Sustainability Report FY2025-26",
      page: 34,
      snippet:
        "Total water withdrawal across all plants was 412 megalitres, of which 68% was sourced from municipal supply.",
    },
    frameworkText:
      "The entity shall disclose total water withdrawal by source, including surface water, groundwater, third-party water and others.",
    analysis: "Water withdrawal totals and source split are disclosed for all plants.",
    gap: "No material gap identified for this disclosure.",
    recommendation: "Add water-stress area classification for withdrawal locations.",
    mandatory: true,
    description: "Total water withdrawal segregated by source across operating locations.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "ENV-007",
    title: "Waste Recycling Data",
    category: "Environmental",
    status: "Partially Covered",
    confidence: 79,
    priority: "Medium",
    evidence: {
      document: "Environmental Policy",
      page: 6,
      snippet:
        "The company operates waste segregation at source and routes hazardous waste to authorised recyclers.",
    },
    frameworkText:
      "The entity shall disclose total waste generated by category and the quantity recovered through recycling, reuse and other recovery operations.",
    analysis:
      "Waste-handling practices are described qualitatively, but recycled and recovered quantities are not reported.",
    gap: "Quantitative recycled and recovered waste volumes by category are missing.",
    recommendation:
      "Publish waste generated and recycled quantities in metric tonnes, split by hazardous and non-hazardous categories.",
    mandatory: true,
    description: "Waste generation and recovery quantities by waste category.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "SOC-001",
    title: "Employee Health & Safety",
    category: "Social",
    status: "Covered",
    confidence: 93,
    priority: "Low",
    evidence: {
      document: "Employee Safety Policy",
      page: 4,
      snippet:
        "An occupational health and safety management system covering all employees and contract workers is maintained and audited annually.",
    },
    frameworkText:
      "The entity shall describe its occupational health and safety management system, its coverage and details of safety-related incidents during the year.",
    analysis:
      "The safety management system, its coverage and the annual audit cadence are described for employees and contract workers.",
    gap: "No material gap identified for this disclosure.",
    recommendation:
      "Add lost time injury frequency rate trends over three years for stronger comparability.",
    mandatory: true,
    description: "Occupational health and safety management system coverage and incidents.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "SOC-002",
    title: "Employee Training & Development Hours",
    category: "Social",
    status: "Partially Covered",
    confidence: 84,
    priority: "Medium",
    evidence: {
      document: "Annual Report 2025-26",
      page: 61,
      snippet:
        "Over 9,200 hours of training were delivered during the year across technical and compliance programmes.",
    },
    frameworkText:
      "The entity shall disclose training and awareness programmes with coverage by employee category and gender.",
    analysis:
      "Aggregate training hours are disclosed, but coverage is not broken down by employee category or gender.",
    gap: "Training coverage disaggregated by employee category and gender is missing.",
    recommendation:
      "Report average training hours per employee, split by gender and employee category.",
    mandatory: false,
    description: "Training and awareness programme coverage across the workforce.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "SOC-003",
    title: "Human Rights Due Diligence",
    category: "Social",
    status: "Human Review Required",
    confidence: 64,
    priority: "High",
    evidence: {
      document: "Annual Report 2025-26",
      page: 74,
      snippet:
        "The company is committed to upholding human rights across its operations and supply chain.",
    },
    frameworkText:
      "The entity shall disclose the process for human rights due diligence, assessments carried out during the year and remediation actions taken.",
    analysis:
      "Only a commitment statement was retrieved. It is unclear whether a formal due-diligence process and assessments took place during the reporting period.",
    gap: "Evidence is ambiguous; a compliance professional should confirm whether assessments were conducted.",
    recommendation:
      "Route to human review and, if assessments exist, disclose scope, findings and remediation actions.",
    mandatory: true,
    description: "Human rights due-diligence process, assessments and remediation.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "SOC-004",
    title: "Community Development Spend",
    category: "Social",
    status: "Covered",
    confidence: 91,
    priority: "Low",
    evidence: {
      document: "Annual Report 2025-26",
      page: 82,
      snippet:
        "CSR expenditure of INR 4.6 crore was directed to education, healthcare and local infrastructure projects.",
    },
    frameworkText:
      "The entity shall disclose CSR and community development expenditure along with the beneficiary areas covered.",
    analysis: "CSR spend and beneficiary programme areas are disclosed with amounts.",
    gap: "No material gap identified for this disclosure.",
    recommendation: "Add beneficiary counts and outcome indicators per programme.",
    mandatory: false,
    description: "Community and CSR investment with beneficiary coverage.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "GOV-001",
    title: "Anti-Corruption Policy",
    category: "Governance",
    status: "Covered",
    confidence: 95,
    priority: "Low",
    evidence: {
      document: "Annual Report 2025-26",
      page: 98,
      snippet:
        "A board-approved anti-bribery and anti-corruption policy applies to all employees, directors and business partners.",
    },
    frameworkText:
      "The entity shall disclose whether an anti-corruption or anti-bribery policy exists, its coverage and details of disciplinary actions taken during the year.",
    analysis:
      "A board-approved policy is disclosed with explicit coverage of employees, directors and partners.",
    gap: "No material gap identified for this disclosure.",
    recommendation:
      "Disclose the number of corruption-related complaints and disciplinary actions during the year.",
    mandatory: true,
    description: "Anti-bribery and anti-corruption policy, coverage and enforcement.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "GOV-002",
    title: "Board Composition & Independence",
    category: "Governance",
    status: "Covered",
    confidence: 89,
    priority: "Low",
    evidence: {
      document: "Annual Report 2025-26",
      page: 45,
      snippet:
        "The Board comprises 10 directors, of whom 5 are independent and 3 are women directors.",
    },
    frameworkText:
      "The entity shall disclose board composition, including independence and diversity of directors.",
    analysis: "Board size, independence and gender diversity are quantified.",
    gap: "No material gap identified for this disclosure.",
    recommendation: "Add board tenure and skills matrix for completeness.",
    mandatory: true,
    description: "Composition, independence and diversity of the board of directors.",
    version: "BRSR 2023 (v1.2)",
  },
  {
    id: "GOV-003",
    title: "Whistleblower Mechanism",
    category: "Governance",
    status: "Evidence Not Found",
    confidence: 76,
    priority: "High",
    evidence: null,
    frameworkText:
      "The entity shall disclose the existence of a vigil or whistleblower mechanism, its accessibility to stakeholders and complaints received during the year.",
    analysis:
      "No passage describing a vigil mechanism, reporting channel or complaint statistics was retrieved from the analysed documents.",
    gap: "Whistleblower mechanism details and complaint statistics are not disclosed.",
    recommendation:
      "Publish the vigil mechanism, reporting channels available to internal and external stakeholders, and annual complaint statistics.",
    mandatory: true,
    description: "Vigil mechanism availability, accessibility and complaint handling.",
    version: "BRSR 2023 (v1.2)",
  },
];

export const priorityGaps = [
  { id: "ENV-005", title: "Scope 3 GHG Emissions", status: "Evidence Not Found", priority: "High" },
  {
    id: "GOV-003",
    title: "Whistleblower Mechanism",
    status: "Evidence Not Found",
    priority: "High",
  },
  {
    id: "SOC-003",
    title: "Human Rights Due Diligence",
    status: "Human Review Required",
    priority: "High",
  },
  {
    id: "ENV-002",
    title: "Renewable Energy Breakdown",
    status: "Partially Covered",
    priority: "Medium",
  },
  { id: "ENV-007", title: "Waste Recycling Data", status: "Partially Covered", priority: "Medium" },
  { id: "ENV-006", title: "Water Withdrawal Disclosure", status: "Covered", priority: "Low" },
] as const;

export const categoryScores = [
  { category: "Environmental", score: ORG.environmental },
  { category: "Social", score: ORG.social },
  { category: "Governance", score: ORG.governance },
];

export const statusDistribution = [
  { name: "Covered", value: 42, color: "var(--color-success)" },
  { name: "Partially Covered", value: 8, color: "var(--color-warning)" },
  { name: "Evidence Not Found", value: 6, color: "var(--color-danger)" },
  { name: "Human Review Required", value: 4, color: "var(--color-info)" },
];

export const monthlyImprovement = [
  { month: "Sep", score: 54 },
  { month: "Oct", score: 58 },
  { month: "Nov", score: 63 },
  { month: "Dec", score: 66 },
  { month: "Jan", score: 71 },
  { month: "Feb", score: 74 },
  { month: "Mar", score: 78 },
];

export const gapBreakdown = [
  { category: "Environmental", covered: 16, partial: 5, missing: 4 },
  { category: "Social", covered: 15, partial: 2, missing: 1 },
  { category: "Governance", covered: 11, partial: 1, missing: 1 },
];

export const aiInsights = [
  {
    title: "Close the Scope 3 disclosure gap first",
    body: "Scope 3 emissions (ENV-005) is the highest-weighted missing disclosure in BRSR Principle 6. A category screening of purchased goods and logistics would lift environmental readiness by an estimated 6 points.",
    impact: "High impact",
  },
  {
    title: "Split renewable and non-renewable energy totals",
    body: "ENV-002 already cites a 30% renewable share on page 28 of the Sustainability Report. Publishing absolute renewable and non-renewable values would move this requirement from partial to covered.",
    impact: "Quick win",
  },
  {
    title: "Quantify waste recovery volumes",
    body: "ENV-007 currently relies on qualitative policy language. Adding recycled tonnage by hazardous and non-hazardous category converts narrative text into auditable evidence.",
    impact: "Medium impact",
  },
  {
    title: "Route ambiguous human rights evidence to review",
    body: "SOC-003 retrieved only a commitment statement at 64% confidence. A compliance professional should confirm whether due-diligence assessments were performed this period.",
    impact: "Needs human review",
  },
];

export interface DocumentRecord {
  id: string;
  name: string;
  type: string;
  year: string;
  uploaded: string;
  status: "Analyzed" | "Processing" | "Queued";
  pages: number;
  category: string;
}

export const documents: DocumentRecord[] = [
  {
    id: "doc-1",
    name: "Sustainability Report FY2025-26",
    type: "Sustainability Report",
    year: "FY 2025-26",
    uploaded: "12 Mar 2026",
    status: "Analyzed",
    pages: 84,
    category: "Environmental",
  },
  {
    id: "doc-2",
    name: "Annual Report 2025-26",
    type: "Annual Report",
    year: "FY 2025-26",
    uploaded: "12 Mar 2026",
    status: "Analyzed",
    pages: 156,
    category: "Governance",
  },
  {
    id: "doc-3",
    name: "Energy Consumption Report",
    type: "Operational Data",
    year: "FY 2025-26",
    uploaded: "18 Mar 2026",
    status: "Analyzed",
    pages: 22,
    category: "Environmental",
  },
  {
    id: "doc-4",
    name: "Environmental Policy",
    type: "Policy Document",
    year: "FY 2025-26",
    uploaded: "20 Mar 2026",
    status: "Analyzed",
    pages: 11,
    category: "Environmental",
  },
  {
    id: "doc-5",
    name: "Employee Safety Policy",
    type: "Policy Document",
    year: "FY 2025-26",
    uploaded: "21 Mar 2026",
    status: "Analyzed",
    pages: 9,
    category: "Social",
  },
  {
    id: "doc-6",
    name: "Supplier Code of Conduct",
    type: "Policy Document",
    year: "FY 2024-25",
    uploaded: "24 Mar 2026",
    status: "Processing",
    pages: 14,
    category: "Governance",
  },
  {
    id: "doc-7",
    name: "Water Stewardship Data Sheet",
    type: "Operational Data",
    year: "FY 2025-26",
    uploaded: "26 Mar 2026",
    status: "Queued",
    pages: 6,
    category: "Environmental",
  },
];

export const frameworks = [
  {
    id: "brsr",
    name: "SEBI BRSR",
    fullName: "Business Responsibility and Sustainability Report",
    status: "Active",
    region: "India",
    requirements: 56,
    description:
      "Mandatory sustainability disclosure format prescribed by SEBI for the top listed entities in India, structured around nine principles.",
  },
  {
    id: "gri",
    name: "GRI Standards",
    fullName: "Global Reporting Initiative Standards",
    status: "Planned",
    region: "Global",
    requirements: 0,
    description:
      "Widely adopted modular standards for reporting economic, environmental and social impacts.",
  },
  {
    id: "ifrs",
    name: "IFRS S1 / S2",
    fullName: "IFRS Sustainability Disclosure Standards",
    status: "Planned",
    region: "Global",
    requirements: 0,
    description:
      "General sustainability-related and climate-related financial disclosure requirements issued by the ISSB.",
  },
  {
    id: "esrs",
    name: "ESRS",
    fullName: "European Sustainability Reporting Standards",
    status: "Planned",
    region: "European Union",
    requirements: 0,
    description:
      "Reporting standards under the CSRD covering double materiality across environment, social and governance topics.",
  },
];

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

export const generatedReports = [
  {
    id: "RPT-1042",
    name: "ESG Gap Assessment Report",
    period: "FY 2025-26",
    generated: "28 Mar 2026, 10:24",
    generatedBy: "Priya Nair",
    format: "PDF",
    size: "1.8 MB",
  },
  {
    id: "RPT-1039",
    name: "Environmental Compliance Summary",
    period: "FY 2025-26",
    generated: "22 Mar 2026, 16:02",
    generatedBy: "Priya Nair",
    format: "PDF",
    size: "0.9 MB",
  },
  {
    id: "RPT-1035",
    name: "Executive Sustainability Summary",
    period: "FY 2025-26",
    generated: "14 Mar 2026, 09:41",
    generatedBy: "R. Iyer",
    format: "PDF",
    size: "0.4 MB",
  },
  {
    id: "RPT-1028",
    name: "Missing Evidence Report",
    period: "FY 2024-25",
    generated: "02 Feb 2026, 12:15",
    generatedBy: "Priya Nair",
    format: "PDF",
    size: "0.6 MB",
  },
];

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
  citations?: {
    requirement: string;
    document: string;
    page: number;
    snippet: string;
  }[];
}

export const suggestedQuestions = [
  "What ESG requirements are currently missing?",
  "Which environmental disclosures need attention?",
  "Why is our renewable energy disclosure marked partial?",
  "What evidence was found for Scope 1 emissions?",
  "Summarize our top five compliance gaps.",
];

export const cannedAnswers: Record<string, ChatMessage> = {
  "What ESG requirements are currently missing?": {
    role: "assistant",
    content:
      "Two BRSR requirements currently have no supporting evidence in the analysed document set: ENV-005 Scope 3 GHG Emissions and GOV-003 Whistleblower Mechanism. A third, SOC-003 Human Rights Due Diligence, retrieved only a commitment statement and is flagged for human review. All three are high priority.",
    citations: [
      {
        requirement: "ENV-005 — Scope 3 GHG Emissions",
        document: "No supporting evidence retrieved",
        page: 0,
        snippet:
          "No passage across 12 analysed documents quantifies value-chain emissions or names the Scope 3 categories assessed.",
      },
      {
        requirement: "GOV-003 — Whistleblower Mechanism",
        document: "No supporting evidence retrieved",
        page: 0,
        snippet:
          "No description of a vigil mechanism, reporting channel or complaint statistics was found.",
      },
    ],
  },
  "Which environmental disclosures need attention?": {
    role: "assistant",
    content:
      "Environmental readiness is 72%, the lowest of the three categories. ENV-005 (Scope 3) has no evidence, while ENV-002 (renewable vs non-renewable split) and ENV-007 (waste recycling volumes) are partially covered because the underlying numbers are narrative rather than quantified.",
    citations: [
      {
        requirement: "ENV-002 — Renewable vs Non-Renewable Energy Breakdown",
        document: "Sustainability Report FY2025-26",
        page: 28,
        snippet:
          "ABC Industries consumed 120,000 kWh of electricity during FY2025-26, of which approximately 30% was supplied from renewable sources.",
      },
      {
        requirement: "ENV-007 — Waste Recycling Data",
        document: "Environmental Policy",
        page: 6,
        snippet:
          "The company operates waste segregation at source and routes hazardous waste to authorised recyclers.",
      },
    ],
  },
  "Why is our renewable energy disclosure marked partial?": {
    role: "assistant",
    content:
      "ENV-002 is marked Partially Covered at 87% confidence. The retrieved passage discloses total electricity consumption and a renewable share, but no separate non-renewable total is stated, which BRSR requires as a distinct value.",
    citations: [
      {
        requirement: "ENV-002 — Renewable vs Non-Renewable Energy Breakdown",
        document: "Sustainability Report FY2025-26",
        page: 28,
        snippet:
          "ABC Industries consumed 120,000 kWh of electricity during FY2025-26, of which approximately 30% was supplied from renewable sources.",
      },
    ],
  },
  "What evidence was found for Scope 1 emissions?": {
    role: "assistant",
    content:
      "ENV-003 Scope 1 GHG Emissions is assessed as Covered at 94% confidence. The disclosure states the absolute figure in tCO2e and names the calculation methodology.",
    citations: [
      {
        requirement: "ENV-003 — Scope 1 GHG Emissions",
        document: "Sustainability Report FY2025-26",
        page: 31,
        snippet:
          "Direct (Scope 1) greenhouse gas emissions for FY2025-26 were 18,420 tCO2e, calculated using the GHG Protocol Corporate Standard.",
      },
    ],
  },
  "Summarize our top five compliance gaps.": {
    role: "assistant",
    content:
      "1. ENV-005 Scope 3 GHG Emissions — evidence not found, high priority.\n2. GOV-003 Whistleblower Mechanism — evidence not found, high priority.\n3. SOC-003 Human Rights Due Diligence — human review required, high priority.\n4. ENV-002 Renewable vs Non-Renewable Energy Breakdown — partially covered, medium priority.\n5. ENV-007 Waste Recycling Data — partially covered, medium priority.\n\nClosing the two environmental partials and the Scope 3 gap has the largest effect on the 78% overall readiness score.",
    citations: [
      {
        requirement: "ENV-005 — Scope 3 GHG Emissions",
        document: "No supporting evidence retrieved",
        page: 0,
        snippet: "Highest-weighted missing disclosure under BRSR Principle 6.",
      },
    ],
  },
};

export const defaultAnswer: ChatMessage = {
  role: "assistant",
  content:
    "Based on the 12 analysed documents for ABC Industries Ltd., overall ESG reporting readiness against SEBI BRSR is 78%: 42 requirements covered, 8 partially covered and 6 without retrieved evidence. Ask about a specific requirement ID or ESG category for an evidence-backed breakdown.",
  citations: [
    {
      requirement: "Portfolio summary — SEBI BRSR",
      document: "Sustainability Report FY2025-26",
      page: 27,
      snippet:
        "Total energy consumption of ABC Industries for FY2025-26 stood at 148,500 GJ across all manufacturing and office facilities.",
    },
  ],
};

export function getRequirement(id: string) {
  return requirements.find((r) => r.id.toLowerCase() === id.toLowerCase());
}
