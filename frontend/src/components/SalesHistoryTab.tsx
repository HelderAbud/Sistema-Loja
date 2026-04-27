import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { cancelSale, listSales } from "../api";
import { TableSkeleton } from "./ui/TableSkeleton";
import { queryKeys } from "../queryKeys";

const money = (n: number) => n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

function toIsoStartOfDay(localDate: string): string | undefined {
  if (!localDate) return undefined;
  const d = new Date(`${localDate}T00:00:00`);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

function toIsoEndOfDay(localDate: string): string | undefined {
  if (!localDate) return undefined;
  const d = new Date(`${localDate}T23:59:59.999`);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

export function SalesHistoryTab() {
  const queryClient = useQueryClient();
  const [draftFrom, setDraftFrom] = useState("");
  const [draftTo, setDraftTo] = useState("");
  const [draftProductId, setDraftProductId] = useState("");

  const [applied, setApplied] = useState({
    fromDay: "",
    toDay: "",
    productId: "",
  });
  const [page, setPage] = useState(0);

  const listArgs = useMemo(() => {
    const pid = applied.productId.trim() === "" ? undefined : Number(applied.productId);
    return {
      page,
      size: 20,
      from: toIsoStartOfDay(applied.fromDay.trim()),
      to: toIsoEndOfDay((applied.toDay.trim() || applied.fromDay).trim()),
      productId: Number.isFinite(pid as number) ? pid : undefined,
    };
  }, [page, applied]);

  const salesQ = useQuery({
    queryKey: queryKeys.sales(listArgs),
    queryFn: () => listSales(listArgs),
  });

  const cancelMut = useMutation({
    mutationFn: (saleId: number) => cancelSale(saleId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["sales"] });
      toast.success("Venda cancelada e stock reposto.");
    },
    onError: (e: unknown) => toast.error(String(e)),
  });

  const data = salesQ.data ?? null;
  const busy = salesQ.isFetching;
  const error = salesQ.error;

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setApplied({
      fromDay: draftFrom,
      toDay: draftTo,
      productId: draftProductId,
    });
    setPage(0);
  }

  return (
    <section className="card">
      <div className="section-head">
        <h2>Histórico de vendas</h2>
      </div>
      <p className="muted small section-lead">
        Datas em branco usam os últimos 30 dias na API. Opcionalmente filtre por produto (id).
      </p>
      <form onSubmit={onSubmit} className="form">
        <div className="field-row">
          <label>
            De
            <input type="date" value={draftFrom} onChange={(ev) => setDraftFrom(ev.target.value)} />
          </label>
          <label>
            Até
            <input type="date" value={draftTo} onChange={(ev) => setDraftTo(ev.target.value)} />
          </label>
          <label>
            Id produto
            <input
              value={draftProductId}
              onChange={(ev) => setDraftProductId(ev.target.value)}
              placeholder="opcional"
            />
          </label>
        </div>
        <button type="submit" className="primary" disabled={busy}>
          {busy ? (
            <span className="btn-inline-loading">
              <span
                className="ui-spinner"
                style={{ width: "0.95rem", height: "0.95rem", color: "#fff" }}
              />
              A consultar…
            </span>
          ) : (
            "Consultar"
          )}
        </button>
      </form>
      {error ? <p className="error">{String(error)}</p> : null}
      {busy && !data ? <TableSkeleton rows={3} label="A carregar histórico de vendas" /> : null}
      {data ? (
        <>
          <p className="muted small">
            {data.totalElements} venda(s) — página {data.number + 1}/{Math.max(1, data.totalPages)}
          </p>
          <div className={`table-wrap table-loading-wrap${busy ? " is-busy" : ""}`}>
            <table className="table">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Data</th>
                  <th>Produto</th>
                  <th>Marca</th>
                  <th>Qtd</th>
                  <th>P. venda</th>
                  <th>P. custo</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((s) => (
                  <tr key={s.id}>
                    <td className="muted">{s.id}</td>
                    <td>{new Date(s.soldAt).toLocaleString("pt-PT")}</td>
                    <td>
                      #{s.productId} {s.productName}
                    </td>
                    <td>{s.brandName}</td>
                    <td>{s.quantity}</td>
                    <td>{money(s.unitPrice)}</td>
                    <td>{money(s.unitCost)}</td>
                    <td>{s.cancelled ? <span className="muted">Cancelada</span> : "Ativa"}</td>
                    <td>
                      {!s.cancelled ? (
                        <button
                          type="button"
                          className="ghost small"
                          disabled={cancelMut.isPending}
                          onClick={() => {
                            if (window.confirm(`Cancelar venda #${s.id}? O stock será reposto.`)) {
                              void cancelMut.mutateAsync(s.id);
                            }
                          }}
                        >
                          Cancelar
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pager">
            <button
              type="button"
              className="ghost"
              disabled={data.first || busy}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Anterior
            </button>
            <button
              type="button"
              className="ghost"
              disabled={data.last || busy}
              onClick={() => setPage((p) => p + 1)}
            >
              Seguinte
            </button>
          </div>
        </>
      ) : null}
    </section>
  );
}
