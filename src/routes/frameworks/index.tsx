import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Globe2, ArrowRight, Library, RefreshCw } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import {
  formatFrameworkStatus,
  frameworkQueryKeys,
  listFrameworks,
  type Framework,
} from "@/lib/framework-api";

export const Route = createFileRoute("/frameworks/")({
  head: () => ({
    meta: [
      { title: "ESG Frameworks | ESGenius" },
      {
        name: "description",
        content:
          "Manage ESG reporting frameworks: SEBI BRSR active, with GRI, IFRS S1/S2 and ESRS planned.",
      },
      { property: "og:title", content: "ESG Frameworks | ESGenius" },
      {
        property: "og:description",
        content: "Framework coverage and requirement libraries powering ESG gap analysis.",
      },
    ],
  }),
  component: Frameworks,
});

const PLANNED_FRAMEWORKS = [
  {
    code: "GRI",
    name: "GRI Standards",
    fullName: "Global Reporting Initiative Standards",
    region: "Global",
    description: "Comprehensive sustainability reporting standards for economic, environmental and social impacts.",
  },
  {
    code: "IFRS",
    name: "IFRS S1 / S2",
    fullName: "IFRS Sustainability Disclosure Standards",
    region: "Global",
    description: "General sustainability-related disclosures and climate-related financial disclosures.",
  },
  {
    code: "ESRS",
    name: "ESRS",
    fullName: "European Sustainability Reporting Standards",
    region: "European Union",
    description: "Mandatory sustainability reporting standards under the EU Corporate Sustainability Reporting Directive.",
  },
] as const;

function FrameworkCard({ framework }: { framework: Framework }) {
  const isBrsr = framework.code === "BRSR";

  return (
    <article className="surface-card flex flex-col p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
            <Library className="size-5" />
          </span>
          <div>
            <h2 className="font-semibold">{framework.name}</h2>
            <p className="text-xs text-muted-foreground">{framework.fullName}</p>
          </div>
        </div>
        <StatusBadge status={formatFrameworkStatus(framework.status)} />
      </div>

      <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
        {isBrsr
          ? "Prototype BRSR requirement subset configured for MVP validation and compliance analysis."
          : `${framework.fullName} framework configuration.`}
      </p>

      <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-border pt-4 text-sm">
        <div>
          <dt className="text-xs text-muted-foreground">Region</dt>
          <dd className="mt-0.5 flex items-center gap-1.5 font-medium">
            <Globe2 className="size-3.5 text-muted-foreground" />
            {framework.region}
          </dd>
        </div>
        <div>
          <dt className="text-xs text-muted-foreground">Requirements</dt>
          <dd className="mt-0.5 font-medium tabular-nums">{framework.requirementCount}</dd>
        </div>
        <div className="col-span-2">
          <dt className="text-xs text-muted-foreground">Version</dt>
          <dd className="mt-0.5 font-medium">{framework.version}</dd>
        </div>
      </dl>

      <div className="mt-5">
        {isBrsr ? (
          <Button asChild>
            <Link to="/frameworks/brsr">
              View Framework Requirements <ArrowRight className="size-4" />
            </Link>
          </Button>
        ) : (
          <Button variant="outline" disabled>
            Planned for a future release
          </Button>
        )}
      </div>
    </article>
  );
}

function PlannedFrameworkCard({
  framework,
}: {
  framework: (typeof PLANNED_FRAMEWORKS)[number];
}) {
  return (
    <article className="surface-card flex flex-col p-5 opacity-90">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
            <Library className="size-5" />
          </span>
          <div>
            <h2 className="font-semibold">{framework.name}</h2>
            <p className="text-xs text-muted-foreground">{framework.fullName}</p>
          </div>
        </div>
        <StatusBadge status="Planned" />
      </div>

      <p className="mt-4 text-sm leading-relaxed text-muted-foreground">{framework.description}</p>

      <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-border pt-4 text-sm">
        <div>
          <dt className="text-xs text-muted-foreground">Region</dt>
          <dd className="mt-0.5 flex items-center gap-1.5 font-medium">
            <Globe2 className="size-3.5 text-muted-foreground" />
            {framework.region}
          </dd>
        </div>
        <div>
          <dt className="text-xs text-muted-foreground">Requirements</dt>
          <dd className="mt-0.5 font-medium text-muted-foreground">—</dd>
        </div>
      </dl>

      <div className="mt-5">
        <Button variant="outline" disabled>
          Planned for a future release
        </Button>
      </div>
    </article>
  );
}

function Frameworks() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: frameworkQueryKeys.list(),
    queryFn: listFrameworks,
  });

  return (
    <AppLayout
      title="ESG Frameworks"
      description="Reporting frameworks configured in the ESGenius knowledge base"
    >
      {isError && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Unable to load frameworks</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>Check that the backend is running and try again.</span>
            <Button variant="outline" size="sm" onClick={() => refetch()}>
              <RefreshCw className="size-3.5" /> Retry
            </Button>
          </AlertDescription>
        </Alert>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {isLoading
          ? Array.from({ length: 2 }).map((_, i) => (
              <Skeleton key={i} className="h-64 rounded-xl" />
            ))
          : data?.map((framework) => <FrameworkCard key={framework.id} framework={framework} />)}

        {PLANNED_FRAMEWORKS.map((framework) => (
          <PlannedFrameworkCard key={framework.code} framework={framework} />
        ))}
      </div>
    </AppLayout>
  );
}
