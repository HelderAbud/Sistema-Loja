import { describe, expect, it } from "vitest";
import {
  canFinalizePosSale,
  canManageBackofficeCatalog,
  canViewCommissionReport,
  canViewFinancialBackoffice,
} from "./backofficeAccess";

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

describe("canViewCommissionReport", () => {
  it("permite USER e recusa CASHIER e REPRESENTATIVE", () => {
    expect(canViewCommissionReport("USER")).toBe(true);
    expect(canViewCommissionReport("MANAGER")).toBe(true);
    expect(canViewCommissionReport("CASHIER")).toBe(false);
    expect(canViewCommissionReport("REPRESENTATIVE")).toBe(false);
    expect(canViewCommissionReport(undefined)).toBe(false);
  });
});

describe("canViewFinancialBackoffice", () => {
  it("permite USER e recusa CASHIER e SELLER", () => {
    expect(canViewFinancialBackoffice("USER")).toBe(true);
    expect(canViewFinancialBackoffice("REPRESENTATIVE")).toBe(true);
    expect(canViewFinancialBackoffice("CASHIER")).toBe(false);
    expect(canViewFinancialBackoffice("SELLER")).toBe(false);
    expect(canViewFinancialBackoffice(undefined)).toBe(false);
  });
});
