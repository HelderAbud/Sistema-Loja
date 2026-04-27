import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { BrandDashboard } from "../../../api";
import { CHART_TOOLTIP, money } from "../domain/chartFormat";

type Props = {
  data: BrandDashboard;
};

export function BrandRevenueChart({ data }: Props) {
  const brandChartData = (data.metrics ?? []).map((m) => ({
    name: m.brand.length > 14 ? `${m.brand.slice(0, 12)}…` : m.brand,
    faturamento: Number(m.faturamento),
    lucro: Number(m.lucro),
  }));

  return (
    <div className="chart-row chart-row-single">
      <div
        className="chart-card chart-card-hero chart-surface-enter"
        style={{ animationDelay: "70ms" }}
      >
        <h3 className="chart-title">Faturamento e lucro por marca</h3>
        <p className="chart-subtitle muted small">
          Áreas: fat. (eixo esq.) · lucro (eixo dir.) — dados da API
        </p>
        <div className="chart-inner chart-inner-hero">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={brandChartData} margin={{ top: 12, right: 8, left: 0, bottom: 4 }}>
              <defs>
                <linearGradient id="gradFatArea" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="var(--chart-blue)" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="var(--chart-blue)" stopOpacity={0.02} />
                </linearGradient>
                <linearGradient id="gradLucArea" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="var(--chart-green)" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="var(--chart-green)" stopOpacity={0.02} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" vertical={false} />
              <XAxis
                dataKey="name"
                tick={{ fontSize: 11, fill: "var(--muted)" }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                yAxisId="L"
                tick={{ fontSize: 11, fill: "var(--muted)" }}
                axisLine={false}
                tickLine={false}
                tickFormatter={(v) => (Math.abs(v) >= 1000 ? `${(v / 1000).toFixed(1)}k` : `${v}`)}
              />
              <YAxis
                yAxisId="R"
                orientation="right"
                tick={{ fontSize: 11, fill: "var(--muted)" }}
                axisLine={false}
                tickLine={false}
                tickFormatter={(v) => (Math.abs(v) >= 1000 ? `${(v / 1000).toFixed(1)}k` : `${v}`)}
              />
              <Tooltip
                formatter={(v) => money(Number(v))}
                labelStyle={{ fontWeight: 600 }}
                {...CHART_TOOLTIP}
              />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Area
                yAxisId="L"
                type="monotone"
                dataKey="faturamento"
                name="Faturamento"
                stroke="var(--chart-blue)"
                strokeWidth={2}
                fill="url(#gradFatArea)"
              />
              <Area
                yAxisId="R"
                type="monotone"
                dataKey="lucro"
                name="Lucro"
                stroke="var(--chart-green)"
                strokeWidth={2}
                fill="url(#gradLucArea)"
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
