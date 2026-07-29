import { StoreHeader } from "./storefrontShared";

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
