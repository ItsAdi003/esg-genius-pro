import { Info } from "lucide-react";
import { cn } from "@/lib/utils";

type PrototypeNoticeVariant = "banner" | "inline";

export function PrototypeNotice({
  title,
  children,
  variant = "banner",
  className,
}: {
  title: string;
  children: React.ReactNode;
  variant?: PrototypeNoticeVariant;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "rounded-lg border border-border/80 bg-muted/30",
        variant === "banner" ? "px-4 py-3" : "px-3 py-2",
        className,
      )}
    >
      <div className="flex gap-2.5">
        <Info className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
        <div className="min-w-0">
          <p className="text-xs font-medium text-foreground">{title}</p>
          <p className="mt-0.5 text-xs leading-relaxed text-muted-foreground">{children}</p>
        </div>
      </div>
    </div>
  );
}

export function PrototypeBadge({ label = "Preview" }: { label?: string }) {
  return (
    <span className="rounded-full border border-border bg-muted/60 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
      {label}
    </span>
  );
}
