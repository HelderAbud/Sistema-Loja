import { describe, expect, it } from "vitest";
import { computePreviousComparableRange, toDateInputValue } from "./dateRange";

describe("computePreviousComparableRange", () => {
  it("retorna null para intervalo inválido", () => {
    expect(computePreviousComparableRange("", "2024-01-02")).toBeNull();
    expect(computePreviousComparableRange("2024-01-10", "2024-01-01")).toBeNull();
  });

  it("calcula período anterior com mesma duração", () => {
    const from = toDateInputValue(new Date(2024, 0, 8));
    const to = toDateInputValue(new Date(2024, 0, 10));
    const prev = computePreviousComparableRange(from, to);
    expect(prev).not.toBeNull();
    expect(prev!.to).toBe(toDateInputValue(new Date(2024, 0, 7)));
    expect(prev!.from).toBe(toDateInputValue(new Date(2024, 0, 5)));
  });
});
