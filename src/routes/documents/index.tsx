import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Eye, ScanSearch, Trash2, FileText, RefreshCw } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { StatusBadge } from "@/components/status-badge";
import { UploadDialog } from "@/components/upload-dialog";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { companyEsgQueryKeys, getCompanies } from "@/lib/company-esg-api";
import { createComplianceAnalysis } from "@/lib/compliance-api";
import {
  deleteDocument,
  documentQueryKeys,
  formatDocumentStatus,
  formatDocumentType,
  formatFileSize,
  formatInstant,
  formatReportingYear,
  listDocuments,
  type DocumentSummary,
} from "@/lib/document-api";

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
  const queryClient = useQueryClient();
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<number | null>(null);
  const [pendingDelete, setPendingDelete] = useState<number | null>(null);

  const companiesQuery = useQuery({
    queryKey: companyEsgQueryKeys.companies(),
    queryFn: getCompanies,
  });

  const companies = companiesQuery.data ?? [];

  useEffect(() => {
    if (selectedOrganizationId != null || companies.length === 0) return;
    const preferred =
      companies.find((company) => company.name === "Infosys Limited") ?? companies[0];
    setSelectedOrganizationId(preferred.id);
  }, [companies, selectedOrganizationId]);

  const documentsQuery = useQuery({
    queryKey:
      selectedOrganizationId != null
        ? documentQueryKeys.list(selectedOrganizationId)
        : [...documentQueryKeys.all, "list", "none"],
    queryFn: () => listDocuments(selectedOrganizationId!),
    enabled: selectedOrganizationId != null,
  });

  const documents = documentsQuery.data ?? [];

  const analyzeMutation = useMutation({
    mutationFn: createComplianceAnalysis,
    onSuccess: (analysis) => {
      toast.success("Compliance analysis completed");
      navigate({
        to: "/compliance",
        search: { analysisId: analysis.id },
      });
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to run compliance analysis");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteDocument,
    onSuccess: () => {
      if (selectedOrganizationId != null) {
        void queryClient.invalidateQueries({
          queryKey: documentQueryKeys.list(selectedOrganizationId),
        });
      }
      setPendingDelete(null);
      toast.success("Document deleted");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to delete document");
    },
  });

  const stats = useMemo(() => {
    const readyCount = documents.filter((document) => document.status === "READY").length;
    const totalPages = documents.reduce((total, document) => total + (document.pageCount ?? 0), 0);
    return [
      { label: "Documents in library", value: documents.length },
      { label: "Ready for analysis", value: readyCount },
      { label: "Total pages indexed", value: totalPages },
    ];
  }, [documents]);

  const target = documents.find((document) => document.id === pendingDelete) ?? null;
  const selectedCompany = companies.find((company) => company.id === selectedOrganizationId);

  return (
    <AppLayout
      title="Documents"
      description="Evidence library used for AI-assisted compliance gap analysis"
      actions={
        <UploadDialog
          companies={companies}
          selectedOrganizationId={selectedOrganizationId}
          onOrganizationChange={setSelectedOrganizationId}
          companiesLoading={companiesQuery.isLoading}
        />
      }
    >
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div className="space-y-1.5">
          <p className="text-sm font-medium">Company</p>
          {companiesQuery.isLoading ? (
            <Skeleton className="h-10 w-56" />
          ) : companiesQuery.isError ? (
            <p className="text-sm text-danger">Failed to load companies</p>
          ) : (
            <Select
              value={selectedOrganizationId != null ? String(selectedOrganizationId) : undefined}
              onValueChange={(value) => setSelectedOrganizationId(Number(value))}
            >
              <SelectTrigger className="w-56">
                <SelectValue placeholder="Select company" />
              </SelectTrigger>
              <SelectContent>
                {companies.map((company) => (
                  <SelectItem key={company.id} value={String(company.id)}>
                    {company.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
        {selectedCompany && (
          <p className="text-sm text-muted-foreground">
            Showing documents for {selectedCompany.name}
          </p>
        )}
      </div>

      {companiesQuery.isError && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Unable to load companies</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{companiesQuery.error.message}</span>
            <Button variant="outline" size="sm" onClick={() => void companiesQuery.refetch()}>
              <RefreshCw className="size-4" /> Retry
            </Button>
          </AlertDescription>
        </Alert>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        {documentsQuery.isLoading
          ? Array.from({ length: 3 }).map((_, index) => (
              <div key={index} className="glass-panel p-4">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="mt-3 h-8 w-16" />
              </div>
            ))
          : stats.map((stat) => (
              <div key={stat.label} className="glass-panel glass-hover p-4">
                <p className="text-sm text-muted-foreground">{stat.label}</p>
                <p className="mt-2 text-2xl font-semibold tabular-nums">{stat.value}</p>
              </div>
            ))}
      </div>

      {documentsQuery.isError && (
        <Alert variant="destructive" className="mt-4">
          <AlertTitle>Unable to load documents</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{documentsQuery.error.message}</span>
            <Button variant="outline" size="sm" onClick={() => void documentsQuery.refetch()}>
              <RefreshCw className="size-4" /> Retry
            </Button>
          </AlertDescription>
        </Alert>
      )}

      <div className="glass-panel mt-4 overflow-hidden">
        {documentsQuery.isLoading ? (
          <div className="space-y-3 p-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} className="h-12 w-full" />
            ))}
          </div>
        ) : documents.length === 0 && !documentsQuery.isError ? (
          <div className="p-10 text-center">
            <FileText className="mx-auto size-10 text-muted-foreground" />
            <p className="mt-4 text-sm font-medium">No documents yet</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Upload a PDF for {selectedCompany?.name ?? "the selected company"} to start building
              the evidence library.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/40">
                  <TableHead>Document Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Reporting Year</TableHead>
                  <TableHead>Upload Date</TableHead>
                  <TableHead>Processing Status</TableHead>
                  <TableHead className="text-right">Size</TableHead>
                  <TableHead className="text-right">Pages</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {documents.map((document) => (
                  <DocumentRow
                    key={document.id}
                    document={document}
                    analyzePending={
                      analyzeMutation.isPending && analyzeMutation.variables === document.id
                    }
                    onAnalyze={() => analyzeMutation.mutate(document.id)}
                    onDelete={() => setPendingDelete(document.id)}
                  />
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      <AlertDialog open={pendingDelete !== null} onOpenChange={(open) => !open && setPendingDelete(null)}>
        <AlertDialogContent className="glass-panel">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this document?</AlertDialogTitle>
            <AlertDialogDescription>
              {target?.originalFilename} will be removed from the evidence library. Requirements
              citing this document will need to be re-analysed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={deleteMutation.isPending || pendingDelete == null}
              onClick={() => {
                if (pendingDelete != null) {
                  deleteMutation.mutate(pendingDelete);
                }
              }}
            >
              {deleteMutation.isPending ? "Deleting…" : "Delete document"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AppLayout>
  );
}

function DocumentRow({
  document,
  analyzePending,
  onAnalyze,
  onDelete,
}: {
  document: DocumentSummary;
  analyzePending: boolean;
  onAnalyze: () => void;
  onDelete: () => void;
}) {
  return (
    <TableRow className="group transition-colors hover:bg-accent/40">
      <TableCell>
        <div className="flex items-center gap-2.5">
          <span className="flex size-8 items-center justify-center rounded-lg bg-accent text-accent-foreground">
            <FileText className="size-4" />
          </span>
          <div>
            <Link
              to="/documents/$documentId"
              params={{ documentId: String(document.id) }}
              className="font-medium hover:text-primary hover:underline"
            >
              {document.originalFilename}
            </Link>
            <p className="text-xs text-muted-foreground">{document.organizationName}</p>
          </div>
        </div>
      </TableCell>
      <TableCell className="text-muted-foreground">
        {formatDocumentType(document.documentType)}
      </TableCell>
      <TableCell className="text-muted-foreground">
        {formatReportingYear(document.reportingYear)}
      </TableCell>
      <TableCell className="text-muted-foreground">{formatInstant(document.uploadedAt)}</TableCell>
      <TableCell>
        <StatusBadge status={formatDocumentStatus(document.status)} />
      </TableCell>
      <TableCell className="text-right tabular-nums text-muted-foreground">
        {formatFileSize(document.fileSize)}
      </TableCell>
      <TableCell className="text-right tabular-nums">
        {document.pageCount ?? "—"}
      </TableCell>
      <TableCell>
        <div className="flex justify-end gap-1 opacity-70 transition-opacity group-hover:opacity-100">
          <Button variant="ghost" size="sm" asChild>
            <Link to="/documents/$documentId" params={{ documentId: String(document.id) }}>
              <Eye className="size-4" /> View
            </Link>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={onAnalyze}
            disabled={document.status !== "READY" || analyzePending}
          >
            <ScanSearch className="size-4" />
            {analyzePending ? "Analyzing…" : "Analyze"}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-danger hover:bg-danger-soft hover:text-danger"
            onClick={onDelete}
          >
            <Trash2 className="size-4" />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}
