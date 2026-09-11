import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { ArrowLeft, RefreshCw } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { PrototypeNotice } from "@/components/prototype-notice";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
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

  const rows = requirementsQuery.data ?? [];
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
      <PrototypeNotice title="Prototype coverage" className="mb-4">
        This implementation currently includes 14 selected BRSR requirements for MVP validation
        and does not represent the complete SEBI BRSR framework.
      </PrototypeNotice>

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

      <Tabs value={tab} onValueChange={(value) => setTab(value as CategoryTab)}>
        <TabsList>
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="ENVIRONMENTAL">Environmental</TabsTrigger>
          <TabsTrigger value="SOCIAL">Social</TabsTrigger>
          <TabsTrigger value="GOVERNANCE">Governance</TabsTrigger>
        </TabsList>
      </Tabs>

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
                      <TableCell className="font-medium">{r.title}</TableCell>
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
        Showing {rows.length} configured prototype requirement{rows.length === 1 ? "" : "s"}
        {brsrFramework ? ` for ${brsrFramework.name}` : ""}.
      </p>
    </AppLayout>
  );
}
