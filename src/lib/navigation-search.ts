export interface NavigationSearchResult {
  id: string;
  title: string;
  subtitle: string;
  to: string;
}

const navigationEntries: NavigationSearchResult[] = [
  {
    id: "dashboard",
    title: "Dashboard",
    subtitle: "Product overview and workspace entry points",
    to: "/",
  },
  {
    id: "documents",
    title: "Documents",
    subtitle: "Upload and manage evidence documents",
    to: "/documents",
  },
  {
    id: "compliance",
    title: "Compliance Analysis",
    subtitle: "Run and review BRSR compliance assessments",
    to: "/compliance",
  },
  {
    id: "comparison",
    title: "Company Comparison",
    subtitle: "Compare prototype ESG profiles across companies",
    to: "/comparison",
  },
  {
    id: "frameworks",
    title: "ESG Frameworks",
    subtitle: "Browse configured reporting frameworks",
    to: "/frameworks",
  },
  {
    id: "frameworks-brsr",
    title: "SEBI BRSR Requirements",
    subtitle: "Prototype BRSR requirement library",
    to: "/frameworks/brsr",
  },
  {
    id: "assistant",
    title: "AI ESG Assistant",
    subtitle: "Prototype preview · planned RAG phase",
    to: "/assistant",
  },
  {
    id: "reports",
    title: "Reports",
    subtitle: "Report generation · planned",
    to: "/reports",
  },
  {
    id: "reports-gap",
    title: "Gap Assessment Report Preview",
    subtitle: "Prototype report layout preview",
    to: "/reports/gap-assessment",
  },
  {
    id: "settings",
    title: "Settings",
    subtitle: "Prototype configuration interface",
    to: "/settings",
  },
];

export function searchNavigation(query: string): NavigationSearchResult[] {
  const q = query.trim().toLowerCase();
  if (!q) return [];

  return navigationEntries.filter(
    (entry) =>
      entry.title.toLowerCase().includes(q) ||
      entry.subtitle.toLowerCase().includes(q) ||
      entry.id.includes(q),
  );
}
