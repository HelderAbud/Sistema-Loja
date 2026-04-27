import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adjustStock, listLowStock } from "../../../api";
import { TableSkeleton } from "../../../components/ui/TableSkeleton";
import { invalidateLojappDataQueries, queryKeys } from "../../../queryKeys";
import { validateManualStockAdjust } from "../domain/manualAdjust";

export function PilotoInventoryTab() {
  const queryClient = useQueryClient();
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("AJUSTE_MANUAL");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);

  const lowQ = useQuery({
    queryKey: queryKeys.lowStock(),
    queryFn: listLowStock,
  });

  const adjustMut = useMutation({
    mutationFn: (args: { productId: number; qty: number; reason: string }) =>
      adjustStock(args.productId, args.qty, args.reason),
    onSuccess: async () => {
      setDone("Ajuste registado.");
      setQuantity("");
      await queryClient.invalidateQueries({ queryKey: queryKeys.lowStock() });
      invalidateLojappDataQueries(queryClient);
    },
    onError: (err: unknown) => {
      setError(String(err));
    },
  });

  const low = lowQ.data ?? null;
  const busy = adjustMut.isPending;
  const listError = lowQ.error;

  async function onAdjust(e: FormEvent) {
    e.preventDefault();
    const parsed = validateManualStockAdjust(productId, quantity, reason);
    if (!parsed.ok) {
      setError(parsed.message);
      return;
    }
    setError(null);
    setDone(null);
    await adjustMut.mutateAsync(parsed.value);
  }

  return (
    <div className="stack">
      <section className="card">
        <div className="row spread">
          <div className="section-head">
            <h2>Stock baixo</h2>
          </div>
          <button
            type="button"
            className="ghost"
            onClick={() => void lowQ.refetch()}
            disabled={lowQ.isFetching}
          >
            Atualizar
          </button>
        </div>
        <p className="muted small section-lead">
          Produtos com saldo inferior ao mínimo definido no catálogo.
        </p>
        {listError ? <p className="error">{String(listError)}</p> : null}
        {low && low.length === 0 ? <p className="muted">Nenhum produto abaixo do mínimo.</p> : null}
        {low && low.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Produto</th>
                  <th>Saldo</th>
                  <th>Mín.</th>
                </tr>
              </thead>
              <tbody>
                {low.map((row) => (
                  <tr key={row.productId}>
                    <td>{row.productId}</td>
                    <td>{row.productName}</td>
                    <td>{row.currentQuantity}</td>
                    <td>{row.minimumStock}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        {!low && lowQ.isPending ? <TableSkeleton rows={5} label="A carregar stock baixo" /> : null}
      </section>

      <section className="card">
        <div className="section-head">
          <h2>Ajustar stock</h2>
        </div>
        <p className="muted small section-lead">
          Quantidade positiva aumenta o saldo; negativa reduz.
        </p>
        <form onSubmit={onAdjust} className="form">
          <label>
            Id do produto
            <input
              inputMode="numeric"
              value={productId}
              onChange={(ev) => setProductId(ev.target.value)}
              placeholder="ex.: 1"
            />
          </label>
          <label>
            Quantidade (use ponto decimal)
            <input
              value={quantity}
              onChange={(ev) => setQuantity(ev.target.value)}
              placeholder="ex.: 10 ou -2"
            />
          </label>
          <label>
            Motivo
            <input value={reason} onChange={(ev) => setReason(ev.target.value)} maxLength={500} />
          </label>
          {error ? <p className="error">{error}</p> : null}
          {done ? <p className="success small">{done}</p> : null}
          <button type="submit" className="primary" disabled={busy}>
            {busy ? (
              <span className="btn-inline-loading">
                <span
                  className="ui-spinner"
                  style={{ width: "0.95rem", height: "0.95rem", color: "#fff" }}
                />
                A registar…
              </span>
            ) : (
              "Registar ajuste"
            )}
          </button>
        </form>
      </section>
    </div>
  );
}
