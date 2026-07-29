# LinkedIn — Dia 17 (rascunho LojApp)

**Uso:** copiar o bloco abaixo para o LinkedIn. Anexar mídia manualmente.  
**Não** contém passwords nem dados reais de clientes.

---

## Texto do post (copiar)

Lojas pequenas ainda vivem de planilha: stock errado, NFe à mão e zero visão de margem no dia a dia.

Construí o **LojApp** — monólito Spring Boot + React onde a **NFe entra por XML**, o **stock atualiza**, a **venda baixa o saldo** e o **dashboard** mostra KPIs e curva ABC, tudo isolado por conta.

Três pontos técnicos que importam no portfólio:

• **NFe → estoque → venda** numa transação com regras de domínio e testes de concorrência (stock não fica negativo sob corrida)  
• **JWT + isolamento multi-loja** (`user_id`): a loja B não lista nem altera o catálogo da loja A — provado com testes de integração  
• **Deploy R$ 0 + CI**: API no Render, front na Vercel, Flyway + Testcontainers no GitHub Actions  

Repo: https://github.com/HelderAbud/Sistema-Loja  
Demo: https://sistema-loja-psi.vercel.app  
Health API: https://lojapp-api.onrender.com/actuator/health  

(Nota: free tier pode “acordar” a API em alguns segundos no primeiro hit.)

#Java #SpringBoot #React #PostgreSQL #Flyway #JWT #Portfolio

---

## Mídia a anexar

| Preferência | Ficheiro | Nota |
|-------------|----------|------|
| 1º | [`docs/screenshots/07-fluxo-principal.gif`](../screenshots/07-fluxo-principal.gif) | Fluxo login → dashboard → venda (~8 MB) |
| Fallback | [`docs/screenshots/02-dashboard.png`](../screenshots/02-dashboard.png) | Se o LinkedIn recusar o GIF por tamanho |

No LinkedIn: **Adicionar media** → ficheiro local (o GitHub raw do GIF também funciona, mas upload local costuma ser mais estável).

---

## 3 bullets (referência rápida / entrevista)

1. **NFe / transação** — XML importa, stock sobe, venda baixa saldo; evidência: fluxo piloto + `SalesConcurrencyIntegrationTest`.  
2. **JWT / multi-loja** — refresh cookie + CSRF por Origin; isolamento por `user_id` (`CatalogIsolationIntegrationTest`).  
3. **Deploy + CI** — Render + Vercel; CI com unitários, integração Postgres, ArchUnit, Playwright, Trivy.

Pitch completo: [`docs/lojapp/pitch-portfolio.md`](../lojapp/pitch-portfolio.md).

---

## Checklist HITL (Helder)

- [ ] Revisei o texto (tom / hashtags)
- [ ] Anexei GIF ou PNG fallback
- [ ] Cliquei nos 3 links (repo, demo, health) antes de publicar
- [ ] **Publiquei** no LinkedIn (data: ____ / URL do post: ____)

> O agente **não** publica na tua conta. Marca os itens acima depois de publicares.
