import { apiJson, apiText } from "./client";

export type CommissionAccrual = {
  id: number;
  saleId: number;
  sellerId: number;
  sellerName: string;
  brandId: number | null;
  brandName: string | null;
  baseAmount: number;
  percent: number;
  amount: number;
  createdAt: string;
};

export async function listCommissionAccruals(params: {
  from?: string;
  to?: string;
}): Promise<CommissionAccrual[]> {
  const q = new URLSearchParams();
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  const suffix = q.toString() ? `?${q.toString()}` : "";
  return apiJson<CommissionAccrual[]>(`/api/v1/lojapp/commission-accruals${suffix}`);
}

export async function downloadCommissionAccrualsCsv(params: {
  from?: string;
  to?: string;
}): Promise<string> {
  const q = new URLSearchParams();
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  const suffix = q.toString() ? `?${q.toString()}` : "";
  return apiText(`/api/v1/lojapp/commission-accruals.csv${suffix}`);
}
