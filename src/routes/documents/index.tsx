import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Eye, ScanSearch, Trash2, FileText } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
import { UploadDialog } from "@/components/upload-dialog";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { documents as allDocuments } from "@/lib/esg-data";

export const Route = createFileRoute("/documents/")({
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
  const navigate = useNavigate();
  const [removed, setRemoved] = useState<string[]>([]);
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);

  const documents = allDocuments.filter((d) => !removed.includes(d.id));
  const analyzed = documents.filter((d) => d.status === "Analyzed").length;
  const pages = documents.reduce((a, d) => a + d.pages, 0);
  const target = allDocuments.find((d) => d.id === pendingDelete);

  const stats = [
    { label: "Documents in library", value: documents.length },
    { label: "Analyzed", value: analyzed },
    { label: "Total pages indexed", value: pages },
  ];

  return (
    <AppLayout
      title="Documents"
      description="Evidence library used for AI-assisted compliance gap analysis"
      actions={<UploadDialog />}
    >
      <div className="grid gap-4 sm:grid-cols-3">
        {stats.map((s) => (
          <div key={s.label} className="glass-panel glass-hover p-4">
            <p className="text-sm text-muted-foreground">{s.label}</p>
            <p className="mt-2 text-2xl font-semibold tabular-nums">{s.value}</p>
          </div>
        ))}
      </div>

      <div className="glass-panel mt-4 overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/40">
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
                <TableRow key={d.id} className="group transition-colors hover:bg-accent/40">
                  <TableCell>
                    <div className="flex items-center gap-2.5">
                      <span className="flex size-8 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                        <FileText className="size-4" />
                      </span>
                      <div>
                        <Link
                          to="/documents/$documentId"
                          params={{ documentId: d.id }}
                          className="font-medium hover:text-primary hover:underline"
                        >
                          {d.name}
                        </Link>
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
                    <div className="flex justify-end gap-1 opacity-70 transition-opacity group-hover:opacity-100">
                      <Button variant="ghost" size="sm" asChild>
                        <Link to="/documents/$documentId" params={{ documentId: d.id }}>
                          <Eye className="size-4" /> View
                        </Link>
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          toast.success(`Analysis started for ${d.name}`);
                          navigate({ to: "/compliance" });
                        }}
                      >
                        <ScanSearch className="size-4" /> Analyze
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-danger hover:bg-danger-soft hover:text-danger"
                        onClick={() => setPendingDelete(d.id)}
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

      <AlertDialog open={pendingDelete !== null} onOpenChange={(o) => !o && setPendingDelete(null)}>
        <AlertDialogContent className="glass-panel">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this document?</AlertDialogTitle>
            <AlertDialogDescription>
              {target?.name} will be removed from the evidence library. Requirements citing this
              document will need to be re-analysed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (pendingDelete) setRemoved((r) => [...r, pendingDelete]);
                setPendingDelete(null);
                toast.success("Document deleted");
              }}
            >
              Delete document
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AppLayout>
  );
}
