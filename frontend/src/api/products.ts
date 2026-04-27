import { apiJson } from "./client";

export type Product = {
  id: number;
  name: string;
  brandName: string;
  ean: string | null;
  ncm: string | null;
  sku: string | null;
  costPrice: number;
  salePrice: number;
  minimumStock: number;
  /** Opcional: base multimarcas (API v9+). */
  supplierId?: number | null;
  productModelId?: number | null;
  variantColor?: string | null;
  variantSize?: string | null;
};

export type ProductPage = {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};

export type ListProductsParams = {
  page?: number;
  size?: number;
  brandId?: number;
  q?: string;
  lowStock?: boolean;
};

export async function listProducts(
  pageOrParams: number | ListProductsParams = 0,
  sizeLegacy = 50,
): Promise<ProductPage> {
  const params: ListProductsParams =
    typeof pageOrParams === "number" ? { page: pageOrParams, size: sizeLegacy } : pageOrParams;
  const q = new URLSearchParams();
  q.set("page", String(params.page ?? 0));
  q.set("size", String(params.size ?? 20));
  q.set("sort", "name,asc");
  if (params.brandId != null) q.set("brandId", String(params.brandId));
  if (params.q?.trim()) q.set("q", params.q.trim());
  if (params.lowStock) q.set("lowStock", "true");
  return apiJson<ProductPage>(`/api/v1/lojapp/products?${q}`);
}
