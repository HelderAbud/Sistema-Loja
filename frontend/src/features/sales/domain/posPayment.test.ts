import { describe, expect, it } from "vitest";
import { buildSinglePosPayment, cashChangePreview, lineSaleTotal } from "./posPayment";

describe("posPayment", () => {
  it("calcula o total da linha", () => {
    expect(lineSaleTotal(2, 18.9)).toBeCloseTo(37.8);
  });

  it("mostra troco só em dinheiro com recebido preenchido", () => {
    expect(cashChangePreview("CASH", 80, "100")).toBe(20);
    expect(cashChangePreview("CASH", 80, "")).toBeNull();
    expect(cashChangePreview("PIX", 80, "100")).toBeNull();
  });

  it("recusa recebido menor que o total", () => {
    const result = buildSinglePosPayment({
      method: "CASH",
      amount: 80,
      receivedRaw: "50",
      cardBrand: "",
      installmentsRaw: "",
      endToEndId: "",
    });
    expect(result).toEqual({ error: "Valor recebido tem de ser um número ≥ ao total da venda." });
  });

  it("monta PIX com endToEndId e crédito com parcelas", () => {
    expect(
      buildSinglePosPayment({
        method: "PIX",
        amount: 20,
        receivedRaw: "",
        cardBrand: "VISA",
        installmentsRaw: "3",
        endToEndId: "E2E1",
      }),
    ).toEqual({
      payment: { paymentMethod: "PIX", amount: 20, endToEndId: "E2E1" },
    });
    expect(
      buildSinglePosPayment({
        method: "CREDIT_CARD",
        amount: 30,
        receivedRaw: "999",
        cardBrand: "VISA",
        installmentsRaw: "3",
        endToEndId: "E2E1",
      }),
    ).toEqual({
      payment: {
        paymentMethod: "CREDIT_CARD",
        amount: 30,
        cardBrand: "VISA",
        installments: 3,
      },
    });
  });
});
