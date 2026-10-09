import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { FileBarChart2, Eye, Download, RefreshCw, Sparkles } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { PrototypeNotice } from "@/components/prototype-notice";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { reportTemplates } from "@/lib/esg-data";
import {
  formatExportSize,
  formatExportTime,
  listReportExports,
  reportQueryKeys,
} from "@/lib/report-api";

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

      <RecentExports />
    </AppLayout>
  );
}

function RecentExports() {
  const exportsQuery = useQuery({
    queryKey: reportQueryKeys.exports(),
    queryFn: listReportExports,
  });
  const exports = exportsQuery.data ?? [];

  return (
    <section className="surface-card mt-4 overflow-hidden">
      <div className="border-b border-border px-5 py-3">
        <p className="text-sm font-semibold">Recent exports</p>
        <p className="text-xs text-muted-foreground">
          Gap Assessment PDFs you have downloaded. Other report types are still planned previews.
        </p>
      </div>

      {exportsQuery.isLoading ? (
        <div className="space-y-2 p-5">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
        </div>
      ) : exportsQuery.isError ? (
        <div className="flex flex-wrap items-center justify-between gap-3 p-5">
          <p className="text-sm text-muted-foreground">{exportsQuery.error.message}</p>
          <Button variant="outline" size="sm" onClick={() => void exportsQuery.refetch()}>
            <RefreshCw className="size-4" /> Reload
          </Button>
        </div>
      ) : exports.length === 0 ? (
        <p className="p-5 text-sm text-muted-foreground">
          No reports exported yet. Open a completed analysis and use Download PDF.
        </p>
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                <TableHead>Document</TableHead>
                <TableHead>Framework</TableHead>
                <TableHead>Format</TableHead>
                <TableHead className="text-right">Size</TableHead>
                <TableHead>Generated</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {exports.map((report) => (
                <TableRow key={report.id}>
                  <TableCell className="font-medium">{report.documentName}</TableCell>
                  <TableCell className="text-muted-foreground">{report.frameworkCode}</TableCell>
                  <TableCell className="text-muted-foreground">{report.format}</TableCell>
                  <TableCell className="text-right tabular-nums text-muted-foreground">
                    {formatExportSize(report.sizeBytes)}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {formatExportTime(report.generatedAt)}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" asChild>
                      <Link to="/reports/gap-assessment" search={{ analysisId: report.analysisId }}>
                        <Eye className="size-4" /> Open report
                      </Link>
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}
