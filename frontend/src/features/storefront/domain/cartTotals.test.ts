import { describe, expect, it } from "vitest";
import { calculateCartSubtotal, calculateCartTotal } from "./cartTotals";

describe("cart totals (domain)", () => {
  it("calcula subtotal corretamente com múltiplos itens", () => {
    const subtotal = calculateCartSubtotal([
      { id: "1", quantity: 2, price: 100 },
      { id: "2", quantity: 1, price: 50.5 },
    ]);
    expect(subtotal).toBe(250.5);
  });

  it("aplica envio quando o subtotal é menor que 500", () => {
    const total = calculateCartTotal([{ id: "1", quantity: 1, price: 100 }]);
    expect(total.shipping).toBe(24.9);
    expect(total.total).toBe(124.9);
  });

  it("não aplica envio para carrinho acima de 500", () => {
    const total = calculateCartTotal([{ id: "1", quantity: 2, price: 260 }]);
    expect(total.shipping).toBe(0);
    expect(total.total).toBe(520);
  });
});
