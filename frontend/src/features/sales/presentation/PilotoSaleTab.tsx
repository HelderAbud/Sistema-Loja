import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  closeCashSession,
  finalizePosSale,
  getCloseCashSessionPreview,
  getCurrentCashSession,
  getProductStock,
  listProducts,
  listSellers,
  openCashSession,
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
  buildPosPayments,
  cashChangePreview,
  lineSaleTotal,
  newPaymentLine,
  POS_PAYMENT_METHOD_OPTIONS,
  remainingFromFilledAmounts,
  remainingToPay,
  resolveLineAmount,
  type PaymentLineDraft,
} from "../domain/posPayment";
import { buildCloseCashBody } from "../domain/cashClose";

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
  const [countedAmount, setCountedAmount] = useState("0");
  const [differenceReason, setDifferenceReason] = useState("");
  const [managerApproval, setManagerApproval] = useState(false);
  const [paymentLines, setPaymentLines] = useState<PaymentLineDraft[]>(() => [
    newPaymentLine(crypto.randomUUID()),
  ]);
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

  const closePreviewMut = useMutation({
    mutationFn: ({
      cashSessionId,
      countedAmount,
    }: {
      cashSessionId: number;
      countedAmount?: number;
    }) => getCloseCashSessionPreview(cashSessionId, countedAmount),
  });

  const closeCashMut = useMutation({
    mutationFn: (body: Parameters<typeof closeCashSession>[0]) => closeCashSession(body),
    onSuccess: async () => {
      closePreviewMut.reset();
      setDifferenceReason("");
      setManagerApproval(false);
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
    totalAmount == null
      ? null
      : paymentLines.reduce<number | null>((sum, line) => {
          const amount = resolveLineAmount(line.amountRaw, totalAmount, paymentLines.length);
          if (amount == null) {
            return sum;
          }
          const change = cashChangePreview(line.method, amount, line.receivedRaw);
          if (change == null) {
            return sum;
          }
          return (sum ?? 0) + change;
        }, null);
  const remaining = totalAmount != null ? remainingToPay(totalAmount, paymentLines) : null;
  const paymentsUnbalanced = remaining == null || !Number.isFinite(remaining) || remaining !== 0;

  function patchPaymentLine(id: string, patch: Partial<PaymentLineDraft>) {
    setPaymentLines((lines) =>
      lines.map((line) => (line.id === id ? { ...line, ...patch } : line)),
    );
  }

  function addPaymentLine() {
    const leftover =
      totalAmount != null ? remainingFromFilledAmounts(totalAmount, paymentLines) : 0;
    const amountRaw = leftover > 0 ? String(leftover) : "";
    setPaymentLines((lines) => [...lines, newPaymentLine(crypto.randomUUID(), "PIX", amountRaw)]);
  }

  function removePaymentLine(id: string) {
    setPaymentLines((lines) =>
      lines.length <= 1 ? lines : lines.filter((line) => line.id !== id),
    );
  }
  const insufficientStock = isInsufficientStock(selected != null, stockQty, qtyNum);
  const cashOpen = Boolean(cashQ.data?.open && cashQ.data.cashSessionId != null);

  useEffect(() => {
    if (cashQ.data?.open) {
      setCountedAmount(String(cashQ.data.expectedAmount ?? 0));
    }
  }, [cashQ.data?.open, cashQ.data?.cashSessionId, cashQ.data?.expectedAmount]);

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

  async function onPreviewClose() {
    if (cashQ.data?.cashSessionId == null) {
      return;
    }
    const counted = parseDecimalInput(countedAmount);
    setError(null);
    try {
      await closePreviewMut.mutateAsync({
        cashSessionId: cashQ.data.cashSessionId,
        countedAmount: Number.isFinite(counted) ? counted : undefined,
      });
    } catch (err: unknown) {
      setError(String(err));
    }
  }

  async function onCloseCash(e: FormEvent) {
    e.preventDefault();
    if (cashQ.data?.cashSessionId == null) {
      setError("Não há turno aberto para fechar.");
      return;
    }
    const built = buildCloseCashBody({
      cashSessionId: cashQ.data.cashSessionId,
      countedRaw: countedAmount,
      differenceReason,
      managerApproval,
    });
    if ("error" in built) {
      setError(built.error);
      return;
    }
    setError(null);
    try {
      await closeCashMut.mutateAsync(built.body);
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
    const built = buildPosPayments(amount, paymentLines);
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
        payments: built.payments,
        sellerId: sellerId === "" ? null : Number(sellerId),
      });
      setSaleId(created.saleId);
      setChangeAmount(created.changeAmount ?? 0);
      await queryClient.invalidateQueries({ queryKey: queryKeys.productStock(productId) });
    } catch (err: unknown) {
      setError(String(err));
    }
  }

  const busy = saleMut.isPending || openCashMut.isPending || closeCashMut.isPending;
  const saleDisabled =
    busy ||
    !cashOpen ||
    stockLoading ||
    stockQ.isError ||
    insufficientStock ||
    (totalAmount != null && paymentsUnbalanced);

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
        Venda no PDV: precisa de turno de caixa aberto. Pode dividir o total por qualquer meio
        (dinheiro, PIX, cartões, boleto, transferência). NSU e end-to-end ficam no registo para
        conciliar pagamentos reais depois. O stock atualiza ao confirmar.
      </p>
      {cashQ.isError ? (
        <p className="error" role="alert">
          Não foi possível ler o caixa. {String(cashQ.error)}
        </p>
      ) : null}
      {cashQ.isLoading ? (
        <p className="muted small">A carregar turno de caixa…</p>
      ) : cashOpen ? (
        <form onSubmit={onCloseCash} className="form">
          <p className="muted small">
            Turno de caixa #{cashQ.data?.cashSessionId} aberto. Esperado:{" "}
            {(cashQ.data?.expectedAmount ?? 0).toLocaleString("pt-BR", {
              style: "currency",
              currency: "BRL",
            })}
          </p>
          <label>
            Valor contado no caixa (R$)
            <input
              value={countedAmount}
              onChange={(ev) => setCountedAmount(ev.target.value)}
              disabled={busy}
            />
          </label>
          <label>
            Motivo da diferença (se o contado ≠ esperado)
            <input
              value={differenceReason}
              onChange={(ev) => setDifferenceReason(ev.target.value)}
              disabled={busy}
            />
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={managerApproval}
              onChange={(ev) => setManagerApproval(ev.target.checked)}
              disabled={busy}
            />
            Confirmo que revisei a diferença
          </label>
          {closePreviewMut.data ? (
            <p className="muted small">
              Prévia: diferença{" "}
              {(closePreviewMut.data.differenceAmount ?? 0).toLocaleString("pt-BR", {
                style: "currency",
                currency: "BRL",
              })}
              {closePreviewMut.data.managerApprovalRequired
                ? " — confirmação de revisão obrigatória"
                : ""}
            </p>
          ) : null}
          <div className="row">
            <button
              type="button"
              className="ghost"
              disabled={busy || closePreviewMut.isPending}
              onClick={() => {
                void onPreviewClose();
              }}
            >
              {closePreviewMut.isPending ? "A calcular prévia…" : "Ver prévia de fecho"}
            </button>
            <button type="submit" className="ghost" disabled={busy}>
              {closeCashMut.isPending ? "A fechar turno…" : "Fechar turno"}
            </button>
          </div>
        </form>
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
            {remaining != null && Number.isFinite(remaining) ? (
              <>
                {" "}
                · Falta alocar:{" "}
                {remaining.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}
              </>
            ) : null}
          </p>
        ) : null}
        {paymentLines.map((line, index) => (
          <fieldset key={line.id} className="form">
            <legend>Parcela {index + 1}</legend>
            <label>
              Método de pagamento
              <select
                value={line.method}
                onChange={(ev) =>
                  patchPaymentLine(line.id, {
                    method: ev.target.value as PaymentLineDraft["method"],
                    pending: ev.target.value === "CASH" ? false : line.pending,
                  })
                }
                disabled={busy}
              >
                {POS_PAYMENT_METHOD_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Valor desta parcela (R$)
              <input
                value={line.amountRaw}
                onChange={(ev) => patchPaymentLine(line.id, { amountRaw: ev.target.value })}
                placeholder={paymentLines.length === 1 ? "vazio = total da venda" : "ex.: 10"}
                disabled={busy}
              />
            </label>
            {line.method === "CASH" ? (
              <label>
                Valor recebido (R$) — opcional para troco
                <input
                  value={line.receivedRaw}
                  onChange={(ev) => patchPaymentLine(line.id, { receivedRaw: ev.target.value })}
                  placeholder="ex.: 100"
                  disabled={busy}
                />
              </label>
            ) : null}
            {line.method === "CARD" ||
            line.method === "CREDIT_CARD" ||
            line.method === "DEBIT_CARD" ? (
              <label>
                Bandeira (opcional)
                <input
                  value={line.cardBrand}
                  onChange={(ev) => patchPaymentLine(line.id, { cardBrand: ev.target.value })}
                  placeholder="ex.: VISA"
                  disabled={busy}
                />
              </label>
            ) : null}
            {line.method === "CARD" || line.method === "CREDIT_CARD" ? (
              <label>
                Parcelas (opcional)
                <input
                  value={line.installmentsRaw}
                  onChange={(ev) => patchPaymentLine(line.id, { installmentsRaw: ev.target.value })}
                  placeholder="ex.: 3"
                  disabled={busy}
                />
              </label>
            ) : null}
            {line.method === "PIX" ? (
              <label>
                End-to-end PIX (opcional)
                <input
                  value={line.endToEndId}
                  onChange={(ev) => patchPaymentLine(line.id, { endToEndId: ev.target.value })}
                  disabled={busy}
                />
              </label>
            ) : null}
            <label>
              NSU / id da transação (opcional)
              <input
                value={line.transactionId}
                onChange={(ev) => patchPaymentLine(line.id, { transactionId: ev.target.value })}
                disabled={busy}
              />
            </label>
            {line.method !== "CASH" ? (
              <label className="check">
                <input
                  type="checkbox"
                  checked={line.pending}
                  onChange={(ev) => patchPaymentLine(line.id, { pending: ev.target.checked })}
                  disabled={busy}
                />
                Pendente (aguardar liquidação real — PIX/cartão/boleto)
              </label>
            ) : null}
            {paymentLines.length > 1 ? (
              <button
                type="button"
                className="ghost"
                disabled={busy}
                onClick={() => removePaymentLine(line.id)}
              >
                Remover parcela {index + 1}
              </button>
            ) : null}
          </fieldset>
        ))}
        <button type="button" className="ghost" disabled={busy} onClick={addPaymentLine}>
          Adicionar meio de pagamento
        </button>
        {changePreview != null && changePreview >= 0 ? (
          <p className="muted small">
            Troco: {changePreview.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}
          </p>
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
