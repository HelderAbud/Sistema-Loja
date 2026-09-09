import { describe, expect, it } from "vitest";
import {
  buildPosPayments,
  buildSinglePosPayment,
  cashChangePreview,
  lineSaleTotal,
  newPaymentLine,
  remainingToPay,
} from "./posPayment";

describe("posPayment", () => {
  it("calcula o total da linha", () => {
    expect(lineSaleTotal(2, 18.9)).toBeCloseTo(37.8);
  });

  it("mostra troco só em dinheiro com recebido preenchido", () => {
    expect(cashChangePreview("CASH", 80, "100")).toBe(20);
    expect(cashChangePreview("CASH", 80, "")).toBeNull();
    expect(cashChangePreview("PIX", 80, "100")).toBeNull();
  });

  it("recusa recebido menor que a parcela", () => {
    const result = buildSinglePosPayment({
      method: "CASH",
      amount: 80,
      receivedRaw: "50",
      cardBrand: "",
      installmentsRaw: "",
      endToEndId: "",
    });
    expect(result).toEqual({
      error: "Valor recebido tem de ser um número ≥ ao valor desta parcela.",
    });
  });

  it("uma parcela vazia cobre o total; split CASH+PIX tem de fechar", () => {
    const cashOnly = newPaymentLine("a", "CASH");
    expect(remainingToPay(18, [cashOnly])).toBe(0);
    expect(buildPosPayments(18, [cashOnly])).toEqual({
      payments: [{ paymentMethod: "CASH", amount: 18 }],
    });

    const split = [newPaymentLine("a", "CASH", "10"), newPaymentLine("b", "PIX", "8")];
    expect(remainingToPay(18, split)).toBe(0);
    expect(buildPosPayments(18, split)).toEqual({
      payments: [
        { paymentMethod: "CASH", amount: 10 },
        { paymentMethod: "PIX", amount: 8 },
      ],
    });
    expect(remainingToPay(18, [newPaymentLine("a", "CASH", "10")])).toBe(8);
  });

  it("monta PIX com endToEndId, NSU e crédito com parcelas", () => {
    expect(
      buildSinglePosPayment({
        method: "PIX",
        amount: 20,
        receivedRaw: "",
        cardBrand: "VISA",
        installmentsRaw: "3",
        endToEndId: "E2E1",
        transactionId: "TX-9",
      }),
    ).toEqual({
      payment: { paymentMethod: "PIX", amount: 20, endToEndId: "E2E1", transactionId: "TX-9" },
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

  it("marca PIX pendente e recusa dinheiro pendente", () => {
    expect(buildPosPayments(20, [{ ...newPaymentLine("p", "PIX", "20"), pending: true }])).toEqual({
      payments: [{ paymentMethod: "PIX", amount: 20, settlementStatus: "PENDING" }],
    });
    expect(
      buildSinglePosPayment({
        method: "CASH",
        amount: 10,
        receivedRaw: "",
        cardBrand: "",
        installmentsRaw: "",
        endToEndId: "",
        pending: true,
      }),
    ).toEqual({
      error: "Pagamento em dinheiro não pode ficar pendente: o valor já está no caixa.",
    });
  });
});
