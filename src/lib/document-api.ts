import { apiUrl } from "@/lib/api-config";

/** Backend DocumentType enum values — must match dev.esgenius.entity.DocumentType */
export type DocumentType =
  | "BRSR"
  | "SUSTAINABILITY_REPORT"
  | "ANNUAL_REPORT"
  | "POLICY"
  | "OTHER";

/** Backend DocumentStatus enum values — must match dev.esgenius.entity.DocumentStatus */
export type DocumentStatus = "UPLOADED" | "PROCESSING" | "READY" | "FAILED";

export interface DocumentSummary {
  id: number;
  organizationId: number;
  organizationName: string;
  originalFilename: string;
  documentType: DocumentType;
  reportingYear: number | null;
  status: DocumentStatus;
  fileSize: number;
  pageCount: number | null;
  uploadedAt: string;
  processedAt: string | null;
}

export interface DocumentDetail extends DocumentSummary {
  extractedText: string | null;
  failureReason: string | null;
}

export interface UploadDocumentParams {
  file: File;
  organizationId: number;
  documentType: DocumentType;
  reportingYear: number;
}

export class DocumentApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "DocumentApiError";
    this.status = status;
  }
}

export const documentQueryKeys = {
  all: ["documents"] as const,
  list: (organizationId: number) => [...documentQueryKeys.all, "list", organizationId] as const,
  detail: (documentId: number) => [...documentQueryKeys.all, "detail", documentId] as const,
};

export const DOCUMENT_TYPE_OPTIONS: { value: DocumentType; label: string }[] = [
  { value: "BRSR", label: "BRSR" },
  { value: "SUSTAINABILITY_REPORT", label: "Sustainability Report" },
  { value: "ANNUAL_REPORT", label: "Annual Report" },
  { value: "POLICY", label: "Policy" },
  { value: "OTHER", label: "Other" },
];

export const REPORTING_YEAR_OPTIONS = [2026, 2025, 2024, 2023, 2022] as const;

async function parseJsonResponse<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (!response.ok) {
    let message = fallbackMessage;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore non-JSON error bodies
    }
    throw new DocumentApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

/**
 * GET /api/v1/documents?organizationId={id}
 */
export async function listDocuments(organizationId: number): Promise<DocumentSummary[]> {
  const params = new URLSearchParams({ organizationId: String(organizationId) });
  const response = await fetch(apiUrl(`/api/v1/documents?${params.toString()}`));
  return parseJsonResponse(response, "Failed to fetch documents");
}

/**
 * GET /api/v1/documents/{id}
 */
export async function getDocument(documentId: number): Promise<DocumentDetail> {
  const response = await fetch(apiUrl(`/api/v1/documents/${documentId}`));
  return parseJsonResponse(response, `Failed to fetch document ${documentId}`);
}

/**
 * POST /api/v1/documents
 */
export async function uploadDocument(params: UploadDocumentParams): Promise<DocumentDetail> {
  const formData = new FormData();
  formData.append("file", params.file);
  formData.append("organizationId", String(params.organizationId));
  formData.append("documentType", params.documentType);
  formData.append("reportingYear", String(params.reportingYear));

  const response = await fetch(apiUrl("/api/v1/documents"), {
    method: "POST",
    body: formData,
  });

  return parseJsonResponse(response, "Failed to upload document");
}

/**
 * DELETE /api/v1/documents/{id}
 */
export async function deleteDocument(documentId: number): Promise<void> {
  const response = await fetch(apiUrl(`/api/v1/documents/${documentId}`), {
    method: "DELETE",
  });
  await parseJsonResponse<void>(response, `Failed to delete document ${documentId}`);
}

export function formatDocumentType(documentType: DocumentType | string): string {
  const match = DOCUMENT_TYPE_OPTIONS.find((option) => option.value === documentType);
  return match?.label ?? documentType.replaceAll("_", " ");
}

export function formatDocumentStatus(status: DocumentStatus | string): string {
  switch (status) {
    case "UPLOADED":
      return "Uploaded";
    case "PROCESSING":
      return "Processing";
    case "READY":
      return "Ready";
    case "FAILED":
      return "Failed";
    default:
      return status;
  }
}

export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes == null) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function formatReportingYear(year: number | null | undefined): string {
  if (year == null) return "—";
  return String(year);
}

export function formatInstant(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso));
}
