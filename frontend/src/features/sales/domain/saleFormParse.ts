export const SALE_SEARCH_DEBOUNCE_MS = 320;
export const SALE_MIN_QUERY_LEN = 1;

export function parseDecimalInput(raw: string): number {
  return Number(String(raw).replace(",", "."));
}

export function isValidPositiveQuantity(q: number): boolean {
  return Number.isFinite(q) && q > 0;
}

/** Quantidade pedida excede saldo (só avalia com produto escolhido e saldo conhecido). */
export function isInsufficientStock(
  hasSelectedProduct: boolean,
  stockQty: number | null,
  qtyNum: number,
): boolean {
  const qtyValid = isValidPositiveQuantity(qtyNum);
  return hasSelectedProduct && stockQty != null && qtyValid && qtyNum > stockQty;
}
