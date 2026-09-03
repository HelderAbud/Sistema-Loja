import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  closeCashSession,
  getCloseCashSessionPreview,
  getCurrentCashSession,
  openCashSession,
} from "../../api";
import { sellerSnapshot } from "../../features/storefront";
import { formatCurrency, StoreHeader } from "./storefrontShared";

export function SellerAreaPage() {
  const [openingAmountInput, setOpeningAmountInput] = useState("100.00");
  const [countedAmountInput, setCountedAmountInput] = useState("");
  const [differenceReason, setDifferenceReason] = useState("");
  const [managerApproval, setManagerApproval] = useState(false);

  const currentCashQ = useQuery({
    queryKey: ["storefront", "pos", "cash-session", "current"],
    queryFn: getCurrentCashSession,
  });

  const openCashSessionMut = useMutation({
    mutationFn: openCashSession,
    onSuccess: () => {
      toast.success("Turno aberto com sucesso.");
      void currentCashQ.refetch();
      setCountedAmountInput("");
      setDifferenceReason("");
      setManagerApproval(false);
    },
    onError: (error: unknown) => toast.error(String(error)),
  });

  const closePreviewMut = useMutation({
    mutationFn: ({
      cashSessionId,
      countedAmount,
    }: {
      cashSessionId: number;
      countedAmount?: number;
    }) => getCloseCashSessionPreview(cashSessionId, countedAmount),
    onError: (error: unknown) => toast.error(String(error)),
  });

  const closeCashSessionMut = useMutation({
    mutationFn: closeCashSession,
    onSuccess: () => {
      toast.success("Turno fechado com sucesso.");
      void currentCashQ.refetch();
      closePreviewMut.reset();
      setCountedAmountInput("");
      setDifferenceReason("");
      setManagerApproval(false);
    },
    onError: (error: unknown) => toast.error(String(error)),
  });

  const currentCash = currentCashQ.data;
  const hasOpenCashSession = Boolean(currentCash?.open && currentCash?.cashSessionId);

  function parseAmount(value: string): number | null {
    const normalized = value.trim().replace(",", ".");
    if (!normalized) return null;
    const parsed = Number(normalized);
    if (!Number.isFinite(parsed)) return null;
    return parsed;
  }

  async function handleOpenCashSession() {
    const openingAmount = parseAmount(openingAmountInput);
    if (openingAmount == null || openingAmount < 0) {
      toast.error("Informe um saldo inicial válido.");
      return;
    }
    await openCashSessionMut.mutateAsync({ openingAmount });
  }

  async function handlePreviewCloseCashSession() {
    if (!currentCash?.cashSessionId) return;
    const countedAmount = parseAmount(countedAmountInput);
    await closePreviewMut.mutateAsync({
      cashSessionId: currentCash.cashSessionId,
      countedAmount: countedAmount ?? undefined,
    });
  }

  async function handleCloseCashSession() {
    if (!currentCash?.cashSessionId) return;
    const countedAmount = parseAmount(countedAmountInput);
    if (countedAmount == null || countedAmount < 0) {
      toast.error("Informe um valor de conferência válido.");
      return;
    }
    await closeCashSessionMut.mutateAsync({
      cashSessionId: currentCash.cashSessionId,
      countedAmount,
      differenceReason: differenceReason.trim() || null,
      managerApproval,
    });
  }

  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-card">
          <h2>Área básica de lojista</h2>
          <p className="store-muted">Painel mínimo para acompanhar operação comercial.</p>
          <div className="store-kpis">
            <article>
              <strong>{formatCurrency(sellerSnapshot.monthRevenue)}</strong>
              <span>faturamento do mês</span>
            </article>
            <article>
              <strong>{sellerSnapshot.monthOrders}</strong>
              <span>pedidos no mês</span>
            </article>
            <article>
              <strong>{sellerSnapshot.conversionRate}%</strong>
              <span>taxa de conversão</span>
            </article>
          </div>
          <p className="store-muted">Marca com melhor performance: {sellerSnapshot.topBrand}</p>
        </section>
        <section className="store-card">
          <h2>Caixa PDV</h2>
          <p className="store-muted">Abertura, acompanhamento e fechamento do turno atual.</p>

          {currentCashQ.isPending ? (
            <p className="store-muted">A carregar sessão de caixa…</p>
          ) : null}
          {currentCashQ.error ? <p className="error banner">{String(currentCashQ.error)}</p> : null}

          {hasOpenCashSession ? (
            <div className="store-kpis">
              <article>
                <strong>#{currentCash?.cashSessionId}</strong>
                <span>turno aberto</span>
              </article>
              <article>
                <strong>{formatCurrency(Number(currentCash?.expectedAmount ?? 0))}</strong>
                <span>total esperado</span>
              </article>
              <article>
                <strong>{formatCurrency(Number(currentCash?.expectedCashAmount ?? 0))}</strong>
                <span>dinheiro</span>
              </article>
              <article>
                <strong>{formatCurrency(Number(currentCash?.expectedCardAmount ?? 0))}</strong>
                <span>cartão</span>
              </article>
              <article>
                <strong>{formatCurrency(Number(currentCash?.expectedPixAmount ?? 0))}</strong>
                <span>pix</span>
              </article>
            </div>
          ) : (
            <p className="store-muted">Sem turno aberto no momento.</p>
          )}

          <div className="field-row">
            <label>
              Saldo inicial (abertura)
              <input
                value={openingAmountInput}
                onChange={(event) => setOpeningAmountInput(event.target.value)}
                placeholder="100.00"
              />
            </label>
            <div className="store-cta-row">
              <button
                type="button"
                className="primary"
                disabled={openCashSessionMut.isPending || hasOpenCashSession}
                onClick={() => {
                  void handleOpenCashSession();
                }}
              >
                {openCashSessionMut.isPending ? "A abrir..." : "Abrir turno"}
              </button>
            </div>
          </div>

          <hr />

          <div className="field-row">
            <label>
              Valor contado no caixa
              <input
                value={countedAmountInput}
                onChange={(event) => setCountedAmountInput(event.target.value)}
                placeholder="100.00"
                disabled={!hasOpenCashSession}
              />
            </label>
            <label>
              Motivo da diferença (se houver)
              <input
                value={differenceReason}
                onChange={(event) => setDifferenceReason(event.target.value)}
                placeholder="ex.: diferença no troco"
                disabled={!hasOpenCashSession}
              />
            </label>
            <label>
              <input
                type="checkbox"
                checked={managerApproval}
                onChange={(event) => setManagerApproval(event.target.checked)}
                disabled={!hasOpenCashSession}
              />{" "}
              Confirmo que revisei a diferença
            </label>
          </div>

          <div className="store-cta-row">
            <button
              type="button"
              className="ghost"
              disabled={closePreviewMut.isPending || !hasOpenCashSession}
              onClick={() => {
                void handlePreviewCloseCashSession();
              }}
            >
              {closePreviewMut.isPending ? "A calcular..." : "Ver prévia de fechamento"}
            </button>
            <button
              type="button"
              className="primary"
              disabled={closeCashSessionMut.isPending || !hasOpenCashSession}
              onClick={() => {
                void handleCloseCashSession();
              }}
            >
              {closeCashSessionMut.isPending ? "A fechar..." : "Fechar turno"}
            </button>
          </div>

          {closePreviewMut.data ? (
            <div className="store-kpis">
              <article>
                <strong>{formatCurrency(Number(closePreviewMut.data.expectedAmount))}</strong>
                <span>esperado</span>
              </article>
              <article>
                <strong>{formatCurrency(Number(closePreviewMut.data.countedAmount ?? 0))}</strong>
                <span>contado</span>
              </article>
              <article>
                <strong>
                  {formatCurrency(Number(closePreviewMut.data.differenceAmount ?? 0))}
                </strong>
                <span>diferença</span>
              </article>
              <article>
                <strong>{formatCurrency(Number(closePreviewMut.data.toleranceAmount))}</strong>
                <span>tolerância</span>
              </article>
              <article>
                <strong>{closePreviewMut.data.managerApprovalRequired ? "Sim" : "Não"}</strong>
                <span>confirmação de revisão</span>
              </article>
            </div>
          ) : null}
        </section>
      </main>
    </div>
  );
}
