/**
 * Layout de placeholder alinhado ao dashboard executivo (KPIs + cartões de gráfico).
 */
export function DashboardSkeleton() {
  return (
    <div className="dashboard-skeleton" aria-hidden>
      <div className="kpi-grid">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="kpi-card skeleton-block" />
        ))}
      </div>
      <div className="chart-row chart-row-single">
        <div className="chart-card chart-card-hero skeleton-block tall" />
      </div>
      <div className="chart-row">
        <div className="chart-card skeleton-block tall" />
        <div className="chart-card skeleton-block tall" />
      </div>
      <div className="chart-row chart-row-single">
        <div className="chart-card skeleton-block tall" />
      </div>
    </div>
  );
}
