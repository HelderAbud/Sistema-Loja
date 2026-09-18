/**
 * Preço/custo unitário na listagem: só faz sentido numa venda de 1 linha.
 * Com várias linhas o header/1.ª linha mentiria; o total vem de `lineTotal`.
 */
export function saleListUnitAmount(itemCount: number | undefined, unit: number): number | null {
  if ((itemCount ?? 1) > 1) {
    return null;
  }
  return unit;
}
