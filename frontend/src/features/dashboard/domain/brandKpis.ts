import type { BrandDashboard, ProductAbcDashboard } from "../../../api";

export type BrandKpiSummary = {
  faturamento: number;
  lucro: number;
  abcRevenue: number;
  unitsSold: number;
  ticketMedio: number;
  margemMediaPct: number;
};

export function computeBrandKpiSummary(
  data: BrandDashboard,
  abc: ProductAbcDashboard | undefined,
): BrandKpiSummary {
  const metrics = data.metrics ?? [];
  const faturamento = metrics.reduce((s, m) => s + Number(m.faturamento), 0);
  const lucro = metrics.reduce((s, m) => s + Number(m.lucro), 0);
  const unitsSold = metrics.reduce((s, m) => s + Number(m.quantidadeVendida), 0);
  const ticketMedio = unitsSold > 0 ? faturamento / unitsSold : 0;
  const margemMediaPct = faturamento > 0 ? (lucro / faturamento) * 100 : 0;
  const abcRevenue = abc ? Number(abc.totalRevenue) : 0;
  return { faturamento, lucro, abcRevenue, unitsSold, ticketMedio, margemMediaPct };
}
