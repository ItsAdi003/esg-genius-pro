import { afterEach, describe, expect, it, vi } from "vitest";

import {
  fetchMe,
  formatResetIn,
  MeApiError,
  parseMeProfile,
  usageFraction,
} from "@/lib/me-api";

vi.mock("@/lib/api-config", () => ({
  apiFetch: vi.fn(),
}));

import { apiFetch } from "@/lib/api-config";

const fullProfile = {
  email: "analyst@example.com",
  admin: true,
  limits: {
    uploadsPerDay: { limit: 10, used: 3, resetsInSeconds: 7200 },
    analysesPerDay: { limit: 5, used: 0, resetsInSeconds: 0 },
    assistantAsksPerHour: null,
  },
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.resetAllMocks();
});

describe("formatResetIn", () => {
  it("returns now for zero or negative seconds", () => {
    expect(formatResetIn(0)).toBe("now");
    expect(formatResetIn(-10)).toBe("now");
  });

  it("formats sub-hour windows in minutes", () => {
    expect(formatResetIn(300)).toBe("in 5m");
    expect(formatResetIn(59)).toBe("in 1m");
  });

  it("formats hour and minute parts", () => {
    expect(formatResetIn(11_520)).toBe("in 3h 12m");
    expect(formatResetIn(3600)).toBe("in 1h");
  });
});

describe("usageFraction", () => {
  it("returns used divided by limit clamped to 0..1", () => {
    expect(usageFraction(3, 10)).toBe(0.3);
    expect(usageFraction(10, 10)).toBe(1);
    expect(usageFraction(15, 10)).toBe(1);
  });

  it("returns 0 when limit is not positive", () => {
    expect(usageFraction(5, 0)).toBe(0);
  });
});

describe("parseMeProfile", () => {
  it("parses a well-formed payload", () => {
    expect(parseMeProfile(fullProfile)).toEqual(fullProfile);
  });

  it("defaults missing or malformed fields safely", () => {
    expect(parseMeProfile(null)).toEqual({
      email: null,
      admin: false,
      limits: {
        uploadsPerDay: null,
        analysesPerDay: null,
        assistantAsksPerHour: null,
      },
    });
    expect(
      parseMeProfile({
        email: 123,
        admin: "yes",
        limits: {
          uploadsPerDay: { limit: 1, used: 2 },
        },
      }),
    ).toEqual({
      email: null,
      admin: false,
      limits: {
        uploadsPerDay: null,
        analysesPerDay: null,
        assistantAsksPerHour: null,
      },
    });
  });
});

describe("fetchMe", () => {
  it("returns parsed profile on success", async () => {
    vi.mocked(apiFetch).mockResolvedValue(jsonResponse(fullProfile));
    await expect(fetchMe()).resolves.toEqual(fullProfile);
    expect(apiFetch).toHaveBeenCalledWith("/api/v1/me");
  });

  it("treats 404 (backend without the endpoint) as unavailable", async () => {
    vi.mocked(apiFetch).mockResolvedValue(jsonResponse({ message: "Not found" }, 404));
    await expect(fetchMe()).resolves.toBeNull();
  });

  it("surfaces the server message and status on other errors", async () => {
    vi.mocked(apiFetch).mockResolvedValue(jsonResponse({ message: "Boom" }, 500));
    const error = await fetchMe().catch((e: unknown) => e);
    expect(error).toBeInstanceOf(MeApiError);
    expect((error as MeApiError).message).toBe("Boom");
    expect((error as MeApiError).status).toBe(500);
  });
});
