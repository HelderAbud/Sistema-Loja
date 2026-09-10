# ADR 0002 — Pagamento no PDV é lançamento, sem PSP

**Status:** Accepted  
**Data:** 2026-09-10

## Contexto

O PDV já regista métodos de pagamento (`CASH`, `PIX`, `CARD`, cartão crédito/débito, transferência, boleto, `OTHER`), split, estado `CONFIRMED` / `PENDING` com confirmação HITL, metadados (bandeira, parcelas, e2e) e resumo para relatórios. O stock baixa em `POST /pos/sales/finalize` (e na Venda Rápida).

Foi explorado um gateway PIX (Mercado Pago: QR, polling, webhook, intent antes da venda). O produto **não** vende online: cada lojista cobra o cliente **fora** do LojApp (maquininha, PIX da conta, dinheiro). O sistema existe para **baixa de estoque e controlo** (caixa, pendentes, relatórios).

## Decisão

- **Não** integrar PSP/adquirente no MVP (Mercado Pago, Stone, Cielo, TEF, pinpad, QR gerado pelo LojApp, webhook de pagamento, checkout online).
- Pagamento no PDV é **lançamento operacional**: o operador escolhe o método que a loja já usa e informa o que precisa para o controlo (valor, pendente vs confirmado, e2e/NSU à mão se quiser).
- Manter o contrato actual de finalize, confirmação HITL e relatórios. **Não** alterar `SaleRequest` / Venda Rápida nesta decisão.
- Reabrir PSP só com ADR nova e pedido explícito.

## Consequências

- Fatia de gateway PIX/cartão **cancelada**; não criar tabela de intent nem rotas `/pix/*`.
- Vitrine pública continua fora do funil de venda (já redireccionada); não é canal de checkout.
- Lojista continua responsável pelo dinheiro no mundo real; o LojApp não confirma pagamento junto ao banco.

## Verificação

- Documental: este ADR + vocabulário em `docs/CONTEXT.md`.
- Código: nenhuma mudança de comportamento nesta fatia.
