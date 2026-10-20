import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { ArrowLeft, RefreshCw } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  BRSR_INDICATOR_TYPES,
  BRSR_SECTIONS,
  formatPlacement,
  hasStructuredSections,
} from "@/lib/brsr-structure";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  findFrameworkByCode,
  formatEsgCategory,
  frameworkQueryKeys,
  getFrameworkRequirements,
  listFrameworks,
  type EsgCategory,
} from "@/lib/framework-api";

export const Route = createFileRoute("/frameworks/brsr")({
  head: () => ({
    meta: [
      { title: "SEBI BRSR Requirements | ESGenius" },
      {
        name: "description",
        content:
          "Structured SEBI BRSR requirement library with ESG category, description, mandatory status and framework version.",
      },
      { property: "og:title", content: "SEBI BRSR Requirements | ESGenius" },
      {
        property: "og:description",
        content: "The BRSR requirement knowledge base used for ESG gap analysis.",
      },
    ],
  }),
  component: BrsrRequirements,
});

type CategoryTab = "all" | EsgCategory;

const TAB_TO_CATEGORY: Record<CategoryTab, EsgCategory | null> = {
  all: null,
  ENVIRONMENTAL: "ENVIRONMENTAL",
  SOCIAL: "SOCIAL",
  GOVERNANCE: "GOVERNANCE",
};

function BrsrRequirements() {
  const [tab, setTab] = useState<CategoryTab>("all");
  const [section, setSection] = useState("all");
  const [indicator, setIndicator] = useState("all");

  const frameworksQuery = useQuery({
    queryKey: frameworkQueryKeys.list(),
    queryFn: listFrameworks,
  });

  const brsrFramework = findFrameworkByCode(frameworksQuery.data ?? [], "BRSR");
  const category = TAB_TO_CATEGORY[tab];

  const requirementsQuery = useQuery({
    queryKey: frameworkQueryKeys.requirements(brsrFramework?.id ?? 0, category),
    queryFn: () => getFrameworkRequirements(brsrFramework!.id, category),
    enabled: brsrFramework != null,
  });

  const allRows = requirementsQuery.data ?? [];
  const rows = allRows.filter(
    (r) =>
      (section === "all" || r.section === section) &&
      (indicator === "all" || r.indicatorType === indicator),
  );
  const hasSections = hasStructuredSections(allRows);
  const isLoading = frameworksQuery.isLoading || requirementsQuery.isLoading;
  const isError = frameworksQuery.isError || requirementsQuery.isError;

  return (
    <AppLayout
      title="SEBI BRSR — Framework Requirements"
      description={
        brsrFramework
          ? `${brsrFramework.fullName} · ${brsrFramework.version}`
          : "Loading framework metadata…"
      }
      actions={
        <Button variant="outline" asChild>
          <Link to="/frameworks">
            <ArrowLeft className="size-4" /> All frameworks
          </Link>
        </Button>
      }
    >
      <p className="mb-4 text-sm text-muted-foreground">
        Every numbered disclosure question of the SEBI BRSR format (Annexure I): Section A general
        disclosures, Section B management and process, and Section C — the nine principles, each with
        Essential and Leadership indicators. Essential indicators are mandatory; Leadership indicators
        are voluntary. Later SEBI revisions (BRSR Core, value-chain disclosures) are not included.
      </p>

      {isError && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Unable to load BRSR requirements</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>Check that the backend is running and try again.</span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                frameworksQuery.refetch();
                requirementsQuery.refetch();
              }}
            >
              <RefreshCw className="size-3.5" /> Retry
            </Button>
          </AlertDescription>
        </Alert>
      )}

      <div className="flex flex-wrap items-center gap-3">
        <Tabs value={tab} onValueChange={(value) => setTab(value as CategoryTab)}>
          <TabsList>
            <TabsTrigger value="all">All</TabsTrigger>
            <TabsTrigger value="ENVIRONMENTAL">Environmental</TabsTrigger>
            <TabsTrigger value="SOCIAL">Social</TabsTrigger>
            <TabsTrigger value="GOVERNANCE">Governance</TabsTrigger>
          </TabsList>
        </Tabs>
        {hasSections && (
          <>
            <Select value={section} onValueChange={setSection}>
              <SelectTrigger className="w-full sm:w-[340px]">
                <SelectValue placeholder="Section" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Sections</SelectItem>
                {BRSR_SECTIONS.map((entry) => (
                  <SelectItem key={entry.value} value={entry.value}>
                    {entry.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={indicator} onValueChange={setIndicator}>
              <SelectTrigger className="w-full sm:w-[200px]">
                <SelectValue placeholder="Indicator type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Indicator Types</SelectItem>
                {BRSR_INDICATOR_TYPES.map((entry) => (
                  <SelectItem key={entry.value} value={entry.value}>
                    {entry.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </>
        )}
      </div>

      <div className="surface-card mt-4 overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                <TableHead className="w-24">Req. ID</TableHead>
                <TableHead className="min-w-[220px]">Requirement Name</TableHead>
                <TableHead>ESG Category</TableHead>
                <TableHead className="min-w-[320px]">Description</TableHead>
                <TableHead>Mandatory / Optional</TableHead>
                <TableHead>Version</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading
                ? Array.from({ length: 5 }).map((_, i) => (
                    <TableRow key={i}>
                      {Array.from({ length: 6 }).map((__, j) => (
                        <TableCell key={j}>
                          <Skeleton className="h-4 w-full" />
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                : rows.map((r) => (
                    <TableRow key={r.id}>
                      <TableCell className="font-mono text-xs text-muted-foreground">
                        {r.requirementCode}
                      </TableCell>
                      <TableCell className="font-medium">
                        {r.title}
                        {formatPlacement(r.section, r.indicatorType) && (
                          <span className="mt-0.5 block text-xs font-normal text-muted-foreground">
                            {formatPlacement(r.section, r.indicatorType)}
                          </span>
                        )}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatEsgCategory(r.category)}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">{r.description}</TableCell>
                      <TableCell>
                        <span
                          className={
                            r.mandatory
                              ? "rounded-full border border-primary/25 bg-accent px-2.5 py-0.5 text-xs font-medium text-accent-foreground"
                              : "rounded-full border border-border bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground"
                          }
                        >
                          {r.mandatory ? "Mandatory" : "Optional"}
                        </span>
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">{r.version}</TableCell>
                    </TableRow>
                  ))}
            </TableBody>
          </Table>
        </div>
      </div>
      <p className="mt-3 text-xs text-muted-foreground">
        Showing {rows.length} of {brsrFramework?.requirementCount ?? allRows.length} requirement
        {(brsrFramework?.requirementCount ?? allRows.length) === 1 ? "" : "s"}
        {brsrFramework ? ` in ${brsrFramework.name}` : ""}.
      </p>
    </AppLayout>
  );
}
