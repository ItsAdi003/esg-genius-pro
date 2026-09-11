import { apiUrl } from "@/lib/api-config";
import { formatEsgCategory } from "@/lib/compliance-api";

/** Backend FrameworkStatus enum — dev.esgenius.entity.FrameworkStatus */
export type FrameworkStatus = "ACTIVE" | "PLANNED";

/** Backend EsgCategory enum — dev.esgenius.entity.EsgCategory */
export type EsgCategory = "ENVIRONMENTAL" | "SOCIAL" | "GOVERNANCE";

export interface Framework {
  id: number;
  code: string;
  name: string;
  fullName: string;
  region: string;
  version: string;
  status: FrameworkStatus;
  requirementCount: number;
}

export interface FrameworkRequirement {
  id: number;
  requirementCode: string;
  title: string;
  category: EsgCategory;
  description: string;
  frameworkText: string;
  mandatory: boolean;
  version: string;
}

export class FrameworkApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "FrameworkApiError";
    this.status = status;
  }
}

export const frameworkQueryKeys = {
  all: ["frameworks"] as const,
  list: () => [...frameworkQueryKeys.all, "list"] as const,
  detail: (frameworkId: number) => [...frameworkQueryKeys.all, "detail", frameworkId] as const,
  requirements: (frameworkId: number, category?: EsgCategory | null) =>
    [...frameworkQueryKeys.all, "requirements", frameworkId, category ?? "all"] as const,
};

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
    throw new FrameworkApiError(message, response.status);
  }

  return response.json() as Promise<T>;
}

/**
 * GET /api/v1/frameworks
 */
export async function listFrameworks(): Promise<Framework[]> {
  const response = await fetch(apiUrl("/api/v1/frameworks"));
  return parseJsonResponse(response, "Failed to fetch frameworks");
}

/**
 * GET /api/v1/frameworks/{frameworkId}
 */
export async function getFramework(frameworkId: number): Promise<Framework> {
  const response = await fetch(apiUrl(`/api/v1/frameworks/${frameworkId}`));
  return parseJsonResponse(response, `Failed to fetch framework ${frameworkId}`);
}

/**
 * GET /api/v1/frameworks/{frameworkId}/requirements
 */
export async function getFrameworkRequirements(
  frameworkId: number,
  category?: EsgCategory | null,
): Promise<FrameworkRequirement[]> {
  const params = new URLSearchParams();
  if (category) {
    params.set("category", category);
  }
  const query = params.toString();
  const path = `/api/v1/frameworks/${frameworkId}/requirements${query ? `?${query}` : ""}`;
  const response = await fetch(apiUrl(path));
  return parseJsonResponse(response, `Failed to fetch requirements for framework ${frameworkId}`);
}

export function formatFrameworkStatus(status: FrameworkStatus | string): string {
  switch (status) {
    case "ACTIVE":
      return "Active";
    case "PLANNED":
      return "Planned";
    default:
      return status.replaceAll("_", " ");
  }
}

export function findFrameworkByCode(frameworks: Framework[], code: string): Framework | undefined {
  return frameworks.find((framework) => framework.code.toLowerCase() === code.toLowerCase());
}

export { formatEsgCategory };
