import { describe, expect, it } from "vitest";
import { saleListUnitAmount } from "./saleListDisplay";

describe("saleListUnitAmount", () => {
  it("devolve o unitário numa venda de uma linha", () => {
    expect(saleListUnitAmount(1, 10.5)).toBe(10.5);
    expect(saleListUnitAmount(undefined, 8)).toBe(8);
  });

  it("esconde o unitário da 1.ª linha quando há várias linhas", () => {
    expect(saleListUnitAmount(2, 10.5)).toBeNull();
    expect(saleListUnitAmount(3, 1)).toBeNull();
  });
});
