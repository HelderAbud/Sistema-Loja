import { apiJson } from "./client";

export async function adjustStock(
  productId: number,
  quantity: number,
  reason: string,
): Promise<void> {
  await apiJson<void>("/api/v1/lojapp/inventory/adjust", {
    method: "POST",
    body: JSON.stringify({ productId, quantity, reason }),
  });
}

export type LowStockRow = {
  productId: number;
  productName: string;
  currentQuantity: number;
  minimumStock: number;
};

export async function listLowStock(): Promise<LowStockRow[]> {
  return apiJson<LowStockRow[]>("/api/v1/lojapp/inventory/low-stock");
}

export type ProductStock = { quantity: number };

export async function getProductStock(productId: number): Promise<ProductStock> {
  return apiJson<ProductStock>(`/api/v1/lojapp/inventory/products/${productId}/stock`);
}
