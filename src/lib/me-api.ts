import { apiFetch } from "@/lib/api-config";

export interface UsageLimit {
  limit: number;
  used: number;
  resetsInSeconds: number;
}

export interface MeLimits {
  uploadsPerDay: UsageLimit | null;
  analysesPerDay: UsageLimit | null;
  assistantAsksPerHour: UsageLimit | null;
}

export interface MeProfile {
  email: string | null;
  admin: boolean;
  limits: MeLimits;
}

export class MeApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "MeApiError";
    this.status = status;
  }
}

export const meQueryKeys = {
  all: ["me"] as const,
  profile: () => [...meQueryKeys.all, "profile"] as const,
};

/**
 * GET /api/v1/me — account email, role, and rolling usage limits.
 * An older backend without this endpoint answers 404; treat that as unavailable (hide UI).
 */
export async function fetchMe(): Promise<MeProfile | null> {
  const response = await apiFetch("/api/v1/me");

  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    let message = "Failed to load account usage";
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore non-JSON error bodies
    }
    throw new MeApiError(message, response.status);
  }

  return parseMeProfile(await response.json());
}

export function parseMeProfile(payload: unknown): MeProfile {
  const defaults: MeLimits = {
    uploadsPerDay: null,
    analysesPerDay: null,
    assistantAsksPerHour: null,
  };

  if (typeof payload !== "object" || payload === null) {
    return { email: null, admin: false, limits: defaults };
  }

  const row = payload as Record<string, unknown>;
  const limitsRaw = row["limits"];

  let limits = defaults;
  if (typeof limitsRaw === "object" && limitsRaw !== null) {
    const lim = limitsRaw as Record<string, unknown>;
    limits = {
      uploadsPerDay: parseUsageLimit(lim["uploadsPerDay"]),
      analysesPerDay: parseUsageLimit(lim["analysesPerDay"]),
      assistantAsksPerHour: parseUsageLimit(lim["assistantAsksPerHour"]),
    };
  }

  return {
    email: typeof row["email"] === "string" ? row["email"] : row["email"] === null ? null : null,
    admin: row["admin"] === true,
    limits,
  };
}

function parseUsageLimit(value: unknown): UsageLimit | null {
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value !== "object" || value === null) {
    return null;
  }
  const row = value as Record<string, unknown>;
  if (
    typeof row["limit"] !== "number" ||
    typeof row["used"] !== "number" ||
    typeof row["resetsInSeconds"] !== "number"
  ) {
    return null;
  }
  return {
    limit: row["limit"],
    used: row["used"],
    resetsInSeconds: row["resetsInSeconds"],
  };
}

/** Human-readable rolling reset countdown for usage meters. */
export function formatResetIn(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return "now";
  }
  const total = Math.floor(seconds);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  if (hours > 0) {
    return minutes > 0 ? `in ${hours}h ${minutes}m` : `in ${hours}h`;
  }
  const mins = Math.max(1, Math.ceil(total / 60));
  return `in ${mins}m`;
}

/** Progress fraction for a limit meter, clamped to 0..1. */
export function usageFraction(used: number, limit: number): number {
  if (!Number.isFinite(used) || !Number.isFinite(limit) || limit <= 0) {
    return 0;
  }
  return Math.min(1, Math.max(0, used / limit));
}

export const ME_LIMIT_ROWS: { key: keyof MeLimits; label: string }[] = [
  { key: "uploadsPerDay", label: "Document uploads per day" },
  { key: "analysesPerDay", label: "Compliance analyses per day" },
  { key: "assistantAsksPerHour", label: "Assistant questions per hour" },
];
