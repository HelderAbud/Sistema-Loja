import { apiJson } from "./client";

export type NfeImportResponse = {
  nfeEntryId: number;
  nfeNumber: string;
  importedItems: number;
  /** Presente quando o XML traz CNPJ/CPF do emitente. */
  supplierId?: number | null;
  /** Heurística (substring); confirmar antes de aplicar ao catálogo. */
  suggestedBrandId?: number | null;
  suggestedBrandName?: string | null;
  /** Produtos criados na importação com preço de venda = custo (rever no catálogo). */
  productsCreatedWithoutSalePrice: number;
};

export async function importNfe(rawXml: string): Promise<NfeImportResponse> {
  return apiJson<NfeImportResponse>("/api/v1/lojapp/nfe/import", {
    method: "POST",
    body: JSON.stringify({ rawXml }),
  });
}

export type NfeApplySuggestionsResponse = {
  nfeLineCount: number;
  brandAssignedCount: number;
  supplierAssignedCount: number;
  brandSkippedModelConflictCount: number;
  appliedBrandId?: number | null;
  appliedBrandName?: string | null;
  supplierIdFromEntry?: number | null;
};

/** Objeto vazio no corpo = defeito da API (marca e fornecedor quando aplicável). */
export async function applyNfeImportSuggestions(
  nfeEntryId: number,
  body?: {
    setBrandOnImportedProducts?: boolean | null;
    setSupplierOnImportedProducts?: boolean | null;
  },
): Promise<NfeApplySuggestionsResponse> {
  return apiJson<NfeApplySuggestionsResponse>(
    `/api/v1/lojapp/nfe/entries/${nfeEntryId}/apply-suggestions`,
    {
      method: "POST",
      body: JSON.stringify(body ?? {}),
    },
  );
}
