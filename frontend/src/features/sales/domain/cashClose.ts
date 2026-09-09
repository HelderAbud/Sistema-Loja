import { parseDecimalInput } from "./saleFormParse";

export type CloseCashBody = {
  cashSessionId: number;
  countedAmount: number;
  differenceReason: string | null;
  managerApproval: boolean;
};

export function buildCloseCashBody(input: {
  cashSessionId: number;
  countedRaw: string;
  differenceReason: string;
  managerApproval: boolean;
}): { body: CloseCashBody } | { error: string } {
  const countedAmount = parseDecimalInput(input.countedRaw);
  if (!Number.isFinite(countedAmount) || countedAmount < 0) {
    return { error: "Valor contado no caixa inválido." };
  }
  const reason = input.differenceReason.trim();
  return {
    body: {
      cashSessionId: input.cashSessionId,
      countedAmount,
      differenceReason: reason === "" ? null : reason,
      managerApproval: input.managerApproval,
    },
  };
}
