import type { InventoryKpis } from "../../../api";

type Props = {
  inv?: InventoryKpis;
};

export function InventoryKpiSection({ inv }: Props) {
  if (!inv) return null;
  return (
    <p className="muted small inv-summary">
      Unidades em stock: <strong>{Number(inv.totalUnits).toLocaleString("pt-BR")}</strong> · SKUs
      com saldo &gt; 0: <strong>{inv.skusWithPositiveStock}</strong>
    </p>
  );
}
