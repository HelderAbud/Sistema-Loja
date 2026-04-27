import { describe, expect, it } from "vitest";
import { validateManualStockAdjust } from "./manualAdjust";

describe("validateManualStockAdjust", () => {
  it("aceita entrada válida", () => {
    const r = validateManualStockAdjust("5", "10", " INVENTARIO ");
    expect(r.ok).toBe(true);
    if (r.ok) {
      expect(r.value).toEqual({ productId: 5, qty: 10, reason: "INVENTARIO" });
    }
  });

  it("rejeita quantidade zero", () => {
    const r = validateManualStockAdjust("1", "0", "x");
    expect(r.ok).toBe(false);
  });
});
