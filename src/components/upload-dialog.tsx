import { useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { UploadCloud, FileText, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import type { CompanySummary } from "@/lib/company-esg-api";
import {
  DOCUMENT_TYPE_OPTIONS,
  REPORTING_YEAR_OPTIONS,
  documentQueryKeys,
  uploadDocument,
  type DocumentType,
} from "@/lib/document-api";

interface UploadDialogProps {
  trigger?: React.ReactNode;
  companies: CompanySummary[];
  selectedOrganizationId: number | null;
  onOrganizationChange: (organizationId: number) => void;
  companiesLoading?: boolean;
}

export function UploadDialog({
  trigger,
  companies,
  selectedOrganizationId,
  onOrganizationChange,
  companiesLoading = false,
}: UploadDialogProps) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [open, setOpen] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [organizationId, setOrganizationId] = useState<number | null>(selectedOrganizationId);
  const [documentType, setDocumentType] = useState<DocumentType | "">("");
  const [reportingYear, setReportingYear] = useState<string>("");

  const reset = () => {
    setFile(null);
    setDocumentType("");
    setReportingYear("");
    setOrganizationId(selectedOrganizationId);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const uploadMutation = useMutation({
    mutationFn: uploadDocument,
    onSuccess: (document) => {
      toast.success(`${document.originalFilename} uploaded successfully`);
      void queryClient.invalidateQueries({
        queryKey: documentQueryKeys.list(document.organizationId),
      });
      setOpen(false);
      reset();
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to upload document");
    },
  });

  const handleFileSelection = (selectedFile: File | undefined) => {
    if (!selectedFile) return;
    if (!selectedFile.name.toLowerCase().endsWith(".pdf")) {
      toast.error("Only PDF files are supported");
      return;
    }
    if (selectedFile.size > 25 * 1024 * 1024) {
      toast.error("File size exceeds maximum allowed size of 25 MB");
      return;
    }
    setFile(selectedFile);
  };

  const canUpload =
    file != null &&
    organizationId != null &&
    documentType !== "" &&
    reportingYear !== "" &&
    !uploadMutation.isPending;

  const handleUpload = () => {
    if (!canUpload || organizationId == null || documentType === "") return;

    uploadMutation.mutate({
      file,
      organizationId,
      documentType,
      reportingYear: Number(reportingYear),
    });
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(value) => {
        setOpen(value);
        if (value) {
          setOrganizationId(selectedOrganizationId);
        } else {
          reset();
        }
      }}
    >
      <DialogTrigger asChild>
        {trigger ?? (
          <Button>
            <UploadCloud className="size-4" /> Upload Document
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Upload Document</DialogTitle>
          <DialogDescription>
            Upload a PDF to the evidence library. Text is extracted immediately using PDFBox.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div
            onDragOver={(event) => {
              event.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={(event) => {
              event.preventDefault();
              setDragging(false);
              handleFileSelection(event.dataTransfer.files?.[0]);
            }}
            className={cn(
              "flex flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-9 text-center transition-colors",
              dragging ? "border-primary bg-accent/60" : "border-border bg-muted/40",
            )}
          >
            <UploadCloud className="size-8 text-primary" />
            <p className="mt-3 text-sm font-medium">Drag and drop a PDF here</p>
            <p className="mt-1 text-xs text-muted-foreground">or</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              type="button"
              disabled={uploadMutation.isPending}
              onClick={() => fileInputRef.current?.click()}
            >
              Browse files
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept="application/pdf,.pdf"
              className="hidden"
              onChange={(event) => handleFileSelection(event.target.files?.[0])}
            />
            <div className="mt-3 flex items-center gap-2 text-[11px] text-muted-foreground">
              <span className="rounded border border-border bg-card px-1.5 py-0.5">PDF</span>
              <span>up to 25 MB</span>
            </div>
          </div>

          {file && (
            <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm">
              <FileText className="size-4 text-primary" />
              <span className="truncate">{file.name}</span>
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5 sm:col-span-2">
              <Label>Company</Label>
              <Select
                value={organizationId != null ? String(organizationId) : undefined}
                onValueChange={(value) => {
                  const id = Number(value);
                  setOrganizationId(id);
                  onOrganizationChange(id);
                }}
                disabled={companiesLoading || uploadMutation.isPending}
              >
                <SelectTrigger>
                  <SelectValue placeholder={companiesLoading ? "Loading companies…" : "Select company"} />
                </SelectTrigger>
                <SelectContent>
                  {companies.map((company) => (
                    <SelectItem key={company.id} value={String(company.id)}>
                      {company.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label>Document Type</Label>
              <Select
                value={documentType || undefined}
                onValueChange={(value) => setDocumentType(value as DocumentType)}
                disabled={uploadMutation.isPending}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  {DOCUMENT_TYPE_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label>Reporting Year</Label>
              <Select
                value={reportingYear || undefined}
                onValueChange={setReportingYear}
                disabled={uploadMutation.isPending}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select year" />
                </SelectTrigger>
                <SelectContent>
                  {REPORTING_YEAR_OPTIONS.map((year) => (
                    <SelectItem key={year} value={String(year)}>
                      {year}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {uploadMutation.isPending && (
            <div className="flex items-center gap-2 rounded-lg border border-border bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
              <Loader2 className="size-4 animate-spin text-primary" />
              Uploading and extracting text…
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)} disabled={uploadMutation.isPending}>
            Cancel
          </Button>
          <Button onClick={handleUpload} disabled={!canUpload}>
            {uploadMutation.isPending ? (
              <>
                <Loader2 className="size-4 animate-spin" /> Uploading…
              </>
            ) : (
              "Upload"
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
