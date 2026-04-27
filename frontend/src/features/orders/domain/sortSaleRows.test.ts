import { describe, expect, it } from "vitest";
import { sortSaleRows } from "./sortSaleRows";

const rows = [
  { id: 1, quantity: 2, unitPrice: 10, soldAt: "2024-01-02T10:00:00Z" },
  { id: 2, quantity: 1, unitPrice: 50, soldAt: "2024-01-01T10:00:00Z" },
  { id: 3, quantity: 3, unitPrice: 5, soldAt: "2024-01-03T10:00:00Z" },
];

describe("sortSaleRows", () => {
  it("ordena por data descendente", () => {
    const sorted = sortSaleRows(rows, "soldAt", "desc");
    expect(sorted.map((r) => r.id)).toEqual([3, 1, 2]);
  });

  it("ordena por total ascendente", () => {
    const sorted = sortSaleRows(rows, "total", "asc");
    expect(sorted.map((r) => r.id)).toEqual([3, 1, 2]);
  });

  it("ordena por quantidade descendente", () => {
    const sorted = sortSaleRows(rows, "quantity", "desc");
    expect(sorted.map((r) => r.id)).toEqual([3, 1, 2]);
  });
});
