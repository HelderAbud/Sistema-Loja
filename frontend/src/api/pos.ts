import { apiJson } from "./client";

export type PosPaymentMethod = "CASH" | "CARD" | "PIX";

export type CurrentCashSessionResponse = {
  open: boolean;
  cashSessionId: number | null;
  openingAmount: number | null;
  openedAt: string | null;
  expectedAmount: number;
  expectedCashAmount: number;
  expectedCardAmount: number;
  expectedPixAmount: number;
};

export type OpenCashSessionResponse = {
  cashSessionId: number;
  openingAmount: number;
  openedAt: string;
  status: "OPEN" | "CLOSED";
};

export type CloseCashSessionPreviewResponse = {
  cashSessionId: number;
  expectedAmount: number;
  expectedCashAmount: number;
  expectedCardAmount: number;
  expectedPixAmount: number;
  countedAmount: number | null;
  differenceAmount: number | null;
  toleranceAmount: number;
  managerApprovalRequired: boolean;
};

export type CloseCashSessionResponse = {
  cashSessionId: number;
  expectedAmount: number;
  countedAmount: number;
  differenceAmount: number;
  closedAt: string;
  status: "OPEN" | "CLOSED";
};

export type PosSalePaymentRequest = {
  paymentMethod: PosPaymentMethod;
  amount: number;
};

export type PosSaleFinalizeRequest = {
  cashSessionId: number;
  productId: number;
  quantity: number;
  unitPrice: number;
  unitCost?: number | null;
  payments: PosSalePaymentRequest[];
};

export type PosSaleFinalizeResponse = {
  saleId: number;
  cashSessionId: number;
  totalAmount: number;
  soldAt: string;
};

export async function openCashSession(body: {
  openingAmount: number;
}): Promise<OpenCashSessionResponse> {
  return apiJson<OpenCashSessionResponse>("/api/v1/lojapp/pos/cash-sessions/open", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function getCurrentCashSession(): Promise<CurrentCashSessionResponse> {
  return apiJson<CurrentCashSessionResponse>("/api/v1/lojapp/pos/cash-sessions/current");
}

export async function getCloseCashSessionPreview(
  cashSessionId: number,
  countedAmount?: number,
): Promise<CloseCashSessionPreviewResponse> {
  const q = new URLSearchParams();
  if (countedAmount != null) q.set("countedAmount", String(countedAmount));
  const suffix = q.toString() ? `?${q.toString()}` : "";
  return apiJson<CloseCashSessionPreviewResponse>(
    `/api/v1/lojapp/pos/cash-sessions/${cashSessionId}/close-preview${suffix}`,
  );
}

export async function closeCashSession(body: {
  cashSessionId: number;
  countedAmount: number;
  differenceReason?: string | null;
  managerApproval: boolean;
}): Promise<CloseCashSessionResponse> {
  return apiJson<CloseCashSessionResponse>("/api/v1/lojapp/pos/cash-sessions/close", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function finalizePosSale(
  body: PosSaleFinalizeRequest,
  idempotencyKey?: string,
): Promise<PosSaleFinalizeResponse> {
  const headers: Record<string, string> = {};
  if (idempotencyKey && idempotencyKey.trim()) {
    headers["Idempotency-Key"] = idempotencyKey.trim();
  }
  return apiJson<PosSaleFinalizeResponse>("/api/v1/lojapp/pos/sales/finalize", {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
}
