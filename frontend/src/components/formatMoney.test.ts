import { describe, expect, it } from "vitest";
import { formatMoneyBrl } from "./formatMoney";

describe("formatMoneyBrl", () => {
  it("mostra traço quando o custo é null", () => {
    expect(formatMoneyBrl(null)).toBe("—");
  });

  it("formata número em BRL", () => {
    const formatted = formatMoneyBrl(10);
    expect(formatted).toMatch(/10/);
    expect(formatted).toMatch(/R\$/);
    expect(formatted).not.toMatch(/€/);
  });
});
