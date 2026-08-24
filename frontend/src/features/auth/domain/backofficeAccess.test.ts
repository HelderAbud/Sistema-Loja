import { describe, expect, it } from "vitest";
import { canFinalizePosSale, canManageBackofficeCatalog } from "./backofficeAccess";

describe("canManageBackofficeCatalog", () => {
  it("permite USER e recusa CASHIER", () => {
    expect(canManageBackofficeCatalog("USER")).toBe(true);
    expect(canManageBackofficeCatalog("CASHIER")).toBe(false);
    expect(canManageBackofficeCatalog("SELLER")).toBe(false);
    expect(canManageBackofficeCatalog(undefined)).toBe(false);
  });
});

describe("canFinalizePosSale", () => {
  it("permite CASHIER e recusa REPRESENTATIVE", () => {
    expect(canFinalizePosSale("CASHIER")).toBe(true);
    expect(canFinalizePosSale("REPRESENTATIVE")).toBe(false);
    expect(canFinalizePosSale(undefined)).toBe(false);
  });
});
