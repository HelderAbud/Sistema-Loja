import { apiJson } from "./client";

export type Brand = { id: number; name: string };

export async function listBrands(): Promise<Brand[]> {
  return apiJson<Brand[]>("/api/v1/lojapp/brands");
}

export async function createBrand(name: string): Promise<Brand> {
  return apiJson<Brand>("/api/v1/lojapp/brands", {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}
