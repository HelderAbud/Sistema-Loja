/** Formata BRL; `null`/`undefined` (custo omitido a CASHIER/SELLER) vira traço. */
export function formatMoneyBrl(n: number | null | undefined): string {
  if (n == null || Number.isNaN(Number(n))) {
    return "—";
  }
  return Number(n).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}
