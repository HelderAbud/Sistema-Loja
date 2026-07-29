import { socialProof } from "../../features/storefront";
import { StoreHeader } from "./storefrontShared";

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
