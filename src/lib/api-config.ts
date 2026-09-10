const DEFAULT_API_BASE_URL = "http://localhost:8081";

/**
 * Base URL for the ESGenius Spring Boot API (no trailing slash).
 * Override with VITE_API_BASE_URL in .env / deployment config.
 */
export function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL;
  if (typeof configured === "string" && configured.trim().length > 0) {
    return configured.trim().replace(/\/$/, "");
  }
  return DEFAULT_API_BASE_URL;
}

export function apiUrl(path: string): string {
  const base = getApiBaseUrl();
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${base}${normalizedPath}`;
}
