import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const DEFAULT_BASE = "http://localhost:8081";

async function loadApiConfig() {
  vi.resetModules();
  return import("@/lib/api-config");
}

describe("getApiBaseUrl", () => {
  beforeEach(() => {
    vi.unstubAllEnvs();
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it("uses default when VITE_API_BASE_URL is missing or blank", async () => {
    vi.stubEnv("VITE_API_BASE_URL", "");
    const { getApiBaseUrl } = await loadApiConfig();
    expect(getApiBaseUrl()).toBe(DEFAULT_BASE);

    vi.unstubAllEnvs();
    vi.resetModules();
    const { getApiBaseUrl: getDefault } = await loadApiConfig();
    expect(getDefault()).toBe(DEFAULT_BASE);
  });

  it("trims configured value and strips trailing slash", async () => {
    vi.stubEnv("VITE_API_BASE_URL", "  https://api.example.com/  ");
    const { getApiBaseUrl } = await loadApiConfig();
    expect(getApiBaseUrl()).toBe("https://api.example.com");
  });
});

describe("apiUrl", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it("joins base URL with normalized path", async () => {
    vi.stubEnv("VITE_API_BASE_URL", "https://api.example.com");
    const { apiUrl } = await loadApiConfig();
    expect(apiUrl("/api/v1/health")).toBe("https://api.example.com/api/v1/health");
    expect(apiUrl("api/v1/health")).toBe("https://api.example.com/api/v1/health");
  });
});
