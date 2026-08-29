import { FormEvent, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { downloadCommissionAccrualsCsv, listCommissionAccruals } from "@/api";
import { toIsoEndOfDay, toIsoStartOfDay } from "@/features/dashboard/domain/dateIsoRange";
import { queryKeys } from "@/queryKeys";

const money = (n: number) => n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export function PilotoCommissionTab() {
  const [draftFrom, setDraftFrom] = useState("");
  const [draftTo, setDraftTo] = useState("");
  const [applied, setApplied] = useState({ fromDay: "", toDay: "" });
  const [csvError, setCsvError] = useState<string | null>(null);
  const [csvBusy, setCsvBusy] = useState(false);

  const listArgs = useMemo(
    () => ({
      from: toIsoStartOfDay(applied.fromDay.trim()),
      to: toIsoEndOfDay((applied.toDay.trim() || applied.fromDay).trim()),
    }),
    [applied],
  );

  const accrualsQ = useQuery({
    queryKey: queryKeys.commissionAccruals(listArgs),
    queryFn: () => listCommissionAccruals(listArgs),
  });

  const rows = accrualsQ.data ?? [];
  const total = rows.reduce((sum, row) => sum + row.amount, 0);

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setApplied({ fromDay: draftFrom, toDay: draftTo });
  }

  async function onExportCsv() {
    setCsvError(null);
    setCsvBusy(true);
    try {
      const csv = await downloadCommissionAccrualsCsv(listArgs);
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = "commission-accruals.csv";
      a.click();
      URL.revokeObjectURL(href);
    } catch (error: unknown) {
      setCsvError(String(error));
    } finally {
      setCsvBusy(false);
    }
  }

  return (
    <section className="card">
      <div className="section-head">
        <h2>Comissões</h2>
      </div>
      <p className="muted small section-lead">
        Lançamentos no intervalo. Sem datas, a API usa os últimos 30 dias.
      </p>
      <form onSubmit={onSubmit} className="form">
        <label>
          De
          <input
            type="date"
            value={draftFrom}
            onChange={(event) => setDraftFrom(event.target.value)}
          />
        </label>
        <label>
          Até
          <input type="date" value={draftTo} onChange={(event) => setDraftTo(event.target.value)} />
        </label>
        <div className="row">
          <button type="submit" className="primary">
            Filtrar
          </button>
          <button
            type="button"
            className="ghost"
            disabled={csvBusy}
            onClick={() => void onExportCsv()}
          >
            {csvBusy ? "A exportar…" : "Exportar CSV"}
          </button>
        </div>
      </form>
      {csvError ? <p className="error">{csvError}</p> : null}
      {accrualsQ.error ? <p className="error">{String(accrualsQ.error)}</p> : null}
      <p className="muted small">Total no período: {money(total)}</p>
      {rows.length === 0 && !accrualsQ.isPending ? (
        <p className="muted">Nenhum lançamento neste intervalo.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Data</th>
              <th>Venda</th>
              <th>Vendedora</th>
              <th>Marca</th>
              <th>Base</th>
              <th>%</th>
              <th>Comissão</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{new Date(row.createdAt).toLocaleString("pt-BR")}</td>
                <td>#{row.saleId}</td>
                <td>{row.sellerName}</td>
                <td>{row.brandName ?? "—"}</td>
                <td>{money(row.baseAmount)}</td>
                <td>{row.percent}</td>
                <td>{money(row.amount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
