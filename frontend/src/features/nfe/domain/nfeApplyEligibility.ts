import type { NfeImportResponse } from "@/api";

/** Pode aplicar sugestões (botão ativo): há resultado com marca ou fornecedor e não há mutação em curso. */
export function isNfeApplySuggestionsReady(
  result: NfeImportResponse | null,
  applyPending: boolean,
): boolean {
  return (
    result != null &&
    (result.suggestedBrandId != null || result.supplierId != null) &&
    !applyPending
  );
}
