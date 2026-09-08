import { Link } from "react-router-dom";
import { BRAND_NAME } from "../../brand";
import { StoreHeader } from "./storefrontShared";

export function NotFoundPage() {
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-hero" aria-labelledby="not-found-heading">
          <p className="store-chip">Erro 404</p>
          <h1 id="not-found-heading">Esta página não existe</h1>
          <p className="store-hero-lead">
            O endereço não corresponde a nenhuma rota do {BRAND_NAME}. A home, o pitch e o login
            continuam no sítio habitual.
          </p>
          <div className="store-cta-row">
            <Link to="/" className="primary store-cta">
              Ir para a home
            </Link>
            <Link to="/login" className="ghost store-cta">
              Entrar no painel
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}
