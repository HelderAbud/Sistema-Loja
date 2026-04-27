import { FormEvent, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { listBrands, listProducts } from "../api";
import { queryKeys } from "../queryKeys";
import { PageHeader } from "./ui/PageHeader";

const money = (n: number) => n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const SIZE = 20;

type AppliedFilters = {
  brandId: string;
  q: string;
  lowStock: boolean;
  page: number;
};

export function ProductsBrowseTab() {
  const [draftBrandId, setDraftBrandId] = useState("");
  const [draftQ, setDraftQ] = useState("");
  const [draftLowStock, setDraftLowStock] = useState(false);

  const [applied, setApplied] = useState<AppliedFilters>({
    brandId: "",
    q: "",
    lowStock: false,
    page: 0,
  });

  const resolvedBrandId = useMemo(() => {
    if (applied.brandId === "") return undefined;
    const n = Number(applied.brandId);
    return Number.isFinite(n) ? n : undefined;
  }, [applied.brandId]);

  const brandsQ = useQuery({
    queryKey: queryKeys.brands(),
    queryFn: listBrands,
  });

  const productsQ = useQuery({
    queryKey: queryKeys.products({
      page: applied.page,
      size: SIZE,
      brandId: resolvedBrandId,
      q: applied.q.trim() || undefined,
      lowStock: applied.lowStock || undefined,
    }),
    queryFn: () =>
      listProducts({
        page: applied.page,
        size: SIZE,
        brandId: resolvedBrandId,
        q: applied.q.trim() || undefined,
        lowStock: applied.lowStock || undefined,
      }),
  });

  const brands = brandsQ.data ?? [];
  const productPage = productsQ.data ?? null;
  const busy = productsQ.isFetching;
  const listLoading = productsQ.isPending;
  const error = brandsQ.error ?? productsQ.error;

  function onFilter(e: FormEvent) {
    e.preventDefault();
    setApplied({
      brandId: draftBrandId,
      q: draftQ,
      lowStock: draftLowStock,
      page: 0,
    });
  }

  return (
    <section className="card">
      <PageHeader
        title="Catálogo de produtos"
        lead="Filtre por marca, texto no nome ou stock abaixo do mínimo. Paginação alinhada à API."
      />
      <form onSubmit={onFilter} className="form">
        <div className="field-row">
          <label>
            Marca
            <select value={draftBrandId} onChange={(ev) => setDraftBrandId(ev.target.value)}>
              <option value="">Todas</option>
              {brands.map((b) => (
                <option key={b.id} value={String(b.id)}>
                  {b.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Nome contém
            <input
              value={draftQ}
              onChange={(ev) => setDraftQ(ev.target.value)}
              placeholder="ex.: caderno"
            />
          </label>
          <label className="check mid">
            <input
              type="checkbox"
              checked={draftLowStock}
              onChange={(ev) => setDraftLowStock(ev.target.checked)}
            />
            Só stock baixo
          </label>
        </div>
        <button type="submit" className="primary" disabled={busy}>
          {busy ? (
            <span className="btn-inline-loading">
              <span
                className="ui-spinner"
                style={{ width: "0.95rem", height: "0.95rem", color: "#fff" }}
              />
              A filtrar…
            </span>
          ) : (
            "Aplicar filtros"
          )}
        </button>
      </form>
      {error ? <p className="error banner">{String(error)}</p> : null}
      {busy && productPage ? <p className="muted small">A atualizar lista…</p> : null}
      {productPage ? (
        <>
          <p className="muted small">
            {productPage.totalElements} produto(s) — página {productPage.number + 1}/
            {Math.max(1, productPage.totalPages)}
          </p>
          <div className={`table-wrap table-loading-wrap${busy ? " is-busy" : ""}`}>
            <table className="table">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Nome</th>
                  <th>Marca</th>
                  <th>EAN</th>
                  <th>Custo</th>
                  <th>Venda</th>
                  <th>Mín.</th>
                </tr>
              </thead>
              <tbody>
                {productPage.content.map((p) => (
                  <tr key={p.id}>
                    <td className="muted">{p.id}</td>
                    <td>{p.name}</td>
                    <td>{p.brandName}</td>
                    <td className="muted">{p.ean ?? "—"}</td>
                    <td>{money(p.costPrice)}</td>
                    <td>{money(p.salePrice)}</td>
                    <td>{p.minimumStock}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pager">
            <button
              type="button"
              className="ghost"
              disabled={productPage.first || busy}
              onClick={() => setApplied((prev) => ({ ...prev, page: Math.max(0, prev.page - 1) }))}
            >
              Anterior
            </button>
            <button
              type="button"
              className="ghost"
              disabled={productPage.last || busy}
              onClick={() => setApplied((prev) => ({ ...prev, page: prev.page + 1 }))}
            >
              Seguinte
            </button>
          </div>
        </>
      ) : listLoading || (busy && !productPage) ? (
        <div className="table-skeleton" aria-hidden>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton-block skeleton-row" />
          ))}
        </div>
      ) : (
        <p className="muted">Sem dados.</p>
      )}
    </section>
  );
}
