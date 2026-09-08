import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { listProducts } from "../../api";
import { useAuthStore } from "../../authStore";
import { BRAND_NAME } from "../../brand";
import { formatMoneyBrl } from "../../components/formatMoney";
import { storefrontProducts } from "../../features/storefront";

/** Alias da vitrine: mesma moeda do piloto (BRL / pt-BR). */
export function formatCurrency(value: number) {
  return formatMoneyBrl(value);
}

export function percentDelta(current: number, previous: number) {
  if (previous === 0) return current === 0 ? 0 : 100;
  return ((current - previous) / previous) * 100;
}

export function csvEscape(value: string | number) {
  const text = String(value ?? "");
  if (text.includes(",") || text.includes('"') || text.includes("\n")) {
    return `"${text.replaceAll('"', '""')}"`;
  }
  return text;
}

export function formatPercent(value: number) {
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(1)}%`;
}

export type SummaryTemplate = "direct" | "executive" | "whatsapp";
export const SUMMARY_TEMPLATE_STORAGE_KEY = "lojapp_summary_template";
export const SUMMARY_CUSTOM_TEXT_STORAGE_KEY = "lojapp_summary_custom_text";

export function getSavedSummaryTemplate(): SummaryTemplate {
  if (typeof localStorage === "undefined") return "direct";
  const raw = localStorage.getItem(SUMMARY_TEMPLATE_STORAGE_KEY);
  if (raw === "direct" || raw === "executive" || raw === "whatsapp") return raw;
  return "direct";
}

export function getSavedCustomSummaryText(): string {
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

export function useStorefrontCatalog() {
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

export function StoreHeader() {
  return (
    <header className="store-topbar">
      <div className="store-shell store-topbar-content">
        <Link to="/" className="store-logo">
          <span aria-hidden>L</span>
          <strong>{BRAND_NAME}</strong>
        </Link>
        <nav className="store-nav">
          <Link to="/">Home</Link>
          <Link to="/pitch">Pitch</Link>
          <Link to="/login">Entrar</Link>
        </nav>
      </div>
    </header>
  );
}
