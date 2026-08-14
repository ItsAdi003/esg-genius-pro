import { createFileRoute, Link } from "@tanstack/react-router";
import { Eye, ScanSearch, Trash2, FileText } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
import { UploadDialog } from "@/components/upload-dialog";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { documents } from "@/lib/esg-data";

export const Route = createFileRoute("/documents")({
  head: () => ({
    meta: [
      { title: "Document Library | ESGenius" },
      {
        name: "description",
        content:
          "Manage sustainability reports, policies and operational data used as evidence for ESG compliance gap analysis.",
      },
      { property: "og:title", content: "Document Library | ESGenius" },
      {
        property: "og:description",
        content: "Upload, review and analyse ESG evidence documents for SEBI BRSR reporting.",
      },
    ],
  }),
  component: Documents,
});

function Documents() {
  const analyzed = documents.filter((d) => d.status === "Analyzed").length;
  const pages = documents.reduce((a, d) => a + d.pages, 0);

  return (
    <AppLayout
      title="Documents"
      description="Evidence library used for AI-assisted compliance gap analysis"
      actions={<UploadDialog />}
    >
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="surface-card p-4">
          <p className="text-sm text-muted-foreground">Documents in library</p>
          <p className="mt-2 text-2xl font-semibold tabular-nums">{documents.length}</p>
        </div>
        <div className="surface-card p-4">
          <p className="text-sm text-muted-foreground">Analyzed</p>
          <p className="mt-2 text-2xl font-semibold tabular-nums">{analyzed}</p>
        </div>
        <div className="surface-card p-4">
          <p className="text-sm text-muted-foreground">Total pages indexed</p>
          <p className="mt-2 text-2xl font-semibold tabular-nums">{pages}</p>
        </div>
      </div>

      <div className="surface-card mt-4 overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                <TableHead>Document Name</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Reporting Year</TableHead>
                <TableHead>Upload Date</TableHead>
                <TableHead>Processing Status</TableHead>
                <TableHead className="text-right">Pages</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {documents.map((d) => (
                <TableRow key={d.id}>
                  <TableCell>
                    <div className="flex items-center gap-2.5">
                      <span className="flex size-8 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                        <FileText className="size-4" />
                      </span>
                      <div>
                        <p className="font-medium">{d.name}</p>
                        <p className="text-xs text-muted-foreground">{d.category}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{d.type}</TableCell>
                  <TableCell className="text-muted-foreground">{d.year}</TableCell>
                  <TableCell className="text-muted-foreground">{d.uploaded}</TableCell>
                  <TableCell>
                    <StatusBadge status={d.status} />
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{d.pages}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="sm">
                        <Eye className="size-4" /> View
                      </Button>
                      <Button variant="ghost" size="sm" asChild>
                        <Link to="/compliance">
                          <ScanSearch className="size-4" /> Analyze
                        </Link>
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-danger hover:bg-danger-soft hover:text-danger"
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    </AppLayout>
  );
}
