import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  finalizePosSale,
  getCurrentCashSession,
  getProductStock,
  listProducts,
  listSellers,
  openCashSession,
  type PosPaymentMethod,
  type Product,
} from "@/api";
import { SellerPicker } from "./SellerPicker";
import { invalidateLojappDataQueries, queryKeys } from "@/queryKeys";
import {
  isInsufficientStock,
  isValidPositiveQuantity,
  parseDecimalInput,
  SALE_MIN_QUERY_LEN,
  SALE_SEARCH_DEBOUNCE_MS,
} from "../domain/saleFormParse";
import {
  buildSinglePosPayment,
  cashChangePreview,
  lineSaleTotal,
  POS_PAYMENT_METHOD_OPTIONS,
} from "../domain/posPayment";

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
  const [changeAmount, setChangeAmount] = useState<number | null>(null);
  const [sellerId, setSellerId] = useState("");
  const [openingAmount, setOpeningAmount] = useState("0");
  const [paymentMethod, setPaymentMethod] = useState<PosPaymentMethod>("CASH");
  const [receivedAmount, setReceivedAmount] = useState("");
  const [cardBrand, setCardBrand] = useState("");
  const [installments, setInstallments] = useState("");
  const [endToEndId, setEndToEndId] = useState("");
  const saleIdempotencyKeyRef = useRef(crypto.randomUUID());

  const cashQ = useQuery({
    queryKey: queryKeys.cashSessionCurrent(),
    queryFn: getCurrentCashSession,
  });

  const stockQ = useQuery({
    queryKey: selected != null ? queryKeys.productStock(selected.id) : ["productStock", -1],
    queryFn: () => getProductStock(selected!.id),
    enabled: selected != null,
  });

  const sellersQ = useQuery({
    queryKey: queryKeys.sellers(),
    queryFn: listSellers,
  });

  const openCashMut = useMutation({
    mutationFn: (body: { openingAmount: number }) => openCashSession(body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.cashSessionCurrent() });
    },
  });

  const saleMut = useMutation({
    mutationFn: (body: Parameters<typeof finalizePosSale>[0]) =>
      finalizePosSale(body, saleIdempotencyKeyRef.current),
    onSuccess: async () => {
      saleIdempotencyKeyRef.current = crypto.randomUUID();
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
  const priceNum = parseDecimalInput(unitPrice);
  const totalAmount =
    qtyValid && Number.isFinite(priceNum) && priceNum >= 0 ? lineSaleTotal(qtyNum, priceNum) : null;
  const changePreview =
    totalAmount != null ? cashChangePreview(paymentMethod, totalAmount, receivedAmount) : null;
  const insufficientStock = isInsufficientStock(selected != null, stockQty, qtyNum);
  const cashOpen = Boolean(cashQ.data?.open && cashQ.data.cashSessionId != null);

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

  async function onOpenCash(e: FormEvent) {
    e.preventDefault();
    const amount = parseDecimalInput(openingAmount);
    if (!Number.isFinite(amount) || amount < 0) {
      setError("Saldo inicial de caixa inválido.");
      return;
    }
    setError(null);
    try {
      await openCashMut.mutateAsync({ openingAmount: amount });
    } catch (err: unknown) {
      setError(String(err));
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!cashOpen || cashQ.data?.cashSessionId == null) {
      setError("Abra o turno de caixa para vender no PDV.");
      return;
    }
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
    if (stockLoading) {
      setError("Aguarde o saldo de stock.");
      return;
    }
    if (stockQ.isError) {
      setError("Não foi possível ler o stock. Tente outra vez.");
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
    const amount = lineSaleTotal(qtyNum, price);
    const built = buildSinglePosPayment({
      method: paymentMethod,
      amount,
      receivedRaw: receivedAmount,
      cardBrand,
      installmentsRaw: installments,
      endToEndId,
    });
    if ("error" in built) {
      setError(built.error);
      return;
    }
    setError(null);
    setSaleId(null);
    setChangeAmount(null);
    try {
      const created = await saleMut.mutateAsync({
        cashSessionId: cashQ.data.cashSessionId,
        productId,
        quantity: qtyNum,
        unitPrice: price,
        ...(uc != null ? { unitCost: uc } : {}),
        payments: [built.payment],
        sellerId: sellerId === "" ? null : Number(sellerId),
      });
      setSaleId(created.saleId);
      setChangeAmount(created.changeAmount ?? 0);
      await queryClient.invalidateQueries({ queryKey: queryKeys.productStock(productId) });
    } catch (err: unknown) {
      setError(String(err));
    }
  }

  const busy = saleMut.isPending || openCashMut.isPending;
  const saleDisabled = busy || !cashOpen || stockLoading || stockQ.isError || insufficientStock;

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
        Venda no PDV: precisa de turno de caixa aberto. O stock é atualizado após confirmar; o saldo
        tem de ser suficiente.
      </p>
      {cashQ.isError ? (
        <p className="error" role="alert">
          Não foi possível ler o caixa. {String(cashQ.error)}
        </p>
      ) : null}
      {cashQ.isLoading ? (
        <p className="muted small">A carregar turno de caixa…</p>
      ) : cashOpen ? (
        <p className="muted small">Turno de caixa #{cashQ.data?.cashSessionId} aberto.</p>
      ) : (
        <form onSubmit={onOpenCash} className="form">
          <p className="error small" role="status">
            Sem turno aberto: a venda PDV fica bloqueada.
          </p>
          <label>
            Saldo inicial do caixa (R$)
            <input
              value={openingAmount}
              onChange={(ev) => setOpeningAmount(ev.target.value)}
              disabled={busy}
            />
          </label>
          <button type="submit" className="primary" disabled={busy}>
            {openCashMut.isPending ? "A abrir caixa…" : "Abrir caixa"}
          </button>
        </form>
      )}
      <form onSubmit={onSubmit} className="form">
        <label className="combobox-wrap">
          Produto — pesquisar por nome
          <div className="combobox">
            <input
              autoComplete="off"
              role="combobox"
              aria-autocomplete="list"
              aria-expanded={listOpen && (suggestLoading || suggestions.length > 0)}
              aria-controls="piloto-sale-product-listbox"
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
              <ul
                id="piloto-sale-product-listbox"
                className="combobox-list"
                role="listbox"
                aria-label="Sugestões de produto"
              >
                {suggestLoading ? (
                  <li className="combobox-status muted small" role="option" aria-disabled="true">
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
                    <li key={p.id} role="option">
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
        {totalAmount != null ? (
          <p className="muted small">
            Total: {totalAmount.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}
          </p>
        ) : null}
        <label>
          Método de pagamento
          <select
            value={paymentMethod}
            onChange={(ev) => setPaymentMethod(ev.target.value as PosPaymentMethod)}
            disabled={busy}
          >
            {POS_PAYMENT_METHOD_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        {paymentMethod === "CASH" ? (
          <label>
            Valor recebido (R$) — opcional para troco
            <input
              value={receivedAmount}
              onChange={(ev) => setReceivedAmount(ev.target.value)}
              placeholder="ex.: 100"
              disabled={busy}
            />
          </label>
        ) : null}
        {changePreview != null && changePreview >= 0 ? (
          <p className="muted small">
            Troco: {changePreview.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}
          </p>
        ) : null}
        {paymentMethod === "CARD" ||
        paymentMethod === "CREDIT_CARD" ||
        paymentMethod === "DEBIT_CARD" ? (
          <label>
            Bandeira (opcional)
            <input
              value={cardBrand}
              onChange={(ev) => setCardBrand(ev.target.value)}
              placeholder="ex.: VISA"
              disabled={busy}
            />
          </label>
        ) : null}
        {paymentMethod === "CARD" || paymentMethod === "CREDIT_CARD" ? (
          <label>
            Parcelas (opcional)
            <input
              value={installments}
              onChange={(ev) => setInstallments(ev.target.value)}
              placeholder="ex.: 3"
              disabled={busy}
            />
          </label>
        ) : null}
        {paymentMethod === "PIX" ? (
          <label>
            End-to-end PIX (opcional)
            <input
              value={endToEndId}
              onChange={(ev) => setEndToEndId(ev.target.value)}
              disabled={busy}
            />
          </label>
        ) : null}
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
        <SellerPicker
          sellers={sellersQ.data ?? []}
          value={sellerId}
          onChange={setSellerId}
          disabled={busy}
        />
        {error ? <p className="error">{error}</p> : null}
        {saleId != null ? (
          <p className="success small">
            Venda registada — id <strong>{saleId}</strong>
            {changeAmount != null && changeAmount > 0
              ? ` · Troco ${changeAmount.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}`
              : null}
          </p>
        ) : null}
        <button type="submit" className="primary" disabled={saleDisabled}>
          {busy && saleMut.isPending ? (
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
