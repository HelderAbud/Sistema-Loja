# Decisões curtas e checklist de entrega (LojApp)

Complementa `11-checklist-pr-e-convencoes-repositorio.md` e o roadmap `plano-execucao-sprint-1-a-6.md`. Objetivo: **cada entrega** ter aceite explícito e decisões rastreáveis sem burocracia.

## Registo de decisões (modelo)

Copiar bloco para o final deste ficheiro (ou para notas do sprint) quando houver escolha que importa para o produto ou para operação.

```text
### YYYY-MM-DD — Título curto

- Contexto: (1–2 frases)
- Decisão: A, não B
- Consequências: (migração, risco, follow-up)
```

### Exemplo já aplicável no código

- **Domínio explícito em value objects:** venda (`SaleRegistrationLine`, `SalePendingCancellation`), stock (`ManualStockAdjustment`, `StockLedgerDelta`), NFe (`NfeStockReceiptLine`) — invariantes perto da linguagem de negócio; JPA em `entity` para persistência.

## Checklist de aceite por entrega (mínimo)

Usar antes de considerar um item do roadmap “fechado”.

- [ ] **Objetivo** do item está claro (uma frase).
- [ ] **Comportamento** verificável: teste automatizado ou passos manuais documentados no PR/nota.
- [ ] **Contrato** relevante intacto ou versionado (ver `17-versionamento-api-rest.md` se API pública mudou).
- [ ] **Sem regressão** óbvia: `mvn test` e, se tocou frontend, `npm run test` / lint conforme `AGENTS.md`.
- [ ] **Roadmap** (`plano-execucao-sprint-1-a-6.md`) actualizado se o item fazia parte de um sprint activo.
- [ ] **Decisão** não trivial registada aqui (secção modelo acima), se alguém perguntar “porquê assim?” daqui a três meses.

## Critérios transversais (compromisso de qualidade)

| Critério | O que significa na prática |
|----------|----------------------------|
| Checklist de aceite | Não fechar sprint só com código; fechar com evidência (teste ou verificação explícita). |
| Decisões versionadas | ADR curto neste doc ou referência a issue/PR; evitar “está na cabeça de alguém”. |
| Escopo | Uma entrega = um objetivo; dividir PRs se o diff crescer sem necessidade. |
| Linguagem de domínio | Nomes de classes/métodos alinhados ao negócio (venda, stock, NFe), não só ao framework. |
| Evidência | Comando + resultado ou relatório; não afirmar “passou” sem correr verificação quando o ambiente permite. |

## Ligações

- PR e repositório: `11-checklist-pr-e-convencoes-repositorio.md`
- Plano sequencial: `plano-execucao-sprint-1-a-6.md`

---

### 2026-04-26 — Linha NFe para entrada em stock (`NfeStockReceiptLine`)

- Contexto: `ImportNfeUseCase` aplicava `item.quantity()` directamente em `NfeItem` e em `increaseFromNfe`.
- Decisão: value object `com.lojapp.domain.nfe.NfeStockReceiptLine` com validação e mensagem de domínio; o caso de uso passa a usar esse tipo para quantidade persistida, total de linha e movimento de stock.
- Consequências: `InventoryService.increaseFromNfe` continua a validar via `StockLedgerDelta` (defesa em profundidade); próxima evolução pode ser factorizar custo/unit em outro VO se as regras fiscais crescerem.

### 2026-04-26 — Critérios transversais do roadmap

- Contexto: o plano listava critérios qualitativos sem processo nem sítio único.
- Decisão: este documento (`18`) concentra checklist de aceite por entrega, tabela de compromissos e modelo de ADR curto; o roadmap referencia-o explicitamente.
- Consequências: novas decisões “porquê assim?” devem ganhar uma entrada datada aqui (ou link para PR) em vez de ficar só no chat.
