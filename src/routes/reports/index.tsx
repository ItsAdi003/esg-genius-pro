import { createFileRoute, Link } from "@tanstack/react-router";
import { FileBarChart2, Eye, Download, Sparkles } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { PrototypeNotice } from "@/components/prototype-notice";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { reportTemplates } from "@/lib/esg-data";

export const Route = createFileRoute("/reports/")({
  head: () => ({
    meta: [
      { title: "ESG Reports | ESGenius" },
      {
        name: "description",
        content:
          "Generate ESG gap assessment, environmental summary and executive sustainability reports from evidence-linked analysis.",
      },
      { property: "og:title", content: "ESG Reports | ESGenius" },
      {
        property: "og:description",
        content: "Report templates and previously generated ESG compliance documents.",
      },
    ],
  }),
  component: Reports,
});

function Reports() {
  return (
    <AppLayout
      title="Reports"
      description="Report generation · planned · UI preview available"
    >
      <PrototypeNotice title="Report Generation — Partial" className="mb-4">
        Gap Assessment reports support backend PDF export — open a completed analysis under
        Compliance Analysis, choose View Gap Assessment, then Download PDF. Other report types
        below remain planned previews.
      </PrototypeNotice>

      <div className="grid gap-4 md:grid-cols-2">
        {reportTemplates.map((t) => (
          <article key={t.id} className="surface-card flex flex-col p-5">
            <div className="flex items-start gap-3">
              <span className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                <FileBarChart2 className="size-5" />
              </span>
              <div>
                <h2 className="font-semibold">{t.name}</h2>
                <p className="text-xs text-muted-foreground">Approx. {t.pages} pages · PDF</p>
              </div>
            </div>
            <p className="mt-3 flex-1 text-sm leading-relaxed text-muted-foreground">
              {t.description}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <span className="inline-flex">
                      <Button disabled>
                        <Sparkles className="size-4" /> Generate Report
                      </Button>
                    </span>
                  </TooltipTrigger>
                  <TooltipContent>Report generation is planned for a future release</TooltipContent>
                </Tooltip>
              </TooltipProvider>
              {t.id === "gap-assessment" ? (
                <Button variant="outline" asChild>
                  <Link to="/compliance">
                    <Eye className="size-4" /> Open from Compliance
                  </Link>
                </Button>
              ) : (
                <Button variant="outline" disabled>
                  <Eye className="size-4" /> Preview
                </Button>
              )}
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <span className="inline-flex">
                      <Button variant="ghost" disabled>
                        <Download className="size-4" /> Download PDF
                      </Button>
                    </span>
                  </TooltipTrigger>
                  <TooltipContent>
                    {t.id === "gap-assessment"
                      ? "Open a completed analysis from Compliance to download its PDF"
                      : "PDF download is planned for a future release"}
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            </div>
          </article>
        ))}
      </div>

      <p className="mt-4 text-sm text-muted-foreground">
        Generated reports are not stored yet. Download Gap Assessment PDFs from a completed
        analysis under Compliance Analysis.
      </p>
    </AppLayout>
  );
}
