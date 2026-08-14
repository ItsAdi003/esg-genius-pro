import { useEffect, useState } from "react";
import { UploadCloud, FileText, CheckCircle2, Loader2 } from "lucide-react";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

const stages = [
  "Uploading document",
  "Extracting text",
  "Splitting document into sections",
  "Preparing document for AI analysis",
  "Ready for compliance analysis",
];

export function UploadDialog({ trigger }: { trigger?: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [stage, setStage] = useState(-1);
  const [dragging, setDragging] = useState(false);
  const [fileName, setFileName] = useState<string | null>(null);

  useEffect(() => {
    if (!processing || stage >= stages.length - 1) return;
    const t = setTimeout(() => setStage((s) => s + 1), 1100);
    return () => clearTimeout(t);
  }, [processing, stage]);

  const reset = () => {
    setProcessing(false);
    setStage(-1);
    setFileName(null);
  };

  const progress = processing ? ((stage + 1) / stages.length) * 100 : 0;

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        setOpen(v);
        if (!v) reset();
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
            Add a sustainability document to the evidence library for AI-assisted analysis.
          </DialogDescription>
        </DialogHeader>

        {!processing ? (
          <div className="space-y-4">
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setDragging(true);
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDragging(false);
                setFileName(e.dataTransfer.files?.[0]?.name ?? "Sustainability_Report.pdf");
              }}
              className={cn(
                "flex flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-9 text-center transition-colors",
                dragging ? "border-primary bg-accent/60" : "border-border bg-muted/40",
              )}
            >
              <UploadCloud className="size-8 text-primary" />
              <p className="mt-3 text-sm font-medium">Drag and drop a file here</p>
              <p className="mt-1 text-xs text-muted-foreground">or</p>
              <Button
                variant="outline"
                size="sm"
                className="mt-2"
                onClick={() => setFileName("Sustainability_Report_FY2025-26.pdf")}
              >
                Browse files
              </Button>
              <div className="mt-3 flex items-center gap-2 text-[11px] text-muted-foreground">
                <span className="rounded border border-border bg-card px-1.5 py-0.5">PDF</span>
                <span className="rounded border border-border bg-card px-1.5 py-0.5">DOCX</span>
                <span>up to 25 MB</span>
              </div>
            </div>

            {fileName && (
              <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm">
                <FileText className="size-4 text-primary" />
                <span className="truncate">{fileName}</span>
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="doc-name">Document Name</Label>
                <Input id="doc-name" placeholder="Sustainability Report FY2025-26" />
              </div>
              <div className="space-y-1.5">
                <Label>Document Type</Label>
                <Select>
                  <SelectTrigger>
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="sustainability">Sustainability Report</SelectItem>
                    <SelectItem value="annual">Annual Report</SelectItem>
                    <SelectItem value="policy">Policy Document</SelectItem>
                    <SelectItem value="operational">Operational Data</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Reporting Year</Label>
                <Select>
                  <SelectTrigger>
                    <SelectValue placeholder="Select year" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="2025-26">FY 2025-26</SelectItem>
                    <SelectItem value="2024-25">FY 2024-25</SelectItem>
                    <SelectItem value="2023-24">FY 2023-24</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label>ESG Category</Label>
                <Select>
                  <SelectTrigger>
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="environmental">Environmental</SelectItem>
                    <SelectItem value="social">Social</SelectItem>
                    <SelectItem value="governance">Governance</SelectItem>
                    <SelectItem value="cross">Cross-category</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <Progress value={progress} />
            <ul className="space-y-3">
              {stages.map((s, i) => (
                <li key={s} className="flex items-center gap-3 text-sm">
                  {i < stage ? (
                    <CheckCircle2 className="size-4 text-success" />
                  ) : i === stage ? (
                    <Loader2 className="size-4 animate-spin text-primary" />
                  ) : (
                    <span className="size-4 rounded-full border border-border" />
                  )}
                  <span
                    className={cn(
                      i <= stage ? "text-foreground" : "text-muted-foreground",
                      i === stages.length - 1 && stage === i && "font-medium text-success",
                    )}
                  >
                    {s}
                  </span>
                </li>
              ))}
            </ul>
            <p className="text-xs text-muted-foreground">
              Simulated processing pipeline. No document is transmitted in this preview.
            </p>
          </div>
        )}

        <DialogFooter>
          {!processing ? (
            <>
              <Button variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button
                onClick={() => {
                  setProcessing(true);
                  setStage(0);
                }}
              >
                Upload
              </Button>
            </>
          ) : (
            <Button
              disabled={stage < stages.length - 1}
              onClick={() => {
                setOpen(false);
                reset();
              }}
            >
              {stage < stages.length - 1 ? "Processing…" : "Done"}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
