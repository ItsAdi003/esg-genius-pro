import { createFileRoute, Link, useNavigate, useParams } from "@tanstack/react-router";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Download,
  ScanSearch,
  Trash2,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { getDocument, getDocumentDetail } from "@/lib/esg-data";

export const Route = createFileRoute("/documents/$documentId")({
  head: () => ({
    meta: [
      { title: "Document Preview | ESGenius" },
      {
        name: "description",
        content:
          "Preview an uploaded ESG evidence document with extracted text, detected sustainability metrics and analysis history.",
      },
      { property: "og:title", content: "Document Preview | ESGenius" },
      {
        property: "og:description",
        content: "Document viewer with extracted ESG entities, metrics and source page references.",
      },
    ],
  }),
  component: DocumentDetailPage,
});

function DocumentDetailPage() {
  const { documentId } = useParams({ from: "/documents/$documentId" });
  const navigate = useNavigate();
  const doc = getDocument(documentId);
  const [pageIndex, setPageIndex] = useState(0);
  const [zoom, setZoom] = useState(100);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const detail = useMemo(() => (doc ? getDocumentDetail(doc) : null), [doc]);

  if (!doc || !detail) {
    return (
      <AppLayout title="Document not found" description="This document is no longer available">
        <div className="glass-panel p-8 text-center">
          <p className="text-sm text-muted-foreground">
            We couldn't find that document in the evidence library.
          </p>
          <Button className="mt-4" asChild>
            <Link to="/documents">Back to Documents</Link>
          </Button>
        </div>
      </AppLayout>
    );
  }

  const pages = detail.pagesContent;
  const current = pages[Math.min(pageIndex, pages.length - 1)]!;
  const processing =
    doc.status === "Analyzed" ? "Ready for Analysis" : doc.status;

  return (
    <AppLayout
      title={doc.name}
      description={`${doc.type} · Reporting Year ${doc.year} · ${doc.pages} pages`}
      actions={
        <>
          <Button variant="ghost" asChild>
            <Link to="/documents">
              <ArrowLeft className="size-4" /> Back to Documents
            </Link>
          </Button>
          <Button
            onClick={() => {
              toast.success(`Analysis started for ${doc.name}`);
              navigate({ to: "/compliance" });
            }}
          >
            <ScanSearch className="size-4" /> Run Compliance Analysis
          </Button>
          <Button variant="outline" onClick={() => toast("Download started (demo file)")}>
            <Download className="size-4" /> Download
          </Button>
          <Button
            variant="ghost"
            className="text-danger hover:bg-danger-soft hover:text-danger"
            onClick={() => setConfirmDelete(true)}
          >
            <Trash2 className="size-4" /> Delete
          </Button>
        </>
      }
    >
      <div className="glass-panel grid gap-5 p-5 sm:grid-cols-2 lg:grid-cols-5">
        {[
          { label: "Document Type", value: doc.type },
          { label: "Reporting Year", value: doc.year },
          { label: "Upload Date", value: doc.uploaded },
          { label: "Pages", value: String(doc.pages) },
        ].map((f) => (
          <div key={f.label}>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">{f.label}</p>
            <p className="mt-1 font-medium">{f.value}</p>
          </div>
        ))}
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Processing Status</p>
          <div className="mt-1.5">
            <StatusBadge status={processing} />
          </div>
        </div>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-[1.35fr_1fr]">
        <section className="glass-panel overflow-hidden">
          <div className="flex flex-wrap items-center gap-2 border-b border-border/70 px-4 py-2.5">
            <Button
              variant="ghost"
              size="icon"
              disabled={pageIndex === 0}
              onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
              aria-label="Previous page"
            >
              <ChevronLeft className="size-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              disabled={pageIndex >= pages.length - 1}
              onClick={() => setPageIndex((p) => Math.min(pages.length - 1, p + 1))}
              aria-label="Next page"
            >
              <ChevronRight className="size-4" />
            </Button>
            <Select
              value={String(pageIndex)}
              onValueChange={(v) => setPageIndex(Number(v))}
            >
              <SelectTrigger className="h-8 w-44">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {pages.map((p, i) => (
                  <SelectItem key={p.page} value={String(i)}>
                    Page {p.page} — {p.heading.split("—").pop()?.trim()}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <span className="text-xs text-muted-foreground">
              Page {current.page} of {doc.pages}
            </span>
            <div className="ml-auto flex items-center gap-1">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setZoom((z) => Math.max(70, z - 10))}
                aria-label="Zoom out"
              >
                <ZoomOut className="size-4" />
              </Button>
              <span className="w-12 text-center text-xs tabular-nums text-muted-foreground">
                {zoom}%
              </span>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setZoom((z) => Math.min(150, z + 10))}
                aria-label="Zoom in"
              >
                <ZoomIn className="size-4" />
              </Button>
            </div>
          </div>

          <div className="max-h-[70vh] overflow-auto bg-muted/40 p-6">
            <article
              className="mx-auto max-w-2xl origin-top rounded-lg border border-border bg-card p-8 shadow-lg transition-transform duration-200"
              style={{ transform: `scale(${zoom / 100})` }}
            >
              <p className="text-[11px] uppercase tracking-wide text-muted-foreground">
                {doc.name} · Page {current.page}
              </p>
              <h2 className="mt-3 text-lg font-semibold">{current.heading}</h2>
              <div className="mt-4 space-y-3 text-sm leading-relaxed text-muted-foreground">
                {current.body.map((para, i) => (
                  <p key={i}>{para}</p>
                ))}
              </div>
              <div className="mt-8 space-y-2" aria-hidden>
                {[92, 86, 78, 94, 64].map((w, i) => (
                  <div
                    key={i}
                    className="h-2 rounded bg-muted"
                    style={{ width: `${w}%` }}
                  />
                ))}
              </div>
            </article>
          </div>
        </section>

        <section className="glass-panel p-5">
          <h2 className="text-sm font-semibold">Extracted Information</h2>
          <Tabs defaultValue="text" className="mt-3">
            <TabsList className="grid w-full grid-cols-4">
              <TabsTrigger value="text">Text</TabsTrigger>
              <TabsTrigger value="entities">Entities</TabsTrigger>
              <TabsTrigger value="metrics">Metrics</TabsTrigger>
              <TabsTrigger value="history">History</TabsTrigger>
            </TabsList>

            <TabsContent value="text" className="mt-4">
              <p className="rounded-lg border border-border/70 bg-muted/40 p-4 text-sm leading-relaxed text-muted-foreground">
                {detail.extractedText}
              </p>
            </TabsContent>

            <TabsContent value="entities" className="mt-4 space-y-2">
              {detail.entities.map((e) => (
                <div
                  key={e.name}
                  className="flex items-center justify-between rounded-lg border border-border/70 bg-card/60 px-3 py-2"
                >
                  <div>
                    <p className="text-sm font-medium">{e.name}</p>
                    <p className="text-[11px] text-muted-foreground">{e.type}</p>
                  </div>
                  <span className="text-[11px] text-muted-foreground">Page {e.page}</span>
                </div>
              ))}
            </TabsContent>

            <TabsContent value="metrics" className="mt-4 space-y-2">
              {detail.metrics.map((m) => (
                <div
                  key={m.label}
                  className="flex items-center justify-between rounded-lg border border-border/70 bg-card/60 px-3 py-2"
                >
                  <div>
                    <p className="text-sm font-medium">{m.label}</p>
                    <p className="text-[11px] text-muted-foreground">{m.category}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-semibold tabular-nums text-primary">{m.value}</p>
                    <p className="text-[11px] text-muted-foreground">Page {m.page}</p>
                  </div>
                </div>
              ))}
            </TabsContent>

            <TabsContent value="history" className="mt-4">
              <ol className="relative space-y-4 border-l border-border pl-5">
                {detail.history.map((h, i) => (
                  <li key={i}>
                    <span className="absolute -left-[5px] mt-1.5 size-2.5 rounded-full bg-primary" />
                    <p className="text-sm font-medium">{h.event}</p>
                    <p className="text-[11px] text-muted-foreground">
                      {h.date} · {h.detail}
                    </p>
                  </li>
                ))}
              </ol>
            </TabsContent>
          </Tabs>
        </section>
      </div>

      <AlertDialog open={confirmDelete} onOpenChange={setConfirmDelete}>
        <AlertDialogContent className="glass-panel">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this document?</AlertDialogTitle>
            <AlertDialogDescription>
              {doc.name} will be removed from the evidence library in this demo session.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                toast.success("Document deleted");
                navigate({ to: "/documents" });
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
