import { Link } from "react-router-dom";
import { BRAND_NAME } from "../../brand";
import { StoreHeader } from "./storefrontShared";

const REPO_URL = "https://github.com/HelderAbud/Sistema-Loja";

export function PitchPage() {
  return (
    <div className="store-bg">
      <StoreHeader />
      <main className="store-shell">
        <section className="store-card">
          <p className="store-chip">Pitch técnico · produto real do repositório</p>
          <h2>O que o {BRAND_NAME} faz hoje</h2>
          <p className="store-muted">
            Lojas pequenas ainda dependem de planilhas: stock errado, nota à mão e pouca visão de
            margem. O {BRAND_NAME} é um monólito Spring Boot + React em que a NFe entra por XML, o
            stock atualiza, a venda baixa saldo e o dashboard mostra KPIs — tudo isolado por conta
            (1 login = 1 loja).
          </p>
        </section>

        <section className="store-card store-landing-section">
          <h2>No painel autenticado</h2>
          <ul className="store-list">
            <li>Catálogo: marcas, produtos, fornecedores e modelos.</li>
            <li>Importação de NFe em XML, com deduplicação de entradas.</li>
            <li>Stock com movimentos e alertas de mínimo.</li>
            <li>PDV: turno de caixa aberto, venda e histórico.</li>
            <li>Comissões por vendedora (relatório e CSV).</li>
            <li>KPIs, curva ABC e indicadores por marca.</li>
            <li>Autenticação JWT; cada conta só vê os seus dados.</li>
          </ul>
        </section>

        <section className="store-card store-landing-section">
          <h2>Como está construído</h2>
          <ul className="store-list">
            <li>Java 21, Spring Boot, PostgreSQL, Flyway.</li>
            <li>Frontend React com TanStack Query.</li>
            <li>
              CI no GitHub: testes unitários, integração com Testcontainers, ArchUnit e E2E
              Playwright.
            </li>
            <li>
              Evidência de risco: concorrência de stock, isolamento entre lojas, idempotência em
              APIs críticas.
            </li>
          </ul>
          <p className="store-muted small">
            Isto é um MVP demonstrável, não um SaaS com dezenas de lojas em produção. Números de
            “lojas ativas” não são inventados nesta página.
          </p>
        </section>

        <div className="store-cta-row store-landing-cta-row">
          <Link to="/login" className="primary store-cta">
            Entrar no painel
          </Link>
          <a className="ghost store-cta" href={REPO_URL} target="_blank" rel="noreferrer">
            Ver repositório
          </a>
        </div>
      </main>
    </div>
  );
}
