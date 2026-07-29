import { Link } from "react-router-dom";
import { useCartStore } from "../../features/storefront";
import { StoreCatalogGridSkeleton } from "../../components/ui/StoreCatalogGridSkeleton";
import { StoreHeader, formatCurrency, useStorefrontCatalog } from "./storefrontShared";

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

