import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  finalizePosSale,
  getCurrentCashSession,
  listSellers,
  type PosPaymentMethod,
} from "../../api";
import { SellerPicker } from "../../features/sales/presentation/SellerPicker";
import { calculateCartSubtotal, useCartStore, useCartSummary } from "../../features/storefront";
import { StoreHeader, formatCurrency } from "./storefrontShared";

export function CartPage() {
  const { items, totals } = useCartSummary();
  const clear = useCartStore((state) => state.clear);
  const removeItem = useCartStore((state) => state.removeItem);
  const [checkoutMessage, setCheckoutMessage] = useState<string | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PosPaymentMethod>("CARD");
  const [checkoutNonce, setCheckoutNonce] = useState(0);
  const [sellerId, setSellerId] = useState("");
  const currentCashQ = useQuery({
    queryKey: ["storefront", "pos", "cash-session", "current"],
    queryFn: getCurrentCashSession,
  });
  const sellersQ = useQuery({
    queryKey: ["storefront", "sellers"],
    queryFn: listSellers,
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
    if (items.length === 0) {
      setCheckoutMessage("Adicione pelo menos um item ao carrinho.");
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
      const lineItems = items.map((item) => ({
        productId: Number(item.id),
        quantity: item.quantity,
        unitPrice: item.price,
      }));
      const merchandiseTotal = calculateCartSubtotal(items);
      const cartFingerprint = lineItems
        .map((line) => `${line.productId}x${line.quantity}@${line.unitPrice}`)
        .sort()
        .join("|");
      const idempotencyKey = `pdv-checkout-${cartFingerprint}-${checkoutNonce}`;
      await saleMut.mutateAsync({
        body: {
          cashSessionId: currentCashQ.data.cashSessionId,
          items: lineItems,
          payments: [{ paymentMethod, amount: merchandiseTotal }],
          sellerId: sellerId === "" ? null : Number(sellerId),
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
            <SellerPicker
              sellers={sellersQ.data ?? []}
              value={sellerId}
              onChange={setSellerId}
              disabled={saleMut.isPending}
            />
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
          {checkoutMessage ? <p className="muted">{checkoutMessage}</p> : null}
          <div className="store-cta-row">
            <button
              type="button"
              className="primary"
              disabled={saleMut.isPending || items.length === 0 || !currentCashQ.data?.open}
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
