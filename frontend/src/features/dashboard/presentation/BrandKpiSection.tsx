import type { BrandDashboard, InventoryKpis, ProductAbcDashboard } from "@/api";
import { computeBrandKpiSummary } from "../domain/brandKpis";
import { money } from "../domain/chartFormat";
import {
  IconMargin,
  IconProfit,
  IconRevenue,
  IconStock,
  IconTicket,
  IconUnits,
  KpiTile,
} from "./DashboardUi";

type Props = {
  data: BrandDashboard;
  abc?: ProductAbcDashboard;
  inv?: InventoryKpis;
  inventoryPending: boolean;
};

export function BrandKpiSection({ data, abc, inv, inventoryPending }: Props) {
  const kpis = computeBrandKpiSummary(data, abc);
  return (
    <>
      <p className="period-pill muted small period-pill-enter">
        <span className="period-pill-inner">
          Período: <strong>{data.from}</strong> → <strong>{data.to}</strong>
        </span>
      </p>
      <div className="kpi-grid">
        <KpiTile
          label="Faturamento (marcas)"
          variant="accent"
          icon={<IconRevenue />}
          enterDelayMs={0}
          sub={kpis.abcRevenue > 0 ? <>ABC produtos: {money(kpis.abcRevenue)}</> : undefined}
        >
          {money(kpis.faturamento)}
        </KpiTile>
        <KpiTile label="Lucro (marcas)" variant="profit" icon={<IconProfit />} enterDelayMs={55}>
          {money(kpis.lucro)}
        </KpiTile>
        <KpiTile
          label="Ticket médio"
          icon={<IconTicket />}
          enterDelayMs={110}
          sub={<>por unidade vendida</>}
        >
          {money(kpis.ticketMedio)}
        </KpiTile>
        <KpiTile label="Unidades vendidas" icon={<IconUnits />} enterDelayMs={165}>
          {kpis.unitsSold.toLocaleString("pt-BR")}
        </KpiTile>
        <KpiTile label="Stock baixo / SKUs" variant="warn" icon={<IconStock />} enterDelayMs={220}>
          {inventoryPending ? (
            <span
              className="skeleton-block"
              style={{ display: "inline-block", minHeight: "1.35rem", width: "6.5rem" }}
              aria-hidden
            />
          ) : inv ? (
            `${inv.lowStockCount} / ${inv.totalSkus}`
          ) : (
            "—"
          )}
        </KpiTile>
        <KpiTile
          label="Margem média"
          icon={<IconMargin />}
          enterDelayMs={275}
          sub={<>lucro ÷ faturamento</>}
        >
          {kpis.margemMediaPct > 0 ? `${kpis.margemMediaPct.toFixed(1)} %` : "—"}
        </KpiTile>
      </div>
    </>
  );
}
