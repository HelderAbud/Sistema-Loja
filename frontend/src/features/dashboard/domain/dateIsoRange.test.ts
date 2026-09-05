import { describe, expect, it } from "vitest";
import {
  toIsoEndOfDay,
  toIsoStartOfDay,
  toSalesApiInstantRange,
} from "./dateIsoRange";

describe("dateIsoRange", () => {
  it("toIsoStartOfDay / toIsoEndOfDay devolvem Instant ISO, não YYYY-MM-DD", () => {
    const start = toIsoStartOfDay("2026-09-01");
    const end = toIsoEndOfDay("2026-09-01");
    expect(start).toMatch(/^\d{4}-\d{2}-\d{2}T.*Z$/);
    expect(end).toMatch(/^\d{4}-\d{2}-\d{2}T.*Z$/);
    expect(start).not.toBe("2026-09-01");
    expect(end).not.toBe("2026-09-01");
    expect(Date.parse(start!)).toBeLessThan(Date.parse(end!));
  });

  it("vazio ou inválido fica undefined", () => {
    expect(toIsoStartOfDay("")).toBeUndefined();
    expect(toIsoEndOfDay("nao-e-data")).toBeUndefined();
    expect(toSalesApiInstantRange("", "")).toEqual({ from: undefined, to: undefined });
  });

  it("toSalesApiInstantRange mapeia início e fim do dia", () => {
    expect(toSalesApiInstantRange("2026-09-01", "2026-09-30")).toEqual({
      from: toIsoStartOfDay("2026-09-01"),
      to: toIsoEndOfDay("2026-09-30"),
    });
  });
});
