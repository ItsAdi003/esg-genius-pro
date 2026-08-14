import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Search, ArrowRight, FileText, MinusCircle } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { ConfidenceMeter, PriorityBadge, StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ORG, requirements } from "@/lib/esg-data";

export const Route = createFileRoute("/compliance/")({
  head: () => ({
    meta: [
      { title: "Compliance Gap Analysis | ESGenius" },
      {
        name: "description",
        content:
          "Requirement-level SEBI BRSR gap analysis with evidence citations, confidence scores and remediation priority.",
      },
      { property: "og:title", content: "Compliance Gap Analysis | ESGenius" },
      {
        property: "og:description",
        content:
          "Review every BRSR requirement with its assessment status, retrieved evidence and priority.",
      },
    ],
  }),
  component: ComplianceAnalysis,
});

function ComplianceAnalysis() {
  const [category, setCategory] = useState("all");
  const [status, setStatus] = useState("all");
  const [priority, setPriority] = useState("all");
  const [query, setQuery] = useState("");

  const rows = useMemo(
    () =>
      requirements.filter(
        (r) =>
          (category === "all" || r.category === category) &&
          (status === "all" || r.status === status) &&
          (priority === "all" || r.priority === priority) &&
          (query.trim() === "" ||
            `${r.id} ${r.title}`.toLowerCase().includes(query.trim().toLowerCase())),
      ),
    [category, status, priority, query],
  );

  return (
    <AppLayout
      title="Compliance Analysis"
      description="AI-assisted assessment of disclosures against framework requirements"
      actions={
        <Button variant="outline" asChild>
          <Link to="/reports/gap-assessment">Preview Gap Report</Link>
        </Button>
      }
    >
      <div className="surface-card flex flex-wrap items-center gap-x-10 gap-y-4 p-5">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Framework</p>
          <p className="mt-1 font-medium">{ORG.framework}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Organization</p>
          <p className="mt-1 font-medium">{ORG.name}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Reporting Period</p>
          <p className="mt-1 font-medium">{ORG.reportingPeriod}</p>
        </div>
        <div className="ml-auto text-right">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">
            Overall ESG Reporting Readiness
          </p>
          <p className="mt-1 text-3xl font-semibold tabular-nums text-primary">{ORG.readiness}%</p>
        </div>
      </div>

      <div className="surface-card mt-4 grid gap-3 p-4 md:grid-cols-4">
        <div className="relative md:col-span-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search requirement"
            className="pl-9"
          />
        </div>
        <Select value={category} onValueChange={setCategory}>
          <SelectTrigger>
            <SelectValue placeholder="ESG Category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All ESG Categories</SelectItem>
            <SelectItem value="Environmental">Environmental</SelectItem>
            <SelectItem value="Social">Social</SelectItem>
            <SelectItem value="Governance">Governance</SelectItem>
          </SelectContent>
        </Select>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger>
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Statuses</SelectItem>
            <SelectItem value="Covered">Covered</SelectItem>
            <SelectItem value="Partially Covered">Partially Covered</SelectItem>
            <SelectItem value="Evidence Not Found">Evidence Not Found</SelectItem>
            <SelectItem value="Human Review Required">Human Review Required</SelectItem>
          </SelectContent>
        </Select>
        <Select value={priority} onValueChange={setPriority}>
          <SelectTrigger>
            <SelectValue placeholder="Priority" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Priorities</SelectItem>
            <SelectItem value="High">High</SelectItem>
            <SelectItem value="Medium">Medium</SelectItem>
            <SelectItem value="Low">Low</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="surface-card mt-4 overflow-hidden">
        <div className="flex items-center justify-between border-b border-border px-5 py-3">
          <p className="text-sm font-semibold">Requirement Assessment</p>
          <p className="text-xs text-muted-foreground">
            {rows.length} of {requirements.length} requirements
          </p>
        </div>
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                <TableHead className="w-24">Req. ID</TableHead>
                <TableHead className="min-w-[240px]">Requirement</TableHead>
                <TableHead>ESG Category</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Confidence</TableHead>
                <TableHead className="min-w-[200px]">Evidence</TableHead>
                <TableHead>Priority</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="font-mono text-xs text-muted-foreground">{r.id}</TableCell>
                  <TableCell className="font-medium">{r.title}</TableCell>
                  <TableCell className="text-muted-foreground">{r.category}</TableCell>
                  <TableCell>
                    <StatusBadge status={r.status} />
                  </TableCell>
                  <TableCell>
                    <ConfidenceMeter value={r.confidence} />
                  </TableCell>
                  <TableCell>
                    {r.evidence ? (
                      <div className="flex items-start gap-2 text-xs">
                        <FileText className="mt-0.5 size-3.5 shrink-0 text-primary" />
                        <span>
                          {r.evidence.document}
                          <span className="block text-muted-foreground">
                            Page {r.evidence.page}
                          </span>
                        </span>
                      </div>
                    ) : (
                      <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <MinusCircle className="size-3.5" /> Evidence not found
                      </span>
                    )}
                  </TableCell>
                  <TableCell>
                    <PriorityBadge priority={r.priority} />
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="outline" size="sm" asChild>
                      <Link to="/compliance/$requirementId" params={{ requirementId: r.id }}>
                        View Details <ArrowRight className="size-3.5" />
                      </Link>
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8} className="py-10 text-center text-sm text-muted-foreground">
                    No requirements match the current filters.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      </div>
    </AppLayout>
  );
}
