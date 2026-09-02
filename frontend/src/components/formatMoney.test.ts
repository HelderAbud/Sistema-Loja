import { describe, expect, it } from "vitest";
import { formatMoneyBrl } from "./formatMoney";

describe("formatMoneyBrl", () => {
  it("mostra traço quando o custo é null", () => {
    expect(formatMoneyBrl(null)).toBe("—");
  });

  it("formata número em BRL", () => {
    expect(formatMoneyBrl(10)).toMatch(/10/);
  });
});
