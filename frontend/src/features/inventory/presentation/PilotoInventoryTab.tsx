import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adjustStock, listLowStock, listProductMovements } from "@/api";
import { canManageBackofficeCatalog } from "@/features/auth";
import { useCurrentUser } from "@/hooks";
import { TableSkeleton } from "@/components/ui/TableSkeleton";
import { invalidateLojappDataQueries, queryKeys } from "@/queryKeys";
import { validateManualStockAdjust } from "../domain/manualAdjust";
import { ProductSearchCombobox } from "./ProductSearchCombobox";

function movementTypeLabel(type: string): string {
  if (type === "SALE") return "Venda";
  if (type === "ENTRY") return "Entrada";
  if (type === "ADJUSTMENT") return "Ajuste";
  return type;
}

function sourceLabel(source: string, sourceId: number | null): string {
  const names: Record<string, string> = {
    MANUAL_ADJUST: "Ajuste manual",
    SALE_REGISTER: "Venda",
    SALE_CANCEL: "Cancelamento",
    NFE_IMPORT: "NFe",
  };
  const name = names[source] ?? source;
  return sourceId != null ? `${name} #${sourceId}` : name;
}

function quantityClass(quantity: number): string {
  if (quantity > 0) return "qty-in";
  if (quantity < 0) return "qty-out";
  return "";
}

function formatMovementWhen(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("pt-BR");
}

export function PilotoInventoryTab() {
  const queryClient = useQueryClient();
  const meQ = useCurrentUser();
  const canAdjust = canManageBackofficeCatalog(meQ.data?.appRole);
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("AJUSTE_MANUAL");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const [historyProductId, setHistoryProductId] = useState<number | null>(null);
  const [historyPage, setHistoryPage] = useState(0);

  const lowQ = useQuery({
    queryKey: queryKeys.lowStock(),
    queryFn: listLowStock,
  });

  const histQ = useQuery({
    queryKey: queryKeys.productMovements(historyProductId ?? 0, historyPage),
    queryFn: () => listProductMovements(historyProductId as number, historyPage),
    enabled: historyProductId != null && historyProductId > 0,
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

      {canAdjust ? (
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
      ) : null}

      <section className="card">
        <div className="section-head">
          <h2>Histórico de movimentos</h2>
        </div>
        <p className="muted small section-lead">
          Kardex do produto: data, tipo, quantidade com sinal, origem e motivo do ajuste.
        </p>
        <ProductSearchCombobox
          id="piloto-inventory-kardex-listbox"
          label="Produto — pesquisar por nome"
          inputAriaLabel="Produto para histórico"
          onSelect={(p) => {
            setHistoryProductId(p.id);
            setHistoryPage(0);
          }}
        />
        {historyProductId == null ? (
          <p className="muted">Pesquise e escolha um produto para ver o kardex.</p>
        ) : null}
        {histQ.error ? <p className="error">{String(histQ.error)}</p> : null}
        {historyProductId != null && histQ.isPending ? (
          <TableSkeleton rows={5} label="A carregar histórico" />
        ) : null}
        {histQ.data && histQ.data.content.length === 0 ? (
          <p className="muted">Nenhum movimento para este produto.</p>
        ) : null}
        {histQ.data && histQ.data.content.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Tipo</th>
                  <th>Quantidade</th>
                  <th>Origem</th>
                  <th>Motivo</th>
                </tr>
              </thead>
              <tbody>
                {histQ.data.content.map((row) => (
                  <tr key={row.id}>
                    <td>{formatMovementWhen(row.createdAt)}</td>
                    <td>{movementTypeLabel(row.movementType)}</td>
                    <td className={quantityClass(Number(row.quantity))}>{row.quantity}</td>
                    <td>{sourceLabel(row.source, row.sourceId)}</td>
                    <td>{row.reason ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        {histQ.data && histQ.data.totalPages > 1 ? (
          <div className="row">
            <button
              type="button"
              className="ghost"
              disabled={histQ.data.first}
              onClick={() => setHistoryPage((p) => Math.max(0, p - 1))}
            >
              Anterior
            </button>
            <span className="muted small">
              Página {histQ.data.number + 1} de {histQ.data.totalPages}
            </span>
            <button
              type="button"
              className="ghost"
              disabled={histQ.data.last}
              onClick={() => setHistoryPage((p) => p + 1)}
            >
              Seguinte
            </button>
          </div>
        ) : null}
      </section>
    </div>
  );
}
