import { describe, expect, it } from "vitest";
import { isInsufficientStock, isValidPositiveQuantity, parseDecimalInput } from "./saleFormParse";

describe("saleFormParse", () => {
  it("parseDecimalInput aceita vírgula", () => {
    expect(parseDecimalInput("1,5")).toBe(1.5);
  });

  it("isInsufficientStock", () => {
    expect(isInsufficientStock(true, 3, 5)).toBe(true);
    expect(isInsufficientStock(true, 10, 2)).toBe(false);
    expect(isInsufficientStock(false, 10, 99)).toBe(false);
  });

  it("isValidPositiveQuantity", () => {
    expect(isValidPositiveQuantity(0)).toBe(false);
    expect(isValidPositiveQuantity(0.5)).toBe(true);
  });
});
