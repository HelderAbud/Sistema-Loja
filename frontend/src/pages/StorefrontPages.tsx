import { type ChangeEvent, useEffect, useMemo, useState } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  closeCashSession,
  finalizePosSale,
  getCloseCashSessionPreview,
  getCurrentCashSession,
  listBrands,
  listProducts,
  listSales,
  openCashSession,
  type PosPaymentMethod,
  summarizeSales,
  summarizeSalesDaily,
} from "../api";
import { useAuthStore } from "../authStore";
import { BRAND_NAME, BRAND_TAGLINE } from "../brand";
import {
  getProductBySlug,
  sellerSnapshot,
  socialProof,
  storefrontProducts,
  useCartStore,
  useCartSummary,
} from "../features/storefront";
import { StoreCatalogGridSkeleton } from "../components/ui/StoreCatalogGridSkeleton";
import { TableSkeleton } from "../components/ui/TableSkeleton";
import {
  type OrdersFilterPreset,
  type OrdersSortKey,
  parseOrderPresetsImport,
  sortSaleRows,
  useStorefrontOrdersFilters,
} from "../features/orders";

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(value);
}

function percentDelta(current: number, previous: number) {
  if (previous === 0) return current === 0 ? 0 : 100;
  return ((current - previous) / previous) * 100;
}

function csvEscape(value: string | number) {
  const text = String(value ?? "");
  if (text.includes(",") || text.includes('"') || text.includes("\n")) {
    return `"${text.replaceAll('"', '""')}"`;
  }
  return text;
}

