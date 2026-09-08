import { Link } from "react-router-dom";
import { BRAND_NAME, BRAND_TAGLINE } from "../../brand";
import { LANDING_SHOTS } from "./landingShots";
import { StoreHeader } from "./storefrontShared";

const REPO_URL = "https://github.com/HelderAbud/Sistema-Loja";

const FEATURES = [
  {
    title: "Importação de NFe por XML",
    body: "A nota entra no sistema; entradas são deduplicadas e ligadas ao catálogo da loja.",
  },
  {
    title: "Stock com baixa na venda",
    body: "Cada venda no PDV atualiza saldo e histórico. Alertas de mínimo ajudam a repor antes da rutura.",
  },
  {
    title: "PDV com turno de caixa",
    body: "Abrir turno, vender e fechar caixa no painel autenticado — não num carrinho público.",
  },
  {
    title: "Comissão de vendedora",
    body: "Relatório e CSV no painel, com vendedora associada à venda quando fizer sentido.",
  },
  {
    title: "Dashboard, KPIs e curva ABC",
    body: "Indicadores por marca e visão de inventário a partir da API, não de planilha copiada.",
  },
  {
    title: "1 conta = 1 loja",
    body: "Dados isolados por utilizador. Outra conta não vê o teu catálogo nem as tuas vendas.",
  },
  {
    title: "JWT com refresh",
    body: "Sessão no painel com token e renovação; a área operacional fica atrás de login.",
  },
] as const;

const STACK_BADGES = ["Java 21", "Spring Boot", "PostgreSQL", "React 19", "JWT", "CI/CD"] as const;

export function LandingPage() {
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-hero" aria-labelledby="landing-hero-heading">
          <p className="store-chip">MVP em demonstração pública · 1 conta = 1 loja</p>
          <h1 id="landing-hero-heading">
            Loja pequena ainda vive de planilha. O {BRAND_NAME} junta stock, NFe e venda no mesmo
            painel.
          </h1>
          <p className="store-hero-lead">
            Problema: stock errado, nota à mão e zero visão de margem no dia a dia. Solução:{" "}
            <strong>{BRAND_NAME}</strong> — {BRAND_TAGLINE.toLowerCase()}, com operação só depois do
            login.
          </p>
          <div className="store-cta-row">
            <Link to="/login" className="primary store-cta">
              Entrar no painel
            </Link>
            <a href="#como-usar" className="ghost store-cta">
              Ver a demo
            </a>
          </div>
        </section>

        <section className="store-landing-section" aria-labelledby="landing-features-heading">
          <h2 id="landing-features-heading" className="store-landing-section-title">
            O que o sistema faz hoje
          </h2>
          <div className="store-grid store-landing-benefits">
            {FEATURES.map((feature) => (
              <article key={feature.title} className="store-card store-benefit-card">
                <h3>{feature.title}</h3>
                <p>{feature.body}</p>
              </article>
            ))}
          </div>
        </section>

        <section
          id="como-usar"
          className="store-landing-section"
          aria-labelledby="landing-howto-heading"
        >
          <h2 id="landing-howto-heading" className="store-landing-section-title">
            Como testar
          </h2>
          <ol className="store-howto-list">
            <li>
              Abre <Link to="/login">Entrar</Link>. A conta da demo pública não está no repositório
              — usa a credencial do ambiente ou cria conta se o registo estiver ligado.
            </li>
            <li>
              Depois do login o painel abre em produtos (
              <code className="store-inline-code">/piloto/products</code>
              ).
            </li>
            <li>
              Percorre dashboard, vendas, stock e importação XML. Venda real só com turno de caixa
              aberto.
            </li>
          </ol>
          <div className="store-shot-grid">
            {LANDING_SHOTS.map((shot) => (
              <figure key={shot.caption} className="store-shot">
                <img src={shot.src} alt={shot.alt} width={1280} height={800} />
                <figcaption>{shot.caption}</figcaption>
              </figure>
            ))}
          </div>
        </section>

        <section className="store-landing-section" aria-labelledby="landing-stack-heading">
          <h2 id="landing-stack-heading" className="store-landing-section-title">
            Stack
          </h2>
          <ul className="store-badge-row">
            {STACK_BADGES.map((badge) => (
              <li key={badge} className="store-chip">
                {badge}
              </li>
            ))}
          </ul>
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
              Entra no painel ou lê o pitch técnico. Não há catálogo nem carrinho na área pública.
            </p>
            <div className="store-cta-row store-landing-cta-row">
              <Link to="/login" className="primary store-cta">
                Iniciar sessão
              </Link>
              <Link to="/pitch" className="ghost store-cta">
                Ver pitch
              </Link>
            </div>
          </div>
        </section>

        <footer className="store-landing-footer">
          <p className="store-muted small store-landing-footer-inner">
            <strong>{BRAND_NAME}</strong>
            {" · "}
            <Link to="/pitch">Pitch</Link>
            {" · "}
            <Link to="/login">Iniciar sessão</Link>
            {" · "}
            <a href={REPO_URL} target="_blank" rel="noreferrer">
              GitHub
            </a>
          </p>
        </footer>
      </main>
    </div>
  );
}
