import { afterEach, describe, expect, it, vi } from "vitest";

import {
  formatDocumentStatus,
  formatDocumentType,
  formatFileSize,
  formatInstant,
  formatReportingYear,
} from "@/lib/document-api";

describe("formatDocumentType", () => {
  it("returns label for known document types", () => {
    expect(formatDocumentType("SUSTAINABILITY_REPORT")).toBe("Sustainability Report");
  });

  it("replaces underscores for unknown types", () => {
    expect(formatDocumentType("CUSTOM_TYPE")).toBe("CUSTOM TYPE");
  });
});

describe("formatDocumentStatus", () => {
  it("maps known statuses", () => {
    expect(formatDocumentStatus("UPLOADED")).toBe("Uploaded");
    expect(formatDocumentStatus("PROCESSING")).toBe("Processing");
    expect(formatDocumentStatus("READY")).toBe("Ready");
    expect(formatDocumentStatus("FAILED")).toBe("Failed");
  });

  it("returns unknown status unchanged", () => {
    expect(formatDocumentStatus("ARCHIVED")).toBe("ARCHIVED");
  });
});

describe("formatFileSize", () => {
  it("returns em dash for nullish values", () => {
    expect(formatFileSize(null)).toBe("—");
    expect(formatFileSize(undefined)).toBe("—");
  });

  it("formats bytes below 1 KiB", () => {
    expect(formatFileSize(0)).toBe("0 B");
    expect(formatFileSize(1023)).toBe("1023 B");
  });

  it("formats kilobytes below 1 MiB", () => {
    expect(formatFileSize(1024)).toBe("1.0 KB");
    expect(formatFileSize(1536)).toBe("1.5 KB");
  });

  it("formats megabytes", () => {
    expect(formatFileSize(1024 * 1024)).toBe("1.0 MB");
  });
});

describe("formatReportingYear", () => {
  it("returns em dash for nullish year", () => {
    expect(formatReportingYear(null)).toBe("—");
    expect(formatReportingYear(undefined)).toBe("—");
  });

  it("stringifies a reporting year", () => {
    expect(formatReportingYear(2024)).toBe("2024");
  });
});

describe("formatInstant", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("returns em dash for missing values", () => {
    expect(formatInstant(null)).toBe("—");
    expect(formatInstant(undefined)).toBe("—");
    expect(formatInstant("")).toBe("—");
  });

  it("delegates to Intl.DateTimeFormat for ISO strings", () => {
    const format = vi.fn(() => "formatted");
    vi.spyOn(Intl, "DateTimeFormat").mockImplementation(
      () => ({ format }) as unknown as Intl.DateTimeFormat,
    );

    expect(formatInstant("2024-06-01T12:00:00.000Z")).toBe("formatted");
    expect(Intl.DateTimeFormat).toHaveBeenCalledWith(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    });
    expect(format).toHaveBeenCalledWith(new Date("2024-06-01T12:00:00.000Z"));
  });
});
