import { apiFetch } from "@/lib/api-config";

export interface AssistantCitation {
  pageNumber: number | null;
  chunkIndex: number;
  snippet: string;
}

export interface AssistantAnswer {
  answer: string;
  citations: AssistantCitation[];
  grounded: boolean;
}

export class AssistantApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "AssistantApiError";
    this.status = status;
  }
}

/**
 * POST /api/v1/documents/{documentId}/assistant/ask
 * Stateless question against one uploaded document.
 */
export async function askDocumentAssistant(
  documentId: number,
  question: string,
): Promise<AssistantAnswer> {
  const response = await apiFetch(`/api/v1/documents/${documentId}/assistant/ask`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });

  if (!response.ok) {
    let message = "The assistant could not answer this question.";
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore non-JSON error bodies
    }
    throw new AssistantApiError(message, response.status);
  }

  return response.json() as Promise<AssistantAnswer>;
}
