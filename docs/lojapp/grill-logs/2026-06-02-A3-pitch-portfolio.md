# Grill — A3 (pitch e narrativa técnica)

**Data:** 2026-06-02  
**Participantes:** utilizador + agente  
**Plano:** `plano-consolidado-melhorias-2026-05-24.md` — tarefa A3

## Escopo desta sessão

Criar `docs/lojapp/pitch-portfolio.md` para entrevista e portfólio (pitch 60–90 s + 3 casos risco/evidência).

## Perguntas respondidas

| # | Pergunta | Resposta acordada | Recomendação do revisor |
|---|----------|-------------------|-------------------------|
| 1 | Qual problema de **loja física real** o LojApp resolve? | Sair da **planilha frágil**: entrada de NFe (XML), stock coerente, venda com impacto no saldo, indicadores para compra/preço — **sem ERP pesado**. Uma conta = uma loja. | Alinhar com README “Visão geral”; não vender como ERP completo. |
| 2 | Qual diferencial técnico é **verificável** (não marketing)? | **Testes de integração** com Postgres (Testcontainers): concorrência de stock, isolamento `user_id`; **Flyway + validate** no CI; **ArchUnit**; idempotência em venda/ajuste. | Mostrar nome de classe de teste no GitHub durante entrevista. |

## Casos escolhidos para as 3 histórias

| Caso | Tema | Evidência principal |
|------|------|---------------------|
| A | Concorrência / stock | `SalesConcurrencyIntegrationTest` |
| B | Isolamento multi-loja | `CatalogIsolationIntegrationTest` |
| C | Schema / CI | Flyway + perfil `ci-integration-tests` + `LojappLayerArchitectureTest` |

*(Idempotência ficou na tabela de diferenciais, não como história principal — pode ser 4.ª pergunta se o entrevistador insistir.)*

## Decisões

- [x] Documento em PT-BR (entrevistas locais + README já em PT).  
- [x] Tom: problema de negócio primeiro, stack depois, prova em teste/doc.  
- [x] Não incluir credenciais, URLs privadas nem dados de piloto real.  
- [x] `CHECKLIST_FINAL.md` secção 9.1 — marcar itens P0 de pitch/diferenciais.  
- [ ] Ensaio em voz alta — **utilizador** (fora do agente).

## Aprovado para executar?

- [x] Sim — `pitch-portfolio.md` criado; índice 28 atualizado na mesma PR/commit.

## Próximo passo no plano

**A4** — commits organizados + ZIP seguro (se ainda não fechado após merge da PR de sync).
