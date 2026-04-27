import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { ProductAbcDashboard } from "../../../api";
import { ABC_COLORS, CHART_TOOLTIP, money } from "../domain/chartFormat";

type Props = {
  abc?: ProductAbcDashboard;
};

export function ProductAbcSection({ abc }: Props) {
  const topProducts = (abc?.rows ?? []).slice(0, 8);
  const abcPieData = (() => {
    const counts = { A: 0, B: 0, C: 0 };
    for (const r of abc?.rows ?? []) {
      const c = r.abcClass as keyof typeof counts;
      if (c in counts) counts[c]++;
    }
    return (Object.entries(counts) as [string, number][])
      .filter(([, v]) => v > 0)
      .map(([name, value]) => ({ name: `Classe ${name}`, value }));
  })();
  const paretoData = (abc?.rows ?? []).map((r, i) => ({
    rank: i + 1,
    cumul: Number(r.cumulativePercent),
  }));

  return (
    <>
      <div className="chart-row">
        <div className="chart-card chart-surface-enter" style={{ animationDelay: "130ms" }}>
          <h3 className="chart-title">Top produtos (faturamento)</h3>
          <div className="chart-inner">
            {topProducts.length === 0 ? (
              <p className="muted small chart-empty">Sem vendas por produto neste período.</p>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={topProducts.map((r) => ({
                    name:
                      r.productName.length > 12 ? `${r.productName.slice(0, 10)}…` : r.productName,
                    revenue: Number(r.revenue),
                  }))}
                  layout="vertical"
                  margin={{ top: 8, right: 16, left: 8, bottom: 0 }}
                >
                  <defs>
                    <linearGradient id="gradTopSku" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stopColor="#a78bfa" />
                      <stop offset="100%" stopColor="#5b21b6" />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" horizontal />
                  <XAxis
                    type="number"
                    tick={{ fontSize: 11, fill: "var(--muted)" }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    type="category"
                    dataKey="name"
                    width={108}
                    tick={{ fontSize: 10, fill: "var(--muted)" }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip formatter={(v) => money(Number(v))} {...CHART_TOOLTIP} />
                  <Bar
                    dataKey="revenue"
                    name="Faturamento"
                    fill="url(#gradTopSku)"
                    radius={[0, 6, 6, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
        <div className="chart-card chart-surface-enter" style={{ animationDelay: "190ms" }}>
          <h3 className="chart-title">Curva ABC (quantidade de SKUs)</h3>
          <div className="chart-inner">
            {abcPieData.length === 0 ? (
              <p className="muted small chart-empty">Sem dados ABC (sem vendas no período).</p>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={abcPieData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius="42%"
                    outerRadius="72%"
                    paddingAngle={2}
                  >
                    {abcPieData.map((entry) => (
                      <Cell
                        key={`cell-${entry.name}`}
                        fill={ABC_COLORS[entry.name.replace("Classe ", "")] ?? "#94a3b8"}
                      />
                    ))}
                  </Pie>
                  <Tooltip {...CHART_TOOLTIP} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>
      {paretoData.length > 0 ? (
        <div className="chart-row chart-row-single">
          <div
            className="chart-card chart-card-wide chart-surface-enter"
            style={{ animationDelay: "250ms" }}
          >
            <h3 className="chart-title">Pareto — % cumulativo de faturamento (ABC)</h3>
            <div className="chart-inner chart-inner-pareto">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={paretoData} margin={{ top: 8, right: 16, left: 0, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" />
                  <XAxis
                    dataKey="rank"
                    tick={{ fontSize: 11, fill: "var(--muted)" }}
                    label={{
                      value: "Rank produto",
                      position: "insideBottom",
                      offset: -12,
                      fontSize: 10,
                      fill: "var(--muted)",
                    }}
                  />
                  <YAxis
                    domain={[0, 100]}
                    tick={{ fontSize: 11, fill: "var(--muted)" }}
                    tickFormatter={(v) => `${v}%`}
                  />
                  <Tooltip
                    formatter={(value) => [`${Number(value ?? 0).toFixed(1)}%`, "Cumulativo"]}
                    labelFormatter={(rank) => `Rank #${rank}`}
                    {...CHART_TOOLTIP}
                  />
                  <ReferenceLine
                    y={80}
                    stroke="var(--muted)"
                    strokeDasharray="4 4"
                    label={{ value: "80%", fontSize: 10, fill: "var(--muted)" }}
                  />
                  <Line
                    type="monotone"
                    dataKey="cumul"
                    name="cumul"
                    stroke="var(--chart-blue)"
                    strokeWidth={2.5}
                    dot={false}
                    activeDot={{ r: 5, strokeWidth: 0 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      ) : null}
      {abc && abc.rows.length > 0 ? (
        <div className="table-wrap">
          <h3 className="chart-title">Detalhe ABC</h3>
          <table className="table">
            <thead>
              <tr>
                <th>Produto</th>
                <th>Marca</th>
                <th>Fat.</th>
                <th>%</th>
                <th>Cumul.</th>
                <th>ABC</th>
              </tr>
            </thead>
            <tbody>
              {abc.rows.map((r) => (
                <tr key={r.productId}>
                  <td>{r.productName}</td>
                  <td>{r.brandName}</td>
                  <td>{money(Number(r.revenue))}</td>
                  <td>{Number(r.sharePercent).toFixed(1)}%</td>
                  <td>{Number(r.cumulativePercent).toFixed(1)}%</td>
                  <td>
                    <span className={`abc-pill abc-${r.abcClass.toLowerCase()}`}>{r.abcClass}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </>
  );
}
