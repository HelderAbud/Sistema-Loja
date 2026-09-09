import type { PosPaymentMethod, PosSalePaymentRequest } from "@/api";
import { parseDecimalInput } from "./saleFormParse";

export const POS_PAYMENT_METHOD_OPTIONS: { value: PosPaymentMethod; label: string }[] = [
  { value: "CASH", label: "Dinheiro" },
  { value: "PIX", label: "PIX" },
  { value: "CREDIT_CARD", label: "Cartão de crédito" },
  { value: "DEBIT_CARD", label: "Cartão de débito" },
  { value: "CARD", label: "Cartão (legado)" },
  { value: "BANK_TRANSFER", label: "Transferência" },
  { value: "BANK_SLIP", label: "Boleto" },
  { value: "OTHER", label: "Outro" },
];

export function lineSaleTotal(quantity: number, unitPrice: number): number {
  return quantity * unitPrice;
}

export function cashChangePreview(
  method: PosPaymentMethod,
  amount: number,
  receivedRaw: string,
): number | null {
  if (method !== "CASH") {
    return null;
  }
  const trimmed = receivedRaw.trim();
  if (!trimmed) {
    return null;
  }
  const received = parseDecimalInput(trimmed);
  if (!Number.isFinite(received)) {
    return null;
  }
  return received - amount;
}

export function buildSinglePosPayment(input: {
  method: PosPaymentMethod;
  amount: number;
  receivedRaw: string;
  cardBrand: string;
  installmentsRaw: string;
  endToEndId: string;
}): { payment: PosSalePaymentRequest } | { error: string } {
  const payment: PosSalePaymentRequest = {
    paymentMethod: input.method,
    amount: input.amount,
  };

  if (input.method === "CASH") {
    const trimmed = input.receivedRaw.trim();
    if (trimmed) {
      const received = parseDecimalInput(trimmed);
      if (!Number.isFinite(received) || received < input.amount) {
        return { error: "Valor recebido tem de ser um número ≥ ao total da venda." };
      }
      payment.receivedAmount = received;
    }
  }

  if (input.method === "CARD" || input.method === "CREDIT_CARD" || input.method === "DEBIT_CARD") {
    const brand = input.cardBrand.trim();
    if (brand) {
      payment.cardBrand = brand;
    }
  }

  if (input.method === "CARD" || input.method === "CREDIT_CARD") {
    const trimmed = input.installmentsRaw.trim();
    if (trimmed) {
      const installments = Number.parseInt(trimmed, 10);
      if (!Number.isInteger(installments) || installments < 1) {
        return { error: "Parcelas inválidas." };
      }
      payment.installments = installments;
    }
  }

  if (input.method === "PIX") {
    const e2e = input.endToEndId.trim();
    if (e2e) {
      payment.endToEndId = e2e;
    }
  }

  return { payment };
}
