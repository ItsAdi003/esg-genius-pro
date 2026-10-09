import { apiFetch } from "@/lib/api-config";
import { formatFileSize, formatInstant } from "@/lib/document-api";

/** Backend ReportExportResponse — must match dev.esgenius.dto.ReportExportResponse */
export interface ReportExport {
  id: number;
  analysisId: number;
  documentId: number;
  documentName: string;
  frameworkCode: string;
  format: string;
  sizeBytes: number;
  generatedAt: string;
}

export class ReportApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ReportApiError";
    this.status = status;
  }
}

export const reportQueryKeys = {
  all: ["reports"] as const,
  exports: () => [...reportQueryKeys.all, "exports"] as const,
};

/**
 * GET /api/v1/reports — the caller's recent report exports, newest first.
 * An older backend without this endpoint answers 404; that is the same as "nothing exported yet".
 */
export async function listReportExports(): Promise<ReportExport[]> {
  const response = await apiFetch("/api/v1/reports");

  if (response.status === 404) {
    return [];
  }

  if (!response.ok) {
    let message = "Failed to fetch exported reports";
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore non-JSON error bodies
    }
    throw new ReportApiError(message, response.status);
  }

  return parseReportExports(await response.json());
}

/** Keep only well-formed rows so one bad record cannot break the whole table. */
export function parseReportExports(payload: unknown): ReportExport[] {
  if (!Array.isArray(payload)) {
    return [];
  }
  return payload.filter(isReportExport);
}

function isReportExport(value: unknown): value is ReportExport {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const row = value as Record<string, unknown>;
  return (
    typeof row["id"] === "number" &&
    typeof row["analysisId"] === "number" &&
    typeof row["documentId"] === "number" &&
    typeof row["documentName"] === "string" &&
    typeof row["frameworkCode"] === "string" &&
    typeof row["format"] === "string" &&
    typeof row["sizeBytes"] === "number" &&
    typeof row["generatedAt"] === "string"
  );
}

export function formatExportSize(bytes: number): string {
  return formatFileSize(bytes);
}

export function formatExportTime(iso: string): string {
  return formatInstant(iso);
}
