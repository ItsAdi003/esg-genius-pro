import { createFileRoute, Link, useNavigate, useParams } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, RefreshCw, ScanSearch, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
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
import { Skeleton } from "@/components/ui/skeleton";
import { createComplianceAnalysis } from "@/lib/compliance-api";
import {
  deleteDocument,
  documentQueryKeys,
  formatDocumentStatus,
  formatDocumentType,
  formatFileSize,
  formatInstant,
  formatReportingYear,
  getDocument,
} from "@/lib/document-api";

export const Route = createFileRoute("/documents/$documentId")({
  head: () => ({
    meta: [
      { title: "Document Preview | ESGenius" },
      {
        name: "description",
        content:
          "Preview an uploaded ESG evidence document with extracted text and processing metadata.",
      },
      { property: "og:title", content: "Document Preview | ESGenius" },
      {
        property: "og:description",
        content: "Document viewer with extracted text from PDFBox processing.",
      },
    ],
  }),
  component: DocumentDetailPage,
});

function DocumentDetailPage() {
  const { documentId: documentIdParam } = useParams({ from: "/documents/$documentId" });
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [confirmDelete, setConfirmDelete] = useState(false);

  const documentId = Number(documentIdParam);
  const isValidId = Number.isInteger(documentId) && documentId > 0;

  const documentQuery = useQuery({
    queryKey: isValidId ? documentQueryKeys.detail(documentId) : [...documentQueryKeys.all, "detail", "invalid"],
    queryFn: () => getDocument(documentId),
    enabled: isValidId,
  });

  const analyzeMutation = useMutation({
    mutationFn: () => createComplianceAnalysis(documentId, "BRSR"),
    onSuccess: (analysis) => {
      toast.success("Compliance analysis completed");
      navigate({
        to: "/compliance",
        search: { analysisId: String(analysis.id) },
      });
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to run compliance analysis");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteDocument,
    onSuccess: () => {
      if (documentQuery.data) {
        void queryClient.invalidateQueries({
          queryKey: documentQueryKeys.list(documentQuery.data.organizationId),
        });
      }
      toast.success("Document deleted");
      navigate({ to: "/documents" });
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to delete document");
    },
  });

  if (!isValidId) {
    return (
      <AppLayout title="Document not found" description="This document is no longer available">
        <div className="glass-panel p-8 text-center">
          <p className="text-sm text-muted-foreground">
            The document ID in the URL is invalid.
          </p>
          <Button className="mt-4" asChild>
            <Link to="/documents">Back to Documents</Link>
          </Button>
        </div>
      </AppLayout>
    );
  }

  if (documentQuery.isLoading) {
    return (
      <AppLayout title="Loading document…" description="Fetching document metadata and extracted text">
        <div className="space-y-4">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-[50vh] w-full" />
        </div>
      </AppLayout>
    );
  }

  if (documentQuery.isError) {
    const notFound = documentQuery.error.message.toLowerCase().includes("not found");

    return (
      <AppLayout title={notFound ? "Document not found" : "Unable to load document"} description="">
        <div className="glass-panel p-8 text-center">
          <p className="text-sm text-muted-foreground">{documentQuery.error.message}</p>
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            {!notFound && (
              <Button variant="outline" onClick={() => void documentQuery.refetch()}>
                <RefreshCw className="size-4" /> Retry
              </Button>
            )}
            <Button asChild>
              <Link to="/documents">Back to Documents</Link>
            </Button>
          </div>
        </div>
      </AppLayout>
    );
  }

  const document = documentQuery.data;
  const statusLabel = formatDocumentStatus(document.status);
  const isFailed = document.status === "FAILED";

  return (
    <AppLayout
      title={document.originalFilename}
      description={`${formatDocumentType(document.documentType)} · Reporting Year ${formatReportingYear(document.reportingYear)} · ${document.pageCount ?? 0} pages`}
      actions={
        <>
          <Button variant="ghost" asChild>
            <Link to="/documents">
              <ArrowLeft className="size-4" /> Back to Documents
            </Link>
          </Button>
          <Button
            disabled={document.status !== "READY" || analyzeMutation.isPending}
            onClick={() => analyzeMutation.mutate()}
          >
            <ScanSearch className="size-4" />
            {analyzeMutation.isPending ? "Analyzing document…" : "Run Compliance Analysis"}
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
      {isFailed && document.failureReason && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Text extraction failed</AlertTitle>
          <AlertDescription>{document.failureReason}</AlertDescription>
        </Alert>
      )}

      <div className="glass-panel grid gap-5 p-5 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: "Organization", value: document.organizationName },
          { label: "Document Type", value: formatDocumentType(document.documentType) },
          { label: "Reporting Year", value: formatReportingYear(document.reportingYear) },
          { label: "File Size", value: formatFileSize(document.fileSize) },
          { label: "Pages", value: document.pageCount != null ? String(document.pageCount) : "—" },
          { label: "Uploaded", value: formatInstant(document.uploadedAt) },
          { label: "Processed", value: formatInstant(document.processedAt) },
        ].map((field) => (
          <div key={field.label}>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">{field.label}</p>
            <p className="mt-1 font-medium">{field.value}</p>
          </div>
        ))}
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Processing Status</p>
          <div className="mt-1.5">
            <StatusBadge status={statusLabel} />
          </div>
        </div>
      </div>

      <section className="glass-panel mt-4 overflow-hidden">
        <div className="border-b border-border/70 px-5 py-4">
          <h2 className="text-sm font-semibold">Extracted Text</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Plain text extracted by PDFBox. Page-level mapping is not available in this phase.
          </p>
        </div>

        <div className="max-h-[70vh] overflow-auto p-5">
          {document.status === "FAILED" ? (
            <p className="text-sm text-muted-foreground">
              No extracted text is available because processing failed.
              {document.failureReason ? ` ${document.failureReason}` : ""}
            </p>
          ) : document.extractedText && document.extractedText.trim().length > 0 ? (
            <pre className="whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-muted-foreground">
              {document.extractedText}
            </pre>
          ) : (
            <p className="text-sm text-muted-foreground">
              {document.status === "PROCESSING" || document.status === "UPLOADED"
                ? "Text extraction is still in progress."
                : "No extracted text is available for this document."}
            </p>
          )}
        </div>
      </section>

      <AlertDialog open={confirmDelete} onOpenChange={setConfirmDelete}>
        <AlertDialogContent className="glass-panel">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this document?</AlertDialogTitle>
            <AlertDialogDescription>
              {document.originalFilename} will be permanently removed from the evidence library.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={deleteMutation.isPending}
              onClick={() => deleteMutation.mutate(document.id)}
            >
              {deleteMutation.isPending ? "Deleting…" : "Delete document"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AppLayout>
  );
}
