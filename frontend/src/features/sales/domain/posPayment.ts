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

export type PaymentLineDraft = {
  id: string;
  method: PosPaymentMethod;
  amountRaw: string;
  receivedRaw: string;
  cardBrand: string;
  installmentsRaw: string;
  endToEndId: string;
  transactionId: string;
  pending: boolean;
};

export function newPaymentLine(
  id: string,
  method: PosPaymentMethod = "CASH",
  amountRaw = "",
): PaymentLineDraft {
  return {
    id,
    method,
    amountRaw,
    receivedRaw: "",
    cardBrand: "",
    installmentsRaw: "",
    endToEndId: "",
    transactionId: "",
    pending: false,
  };
}

export function lineSaleTotal(quantity: number, unitPrice: number): number {
  return quantity * unitPrice;
}

export function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

export function resolveLineAmount(
  amountRaw: string,
  saleTotal: number,
  lineCount: number,
): number | null {
  const trimmed = amountRaw.trim();
  if (!trimmed) {
    return lineCount === 1 ? roundMoney(saleTotal) : null;
  }
  const parsed = parseDecimalInput(trimmed);
  if (!Number.isFinite(parsed) || parsed < 0.01) {
    return null;
  }
  return roundMoney(parsed);
}

export function remainingFromFilledAmounts(saleTotal: number, lines: PaymentLineDraft[]): number {
  let allocated = 0;
  for (const line of lines) {
    const trimmed = line.amountRaw.trim();
    if (!trimmed) {
      continue;
    }
    const parsed = parseDecimalInput(trimmed);
    if (Number.isFinite(parsed) && parsed > 0) {
      allocated += roundMoney(parsed);
    }
  }
  return roundMoney(saleTotal - allocated);
}

export function remainingToPay(saleTotal: number, lines: PaymentLineDraft[]): number {
  const amounts = lines.map((line) => resolveLineAmount(line.amountRaw, saleTotal, lines.length));
  if (amounts.some((amount) => amount == null)) {
    return Number.NaN;
  }
  const allocated = amounts.reduce<number>((sum, amount) => sum + (amount ?? 0), 0);
  return roundMoney(saleTotal - allocated);
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
  transactionId?: string;
  pending?: boolean;
}): { payment: PosSalePaymentRequest } | { error: string } {
  if (input.method === "CASH" && input.pending) {
    return { error: "Pagamento em dinheiro não pode ficar pendente: o valor já está no caixa." };
  }
  const payment: PosSalePaymentRequest = {
    paymentMethod: input.method,
    amount: input.amount,
  };
  if (input.pending) {
    payment.settlementStatus = "PENDING";
  }

  const nsu = (input.transactionId ?? "").trim();
  if (nsu) {
    payment.transactionId = nsu;
  }

  if (input.method === "CASH") {
    const trimmed = input.receivedRaw.trim();
    if (trimmed) {
      const received = parseDecimalInput(trimmed);
      if (!Number.isFinite(received) || received < input.amount) {
        return { error: "Valor recebido tem de ser um número ≥ ao valor desta parcela." };
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

export function buildPosPayments(
  saleTotal: number,
  lines: PaymentLineDraft[],
): { payments: PosSalePaymentRequest[] } | { error: string } {
  if (lines.length === 0) {
    return { error: "Adicione pelo menos um meio de pagamento." };
  }
  const remaining = remainingToPay(saleTotal, lines);
  if (!Number.isFinite(remaining)) {
    return { error: "Informe o valor de cada parcela." };
  }
  if (remaining !== 0) {
    return { error: "A soma das parcelas tem de igualar o total da venda." };
  }

  const payments: PosSalePaymentRequest[] = [];
  for (const line of lines) {
    const amount = resolveLineAmount(line.amountRaw, saleTotal, lines.length);
    if (amount == null) {
      return { error: "Informe o valor de cada parcela." };
    }
    const built = buildSinglePosPayment({
      method: line.method,
      amount,
      receivedRaw: line.receivedRaw,
      cardBrand: line.cardBrand,
      installmentsRaw: line.installmentsRaw,
      endToEndId: line.endToEndId,
      transactionId: line.transactionId,
      pending: line.pending,
    });
    if ("error" in built) {
      return built;
    }
    payments.push(built.payment);
  }
  return { payments };
}
