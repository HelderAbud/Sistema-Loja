import type { BrandDashboard } from "@/api";
import { money } from "../domain/chartFormat";

type Props = {
  data: BrandDashboard;
};

export function BrandTableSection({ data }: Props) {
  return (
    <>
      <div className="table-wrap">
        <h3 className="chart-title">Marcas (tabela)</h3>
        <p className="muted small" style={{ marginBottom: "0.5rem" }}>
          A mostrar {data.metrics.length} de {data.totalBrands} marcas (limite {data.brandLimit},
          offset {data.brandOffset}).
        </p>
        <table className="table">
          <thead>
            <tr>
              <th>Marca</th>
              <th>Lucro</th>
              <th>Faturamento</th>
              <th>Qtd</th>
              <th>Margem %</th>
              <th>Giro</th>
            </tr>
          </thead>
          <tbody>
            {data.metrics.map((m) => (
              <tr key={m.brand}>
                <td>{m.brand}</td>
                <td>{money(m.lucro)}</td>
                <td>{money(m.faturamento)}</td>
                <td>{m.quantidadeVendida}</td>
                <td>{m.margem}</td>
                <td>{m.giro}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {data.metrics[0] ? (
        <p className="hint muted small">Insight (top): {data.metrics[0].insight}</p>
      ) : null}
    </>
  );
}
