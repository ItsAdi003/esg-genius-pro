import { afterEach, describe, expect, it, vi } from "vitest";

import {
  DOCUMENT_POLLING_INTERVAL_MS,
  canModifyDocument,
  formatDocumentStatus,
  formatDocumentType,
  formatFileSize,
  formatInstant,
  formatReportingYear,
  isDocumentInFlight,
  isSharedDocument,
  resolveDocumentPollingInterval,
} from "@/lib/document-api";

describe("formatDocumentType", () => {
  it("returns label for known document types", () => {
    expect(formatDocumentType("SUSTAINABILITY_REPORT")).toBe("Sustainability Report");
  });

  it("replaces underscores for unknown types", () => {
    expect(formatDocumentType("CUSTOM_TYPE")).toBe("CUSTOM TYPE");
  });
});

describe("isDocumentInFlight", () => {
  it("is true for UPLOADED and PROCESSING", () => {
    expect(isDocumentInFlight("UPLOADED")).toBe(true);
    expect(isDocumentInFlight("PROCESSING")).toBe(true);
  });

  it("is false for terminal statuses and missing values", () => {
    expect(isDocumentInFlight("READY")).toBe(false);
    expect(isDocumentInFlight("FAILED")).toBe(false);
    expect(isDocumentInFlight(undefined)).toBe(false);
  });
});

describe("resolveDocumentPollingInterval", () => {
  it("polls while a document status is in flight", () => {
    expect(resolveDocumentPollingInterval("UPLOADED")).toBe(DOCUMENT_POLLING_INTERVAL_MS);
    expect(resolveDocumentPollingInterval("PROCESSING")).toBe(DOCUMENT_POLLING_INTERVAL_MS);
  });

  it("stops polling for terminal statuses or before status is known", () => {
    expect(resolveDocumentPollingInterval("READY")).toBe(false);
    expect(resolveDocumentPollingInterval("FAILED")).toBe(false);
    expect(resolveDocumentPollingInterval(undefined)).toBe(false);
  });

  it("polls the list while any document is in flight", () => {
    expect(
      resolveDocumentPollingInterval([
        { status: "READY" },
        { status: "PROCESSING" },
      ]),
    ).toBe(DOCUMENT_POLLING_INTERVAL_MS);
    expect(
      resolveDocumentPollingInterval([{ status: "UPLOADED" }]),
    ).toBe(DOCUMENT_POLLING_INTERVAL_MS);
  });

  it("does not poll an empty list or a list of only terminal statuses", () => {
    expect(resolveDocumentPollingInterval([])).toBe(false);
    expect(
      resolveDocumentPollingInterval([
        { status: "READY" },
        { status: "FAILED" },
      ]),
    ).toBe(false);
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

describe("isSharedDocument", () => {
  it("is true only when the backend flags the document as shared", () => {
    expect(isSharedDocument({ shared: true })).toBe(true);
    expect(isSharedDocument({ shared: false })).toBe(false);
  });

  it("treats a missing flag (older backend) as not shared", () => {
    expect(isSharedDocument({})).toBe(false);
  });
});

describe("canModifyDocument", () => {
  it("follows the backend flag", () => {
    expect(canModifyDocument({ canModify: true })).toBe(true);
    expect(canModifyDocument({ canModify: false })).toBe(false);
  });

  it("treats a missing flag (older backend) as modifiable so nothing regresses", () => {
    expect(canModifyDocument({})).toBe(true);
  });
});
