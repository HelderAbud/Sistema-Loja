# LinkedIn — Dia 17 (rascunho LojApp)

**Uso:** copiar o bloco abaixo para o LinkedIn. Anexar mídia manualmente.  
**Não** contém passwords nem dados reais de clientes.

---

## Texto do post (copiar)

Lojas pequenas ainda vivem de planilha: stock errado, NFe à mão e zero visão de margem no dia a dia.

Construí o **LojApp** — monólito Spring Boot + React onde a **NFe entra por XML**, o **stock atualiza**, a **venda baixa o saldo** e o **dashboard** mostra KPIs e curva ABC.

Isolamento: **1 login = `user_id`**. Cada conta é uma loja completa (stock, caixa, NFe, funções). Dá para ter várias lojas — cada uma com o seu login, sem misturar dados no mesmo acesso.

Três pontos técnicos que importam no portfólio:

• **NFe → estoque → venda** numa transação com regras de domínio e testes de concorrência (stock não fica negativo sob corrida)  
• **1 login (`user_id`)**: a loja B não lista nem altera o catálogo da loja A — provado com testes de integração  
• **Deploy R$ 0 + CI**: API no Railway, front na Vercel, Flyway + Testcontainers no GitHub Actions  

Repo: https://github.com/HelderAbud/Sistema-Loja  
Demo: https://sistema-loja-psi.vercel.app  
Health API: https://sistema-loja-production-7608.up.railway.app/actuator/health  

(Nota: free tier pode “acordar” a API em alguns segundos no primeiro hit.)

#Java #SpringBoot #React #PostgreSQL #Flyway #JWT #Portfolio

---

## Mídia a anexar

Preferir o **carrossel** (`carrossel-lojapp.pptx` exportado em PNG 1080×1350). GIF só como extra, não dentro do carrossel.

| Preferência | Ficheiro | Nota |
|-------------|----------|------|
| 1º | PNG dos 7 slides do carrossel | Exportar do PowerPoint; ver texto do slide 3 abaixo |
| Extra | Screenshot da demo (dashboard ou NFe) | Dados fake; prova que o produto está no ar |
| Opcional | [`docs/screenshots/07-fluxo-principal.gif`](../screenshots/07-fluxo-principal.gif) | Fora do carrossel (LinkedIn recusa GIF grande) |

### Texto do slide 3 (substituir a linha do “multi-tenant”)

**Tirar:** `Cada conta é uma loja isolada (multi-tenant)`

**Colar:** `1 login (user_id): loja completa e isolada. Outra loja = outro login.`

Slide 5, card 02 — manter `user_id`; não usar a palavra multi-tenant.

No LinkedIn: **Adicionar documento** → os PNG na ordem 1–7.

---

## 3 bullets (referência rápida / entrevista)

1. **NFe / transação** — XML importa, stock sobe, venda baixa saldo; evidência: fluxo piloto + `SalesConcurrencyIntegrationTest`.  
2. **1 login (`user_id`)** — cada conta é uma loja isolada (stock, funções, dados); várias lojas = vários logins; evidência `CatalogIsolationIntegrationTest`.  
3. **Deploy + CI** — Railway + Vercel; CI com unitários, integração Postgres, ArchUnit, Playwright, Trivy.

Pitch completo: [`docs/lojapp/pitch-portfolio.md`](../lojapp/pitch-portfolio.md).

---

## Checklist HITL (Helder)

- [ ] Revisei o texto (tom / hashtags)
- [ ] Anexei GIF ou PNG fallback
- [ ] Cliquei nos 3 links (repo, demo, health) antes de publicar
- [ ] **Publiquei** no LinkedIn (data: ____ / URL do post: ____)

> O agente **não** publica na tua conta. Marca os itens acima depois de publicares.
