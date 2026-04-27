import { apiJson } from "./client";

export type ProductCollection = {
  id: number;
  brandId: number;
  brandName: string;
  name: string;
  createdAt: string;
  updatedAt: string;
};

export async function listProductCollections(brandId: number): Promise<ProductCollection[]> {
  return apiJson<ProductCollection[]>(
    `/api/v1/lojapp/product-collections?brandId=${encodeURIComponent(String(brandId))}`,
  );
}

export async function createProductCollection(body: {
  brandId: number;
  name: string;
}): Promise<ProductCollection> {
  return apiJson<ProductCollection>("/api/v1/lojapp/product-collections", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export type ProductModel = {
  id: number;
  brandId: number;
  brandName: string;
  collectionId: number | null;
  collectionName: string | null;
  name: string;
  createdAt: string;
  updatedAt: string;
};

export async function listProductModels(
  brandId: number,
  collectionId?: number | null,
): Promise<ProductModel[]> {
  const q = new URLSearchParams();
  q.set("brandId", String(brandId));
  if (collectionId != null) q.set("collectionId", String(collectionId));
  return apiJson<ProductModel[]>(`/api/v1/lojapp/product-models?${q}`);
}

export async function createProductModel(body: {
  brandId: number;
  collectionId?: number | null;
  name: string;
}): Promise<ProductModel> {
  return apiJson<ProductModel>("/api/v1/lojapp/product-models", {
    method: "POST",
    body: JSON.stringify(body),
  });
}
