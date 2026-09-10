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

export type InventoryMovement = {
  id: number;
  movementType: string;
  quantity: number;
  source: string;
  sourceId: number | null;
  reason: string | null;
  createdAt: string;
};

export type InventoryMovementPage = {
  content: InventoryMovement[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};

export async function listProductMovements(
  productId: number,
  page = 0,
): Promise<InventoryMovementPage> {
  const q = new URLSearchParams();
  q.set("page", String(page));
  q.set("size", "20");
  return apiJson<InventoryMovementPage>(
    `/api/v1/lojapp/inventory/products/${productId}/movements?${q}`,
  );
}
