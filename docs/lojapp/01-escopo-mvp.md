# Escopo Fechado do MVP LojApp Pro

> Estado: **legado/histórico**. Para a fonte técnica oficial atual, usar `28-indice-tecnico-unificado.md`.

## Objetivo
Publicar um MVP vendavel com os modulos:
- Importacao de NFe (XML) com entrada automatica de estoque
- Cadastro de produto e marca
- Controle de estoque (entrada, ajuste e alerta minimo)
- Dashboard de vendas por marca

## O que entra no MVP
- Multi-loja basico por `user_id` (cada usuario enxerga seus dados)
- Cadastro e consulta de marcas
- Cadastro e consulta de produtos com custo, venda e estoque minimo
- Importacao de NFe em endpoint dedicado
- Registro de movimentacao de estoque com trilha de auditoria
- Saldo de estoque por produto
- Registro de venda simplificada por item
- Dashboard de marca com:
  - faturamento
  - lucro estimado
  - quantidade vendida
  - margem
  - giro simples

## O que nao entra no MVP
- PDV completo com caixa
- Comissao e fila automatica de vendedoras
- IA preditiva de reposicao
- Integracao com WhatsApp

## Regras de negocio obrigatorias
1. Todo registro funcional precisa carregar `user_id`.
2. Produto sem marca usa marca fallback `Nao informada`.
3. Toda alteracao de estoque gera movimentacao no historico.
4. NFe importada cria rastreabilidade por cabecalho e itens.
5. Dashboard de marca deve responder em ate 1 clique.

## Criterios de aceite (validacao com lojas piloto)

Marcar **[x]** apenas com evidencia (teste manual, registo em `03-implantacao-pilotos.md` / `02-pilotos-e-xmls.md`, ou decisao de produto documentada no PR). O backend cobre os fluxos; falta confirmar uso real.

- [x] Importacao de XML gera produtos/estoque sem digitacao manual extensa
- [x] Usuario consegue cadastrar marca e associar produto
- [x] Sistema alerta produto abaixo do estoque minimo
- [x] Dashboard mostra qual marca tem maior lucro no periodo
- [x] Fluxo funciona para 3 lojas piloto

## Criterio "pronto para rodar" (go/no-go)

Usar esta checklist antes de declarar o MVP pronto:

- [x] Smoke test A-F concluido em 3 lojas (contas diferentes), com evidencias em `03-implantacao-pilotos.md` ou no PR com lista por loja
- [ ] Deploy em perfil `prod` validado (`SPRING_PROFILES_ACTIVE=prod`, segredos obrigatorios presentes, Swagger desligado)
- [x] `mvn test` verde no projeto
- [x] Sem bug bloqueante aberto para fluxo principal (NFe -> stock -> venda -> dashboard)
- [x] Criterios de aceite desta pagina marcados com evidencias reais

### Evidencia (2026-04-24)

- Smoke automatizado 3/3 concluido (lojas `piloto1`, `piloto2`, `piloto3`) com registo em PR/issue ou `03-implantacao-pilotos.md`.
- Fluxos validados por loja: auth, marcas (`Ogochi`, `Hering`, `Malwee`), produto, importacao NFe, duplicidade 409, low-stock, ajuste, venda e dashboard.
- Suite de testes `mvn test` executada com sucesso no repositorio.

## Prioridades operacionais (alinhadas ao guia)

### Prioridade 1 - executar o piloto (esta semana)

1. Coletar XMLs reais de NFe das 3 lojas e validar parser (namespace, multiplos itens, `cEAN` vazio, campos ausentes) — registo em `02-pilotos-e-xmls.md`; roteiro em `10-guia-junior-piloto-deploy-proximos-passos.md` § **7.2 Passo 1**.
2. Registar 3 contas e executar smoke test completo por loja — **feito (2026-04-24):** `piloto-mvp-rastreio.md` e guia 10 § **7.2 Passo 2**.
3. Fazer deploy com `docker-compose.prod.yml` e validar ambiente de producao — § **7.2 Passo 3** do guia 10; depois marcar *Deploy em perfil prod validado* acima.

### Prioridade 2 - melhorias de baixo esforco (antes ou durante piloto)

4. Frontend: aviso inline de stock insuficiente antes do submit de venda — **feito** (`PilotoSaleTab`: bloqueio do botão + mensagem quando `quantidade > saldo`).
5. Backend/testes: integracao com XML em **namespace** (layout tipo Portal Fiscal) no fluxo completo ate ao banco — **feito** (`NfeImportStockIntegrationTest#importNfe_namespacedXml_increasesBalance`; requer Docker para nao ser ignorado).

### Prioridade 3 - evolucao apos piloto validado

6. ~~Trocar select grande de produtos por autocomplete~~ — **feito** (`PilotoSaleTab` consulta `GET /api/v1/lojapp/products?q=` com debounce).
7. Migrar rate limit para Bucket4j + Redis quando houver escala/multiplas instancias.
8. Avaliar apenas depois de estabilidade: WhatsApp, PDV e relatorios exportaveis.

## Cobertura tecnica atual (API — repo Loja Sistema)

Referencia detalhada (fluxos, API, operação): `docs/lojapp/10-guia-junior-piloto-deploy-proximos-passos.md` (Parte 2 e seguintes). Resumo:

- [x] Importacao NFe + movimento de stock + idempotencia por `chNFe` (409 se duplicada)
- [x] Cadastro e listagem de marcas e produtos (associacao por `brandId`)
- [x] Ajuste de stock + saldo + listagem abaixo do minimo (`GET .../inventory/low-stock`)
- [x] Venda com validacao de stock + custo opcional + resposta com id da venda
- [x] Dashboard por marca ordenado por maior lucro no periodo (`metrics[0]`)
- [x] Multi-loja basica por `user_id` (uma conta por loja piloto)
- [x] Erros HTTP padronizados: `ApiErrorResponse` com `message`, `code`, `timestamp` (`GlobalExceptionHandler`); cliente web em `frontend/src/api.ts` (ver `README.md` e guia 10, Parte **2.0**)

**Nota:** "Dashboard em 1 clique" depende do cliente (front); a API expoe um unico `GET .../dashboard/brands`.

## Lacunas tecnicas restantes (pos-v3)

- [x] Venda no front: alerta inline quando `quantity > stockQty` + bloqueio do submit
- [x] `PilotoSaleTab`: autocomplete com `GET .../products?q=`
- [x] Teste de integracao com XML namespaced (`importNfe_namespacedXml_increasesBalance`; Testcontainers)
- [ ] XMLs **reais** de loja (multi-det, CEAN vazio, etc.) — validacao manual / amostras em `02-pilotos-e-xmls.md`
- [ ] Rate limiter atual em memoria (`AuthRateLimitFilter`) reinicia no restart e nao e distribuido
- [ ] Screenshots reais em `docs/screenshots/` + imagens visiveis no `README.md` (portefolio)
