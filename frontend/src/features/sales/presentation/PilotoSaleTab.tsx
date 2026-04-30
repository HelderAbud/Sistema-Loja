import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getProductStock, listProducts, registerSale, type Product } from "@/api";
import { invalidateLojappDataQueries, queryKeys } from "@/queryKeys";
import {
  isInsufficientStock,
  isValidPositiveQuantity,
  parseDecimalInput,
  SALE_MIN_QUERY_LEN,
  SALE_SEARCH_DEBOUNCE_MS,
} from "../domain/saleFormParse";

export function PilotoSaleTab() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Product[]>([]);
  const [suggestLoading, setSuggestLoading] = useState(false);
  const [selected, setSelected] = useState<Product | null>(null);
  const [listOpen, setListOpen] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [quantity, setQuantity] = useState("1");
  const [unitPrice, setUnitPrice] = useState("");
  const [unitCost, setUnitCost] = useState("");
  const [useUnitCost, setUseUnitCost] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saleId, setSaleId] = useState<number | null>(null);

  const stockQ = useQuery({
    queryKey: selected != null ? queryKeys.productStock(selected.id) : ["productStock", -1],
    queryFn: () => getProductStock(selected!.id),
    enabled: selected != null,
  });

  const saleMut = useMutation({
    mutationFn: registerSale,
    onSuccess: async () => {
      invalidateLojappDataQueries(queryClient);
    },
  });

  const scheduleSearch = useCallback((raw: string) => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    const q = raw.trim();
    if (q.length < SALE_MIN_QUERY_LEN) {
      setSuggestions([]);
      setSuggestLoading(false);
      return;
    }
    setSuggestLoading(true);
    debounceRef.current = setTimeout(() => {
      listProducts({ page: 0, size: 20, q })
        .then((page) => {
          setSuggestions(page.content);
          setListOpen(true);
        })
        .catch((e: unknown) => setError(String(e)))
        .finally(() => setSuggestLoading(false));
    }, SALE_SEARCH_DEBOUNCE_MS);
  }, []);

  useEffect(
    () => () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    },
    [],
  );

  const stockQty = stockQ.data?.quantity ?? null;
  const stockLoading = stockQ.isFetching;
  const stockHint = stockQ.error ? String(stockQ.error) : null;

  const qtyNum = parseDecimalInput(quantity);
  const qtyValid = isValidPositiveQuantity(qtyNum);
  const insufficientStock = isInsufficientStock(selected != null, stockQty, qtyNum);

  function pickProduct(p: Product) {
    setSelected(p);
    setQuery(`${p.name} · #${p.id}`);
    setSuggestions([]);
    setListOpen(false);
    setError(null);
  }

  function clearSelection() {
    setSelected(null);
    setQuery("");
    setSuggestions([]);
    setListOpen(false);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (selected == null) {
      setError("Escolha um produto na pesquisa.");
      return;
    }
    const productId = selected.id;
    const price = parseDecimalInput(unitPrice);
    if (!qtyValid || !Number.isFinite(price) || price < 0) {
      setError("Quantidade e preço de venda inválidos.");
      return;
    }
    if (insufficientStock) {
      setError("Quantidade superior ao saldo disponível.");
      return;
    }
    let uc: number | null = null;
    if (useUnitCost) {
      uc = parseDecimalInput(unitCost);
      if (!Number.isFinite(uc) || uc < 0) {
        setError("Custo unitário inválido.");
        return;
      }
    }
    setError(null);
    setSaleId(null);
    try {
      const created = await saleMut.mutateAsync({
        productId,
        quantity: qtyNum,
        unitPrice: price,
        unitCost: uc,
      });
      setSaleId(created.id);
      await queryClient.invalidateQueries({ queryKey: queryKeys.productStock(productId) });
    } catch (err: unknown) {
      setError(String(err));
    }
  }

  const busy = saleMut.isPending;

  return (
    <section className="card">
      <div className="row spread">
        <div className="section-head">
          <h2>Registar venda</h2>
        </div>
        <button type="button" className="ghost" onClick={clearSelection}>
          Limpar produto
        </button>
      </div>
      <p className="muted small section-lead">
        O stock é atualizado automaticamente após cada venda; o saldo tem de ser suficiente antes de
        confirmar.
      </p>
      <form onSubmit={onSubmit} className="form">
        <label className="combobox-wrap">
          Produto — pesquisar por nome
          <div className="combobox">
            <input
              autoComplete="off"
              placeholder="Ex.: camiseta (mín. 1 letra)"
              value={query}
              onChange={(ev) => {
                const v = ev.target.value;
                setQuery(v);
                setSelected(null);
                setError(null);
                setListOpen(v.trim().length >= SALE_MIN_QUERY_LEN);
                scheduleSearch(v);
              }}
              onFocus={() => {
                if (query.trim().length >= SALE_MIN_QUERY_LEN && suggestions.length > 0)
                  setListOpen(true);
              }}
              onBlur={() => {
                window.setTimeout(() => setListOpen(false), 180);
              }}
            />
            {listOpen && (suggestLoading || suggestions.length > 0) ? (
              <ul className="combobox-list" role="listbox">
                {suggestLoading ? (
                  <li className="combobox-status muted small">
                    <span className="btn-inline-loading">
                      <span
                        className="ui-spinner"
                        style={{ width: "0.75rem", height: "0.75rem" }}
                      />
                      A pesquisar…
                    </span>
                  </li>
                ) : (
                  suggestions.map((p) => (
                    <li key={p.id}>
                      <button
                        type="button"
                        className="combobox-item"
                        onMouseDown={(ev) => ev.preventDefault()}
                        onClick={() => pickProduct(p)}
                      >
                        #{p.id} — {p.name} ({p.brandName})
                      </button>
                    </li>
                  ))
                )}
              </ul>
            ) : null}
          </div>
        </label>
        {selected != null ? (
          <p className="muted small">
            {stockLoading ? (
              <span className="btn-inline-loading muted small">
                <span className="ui-spinner" style={{ width: "0.75rem", height: "0.75rem" }} />A
                carregar saldo…
              </span>
            ) : stockQty != null ? (
              `Saldo disponível: ${Number.isInteger(stockQty) ? String(stockQty) : stockQty.toLocaleString("pt-BR", { maximumFractionDigits: 4 })}`
            ) : stockHint ? (
              `Saldo: não foi possível carregar (${stockHint})`
            ) : (
              "Saldo: —"
            )}
          </p>
        ) : null}
        <label>
          Quantidade
          <input value={quantity} onChange={(ev) => setQuantity(ev.target.value)} />
        </label>
        {insufficientStock ? (
          <p className="error small" role="alert">
            Quantidade ({qtyNum}) é maior que o saldo ({stockQty}).
          </p>
        ) : null}
        <label>
          Preço de venda unitário (R$)
          <input
            value={unitPrice}
            onChange={(ev) => setUnitPrice(ev.target.value)}
            placeholder="ex.: 18.90"
          />
        </label>
        <label className="check">
          <input
            type="checkbox"
            checked={useUnitCost}
            onChange={(ev) => setUseUnitCost(ev.target.checked)}
          />
          Definir custo unitário na venda (opcional)
        </label>
        {useUnitCost ? (
          <label>
            Custo unitário (R$)
            <input value={unitCost} onChange={(ev) => setUnitCost(ev.target.value)} />
          </label>
        ) : null}
        {error ? <p className="error">{error}</p> : null}
        {saleId != null ? (
          <p className="success small">
            Venda registada — id <strong>{saleId}</strong>
          </p>
        ) : null}
        <button type="submit" className="primary" disabled={busy || insufficientStock}>
          {busy ? (
            <span className="btn-inline-loading">
              <span
                className="ui-spinner"
                style={{ width: "0.95rem", height: "0.95rem", color: "#fff" }}
              />
              A registar…
            </span>
          ) : (
            "Registar venda"
          )}
        </button>
      </form>
    </section>
  );
}
