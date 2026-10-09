import type { User } from "@supabase/supabase-js";
import { describe, expect, it } from "vitest";

import { getSessionDisplayName, getSessionInitials } from "@/lib/use-session-user";

function user(partial: Partial<User> & { email?: string }): User {
  return partial as User;
}

describe("getSessionDisplayName", () => {
  it("returns empty string when user is null", () => {
    expect(getSessionDisplayName(null)).toBe("");
  });

  it("prefers trimmed full_name from user_metadata", () => {
    expect(
      getSessionDisplayName(
        user({
          email: "ada@example.com",
          user_metadata: { full_name: "  Ada Lovelace  " },
        }),
      ),
    ).toBe("Ada Lovelace");
  });

  it("ignores empty or whitespace-only full_name", () => {
    expect(
      getSessionDisplayName(
        user({
          email: "ada@example.com",
          user_metadata: { full_name: "   " },
        }),
      ),
    ).toBe("ada");
  });

  it("uses email local part when full_name is missing", () => {
    expect(getSessionDisplayName(user({ email: "ada.lovelace@example.com" }))).toBe("ada.lovelace");
  });

  it("returns full email when local part is empty", () => {
    expect(getSessionDisplayName(user({ email: "@domain.com" }))).toBe("@domain.com");
  });

  it("returns empty string when email is missing and no full_name", () => {
    expect(getSessionDisplayName(user({}))).toBe("");
  });
});

describe("getSessionInitials", () => {
  it("uses first letters of first two words for multi-word names", () => {
    expect(getSessionInitials("Ada Lovelace")).toBe("AL");
  });

  it("handles extra internal whitespace", () => {
    expect(getSessionInitials("  Ada   Lovelace  ")).toBe("AL");
  });

  it("uses first two characters for a single word", () => {
    expect(getSessionInitials("ada")).toBe("AD");
  });

  it("returns empty string for whitespace-only input", () => {
    expect(getSessionInitials("   ")).toBe("");
  });
});
