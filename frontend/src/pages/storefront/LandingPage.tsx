import { Link } from "react-router-dom";
import { BRAND_NAME, BRAND_TAGLINE } from "../../brand";
import { socialProof } from "../../features/storefront";
import { StoreHeader } from "./storefrontShared";

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

