import { createFileRoute, Link } from "@tanstack/react-router";
import { Globe2, ArrowRight, Library } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { frameworks } from "@/lib/esg-data";

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

function Frameworks() {
  return (
    <AppLayout
      title="ESG Frameworks"
      description="Reporting frameworks available in the ESG knowledge base"
    >
      <div className="grid gap-4 md:grid-cols-2">
        {frameworks.map((f) => (
          <article key={f.id} className="surface-card flex flex-col p-5">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                  <Library className="size-5" />
                </span>
                <div>
                  <h2 className="font-semibold">{f.name}</h2>
                  <p className="text-xs text-muted-foreground">{f.fullName}</p>
                </div>
              </div>
              <StatusBadge status={f.status} />
            </div>

            <p className="mt-4 text-sm leading-relaxed text-muted-foreground">{f.description}</p>

            <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-border pt-4 text-sm">
              <div>
                <dt className="text-xs text-muted-foreground">Region</dt>
                <dd className="mt-0.5 flex items-center gap-1.5 font-medium">
                  <Globe2 className="size-3.5 text-muted-foreground" />
                  {f.region}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Requirements</dt>
                <dd className="mt-0.5 font-medium tabular-nums">
                  {f.requirements > 0 ? f.requirements : "—"}
                </dd>
              </div>
            </dl>

            <div className="mt-5">
              {f.id === "brsr" ? (
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
        ))}
      </div>
    </AppLayout>
  );
}
