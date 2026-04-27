import { DashboardSkeleton } from "../../../components/ui/DashboardSkeleton";
import { useDashboardData } from "../application/useDashboardData";
import { useDashboardFilters } from "../application/useDashboardFilters";
import { DashboardContent } from "./DashboardContent";
import { DashboardFilters } from "./DashboardFilters";

export function PilotoDashboardTab() {
  const { fromDay, toDay, applied, setFromDay, setToDay, onSubmit, loadDefault } =
    useDashboardFilters();
  const { brandsQ, abcQ, invQ, fetchingDash } = useDashboardData(applied);

  const data = brandsQ.data;
  const busy = brandsQ.isPending || abcQ.isPending;

  return (
    <section
      className={`card dashboard-root${data && fetchingDash ? " dashboard-root--sync" : ""}`}
    >
      <div className="dashboard-hero">
        <div className="dashboard-hero-top">
          <h2>Dashboard executivo</h2>
          <span className="dashboard-badge">Tempo real · API</span>
        </div>
        <p className="muted small dashboard-hero-lead">
          KPIs por marca, ranking de produtos, curva ABC e Pareto — mais inventário agregado. Sem
          datas, o intervalo é os últimos 30 dias; os valores refletem a API em tempo real.
        </p>
      </div>
      {data && fetchingDash ? (
        <div className="sync-strip" role="status" aria-live="polite">
          A sincronizar indicadores…
        </div>
      ) : null}
      <DashboardFilters
        fromDay={fromDay}
        toDay={toDay}
        setFromDay={setFromDay}
        setToDay={setToDay}
        busy={busy}
        onSubmit={onSubmit}
        onLoadDefault={loadDefault}
      />

      {busy && !data ? <DashboardSkeleton /> : null}

      {data ? (
        <DashboardContent
          data={data}
          abc={abcQ.data}
          inv={invQ.data}
          inventoryPending={invQ.isPending}
        />
      ) : null}
    </section>
  );
}
