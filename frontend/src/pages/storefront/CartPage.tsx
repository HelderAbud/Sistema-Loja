import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { finalizePosSale, getCurrentCashSession, type PosPaymentMethod } from "../../api";
import { useCartStore, useCartSummary } from "../../features/storefront";
import { StoreHeader, formatCurrency } from "./storefrontShared";

export function CartPage() {
  const { items, totals } = useCartSummary();
  const clear = useCartStore((state) => state.clear);
  const removeItem = useCartStore((state) => state.removeItem);
  const [checkoutMessage, setCheckoutMessage] = useState<string | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PosPaymentMethod>("CARD");
  const [checkoutNonce, setCheckoutNonce] = useState(0);
  const currentCashQ = useQuery({
    queryKey: ["storefront", "pos", "cash-session", "current"],
    queryFn: getCurrentCashSession,
  });
  const saleMut = useMutation({
    mutationFn: ({
      body,
      idempotencyKey,
    }: {
      body: Parameters<typeof finalizePosSale>[0];
      idempotencyKey: string;
    }) => finalizePosSale(body, idempotencyKey),
  });

  async function checkoutOrder() {
    setCheckoutMessage(null);
    if (items.length !== 1) {
      setCheckoutMessage("No MVP atual, cada venda PDV suporta 1 item por vez no carrinho.");
      return;
    }
    const invalidProduct = items.find((item) => !Number.isFinite(Number(item.id)));
    if (invalidProduct) {
      setCheckoutMessage(
        "Alguns itens estão em modo demo. Entre com sessão ativa para checkout real.",
      );
      return;
    }
    if (!currentCashQ.data?.open || !currentCashQ.data?.cashSessionId) {
      setCheckoutMessage("Abra um turno de caixa para concluir venda no PDV.");
      return;
    }

    try {
      const item = items[0];
      const totalAmount = Number(item.price) * Number(item.quantity);
      const idempotencyKey = `pdv-checkout-${item.id}-${item.quantity}-${item.price}-${checkoutNonce}`;
      await saleMut.mutateAsync({
        body: {
          cashSessionId: currentCashQ.data.cashSessionId,
          productId: Number(item.id),
          quantity: item.quantity,
          unitPrice: item.price,
          unitCost: null,
          payments: [{ paymentMethod, amount: totalAmount }],
        },
        idempotencyKey,
      });
      await currentCashQ.refetch();
      clear();
      setCheckoutNonce((value) => value + 1);
      setCheckoutMessage("Venda PDV registada com sucesso.");
    } catch (error: unknown) {
      setCheckoutMessage(`Falha ao concluir venda PDV: ${String(error)}`);
    }
  }

  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-card">
          <h2>Carrinho simples</h2>
          {items.length === 0 ? <p className="store-muted">Seu carrinho está vazio.</p> : null}
          {items.map((item) => (
            <div key={item.id} className="store-cart-row">
              <div>
                <strong>{item.name}</strong>
                <p className="store-muted">Qtd: {item.quantity}</p>
              </div>
              <div className="store-cart-actions">
                <span>{formatCurrency(item.price * item.quantity)}</span>
                <button type="button" className="ghost" onClick={() => removeItem(item.id)}>
                  Remover
                </button>
              </div>
            </div>
          ))}
          <hr />
          <p>Subtotal: {formatCurrency(totals.subtotal)}</p>
          <p>Envio: {formatCurrency(totals.shipping)}</p>
          <p className="store-price-xl">Total: {formatCurrency(totals.total)}</p>
          <div className="field-row">
            <label>
              Método de pagamento
              <select
                value={paymentMethod}
                onChange={(event) => setPaymentMethod(event.target.value as PosPaymentMethod)}
              >
                <option value="CASH">Dinheiro</option>
                <option value="CARD">Cartão</option>
                <option value="PIX">PIX</option>
              </select>
            </label>
          </div>
          {!currentCashQ.data?.open ? (
            <p className="store-muted">Sem turno aberto: checkout PDV bloqueado.</p>
          ) : null}
          {items.length > 1 ? (
            <p className="store-muted">Checkout PDV MVP: mantenha 1 item por venda.</p>
          ) : null}
          {checkoutMessage ? <p className="muted">{checkoutMessage}</p> : null}
          <div className="store-cta-row">
            <button
              type="button"
              className="primary"
              disabled={saleMut.isPending || items.length !== 1 || !currentCashQ.data?.open}
              onClick={checkoutOrder}
            >
              Finalizar venda PDV
            </button>
            <button type="button" className="ghost" onClick={clear}>
              Limpar carrinho
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
