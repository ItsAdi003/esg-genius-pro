import { useCountUp } from "@/hooks/use-count-up";

export function RadialScore({
  value,
  label,
  size = 168,
}: {
  value: number;
  label?: string;
  size?: number;
}) {
  const animated = useCountUp(value, 1200);
  const stroke = 12;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const offset = c - (animated / 100) * c;

  return (
    <div className="flex flex-col items-center">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            strokeWidth={stroke}
            className="stroke-muted"
          />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            strokeWidth={stroke}
            strokeLinecap="round"
            strokeDasharray={c}
            strokeDashoffset={offset}
            className="stroke-primary"
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-4xl font-semibold tabular-nums tracking-tight">
            {Math.round(animated)}%
          </span>
          {label && <span className="text-[11px] text-muted-foreground">{label}</span>}
        </div>
      </div>
    </div>
  );
}
