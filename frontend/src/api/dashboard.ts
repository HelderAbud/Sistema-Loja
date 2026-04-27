import { apiJson } from "./client";

export type BrandKpi = {
  brand: string;
  faturamento: number;
  lucro: number;
  quantidadeVendida: number;
  margem: number;
  giro: string;
  insight: string;
};

export type BrandDashboard = {
  from: string;
  to: string;
  metrics: BrandKpi[];
  totalBrands: number;
  brandLimit: number;
  brandOffset: number;
};

export async function dashboardBrands(
  from?: string,
  to?: string,
  brandLimit?: number,
  brandOffset?: number,
): Promise<BrandDashboard> {
  const q = new URLSearchParams();
  if (from) q.set("from", from);
  if (to) q.set("to", to);
  if (brandLimit != null) q.set("brandLimit", String(brandLimit));
  if (brandOffset != null) q.set("brandOffset", String(brandOffset));
  const qs = q.toString();
  return apiJson<BrandDashboard>(`/api/v1/lojapp/dashboard/brands${qs ? `?${qs}` : ""}`);
}

export type ProductAbcRow = {
  productId: number;
  productName: string;
  brandName: string;
  revenue: number;
  quantitySold: number;
  sharePercent: number;
  cumulativePercent: number;
  abcClass: string;
};

export type ProductAbcDashboard = {
  from: string;
  to: string;
  totalRevenue: number;
  rows: ProductAbcRow[];
};

export async function dashboardProductAbc(
  from?: string,
  to?: string,
): Promise<ProductAbcDashboard> {
  const q = new URLSearchParams();
  if (from) q.set("from", from);
  if (to) q.set("to", to);
  const qs = q.toString();
  return apiJson<ProductAbcDashboard>(`/api/v1/lojapp/dashboard/products-abc${qs ? `?${qs}` : ""}`);
}

export type InventoryKpis = {
  totalSkus: number;
  totalUnits: number;
  lowStockCount: number;
  skusWithPositiveStock: number;
};

export async function dashboardInventoryKpis(): Promise<InventoryKpis> {
  return apiJson<InventoryKpis>("/api/v1/lojapp/dashboard/inventory-kpis");
}