function formatPercent(value: number) {
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(1)}%`;
}

type SummaryTemplate = "direct" | "executive" | "whatsapp";
const SUMMARY_TEMPLATE_STORAGE_KEY = "lojapp_summary_template";
const SUMMARY_CUSTOM_TEXT_STORAGE_KEY = "lojapp_summary_custom_text";

function getSavedSummaryTemplate(): SummaryTemplate {
  if (typeof localStorage === "undefined") return "direct";
  const raw = localStorage.getItem(SUMMARY_TEMPLATE_STORAGE_KEY);
  if (raw === "direct" || raw === "executive" || raw === "whatsapp") return raw;
  return "direct";
}

function getSavedCustomSummaryText(): string {
  if (typeof localStorage === "undefined") return "";
  return localStorage.getItem(SUMMARY_CUSTOM_TEXT_STORAGE_KEY) ?? "";
}

function mapApiProductToStorefront(
  product: Awaited<ReturnType<typeof listProducts>>["content"][number],
) {
  return {
    id: String(product.id),
    slug: `produto-${product.id}`,
    name: product.name,
    brand: product.brandName,
    category: "Catálogo",
    price: product.salePrice,
    previousPrice: undefined,
    rating: 4.7,
    reviews: 24,
    stock: product.minimumStock,
    description: `EAN ${product.ean ?? "não informado"} · SKU ${product.sku ?? "não informado"}`,
  };
}

function useStorefrontCatalog() {
  const token = useAuthStore((state) => state.accessToken);
  const productsQ = useQuery({
    queryKey: ["storefront", "products", "top-24"],
    queryFn: () => listProducts({ page: 0, size: 24 }),
    enabled: Boolean(token),
  });

  const apiProducts = productsQ.data?.content?.map(mapApiProductToStorefront) ?? [];
  const products = apiProducts.length > 0 ? apiProducts : storefrontProducts;

  return {
    products,
    usingApiData: apiProducts.length > 0,
    isLoading: productsQ.isPending,
    isFetching: productsQ.isFetching,
    error: productsQ.error,
  };
}

function StoreHeader() {
  const cartCount = useCartStore((state) =>
    state.items.reduce((acc, item) => acc + item.quantity, 0),
  );
  return (
    <header className="store-topbar">
      <div className="store-shell store-topbar-content">
        <Link to="/" className="store-logo">
          <span aria-hidden>L</span>
          <strong>{BRAND_NAME}</strong>
        </Link>
        <nav className="store-nav">
          <Link to="/home">Home</Link>
          <Link to="/catalog">Catálogo</Link>
          <Link to="/orders">Pedidos</Link>
          <Link to="/seller">Área Lojista</Link>
          <Link to="/pitch">Pitch</Link>
          <Link to="/cart">Carrinho ({cartCount})</Link>
        </nav>
      </div>
    </header>
  );
}

export function LandingPage() {
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-hero" aria-labelledby="landing-hero-heading">
          <p className="store-chip">
            +{socialProof.stores} lojas em piloto · confiança em evolução
          </p>
          <h1 id="landing-hero-heading">
            Operação comercial e fiscal alinhadas — sem folhas de cálculo soltas.
          </h1>
          <p className="store-hero-lead">
            <strong>{BRAND_NAME}</strong> — {BRAND_TAGLINE.toLowerCase()}. Catálogo e carrinho para
            experimentar hoje; painel com stock, NFe, vendas e indicadores quando estiver pronto a
            operar a sério.
          </p>
          <div className="store-cta-row">
            <Link to="/catalog" className="primary store-cta">
              Explorar catálogo
            </Link>
            <Link to="/login" className="ghost store-cta">
              Entrar no painel
            </Link>
            <Link to="/pitch" className="ghost store-cta">
              Ver demonstração
            </Link>
          </div>
        </section>

        <section className="store-landing-section" aria-labelledby="landing-why-heading">
          <h2 id="landing-why-heading" className="store-landing-section-title">
            Porquê equipas de loja escolhem este fluxo
          </h2>
          <div className="store-grid store-landing-benefits">
            <article className="store-card store-benefit-card">
              <h3>Fiscal com rastreio</h3>
              <p>
                Importação de NFe em XML, entradas deduplicadas e ligação ao catálogo — menos
                retrabalho entre armazém e contabilidade.
              </p>
            </article>
            <article className="store-card store-benefit-card">
              <h3>Stock e vendas coerentes</h3>
              <p>
                Cada venda atualiza saldos e histórico; alertas de stock baixo ajudam a repor antes
                de ruturas.
              </p>
            </article>
            <article className="store-card store-benefit-card">
              <h3>Indicadores por marca</h3>
              <p>
                Dashboard com KPIs, curva ABC e visão de inventário — decisões com números frescos
                da API, não de cópias estáticas.
              </p>
            </article>
          </div>
        </section>

        <section
          className="store-landing-section store-landing-cta-band"
          aria-labelledby="landing-cta-heading"
        >
          <div className="store-card store-landing-cta-card">
            <h2 id="landing-cta-heading" className="store-landing-cta-title">
              Pronto para o próximo passo?
            </h2>
            <p className="store-muted store-landing-cta-lead">
              Use o modo demonstração do storefront ou avance diretamente para o painel com a sua
              conta.
            </p>
            <div className="store-cta-row store-landing-cta-row">
              <Link to="/seller" className="primary store-cta">
                Área lojista (demo)
              </Link>
              <Link to="/orders" className="ghost store-cta">
                Analisar pedidos
              </Link>
            </div>
          </div>
        </section>

        <footer className="store-landing-footer">
          <p className="store-muted small store-landing-footer-inner">
            <strong>{BRAND_NAME}</strong>
            {" · "}
            <Link to="/catalog">Catálogo</Link>
            {" · "}
            <Link to="/pitch">Pitch</Link>
            {" · "}
            <Link to="/login">Iniciar sessão</Link>
          </p>
        </footer>
      </main>
    </div>
  );
}

export function HomePage() {
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-card">
          <h2>Home premium da loja</h2>
          <p>Curadoria inteligente, visual consistente e foco em conversão.</p>
          <div className="store-kpis">
            <article>
              <strong>{socialProof.orders.toLocaleString("pt-PT")}+</strong>
              <span>pedidos processados</span>
            </article>
            <article>
              <strong>{socialProof.averageRating.toFixed(1)}/5</strong>
              <span>avaliação média</span>
            </article>
            <article>
              <strong>Entrega 24h</strong>
              <span>nas capitais</span>
            </article>
          </div>
        </section>
      </main>
    </div>
  );
}

export function CatalogPage() {
  const addItem = useCartStore((state) => state.addItem);
  const { products, usingApiData, isLoading, isFetching, error } = useStorefrontCatalog();
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <div className="store-data-pill-wrap">
          <span className="store-chip">
            {usingApiData ? "Catálogo com dados reais da API" : "Catálogo em modo demo"}
          </span>
          {isFetching ? <span className="store-muted small">A atualizar dados…</span> : null}
        </div>
        {error ? <p className="error banner">{String(error)}</p> : null}
        {isLoading ? (
          <StoreCatalogGridSkeleton />
        ) : (
          <section className="store-grid">
            {products.map((product) => (
              <article key={product.id} className="store-product-card">
                <p className="store-muted">{product.brand}</p>
                <h3>{product.name}</h3>
                <p className="store-muted">{product.description}</p>
                <div className="store-product-foot">
                  <strong>{formatCurrency(product.price)}</strong>
                  <button
                    type="button"
                    className="ghost"
                    onClick={() =>
                      addItem({ id: product.id, name: product.name, price: product.price })
                    }
                  >
                    Adicionar
                  </button>
                  <Link to={`/product/${product.slug}`} className="store-link">
                    Ver produto
                  </Link>
                </div>
              </article>
            ))}
          </section>
        )}
      </main>
    </div>
  );
}

export function ProductPage() {
  const params = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const addItem = useCartStore((state) => state.addItem);
  const { products } = useStorefrontCatalog();
  const product = params.slug
    ? (products.find((item) => item.slug === params.slug) ?? getProductBySlug(params.slug))
    : null;
  if (!product) return <Navigate to="/catalog" replace />;
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <article className="store-card">
          <p className="store-chip">{product.category}</p>
          <h2>{product.name}</h2>
          <p>{product.description}</p>
          <p className="store-price-xl">{formatCurrency(product.price)}</p>
          <div className="store-cta-row">
            <button
              type="button"
              className="primary"
              onClick={() => {
                addItem({ id: product.id, name: product.name, price: product.price });
                navigate("/cart");
              }}
            >
              Comprar agora
            </button>
            <button
              type="button"
              className="ghost"
              onClick={() => addItem({ id: product.id, name: product.name, price: product.price })}
            >
              Adicionar ao carrinho
            </button>
          </div>
        </article>
      </main>
    </div>
  );
}

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
              Aprovação de manager
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
                <span>aprovação manager</span>
              </article>
            </div>
          ) : null}
        </section>
      </main>
    </div>
  );
}

export function OrdersPage() {
  const {
    from,
    setFrom,
    to,
    setTo,
    productId,
    setProductId,
    brandId,
    setBrandId,
    page,
    setPage,
    presetName,
    setPresetName,
    filterPresets,
    setFilterPresets,
    defaultPresetId,
    setDefaultPresetId,
    editingPresetId,
    setEditingPresetId,
    editingPresetName,
    setEditingPresetName,
    ordersSortKey,
    setOrdersSortKey,
    ordersSortDir,
    setOrdersSortDir,
    parsedProductId,
    parsedBrandId,
    previousRange,
    applyQuickRange,
  } = useStorefrontOrdersFilters();

  const salesQ = useQuery({
    queryKey: [
      "storefront",
      "orders",
      { page, from, to, productId: parsedProductId, brandId: parsedBrandId },
    ],
    queryFn: () =>
      listSales({
        page,
        size: 25,
        from: from || undefined,
        to: to || undefined,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
  });
  const brandsQ = useQuery({
    queryKey: ["storefront", "brands"],
    queryFn: listBrands,
  });
  const summaryQ = useQuery({
    queryKey: [
      "storefront",
      "orders-summary",
      { from, to, productId: parsedProductId, brandId: parsedBrandId },
    ],
    queryFn: () =>
      summarizeSales({
        from: from || undefined,
        to: to || undefined,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
  });
  const dailyQ = useQuery({
    queryKey: [
      "storefront",
      "orders-daily",
      { from, to, productId: parsedProductId, brandId: parsedBrandId },
    ],
    queryFn: () =>
      summarizeSalesDaily({
        from: from || undefined,
        to: to || undefined,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
  });
  const previousSummaryQ = useQuery({
    queryKey: [
      "storefront",
      "orders-summary-previous",
      {
        from: previousRange?.from,
        to: previousRange?.to,
        productId: parsedProductId,
        brandId: parsedBrandId,
      },
    ],
    queryFn: () =>
      summarizeSales({
        from: previousRange?.from,
        to: previousRange?.to,
        productId: parsedProductId,
        brandId: parsedBrandId,
      }),
    enabled: previousRange != null,
  });

  const rows = useMemo(
    () => sortSaleRows(salesQ.data?.content ?? [], ordersSortKey, ordersSortDir),
    [salesQ.data, ordersSortKey, ordersSortDir],
  );
  const summaryPreview = buildSummaryText();
  const [customSummaryText, setCustomSummaryText] = useState(() => getSavedCustomSummaryText());
  const [isSummaryDirty, setIsSummaryDirty] = useState(false);
  const [summaryTemplate, setSummaryTemplate] = useState<SummaryTemplate>(() =>
    getSavedSummaryTemplate(),
  );

  useEffect(() => {
    if (!summaryPreview) {
      setCustomSummaryText("");
      setIsSummaryDirty(false);
      return;
    }
    if (!isSummaryDirty) {
      setCustomSummaryText(summaryPreview);
    }
  }, [summaryPreview, isSummaryDirty]);

  useEffect(() => {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(SUMMARY_TEMPLATE_STORAGE_KEY, summaryTemplate);
  }, [summaryTemplate]);

  useEffect(() => {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(SUMMARY_CUSTOM_TEXT_STORAGE_KEY, customSummaryText);
  }, [customSummaryText]);

  function exportCsv() {
    const header = ["pedido", "produto", "marca", "quantidade", "total", "data"];
    const lines = rows.map((row) =>
      [
        row.id,
        row.productName,
        row.brandName,
        row.quantity,
        Number(row.unitPrice) * Number(row.quantity),
        new Date(row.soldAt).toISOString(),
      ]
        .map(csvEscape)
        .join(","),
    );
    const summaryLines =
      summaryQ.data && previousSummaryQ.data
        ? [
            "",
            "resumo,valor",
            `faturamento_atual,${csvEscape(summaryQ.data.revenue)}`,
            `unidades_atuais,${csvEscape(summaryQ.data.unitsSold)}`,
            `ticket_medio_atual,${csvEscape(summaryQ.data.averageTicket)}`,
            `variacao_faturamento_percentual,${csvEscape(percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue).toFixed(2))}`,
            `variacao_unidades_percentual,${csvEscape(percentDelta(summaryQ.data.unitsSold, previousSummaryQ.data.unitsSold).toFixed(2))}`,
            `variacao_ticket_medio_percentual,${csvEscape(percentDelta(summaryQ.data.averageTicket, previousSummaryQ.data.averageTicket).toFixed(2))}`,
          ]
        : [];

    const csv = [header.join(","), ...lines, ...summaryLines].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `lojapp-pedidos-${from || "inicio"}-${to || "agora"}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }

  function exportDailyCsv() {
    const header = ["data", "faturamento", "unidades_vendidas"];
    const lines = (dailyQ.data ?? []).map((point) =>
      [point.date, Number(point.revenue), Number(point.unitsSold)].map(csvEscape).join(","),
    );
    const csv = [header.join(","), ...lines].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `lojapp-evolucao-diaria-${from || "inicio"}-${to || "agora"}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }

  async function copySummary() {
    const summary = customSummaryText.trim() || summaryPreview;
    if (!summary) return;

    try {
      await navigator.clipboard.writeText(summary);
      toast.success("Resumo copiado para a área de transferência.");
    } catch {
      toast.error("Não foi possível copiar o resumo.");
    }
  }

  function buildSummaryText(template: SummaryTemplate = summaryTemplate) {
    if (!summaryQ.data) return null;
    const periodLabel = `${from || "início"} até ${to || "agora"}`;
    const revenueText = formatCurrency(summaryQ.data.revenue);
    const unitsText = Number(summaryQ.data.unitsSold).toLocaleString("pt-BR");
    const ticketText = formatCurrency(summaryQ.data.averageTicket);
    const varianceTextRaw =
      previousSummaryQ.data && previousRange
        ? `\nVariação vs período anterior (${previousRange.from} a ${previousRange.to}):\n- Faturamento: ${formatPercent(percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue))}\n- Unidades: ${formatPercent(percentDelta(summaryQ.data.unitsSold, previousSummaryQ.data.unitsSold))}\n- Ticket médio: ${formatPercent(percentDelta(summaryQ.data.averageTicket, previousSummaryQ.data.averageTicket))}`
        : "";
    const varianceInline =
      previousSummaryQ.data && previousRange
        ? ` | Var. fat.: ${formatPercent(percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue))}`
        : "";

    if (template === "executive") {
      return `Resumo Executivo - LojApp\nPeríodo analisado: ${periodLabel}\nIndicadores principais:\n- Faturamento consolidado: ${revenueText}\n- Unidades vendidas: ${unitsText}\n- Ticket médio: ${ticketText}${varianceTextRaw}\nRecomendação: manter acompanhamento diário e atuar em produtos com maior tração.`;
    }
    if (template === "whatsapp") {
      return `LojApp | ${periodLabel}\nFaturamento: ${revenueText}\nUnidades: ${unitsText}\nTicket: ${ticketText}${varianceInline}`;
    }
    return `Resumo comercial LojApp\nPeríodo: ${periodLabel}\n- Faturamento: ${revenueText}\n- Unidades vendidas: ${unitsText}\n- Ticket médio: ${ticketText}${varianceTextRaw}`;
  }

  function exportPresetsJson() {
    const payload = {
      exportedAt: new Date().toISOString(),
      presets: filterPresets,
      defaultPresetId,
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], {
      type: "application/json;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "lojapp-order-presets.json";
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }

  function importPresetsJson(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      try {
        const parsed = JSON.parse(String(reader.result ?? "{}"));
        const result = parseOrderPresetsImport(parsed);
        if (!result.ok) {
          toast.error("Arquivo inválido: presets não encontrados.");
          return;
        }
        setFilterPresets(result.presets);
        setDefaultPresetId(result.defaultPresetId);
        toast.success("Presets importados com sucesso.");
      } catch {
        toast.error("Não foi possível ler o JSON de presets.");
      } finally {
        event.target.value = "";
      }
    };
    reader.readAsText(file);
  }

  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-card">
          <h2>Histórico de pedidos</h2>
          <p className="store-muted">Últimas vendas registadas no backend do LojApp.</p>
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              setPage(0);
              void salesQ.refetch();
            }}
          >
            <div className="store-cta-row">
              <button type="button" className="ghost" onClick={() => applyQuickRange(7)}>
                7d
              </button>
              <button type="button" className="ghost" onClick={() => applyQuickRange(30)}>
                30d
              </button>
              <button type="button" className="ghost" onClick={() => applyQuickRange(90)}>
                90d
              </button>
            </div>
            <div className="field-row">
              <label>
                Ordenar por
                <select
                  value={ordersSortKey}
                  onChange={(event) => setOrdersSortKey(event.target.value as OrdersSortKey)}
                >
                  <option value="soldAt">Data</option>
                  <option value="total">Total</option>
                  <option value="quantity">Quantidade</option>
                </select>
              </label>
              <label>
                Direção
                <select
                  value={ordersSortDir}
                  onChange={(event) => setOrdersSortDir(event.target.value as "asc" | "desc")}
                >
                  <option value="desc">Descendente</option>
                  <option value="asc">Ascendente</option>
                </select>
              </label>
              <label>
                Template do resumo
                <select
                  value={summaryTemplate}
                  onChange={(event) => {
                    const nextTemplate = event.target.value as SummaryTemplate;
                    setSummaryTemplate(nextTemplate);
                    setCustomSummaryText(buildSummaryText(nextTemplate) ?? "");
                    setIsSummaryDirty(false);
                  }}
                >
                  <option value="direct">Direto</option>
                  <option value="executive">Executivo</option>
                  <option value="whatsapp">WhatsApp curto</option>
                </select>
              </label>
              <label>
                De
                <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
              </label>
              <label>
                Até
                <input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
              </label>
              <label>
                Product ID
                <input
                  value={productId}
                  onChange={(event) => setProductId(event.target.value)}
                  placeholder="ex.: 12"
                />
              </label>
              <label>
                Marca
                <select value={brandId} onChange={(event) => setBrandId(event.target.value)}>
                  <option value="">Todas</option>
                  {(brandsQ.data ?? []).map((brand) => (
                    <option key={brand.id} value={String(brand.id)}>
                      {brand.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Nome do preset
                <input
                  value={presetName}
                  onChange={(event) => setPresetName(event.target.value)}
                  placeholder="ex.: Minha visão semanal"
                />
              </label>
            </div>
            <div className="store-cta-row">
              <button type="submit" className="primary" disabled={salesQ.isFetching}>
                Aplicar filtros
              </button>
              <button
                type="button"
                className="ghost"
                onClick={exportCsv}
                disabled={rows.length === 0}
              >
                Exportar CSV
              </button>
              <button
                type="button"
                className="ghost"
                onClick={exportDailyCsv}
                disabled={!dailyQ.data || dailyQ.data.length === 0}
              >
                Exportar CSV diário
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  void copySummary();
                }}
                disabled={!summaryQ.data}
              >
                Copiar resumo
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  const summary = customSummaryText.trim() || summaryPreview;
                  if (!summary) return;
                  const url = `https://wa.me/?text=${encodeURIComponent(summary)}`;
                  window.open(url, "_blank", "noopener,noreferrer");
                }}
                disabled={!summaryQ.data}
              >
                Enviar para WhatsApp
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  setFrom("");
                  setTo("");
                  setProductId("");
                  setBrandId("");
                  setPage(0);
                }}
              >
                Limpar
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  const name = presetName.trim();
                  if (!name) {
                    toast.error("Informe um nome para o preset.");
                    return;
                  }
                  const nextPreset: OrdersFilterPreset = {
                    id: `${Date.now()}`,
                    name,
                    filters: { from, to, productId, brandId },
                  };
                  setFilterPresets((current) => [nextPreset, ...current].slice(0, 8));
                  setPresetName("");
                  toast.success("Preset de filtro salvo.");
                }}
              >
                Salvar preset
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  setSummaryTemplate("direct");
                  setCustomSummaryText("");
                  setIsSummaryDirty(false);
                  if (typeof localStorage !== "undefined") {
                    localStorage.removeItem(SUMMARY_TEMPLATE_STORAGE_KEY);
                    localStorage.removeItem(SUMMARY_CUSTOM_TEXT_STORAGE_KEY);
                  }
                  toast.success("Preferências de resumo limpas.");
                }}
              >
                Limpar preferências
              </button>
              <button
                type="button"
                className="ghost"
                onClick={exportPresetsJson}
                disabled={filterPresets.length === 0}
              >
                Exportar presets
              </button>
              <label className="ghost" style={{ display: "inline-flex", alignItems: "center" }}>
                Importar presets
                <input
                  type="file"
                  accept="application/json"
                  onChange={importPresetsJson}
                  style={{ display: "none" }}
                />
              </label>
            </div>
            {filterPresets.length > 0 ? (
              <div className="store-cta-row">
                {filterPresets.map((preset, index) => (
                  <div key={preset.id} className="store-cta-row">
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        setFrom(preset.filters.from);
                        setTo(preset.filters.to);
                        setProductId(preset.filters.productId);
                        setBrandId(preset.filters.brandId);
                        setPage(0);
                        toast.success(`Preset "${preset.name}" aplicado.`);
                      }}
                      title={preset.name}
                    >
                      {preset.name}
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        setEditingPresetId(preset.id);
                        setEditingPresetName(preset.name);
                      }}
                      title={`Renomear ${preset.name}`}
                    >
                      Renomear
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      disabled={index === 0}
                      onClick={() => {
                        if (index === 0) return;
                        setFilterPresets((current) => {
                          const next = [...current];
                          const previousIndex = index - 1;
                          [next[previousIndex], next[index]] = [next[index], next[previousIndex]];
                          return next;
                        });
                      }}
                      title={`Mover ${preset.name} para cima`}
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      disabled={index === filterPresets.length - 1}
                      onClick={() => {
                        if (index === filterPresets.length - 1) return;
                        setFilterPresets((current) => {
                          const next = [...current];
                          const nextIndex = index + 1;
                          [next[nextIndex], next[index]] = [next[index], next[nextIndex]];
                          return next;
                        });
                      }}
                      title={`Mover ${preset.name} para baixo`}
                    >
                      ↓
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        if (defaultPresetId === preset.id) {
                          setDefaultPresetId(null);
                          toast.success(`Preset "${preset.name}" removido como padrão.`);
                          return;
                        }
                        setDefaultPresetId(preset.id);
                        toast.success(`Preset "${preset.name}" definido como padrão.`);
                      }}
                      title={
                        defaultPresetId === preset.id
                          ? `Remover padrão de ${preset.name}`
                          : `Definir ${preset.name} como padrão`
                      }
                    >
                      {defaultPresetId === preset.id ? "Padrão ✓" : "Definir padrão"}
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        setFilterPresets((current) =>
                          current.filter((entry) => entry.id !== preset.id),
                        );
                        if (defaultPresetId === preset.id) {
                          setDefaultPresetId(null);
                        }
                        toast.success(`Preset "${preset.name}" removido.`);
                      }}
                      title={`Remover ${preset.name}`}
                    >
                      Remover
                    </button>
                  </div>
                ))}
              </div>
            ) : null}
            {editingPresetId ? (
              <div className="store-cta-row">
                <input
                  value={editingPresetName}
                  onChange={(event) => setEditingPresetName(event.target.value)}
                  placeholder="Novo nome do preset"
                />
                <button
                  type="button"
                  className="ghost"
                  onClick={() => {
                    const normalized = editingPresetName.trim();
                    if (!normalized) {
                      toast.error("Informe um nome válido para renomear.");
                      return;
                    }
                    setFilterPresets((current) =>
                      current.map((entry) =>
                        entry.id === editingPresetId ? { ...entry, name: normalized } : entry,
                      ),
                    );
                    setEditingPresetId(null);
                    setEditingPresetName("");
                    toast.success("Preset renomeado.");
                  }}
                >
                  Confirmar renomeação
                </button>
                <button
                  type="button"
                  className="ghost"
                  onClick={() => {
                    setEditingPresetId(null);
                    setEditingPresetName("");
                  }}
                >
                  Cancelar
                </button>
              </div>
            ) : null}
          </form>
          {salesQ.isPending ? (
            <TableSkeleton rows={4} label="A carregar pedidos" className="store-orders-skel" />
          ) : null}
          {salesQ.error ? <p className="error banner">{String(salesQ.error)}</p> : null}
          {summaryQ.data ? (
            <div className="store-kpis">
              <article>
                <strong>{formatCurrency(summaryQ.data.revenue)}</strong>
                <span>faturamento global do período</span>
              </article>
              <article>
                <strong>{summaryQ.data.unitsSold}</strong>
                <span>itens vendidos no período</span>
              </article>
              <article>
                <strong>{formatCurrency(summaryQ.data.averageTicket)}</strong>
                <span>ticket médio por pedido</span>
              </article>
            </div>
          ) : null}
          {summaryQ.data && previousSummaryQ.data && previousRange ? (
            <div className="store-kpis">
              <article>
                <strong>
                  {percentDelta(summaryQ.data.revenue, previousSummaryQ.data.revenue).toFixed(1)}%
                </strong>
                <span>
                  variação do faturamento vs período anterior ({previousRange.from} a{" "}
                  {previousRange.to})
                </span>
              </article>
              <article>
                <strong>
                  {percentDelta(summaryQ.data.unitsSold, previousSummaryQ.data.unitsSold).toFixed(
                    1,
                  )}
                  %
                </strong>
                <span>variação de unidades vendidas</span>
              </article>
              <article>
                <strong>
                  {percentDelta(
                    summaryQ.data.averageTicket,
                    previousSummaryQ.data.averageTicket,
                  ).toFixed(1)}
                  %
                </strong>
                <span>variação de ticket médio</span>
              </article>
            </div>
          ) : null}
          {summaryPreview ? (
            <div className="store-card" style={{ marginTop: "0.75rem" }}>
              <h3 style={{ marginTop: 0 }}>Preview do resumo</h3>
              <textarea
                value={customSummaryText}
                onChange={(event) => {
                  setCustomSummaryText(event.target.value);
                  setIsSummaryDirty(true);
                }}
                rows={9}
                style={{ marginBottom: "0.6rem" }}
              />
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  if (!summaryPreview) return;
                  setCustomSummaryText(summaryPreview);
                  setIsSummaryDirty(false);
                }}
              >
                Restaurar texto automático
              </button>
            </div>
          ) : null}
          {rows.length === 0 && !salesQ.isPending ? (
            <p className="store-muted">Ainda não existem pedidos.</p>
          ) : null}
          {rows.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Pedido</th>
                    <th>Produto</th>
                    <th>Marca</th>
                    <th>Qtd</th>
                    <th>Total</th>
                    <th>Data</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.id}>
                      <td>#{row.id}</td>
                      <td>{row.productName}</td>
                      <td>{row.brandName}</td>
                      <td>{row.quantity}</td>
                      <td>{formatCurrency(row.unitPrice * row.quantity)}</td>
                      <td>{new Date(row.soldAt).toLocaleString("pt-BR")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          {dailyQ.data && dailyQ.data.length > 0 ? (
            <div className="chart-row chart-row-single">
              <div className="chart-card chart-surface-enter">
                <h3 className="chart-title">Evolução diária de vendas</h3>
                <p className="chart-subtitle muted small">
                  Faturamento diário dentro do filtro atual.
                </p>
                <div className="chart-inner">
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart
                      data={dailyQ.data.map((point) => ({
                        date: new Date(point.date).toLocaleDateString("pt-BR"),
                        revenue: Number(point.revenue),
                        unitsSold: Number(point.unitsSold),
                      }))}
                      margin={{ top: 8, right: 8, left: 0, bottom: 4 }}
                    >
                      <defs>
                        <linearGradient id="salesDailyGradient" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="var(--chart-blue)" stopOpacity={0.35} />
                          <stop offset="100%" stopColor="var(--chart-blue)" stopOpacity={0.02} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="var(--chart-grid)"
                        vertical={false}
                      />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 11, fill: "var(--muted)" }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        yAxisId="left"
                        tick={{ fontSize: 11, fill: "var(--muted)" }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        yAxisId="right"
                        orientation="right"
                        tick={{ fontSize: 11, fill: "var(--muted)" }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip
                        formatter={(value, name) =>
                          name === "Unidades"
                            ? Number(value).toLocaleString("pt-BR")
                            : formatCurrency(Number(value))
                        }
                        labelStyle={{ fontWeight: 600 }}
                      />
                      <Legend wrapperStyle={{ fontSize: 12 }} />
                      <Area
                        yAxisId="left"
                        type="monotone"
                        dataKey="revenue"
                        name="Faturamento"
                        stroke="var(--chart-blue)"
                        strokeWidth={2}
                        fill="url(#salesDailyGradient)"
                      />
                      <Line
                        yAxisId="right"
                        type="monotone"
                        dataKey="unitsSold"
                        name="Unidades"
                        stroke="var(--chart-green)"
                        strokeWidth={2}
                        dot={false}
                      />
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>
          ) : null}
          {salesQ.data ? (
            <div className="pager">
              <button
                type="button"
                className="ghost"
                disabled={salesQ.data.first || salesQ.isFetching}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Anterior
              </button>
              <button
                type="button"
                className="ghost"
                disabled={salesQ.data.last || salesQ.isFetching}
                onClick={() => setPage((current) => current + 1)}
              >
                Seguinte
              </button>
            </div>
          ) : null}
        </section>
      </main>
    </div>
  );
}

export function PitchPage() {
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-card">
          <h2>Narrativa comercial</h2>
          <ul className="store-list">
            <li>
              Problema: operação fragmentada entre planilhas, ERP pesado e baixa visibilidade de
              margem.
            </li>
            <li>
              Solução: LojApp unifica catálogo, pedido, estoque e fiscal em uma experiência direta.
            </li>
            <li>
              Diferencial: implantação rápida com interface premium e foco em conversão desde o MVP.
            </li>
            <li>
              Resultado: mais velocidade de venda e decisão por dados com menor custo de operação.
            </li>
          </ul>
        </section>
      </main>
    </div>
  );
}
