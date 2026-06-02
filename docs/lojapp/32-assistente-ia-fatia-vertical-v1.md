# Assistente / agentes — fatia vertical v1 (checklist item 1)

**Status:** documentado para execução em ondas.  
**Data de referência:** 2026-04-30.

## Fatia escolhida (v1) — opção **C**

**Nome:** Copiloto operacional — leitura + **escrita via API** com dry-run e gate humano.

**Objetivo:** O utilizador autenticado consulta dados reais (produtos, stock, indicadores) e pode concluir **fluxos que mutam estado** alinhados ao domínio atual do LojApp — em particular **registo de venda** e **movimentação de stock** — **sempre** com: (1) plano e **corpos HTTP propostos visíveis** antes de enviar; (2) **aprovação explícita** humana por passo ou por lote acordado; (3) execução **apenas** através dos endpoints existentes (contratos Swagger/DTOs), sem regras de negócio “só no prompt”.

## Critério de “feito” (v1)

- Fluxo reprodutível com JWT válido e isolamento por `user_id`: leituras + mutações **só** após dry-run e confirmação documentada (roteiro, checklist ou ferramenta com passo “aprovar”).
- Toda mutação tem **identificador de corrida** (ex.: correlation id) para auditoria e suporte a retry seguro onde a API for idempotente.
- Respostas de erro da API (4xx/5xx) são tratadas como fim de passo ou retentativa explícita — sem “inventar” sucesso.
- **Fiscal / NFe:** só entra nesta fatia se estiver coberto por endpoints estáveis e **gate humano** adicional conforme política do projeto; caso contrário mantém-se consulta ou sugestão até decisão explícita.

## Fora de escopo (v1)

- Mutação **sem** visualização do body e **sem** aprovação humana.
- Lógica fiscal ou de stock **substituindo** validação do backend.
- Novo módulo Spring **não** é obrigatório para fechar v1 — automação externa credenciada contra a API é aceitável; segredos e quotas ficam fora do cliente público.

## Próxima onda (não é v1)

- **v2 sugerida:** endurecimento operacional (métricas, limites de custo LLM, filas, políticas por papel) e/ou ampliação controlada a outros casos de uso (ex.: NFe) com o mesmo padrão dry-run + gate.

## Checklist mestre (referência)

Este ficheiro fecha o item **“Fatia vertical escolhida e documentada”**. Os itens seguintes seguem na sequência acordada (endpoints, segurança, inventário, ator, etc.).
