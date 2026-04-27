import { apiJson } from "./client";

export type Supplier = {
  id: number;
  legalName: string;
  taxId: string | null;
  createdAt: string;
  updatedAt: string;
};

export async function listSuppliers(): Promise<Supplier[]> {
  return apiJson<Supplier[]>("/api/v1/lojapp/suppliers");
}

export async function createSupplier(body: {
  legalName: string;
  taxId?: string | null;
}): Promise<Supplier> {
  return apiJson<Supplier>("/api/v1/lojapp/suppliers", {
    method: "POST",
    body: JSON.stringify(body),
  });
}
