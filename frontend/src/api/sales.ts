import { apiJson } from "./client";

export type SaleCreatedResponse = {
  id: number;
  productId: number;
  quantity: number;
  unitPrice: number;
  unitCost: number;
  soldAt: string;
};

export async function registerSale(body: {
  productId: number;
  quantity: number;
  unitPrice: number;
  unitCost?: number | null;
}): Promise<SaleCreatedResponse> {
  const payload: Record<string, unknown> = {
    productId: body.productId,
    quantity: body.quantity,
    unitPrice: body.unitPrice,
  };
  if (body.unitCost != null) {
    payload.unitCost = body.unitCost;
  }
  return apiJson<SaleCreatedResponse>("/api/v1/lojapp/sales", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export type SaleRow = {
  id: number;
  productId: number;
  productName: string;
  brandName: string;
  quantity: number;
  unitPrice: number;
  unitCost: number;
  soldAt: string;
  cancelled: boolean;
};

export type SalePage = {
  content: SaleRow[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};

export async function listSales(params: {
  page?: number;
  size?: number;
  from?: string;
  to?: string;
  productId?: number;
  brandId?: number;
}): Promise<SalePage> {
  const q = new URLSearchParams();
  q.set("page", String(params.page ?? 0));
  q.set("size", String(params.size ?? 20));
  q.set("sort", "soldAt,desc");
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  if (params.productId != null) q.set("productId", String(params.productId));
  if (params.brandId != null) q.set("brandId", String(params.brandId));
  return apiJson<SalePage>(`/api/v1/lojapp/sales?${q}`);
}

export async function cancelSale(saleId: number): Promise<void> {
  await apiJson<void>(`/api/v1/lojapp/sales/${saleId}/cancel`, {
    method: "POST",
  });
}

export type SalesSummary = {
  revenue: number;
  unitsSold: number;
  averageTicket: number;
};

export type SalesDailyPoint = {
  date: string;
  revenue: number;
  unitsSold: number;
};

export async function summarizeSales(params: {
  from?: string;
  to?: string;
  productId?: number;
  brandId?: number;
}): Promise<SalesSummary> {
  const q = new URLSearchParams();
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  if (params.productId != null) q.set("productId", String(params.productId));
  if (params.brandId != null) q.set("brandId", String(params.brandId));
  return apiJson<SalesSummary>(`/api/v1/lojapp/sales/summary?${q}`);
}

export async function summarizeSalesDaily(params: {
  from?: string;
  to?: string;
  productId?: number;
  brandId?: number;
}): Promise<SalesDailyPoint[]> {
  const q = new URLSearchParams();
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  if (params.productId != null) q.set("productId", String(params.productId));
  if (params.brandId != null) q.set("brandId", String(params.brandId));
  return apiJson<SalesDailyPoint[]>(`/api/v1/lojapp/sales/daily?${q}`);
}
