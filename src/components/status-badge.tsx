import { cn } from "@/lib/utils";
import type { Priority, Status } from "@/lib/esg-data";

const statusStyles: Record<string, string> = {
  Covered: "bg-success-soft text-success border-success/25",
  "Partially Covered": "bg-warning-soft text-warning border-warning/30",
  "Evidence Not Found": "bg-danger-soft text-danger border-danger/25",
  "Human Review Required": "bg-info-soft text-info border-info/25",
  Missing: "bg-danger-soft text-danger border-danger/25",
  Partial: "bg-warning-soft text-warning border-warning/30",
  Analyzed: "bg-success-soft text-success border-success/25",
  Processing: "bg-info-soft text-info border-info/25",
  Queued: "bg-muted text-muted-foreground border-border",
  Active: "bg-success-soft text-success border-success/25",
  Planned: "bg-muted text-muted-foreground border-border",
};

const priorityStyles: Record<Priority, string> = {
  High: "bg-danger-soft text-danger border-danger/25",
  Medium: "bg-warning-soft text-warning border-warning/30",
  Low: "bg-muted text-muted-foreground border-border",
};

const base =
  "inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-0.5 text-xs font-medium";

export function StatusBadge({ status, className }: { status: Status | string; className?: string }) {
  return (
    <span className={cn(base, statusStyles[status] ?? statusStyles['Queued'], className)}>
      <span className="size-1.5 rounded-full bg-current" />
      {status}
    </span>
  );
}

export function PriorityBadge({ priority }: { priority: Priority | string }) {
  return (
    <span className={cn(base, priorityStyles[priority as Priority] ?? priorityStyles.Low)}>
      {priority} Priority
    </span>
  );
}

export function ConfidenceMeter({ value }: { value: number }) {
  const tone = value >= 90 ? "bg-success" : value >= 80 ? "bg-primary" : "bg-warning";
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-16 overflow-hidden rounded-full bg-muted">
        <div className={cn("h-full rounded-full", tone)} style={{ width: `${value}%` }} />
      </div>
      <span className="text-xs font-medium tabular-nums text-muted-foreground">{value}%</span>
    </div>
  );
}
