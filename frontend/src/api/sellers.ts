import { apiJson } from "./client";

export type Seller = {
  id: number;
  displayName: string;
  active: boolean;
  sortOrder: number;
  createdAt: string;
};

export async function listSellers(): Promise<Seller[]> {
  return apiJson<Seller[]>("/api/v1/lojapp/sellers");
}
