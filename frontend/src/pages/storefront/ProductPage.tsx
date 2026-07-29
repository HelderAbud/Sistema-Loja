import { Navigate, useNavigate, useParams } from "react-router-dom";
import { getProductBySlug, useCartStore } from "../../features/storefront";
import { StoreHeader, formatCurrency, useStorefrontCatalog } from "./storefrontShared";

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
