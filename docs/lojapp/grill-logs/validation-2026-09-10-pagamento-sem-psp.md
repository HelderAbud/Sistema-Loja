# Grill — Pagamento PDV sem PSP (2026-09-10)

**Trilha:** Normal (contrato)  
**Fatia:** ADR 0002 + vocabulário  
**Pedido:** manter pagamentos como lançamento; cada lojista escolhe o método; sem venda online / gateway.

## Fechado

- Sem Mercado Pago, QR, webhook, intent ou TEF no MVP.
- PDV actual (métodos, split, HITL, relatórios) permanece.
- `SaleRequest` inalterado.

## Evidência

- `docs/adr/0002-pagamento-pdv-lancamento-sem-psp.md` (Accepted)
- `docs/CONTEXT.md` — termos **Pagamento (PDV)** e **HITL de pagamento**
- `docs/README.md` — índice das ADRs

## Não verificado

- Deploy Vercel/Railway e login da demo (fora desta fatia).
- Código de pagamentos (sem diff de comportamento).

## Residual

- Reabrir PSP só com ADR nova e pedido explícito.
