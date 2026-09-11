import type { InventoryKpis } from "@/api";

type Props = {
  inv?: InventoryKpis;
};

export function InventoryKpiSection({ inv }: Props) {
  if (!inv) return null;
  const stockValue = Number(inv.totalStockValue ?? 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
  return (
    <p className="muted small inv-summary">
      Unidades em stock: <strong>{Number(inv.totalUnits).toLocaleString("pt-BR")}</strong> · SKUs
      com saldo &gt; 0: <strong>{inv.skusWithPositiveStock}</strong> · Valor ao custo:{" "}
      <strong>{stockValue}</strong>
    </p>
  );
}
